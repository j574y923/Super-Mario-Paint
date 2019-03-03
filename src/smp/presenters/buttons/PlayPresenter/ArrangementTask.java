package smp.presenters.buttons.PlayPresenter;

import java.io.File;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import smp.models.staff.StaffArrangement;
import smp.models.staff.StaffSequence;
import smp.models.stateMachine.Variables;

/**
 * This class runs an arrangement instead of just a song.
 */
class ArrangementTask extends AnimationTask {

	private ObjectProperty<StaffArrangement> theArrangement;

	public ArrangementTask() {
		super();
		this.theArrangement = Variables.theArrangement;
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
            theSequence = seq.get(i);
            theSequenceFile = files.get(i);
            StateMachine.setNoteExtensions(
                    theSequence.getNoteExtensions());
            controller.getInstBLine().updateNoteExtensions();
            StateMachine.setTempo(theSequence.getTempo());
            lastLine = findLastLine();
            songPlaying = true;
            setTempo(theSequence.getTempo());
            playBars = staffImages.getPlayBars();
            int counter = 0;
            StateMachine.setMeasureLineNum(0);
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
        highlightsOff();
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

