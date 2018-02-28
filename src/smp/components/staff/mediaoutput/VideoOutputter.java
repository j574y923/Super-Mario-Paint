package smp.components.staff.mediaoutput;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import smp.components.Values;
import smp.components.staff.Staff;
import smp.components.staff.sequences.StaffNoteLine;
import smp.stateMachine.StateMachine;

import com.objectplanet.image.PngEncoder;

/**
 * How recording will work:
 * 		>) Calculate frame number (f).
 * 		1) Snapshot(A) the scene. 
 * 		2) Make all playbars visible.
 * 		3) Snapshot(B) the scene with all playbars visible.
 * 		4) Use 10 workers in parallel. For each worker 1..10, 
 * 		   >Calculate worker's frame (f1) from (f)
 * 		   if line is on a frame (f1)
 * 				redraw (A) but on their respective staffline 
 * 				(1..10) draw playbar pixels from (B) at their positions.
 * 		5) Each worker outputs image with Objectplanet's PngEncoder 
 * 		   (significantly faster than ImageIO.write()).
 * 		6) Repeat 1-5 until all lines are processed.
 * 		7) Pass the images into FFMPEG to create the video.
 * 		8) Delete images.
 * @author J
 *
 */
public class VideoOutputter {

	/**
	 * FrameProcessor is a worker that outputs image of sceneImageA rendered
	 * with one playbar at its respective position from sceneImageB with
	 * Objectplanet's PngEncoder (significantly faster than ImageIO.write()).
	 */
	class FrameProcessor implements Callable<Boolean> {

		int id;
		int x,y,w,h;
		
		public FrameProcessor(int id) {
			this.id = id;

			Node staffPlayBar = theStaffPlayBars.getChildren().get(id);
			Bounds staffPlayBarBounds = staffPlayBar.localToScene(staffPlayBar.getBoundsInLocal());
			x = (int) staffPlayBarBounds.getMinX();
			y = (int) staffPlayBarBounds.getMinY();
			w = (int) staffPlayBarBounds.getWidth();
			h = (int) staffPlayBarBounds.getHeight();
		}
		
		@Override
		public Boolean call() throws Exception {

			// overwrite pixels at id position to include playbar
			BufferedImage sceneImageACopy = SwingFXUtils.fromFXImage(sceneImageA, null);
			BufferedImage sceneImageBCopy = SwingFXUtils.fromFXImage(sceneImageB, null);
			// we want to directly access the int buffer for speed
			int[] sceneImageACopyDBI = ((DataBufferInt) sceneImageACopy.getRaster().getDataBuffer()).getData();
			int[] sceneImageBCopyDBI = ((DataBufferInt) sceneImageBCopy.getRaster().getDataBuffer()).getData();

			for (int y0 = y; y0 < y + h; y0++) {
				for (int x0 = x; x0 < x + w; x0++) {
					// int rgbB = sceneImageBCopy.getRGB(x0, y0);
					// sceneImageACopy.setRGB(x0, y0, rgbB);
					int pixel = x0 + y0 * sceneImageACopy.getWidth();
					int rgbB = sceneImageBCopyDBI[pixel];
					sceneImageACopyDBI[pixel] = rgbB;
				}
			}
			
			processedFrames[id] = sceneImageACopy;
			return true;
		}
		
		public int getId() {
			return id;
		}
	}
	
	class FFMPEGRunnable implements Runnable {

		// PngEncoder.INDEXED_COLORS_ORIGINAL seems to look the best
		// PngEncoder.BEST_SPEED speeds it up by an additional ~1.3x
		PngEncoder encoder = new PngEncoder(PngEncoder.INDEXED_COLORS_ORIGINAL, PngEncoder.BEST_SPEED);
		
		/** Controls whether to run or stop. */
		boolean run;

		/** Controls whether to pause and skip behavior. */
		boolean pause;

		/** Signals to finish processing images and stop. */
		boolean finish;

		/** Counter for every next video processed. */
		int increment = 0;
		
