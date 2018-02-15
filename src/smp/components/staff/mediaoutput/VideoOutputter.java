package smp.components.staff.mediaoutput;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import smp.components.Values;
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

	private HBox theStaffPlayBars;
	
	WritableImage sceneImageA;
	WritableImage sceneImageB;

	private final CompletionService<Boolean> executorService;
	
	/**
	 * FrameProcessor is a worker that outputs image of sceneImageA rendered
	 * with one playbar at its respective position from sceneImageB with
	 * Objectplanet's PngEncoder (significantly faster than ImageIO.write()).
	 */
	class FrameProcessor implements Callable<Boolean> {// extends Task<Boolean> {

		int id;
		PngEncoder encoder;
		int x,y,w,h;
		
		public FrameProcessor(int id) {
			this.id = id;
			encoder = new PngEncoder();
			
			Node staffPlayBar = theStaffPlayBars.getChildren().get(id);
			Bounds staffPlayBarBounds = staffPlayBar.localToScene(staffPlayBar.getBoundsInLocal());
			x = (int) staffPlayBarBounds.getMinX();
			y = (int) staffPlayBarBounds.getMinY();
			w = (int) staffPlayBarBounds.getWidth();
			h = (int) staffPlayBarBounds.getHeight();
		}
		
		@Override
		public Boolean call() throws Exception {
			
			//overwrite pixels at id position to include playbar
			BufferedImage sceneImageACopy = SwingFXUtils.fromFXImage(sceneImageA, null);
			BufferedImage sceneImageBCopy = SwingFXUtils.fromFXImage(sceneImageB, null);
			//we want to directly access the int buffer for speed
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
			
			FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + id + ".png");
			encoder.encode(sceneImageACopy, fout);
			fout.close();
			return true;
		}
	}
	
	List<FrameProcessor> frameProcessors;
	
	public VideoOutputter(HBox staffPlayBars) {
		theStaffPlayBars = staffPlayBars;
		
		final ExecutorService pool = Executors.newFixedThreadPool(Values.NOTELINES_IN_THE_WINDOW);
		executorService = new ExecutorCompletionService<Boolean>(pool);
		
		frameProcessors = new ArrayList<>();
		for (int i = 0; i < Values.NOTELINES_IN_THE_WINDOW; i++)
			frameProcessors.add(new FrameProcessor(i));
	}
	
	public void processOutput() {

		long start = System.currentTimeMillis();
		/** 1) Snapshot(A) the scene. */
		sceneImageA = theStaffPlayBars.getScene().snapshot(null);
		
		/** 2) Make all playbars visible. */
		for(Node n : theStaffPlayBars.getChildren())
			n.setVisible(true);
		
		/** 3) Snapshot(B) the scene with all playbars visible. */
		sceneImageB = theStaffPlayBars.getScene().snapshot(null);
		
		/** 		
		 * 	4) Use 10 workers in parallel. For each worker 1..10, 
		 * 		   >Calculate worker's frame (f1) from (f)
		 * 		   if line is on a frame (f1)
		 * 				redraw (A) but on their respective staffline 
		 * 				(1..10) draw playbar pixels from (B) at their positions.
		 */
		for(FrameProcessor fp : frameProcessors)
			executorService.submit(fp);
		
		try {
			for(FrameProcessor fp : frameProcessors)
				executorService.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis()- start);
	}
	
	//simple version which is 2x slower
	public void processOutput2() {

		PngEncoder encoder = new PngEncoder();
		long start = System.currentTimeMillis();
		
		/** 2) Make all playbars visible. */
		for(int i = 0; i < theStaffPlayBars.getChildren().size(); i++) {
			Node n =  theStaffPlayBars.getChildren().get(i);
			n.setVisible(true);
			WritableImage sceneTest = theStaffPlayBars.getScene().snapshot(null);
			try {
				FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + i + ".png");
				encoder.encode(SwingFXUtils.fromFXImage(sceneTest, null), fout);
				fout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			n.setVisible(false);
		}
		
		System.out.println(System.currentTimeMillis()- start);
	}
	
	public void testFFMPEG() {
		long timeStart = System.currentTimeMillis();
		try {
			Runtime.getRuntime().exec("ffmpeg -r 30 -f image2 -s 1920x1080 -i ./tmp/aa_trash_test_%d.png -vcodec libx264 -crf 0 -pix_fmt yuv420p test" + System.currentTimeMillis() + ".mp4");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - timeStart);
		System.out.println("FFMPEG");
	}
}
