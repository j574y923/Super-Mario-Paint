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
	
	private Staff theStaff;
	private HBox theStaffPlayBars;
	
	private WritableImage sceneImageA;
	private WritableImage sceneImageB;

	private final CompletionService<Boolean> executorService;
	
	private List<FrameProcessor> frameProcessors = new ArrayList<>();

	/**
	 * Save time. Capture B when all playbars are visible, capture A when they
	 * aren't.
	 */
	private boolean visibleState;

	/**
	 * FrameProcessors place processed frames at their respective indices. Next
	 * they are enqueued to ffmpegInput.
	 */
	private BufferedImage[] processedFrames = new BufferedImage[Values.NOTELINES_IN_THE_WINDOW];

	private Queue<BufferedImage> queuedFrames = new LinkedList<>();
	
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
		int lastLine = findLastLine();
		for (int i = 0; i <= lastLine; i += Values.NOTELINES_IN_THE_WINDOW) {
			theStaff.setLocation(i);
			processWindow(Math.min(lastLine - i + 1, Values.NOTELINES_IN_THE_WINDOW));
		}
	}
	
	/**
	 * output up to 10 images (1 for each line in the window)
	 * 
	 * @param numLines
	 *            process frames for the first of this number of lines in the
	 *            window
	 */
	public void processWindow(int numLines) {
		long timeStart = System.currentTimeMillis();

		/** 1) Snapshot(A) the scene. */
		if (visibleState)
			sceneImageB = theStaffPlayBars.getScene().snapshot(null);
		else
			sceneImageA = theStaffPlayBars.getScene().snapshot(null);

		/** 2) Make all playbars visible. */
		visibleState = !visibleState;
		for (Node n : theStaffPlayBars.getChildren())
			n.setVisible(visibleState);

		/** 3) Snapshot(B) the scene with all playbars visible. */
		if (visibleState)
			sceneImageB = theStaffPlayBars.getScene().snapshot(null);
		else
			sceneImageA = theStaffPlayBars.getScene().snapshot(null);

		/** 		
		 * 	4) Use 10 workers in parallel. For each worker 1..10, 
		 * 		   >Calculate worker's frame (f1) from (f)
		 * 		   if line is on a frame (f1)
		 * 				redraw (A) but on their respective staffline 
		 * 				(1..10) draw playbar pixels from (B) at their positions.
		 */
		for(int i = 0; i < numLines; i++)
			executorService.submit(frameProcessors.get(i));
		
		try {
			for(int i = 0; i < numLines; i++)
				executorService.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - timeStart);

		/** enqueue processedFrames to pipe into ffmpegInput */
		for (int i = 0; i < numLines; i++)
			if (processedFrames[i] != null) {
				queuedFrames.add(processedFrames[i]);
				processedFrames[i] = null;
			}
	}
	
	public void testFFMPEG() {
		long timeStart = System.currentTimeMillis();
		try {
			Runtime.getRuntime().exec("ffmpeg -r 30 -f image2 -s 1920x1080 -i ./tmp/aa_trash_test_%d.png -vcodec libx264 -crf 0 -pix_fmt yuv420p test" + System.currentTimeMillis() + ".mp4");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - timeStart);
		System.out.println("FFMPEG");
	}
	
	public void testFFMPEGyolo() {
		try {
			File ffmpeg_output_msg = new File("ffmpeg_output_msg.txt");
			ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-framerate", "618/60", "-i", "pipe:0", "-r", "60", "out1.mp4");
			pb.redirectErrorStream(true);
			pb.redirectOutput(ffmpeg_output_msg);
			pb.redirectInput(ProcessBuilder.Redirect.PIPE);
			Process p = pb.start(); 
			OutputStream ffmpegInput = p.getOutputStream();

			// PngEncoder.INDEXED_COLORS_ORIGINAL seems to look the best
			// PngEncoder.BEST_SPEED speeds it up by an additional ~1.3x
			PngEncoder encoderFast = new PngEncoder(PngEncoder.INDEXED_COLORS_ORIGINAL, PngEncoder.BEST_SPEED);
			System.out.println("BEGIN FFMPEG");
			long timeStart = System.currentTimeMillis();
			while (!queuedFrames.isEmpty()) {

				BufferedImage img = queuedFrames.poll();
				
				//when i did this with 384 images, pngencoder finished in 6s and imageio finished in 43s
				encoderFast.encode(img, ffmpegInput);//ImageIO.write(img, "PNG", ffmpegInput);//
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

	/**
	 * Finds the last line in the sequence that we are playing.
	 * 
	 * This is taken from Staff and modified to get the exact last line that is
	 * played.
	 */
	private int findLastLine() {
		ArrayList<StaffNoteLine> lines = theStaff.getSequence().getTheLines();
		for (int i = lines.size() - 1; i >= 0; i--)
			if (!lines.get(i).isEmpty()) {
				// the 0 case
				if(i == 0)
					return 3;
				return (int) (Math.ceil(i / 4.0) * 4) - 1;
			}
		return -1;
    }
}
