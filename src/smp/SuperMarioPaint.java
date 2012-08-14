package smp;

import smp.components.MPWindow;

/**
 * Super Mario Paint
 * Based on the old SNES game from 1992, Mario Paint
 * Inspired by:<br>
 * Robby Mulvany's Mario Paint Composer 1.0 / 2.0 (2007-2008) <br>
 * FordPrefect's Advanced Mario Sequencer (2009) <br>
 * MarioSequencer (2002) <br>
 * TrioSequencer <br>
 * @since 2012.08.07
 * @author RehdBlob
 * @version 1.00
 */
public class SuperMarioPaint {

	/**
	 * Initializes an ImageLoader object and a 
	 * SoundfontLoader as Threads. These will initialize
	 * the necessary resources in memory such that they
	 * will be much easier to access later on. Hopefully
	 * most people have >30MB of RAM.
	 * @param args As per a main class.
	 */
	public static void main(String[] args) {
		Thread imgLd = new Thread(new ImageLoader());
		Thread sfLd = new Thread(new SoundfontLoader());
		imgLd.start();
		sfLd.start();
		while(imgLd.isAlive() || sfLd.isAlive())
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		MPWindow mp = new MPWindow();
		mp.setVisible(true);
	}

}
