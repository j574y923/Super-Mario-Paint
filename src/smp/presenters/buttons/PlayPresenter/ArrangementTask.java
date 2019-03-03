package smp.presenters.buttons.PlayPresenter;

import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.text.ParseException;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import smp.models.staff.StaffArrangement;
import smp.models.staff.StaffSequence;
import smp.models.stateMachine.StateMachine;
import smp.models.stateMachine.Variables;
import smp.presenters.api.load.Utilities;
import smp.presenters.api.load.MPCDecoder;

/**
 * This class runs an arrangement instead of just a song.
 */
class ArrangementTask extends AnimationTask {

	private ObjectProperty<StaffArrangement> theArrangement;
	private DoubleProperty measureLineNum;
	private ObjectProperty<StaffSequence> theSequence;
	private IntegerProperty arrangementListSelectedIndex;

    /** Whether we are playing an arrangement. */
    private boolean arrPlaying = false;
    
	public ArrangementTask() {
		this.theArrangement = Variables.theArrangement;
		this.measureLineNum = StateMachine.getMeasureLineNum();
		this.theSequence = Variables.theSequence;
		this.arrangementListSelectedIndex = Variables.arrangementListSelectedIndex;
	}
	
    @Override
    protected Void call() throws Exception {
    	startArrangement();
        ObservableList<StaffSequence> seq = theArrangement.get().getTheSequences();
        ObservableList<File> files = theArrangement.get().getTheSequenceFiles();
        for (int i = 0; i < seq.size(); i++) {
            while (queue > 0)
                ;
            /* Force emptying of queue before changing songs. */
            queue++;
            setSoundset(seq.get(i).getSoundset().get());
            index = 0;
            advance = false;
            queue++;
            highlightSong(i);
//            theSequenceFile = files.get(i);//TODO: add theSequenceFile logic for arrangementListPresenter
            lastLine = findLastLine();
            songPlaying = true;
            setTempo(theSequence.get().getTempo().get());
            int counter = 0;
            measureLineNum.set(0);
            queue++;
            zeroEverything();
            while (queue > 0)
                ;
            /* Force operations to complete before starting a song. */
            while (songPlaying && arrPlaying) {
                queue++;
                playNextLine();
                counter++;
                if (counter > lastLine && counter % 4 == 0) {
                    songPlaying = false;
                }
                try {
                    Thread.sleep(delayMillis, delayNanos);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
            if (!arrPlaying)
                break;
        }
        hitStop();
        return null;
    }
    
    
    /**
	 * Sets the soundset.
	 *
	 * @param soundset
	 *            The soundset.
	 * @since v1.1.2
	 */
	private void setSoundset(final String soundset) {
		//TODO:
//		Platform.runLater(new Runnable() {
//
//			@Override
//			public void run() {
//				if (!controller.getSoundfontLoader().loadFromCache(soundset)) {
//					try {
//						controller.getSoundfontLoader().loadFromAppData(soundset);
//					} catch (InvalidMidiDataException | IOException | MidiUnavailableException e) {
//						e.printStackTrace();
//					}
//					controller.getSoundfontLoader().storeInCache();
//				}
				queue--;
//			}
//		});
	}

    /**
     * Highlights the currently-playing song in the arranger list.
     *
     * @param i
     *            The index to highlight.
     */
    private void highlightSong(final int i) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	arrangementListSelectedIndex.set(i);
                queue--;
            }

        });
    }
    

    /** Starts an arrangement. */
    public synchronized void startArrangement() {
//        soundPlayer.setRun(true);//TODO
        ObservableList<StaffSequence> seq = theArrangement.get().getTheSequences();
        ObservableList<File> files = theArrangement.get().getTheSequenceFiles();
        measureLineNum.set(0);
        for (int i = 0; i < seq.size(); i++) {
            File f = files.get(i);
            try {
                seq.set(i, Utilities.loadSong(f));
            } catch (StreamCorruptedException | NullPointerException e) {
                try {
                    seq.set(i, MPCDecoder.decode(f));
                } catch (ParseException | IOException e1) {
                    e1.printStackTrace();
                    stopSong();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                stopSong();
                return;
            }
        }
//        soundPlayerThread.start();//TODO
        arrPlaying = true;
//        animationService.restart();//TODO
    }
    
    /** Stops the song that is currently playing. */
    public void stopSong() {
    	//TODO:
//        soundPlayer.setRun(false);
//        try {
//            soundPlayerThread.join();
//        } catch (InterruptedException e) {
//
//        }
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                songPlaying = false;
                arrPlaying = false;
                //TODO
//                animationService.cancel();
//                switch (animationService.getState()) {
//                case CANCELLED:
//                case FAILED:
//                case READY:
//                case SUCCEEDED:
//                    animationService.reset();
//                    break;
//                default:
//                    break;
//                }
            }
        });
        //TODO:
//        while (soundPlayerThread.isAlive()) {
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                // do nothing
//            }
//        }
//        soundPlayerThread = new Thread(soundPlayer);
//        soundPlayerThread.setDaemon(true);
    }

}

