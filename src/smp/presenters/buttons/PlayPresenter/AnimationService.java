package smp.presenters.buttons.PlayPresenter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import smp.components.Values;
import smp.components.staff.sequences.StaffSequence;
import smp.models.stateMachine.ProgramState;
import smp.models.stateMachine.StateMachine;

/**
 * This is a worker thread that helps run the animation on the staff.
 */
class AnimationService extends Service<Void> {
	
	private ObjectProperty<ProgramState> programState;	

    /** Number of lines queued up to play. */
    protected volatile int queue = 0;

    public AnimationService() {
		this.programState = StateMachine.getState();
    }
    
    @Override
    protected Task<Void> createTask() {
        if (!programState.get().equals(ProgramState.ARR_PLAYING))
            return new AnimationTask();
        else
            return new ArrangementTask();
    }

        
        /**
		 * Sets the soundset.
		 *
		 * @param soundset
		 *            The soundset.
		 * @since v1.1.2
		 */
		private void setSoundset(final String soundset) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					if (!controller.getSoundfontLoader().loadFromCache(soundset)) {
						try {
							controller.getSoundfontLoader().loadFromAppData(soundset);
						} catch (InvalidMidiDataException | IOException | MidiUnavailableException e) {
							e.printStackTrace();
						}
						controller.getSoundfontLoader().storeInCache();
					}
					queue--;
				}
			});
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

        /** Sets the scrollbar max/min to the proper values. */
        private void setScrollbar() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    theControls.getScrollbar().setMax(
                            theSequence.getTheLines().size()
                                    - Values.NOTELINES_IN_THE_WINDOW);
                    queue--;
                }
            });
        }

        /** Updates the current tempo - arranger version. */
        private void updateCurrTempo() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    theControls.getCurrTempo().setValue(
                            String.valueOf(StateMachine.getTempo()));
                    queue--;
                }
            });
        }
    }
}