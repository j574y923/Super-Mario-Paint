package smp.components.staff.mediaoutput;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import smp.fx.SMPFXController;

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

	SMPFXController controller;
	Scene scene;
	Node n;
	
//	public VideoOutputter(SMPFXController controller) {
//		test();
//		this.controller = controller;
//	}
	
	public VideoOutputter(Scene scene) {
		// TODO Auto-generated constructor stub
		System.out.println(scene);
		this.scene = scene;
//		test();
		testWithTwoThreadsParallel();
//		test2();
//		testFFMPEG();
//		test1MultiThreaded();
	}
	
	public VideoOutputter(Node n) {
		this.n = n;
		test1();
//		test2();
//		testFFMPEG();
//		test1MultiThreaded();
	}

	/**
	 * test to use javafx's scene snasphot. 30x ~ 3s
	 */
	public void test() {
//		System.out.println(controller);
//		System.out.println(scene);
//		Scene scene = controller.getAddButton().getScene();//.snapshot(writableImage);

		PngEncoder encoder = new PngEncoder();;
		long timeStart = System.currentTimeMillis();
		
		for(int i = 0; i < 120; i++) {
		    WritableImage image = scene.snapshot(null);
//		    File file = new File("./tmp/aa_trash_test_" + i + ".jpg");
		    try {
//		    	BufferedOutputStream imageOutputStream = new BufferedOutputStream(new FileOutputStream(file));
//		        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
//		    	imageOutputStream.close();
		         FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + i + ".png");
		         encoder.encode(SwingFXUtils.fromFXImage(image, null), fout);
		         fout.close();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		System.out.println(System.currentTimeMillis() - timeStart);
//		System.exit(0);
	}
	
	/**
	 * test to use javafx's scene snasphot. WITH PLANETOBJECT'S PNGENCODER. WITH THREE THREADS. 30x ~ 0.3s
	 */
	public void testWithTwoThreadsParallel() {

		final PngEncoder encoder = new PngEncoder();
		final PngEncoder encoder2 = new PngEncoder();
		PngEncoder encoder3 = new PngEncoder();
		
		Task task = new Task<Void>() {
			@Override
			public Void call() {
				for (int i = 0; i < 40; i++) {
					WritableImage image = scene.snapshot(null);
					try {
						FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + i + ".png");
						encoder.encode(SwingFXUtils.fromFXImage(image, null), fout);
						fout.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				return null;
			}
		};
		Task task2 = new Task<Void>() {
			@Override
			public Void call() {
				for (int i = 41; i < 80; i++) {
					WritableImage image = scene.snapshot(null);
					try {
						FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + i + ".png");
						encoder2.encode(SwingFXUtils.fromFXImage(image, null), fout);
						fout.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				return null;
			}
		};
		long timeStart = System.currentTimeMillis();
		new Thread(task).start();
		new Thread(task2).start();
		
		for (int i = 81; i < 120; i++) {
			WritableImage image = scene.snapshot(null);
			try {
				FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + i + ".png");
				encoder3.encode(SwingFXUtils.fromFXImage(image, null), fout);
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(System.currentTimeMillis() - timeStart);
//		System.exit(0);
	}
	
	/**
	 * test to use javafx's node snasphot. 30x (on one staffline) ~ 0.5s
	 */
	public void test1() {
//		System.out.println(controller);
//		System.out.println(scene);
//		Scene scene = controller.getAddButton().getScene();//.snapshot(writableImage);

		long timeStart = System.currentTimeMillis();

		try {
			for (int i = 0; i < 120; i++) {
				WritableImage image = n.snapshot(null, null);// scene.snapshot(null);
				File file = new File("./tmp/aa_trash_test_" + i + ".png");
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - timeStart);
//		System.exit(0);
	}
	
	/**
	 * test to use javafx's node snasphot WITH 6 THREADS ON A SCHEDULE. 30x (on one staffline) ~ 1.5s
	 */
	boolean finished = false;
	long timeStart;
	public void test1MultiThreaded() {
//		System.out.println(controller);
//		System.out.println(scene);
//		Scene scene = controller.getAddButton().getScene();//.snapshot(writableImage);

		final int THREADS = 3;
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(THREADS);
		
		final ConcurrentLinkedQueue <WritableImage> sharedStore  = new ConcurrentLinkedQueue <>();
		
	     
		
		class MyClass implements Runnable {
			int i;
			PngEncoder encoder;
			public MyClass(int i) {
				this.i = i;
				encoder = new PngEncoder();
			}
			
		    @Override
		    public void run() {
//				for (int j = 0; j < 20; j++) {
//					WritableImage image = n.snapshot(null, null);// scene.snapshot(null);
//					File file = new File("./tmp/aa_trash_test_" + (i * j) + ".png");
//					try {
//						ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
				

//				System.out.println("WOT" + i);
				
				if(!sharedStore.isEmpty()) {
					WritableImage image = sharedStore.poll();
//					File file = new File("./tmp/aa_trash_test_" + i + ".png");
					try {
//						ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
				         FileOutputStream fout = new FileOutputStream("./tmp/aa_trash_test_" + i + ".png");
				         encoder.encode(SwingFXUtils.fromFXImage(image, null), fout);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					i+=THREADS;
				}
				
				if (finished && sharedStore.isEmpty()) {
					System.out.println(System.currentTimeMillis() - timeStart);
					scheduler.shutdown();
				}
		    }
		}
		
		for (int i = 1; i <= THREADS; i++) {
		    scheduler.scheduleAtFixedRate(
		             new MyClass(i),
		             1*i,
		             1,
		             TimeUnit.MILLISECONDS
		             );
		}

		timeStart = System.currentTimeMillis();
		
		for (int i = 0; i < 120; i++) {
			WritableImage image = scene.snapshot(null);//n.snapshot(null, null);
			// File file = new File("./tmp/aa_trash_test_" + i + ".png");
			// ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
			sharedStore.add(image);
		}

		System.out.println(System.currentTimeMillis() - timeStart);
		finished = true;
		
//		System.exit(0);
	}
	
	/**
	 * test to use java awt's robot screencapture. 30x ~ 3s
	 */
	public void test2() {
		Robot r = null;
		try {
			r = new Robot();
		} catch (AWTException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		long timeStart = System.currentTimeMillis();
		try {
			for (int i = 0; i < 120; i++) {
				BufferedImage screencapture = r.createScreenCapture(new Rectangle(0, 0, 800, 600));

				// Save as PNG
				File file = new File("./tmp/aa_trash_test_" + i + ".png");

				ImageIO.write(screencapture, "png", file);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - timeStart);
		System.out.println("ROBOT");
//		System.exit(0);
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