		/**
		 * Sets run to true.
		 */
		@Override
		public void run() {

			run = true;
			
			String framerate = (int) StateMachine.getTempo() + "/60";
			String outputFile = theStaff.getArrangementName() + "_" + increment + ".mp4";
			
			File ffmpegOutputMsg = new File(theStaff.getArrangementName() + "_" + increment + "_err.txt");
			ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-framerate", framerate, "-i",
					"pipe:0", "-r", "60", outputFile);
			pb.redirectErrorStream(true);
			pb.redirectOutput(ffmpegOutputMsg);
			pb.redirectInput(ProcessBuilder.Redirect.PIPE);
			Process p = null;
			try {
				p = pb.start();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			OutputStream ffmpegInput = p.getOutputStream();

			long timeStart = System.currentTimeMillis();
			while (run) {

				if (finish && queuedFrames.isEmpty())
					break;

				if (pause) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}

				if (!queuedFrames.isEmpty()) {
					BufferedImage img = queuedFrames.poll();

					try {
						encoder.encode(img, ffmpegInput);
					} catch (IOException e) {
						e.printStackTrace();
						run = false;
					} 
				} else {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			} // end while

			try {
				ffmpegInput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			run = false;
			pause = false;
			finish = false;
			increment++;

			System.out.print(System.currentTimeMillis() - timeStart);
			System.out.println("FFMPEGHUH?");

		}
	}
	
	private Staff theStaff;
	private HBox theStaffPlayBars;
	
	private WritableImage sceneImageA;
	private WritableImage sceneImageB;

	private final CompletionService<Boolean> executorService;
	
	private List<FrameProcessor> frameProcessors = new ArrayList<>();

	/**
	 * This will track the visible state of all playbars. The idea is capture B
	 * when all playbars are visible, capture A when they aren't.
	 */
	private boolean visibleState;

	/**
	 * FrameProcessors place processed frames at their respective indices. Next
	 * they are enqueued to ffmpegInput.
	 */
	private BufferedImage[] processedFrames = new BufferedImage[Values.NOTELINES_IN_THE_WINDOW];

	private Queue<BufferedImage> queuedFrames = new LinkedList<>();
	
	private FFMPEGRunnable ffmpegRunnable = new FFMPEGRunnable();
	
	private Thread ffmpegThread;
	
	/**
	 * Pass in staffPlayBars and capture them as they change. it's the only
	 * thing that changes as you play an arrangement.
	 * 
	 * @param staffPlayBars
	 */
	public VideoOutputter(Staff staff, HBox staffPlayBars) {
		theStaff = staff;
		theStaffPlayBars = staffPlayBars;
		
		final ExecutorService pool = Executors.newFixedThreadPool(Values.NOTELINES_IN_THE_WINDOW);
		executorService = new ExecutorCompletionService<Boolean>(pool);
		
		for (int i = 0; i < Values.NOTELINES_IN_THE_WINDOW; i++)
			frameProcessors.add(new FrameProcessor(i));
	}

	public void processOutput() {
		// TODO: first staging the window (play button pressed, arrangement
		// mode, first song selected for arrangement and at beginning, scrollbar
		// focused, volText cleared, filter icons cleared)
		processArrangement();
	}
	
	/**
	 * Processes frames for all the songs in the arrangement loaded in the
	 * staff.
	 */
	public void processArrangement() {
		int size = theStaff.getArrangement().getTheSequences().size();
		for (int i = 0; i < size; i++) {
			theStaff.selectSong(i);
			processSong();
		}
	}

	/**
	 * Processes frames for the song loaded in the staff. Tells FFMPEG to
	 * convert those frames into a video. This function will not return until
	 * this is completed or the FFMPEGThread is interrupted.
	 */
	public void processSong() {
		runFFMPEGThread();
		int lastLine = theStaff.getLastLine();
		int stopLine = (int) (Math.ceil((lastLine + 1) / 4.0) * 4);
		for (int i = 0; i < stopLine; i += Values.NOTELINES_IN_THE_WINDOW) {
			theStaff.setLocation(i);
			int numLines = Math.min(stopLine - i, Values.NOTELINES_IN_THE_WINDOW);
			processWindow(numLines);
		}
		finishFFMPEGThread();
	}

