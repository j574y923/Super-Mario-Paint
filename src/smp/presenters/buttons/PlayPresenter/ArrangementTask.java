package smp.presenters.buttons.PlayPresenter;

import java.io.File;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import smp.models.staff.StaffArrangement;
import smp.models.staff.StaffSequence;
import smp.models.stateMachine.StateMachine;
import smp.models.stateMachine.Variables;

/**
 * This class runs an arrangement instead of just a song.
 */
class ArrangementTask extends AnimationTask {

	private ObjectProperty<StaffArrangement> theArrangement;
	private DoubleProperty measureLineNum;
	private ObjectProperty<StaffSequence> theSequence;

    /** Whether we are playing an arrangement. */
    private boolean arrPlaying = false;
    
	public ArrangementTask() {
		this.theArrangement = Variables.theArrangement;
		this.measureLineNum = StateMachine.getMeasureLineNum();
		this.theSequence = Variables.theSequence;
	}
	
    @Override
    protected Void call() throws Exception {
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
                theArrangementList.getSelectionModel().select(i);
                theArrangementList.scrollTo(i);
                queue--;
            }

        });
    }
}