	/**
	 * Processes frames for the first numLines in the window adding up to 10
	 * images (1 for each line in the window) to queuedFrames.
	 * 
	 * @param numLines
	 *            The first number of lines to process frames for
	 */
	private void processWindow(int numLines) {
		long timeStart = System.currentTimeMillis();

		// 1) Snapshot(A) the scene.
		if (visibleState)
			sceneImageB = theStaffPlayBars.getScene().snapshot(null);
		else
			sceneImageA = theStaffPlayBars.getScene().snapshot(null);

		// 2) Make all playbars visible.
		visibleState = !visibleState;
		for (Node n : theStaffPlayBars.getChildren())
			n.setVisible(visibleState);

		// 3) Snapshot(B) the scene with all playbars visible.
		if (visibleState)
			sceneImageB = theStaffPlayBars.getScene().snapshot(null);
		else
			sceneImageA = theStaffPlayBars.getScene().snapshot(null);

		//	4) Use 10 workers in parallel. For each worker 1..10, 
		//			redraw (A) but on their respective staffline 
		//			(1..10) draw playbar pixels from (B) at their positions.
		for(int i = 0; i < numLines; i++)
			executorService.submit(frameProcessors.get(i));
		
		try {
			for(int i = 0; i < numLines; i++)
				executorService.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - timeStart);

		// enqueue processedFrames to pipe into ffmpegInput
		for (int i = 0; i < numLines; i++)
			if (processedFrames[i] != null) {
				queuedFrames.add(processedFrames[i]);
				processedFrames[i] = null;
			}
	}

	/**
	 * Tells FFMPEGThread to start if it hasn't already and un-pause if it is
	 * paused.
	 */
	public synchronized void runFFMPEGThread() {
		if (!ffmpegRunnable.run) {
			ffmpegThread = new Thread(ffmpegRunnable);
			ffmpegThread.start();
			ffmpegThread.setPriority(Thread.MAX_PRIORITY);
		}
		
		ffmpegRunnable.pause = false;
	}
	
	/**
	 * Tells FFMPEGThread to do nothing until pause is set to false.
	 */
	public synchronized void pauseFFMPEGThread() {
		if (ffmpegRunnable.run)
			ffmpegRunnable.pause = true;
	}
	
	/**
	 * Tells FFMPEGThread to not run for another cycle, effectively ending the
	 * thread.
	 */
	public synchronized void stopFFMPEGThread() {
		if (ffmpegRunnable.run)
			ffmpegRunnable.run = false;
	}
	
	/**
	 * Tells FFMPEGThread to finish processing the rest of the images on
	 * queuedFrames. The thread will eventually stop. This function will not
	 * return until this is completed or the FFMPEGThread is interrupted.
	 * 
	 * These FFMPEGThread functions should probably use semaphores in the
	 * future.
	 */
	public synchronized void finishFFMPEGThread() {
		if (ffmpegRunnable.run) {
			ffmpegRunnable.finish = true;
			
			try {
				ffmpegThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ffmpegThread = null;
		}
	}
	
	//TODO: 
	//-[DONE]create a video.mp4 out of the first song (or until the tempo changes for later songs)
	//-[DONE]create video_%d.mp4 for each sequence of songs with the same tempos
	//-concat all video_%d.mp4 to create desired end result
	public void testFFMPEGyolo() {
		try {
			File ffmpeg_output_msg = new File("ffmpeg_output_msg.txt");
			ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-framerate", "618/60", "-i", "pipe:0", "-r", "60", "-preset", "ultrafast", "out1.mp4");
			pb.redirectErrorStream(true);
			pb.redirectOutput(ffmpeg_output_msg);
			pb.redirectInput(ProcessBuilder.Redirect.PIPE);
			Process p = pb.start(); 
			OutputStream ffmpegInput = p.getOutputStream();

			// PngEncoder.INDEXED_COLORS_ORIGINAL seems to look the best
			// PngEncoder.BEST_SPEED speeds it up by an additional ~1.3x
			PngEncoder encoderFast = new PngEncoder();//PngEncoder.INDEXED_COLORS_ORIGINAL, PngEncoder.BEST_SPEED);
			System.out.println("BEGIN FFMPEG");    
			long timeStart = System.currentTimeMillis();
			int id = 0;
			while (!queuedFrames.isEmpty()) {

				BufferedImage img = queuedFrames.poll();
				
				//when I did this with 384 images, pngencoder finished in 6s and imageio finished in 43s
				encoderFast.encode(img, ffmpegInput);//ImageIO.write(img, "PNG", ffmpegInput);//
//				FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + id + ".png");
//				encoderFast.encode(img, fout);
//				fout.close();
//				id++;

			}
			ffmpegInput.close();
			System.out.println(System.currentTimeMillis() - timeStart);
			System.out.println("FFMPEGHUH?");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * the playbar is at <code>FrameProcessor</code>'s position, we go through a
	 * list of conditions, and determine should we output an image?
	 */
	private boolean isValidFrame(FrameProcessor fp) {

		return false;
	}
}
