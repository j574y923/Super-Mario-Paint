package smp.presenters.buttons.PlayPresenter;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import smp.components.Values;
import smp.models.staff.StaffSequence;
import smp.models.stateMachine.StateMachine;
import smp.models.stateMachine.Variables;

/**
 * This class keeps track of animation and sound. Note to self: While
 * running a service or a task, crashes do not print stack traces.
 * Therefore, debug like crazy!
 */
class AnimationTask extends Task<Void> {

	private DoubleProperty measureLineNum;
	private ObjectProperty<StaffSequence> theSequence;

    /**
     * This is the current index of the measure line that we are on on
     * the staff.
     */
    protected int index = 0;

    /**
     * Whether we need to advance the staff ahead by a few lines or not.
     */
    protected boolean advance = false;

    /** Number of lines queued up to play. */
    protected volatile int queue = 0;
    
    public AnimationTask() {
    	this.theSequence = Variables.theSequence;
    	this.measureLineNum = StateMachine.getMeasureLineNum();
    }
    
    /**
     * Zeros the staff to the beginning point. Use only at the beginning
     * of a new song file.
     */
    protected void zeroEverything() {
        Platform.runLater(new Runnable() {

			@Override
            public void run() {
                measureLineNum.set(0);
                queue--;
            }
        });
    }

    @Override
    protected Void call() throws Exception {
        playBars = staffImages.getPlayBars();
        int counter = StateMachine.getMeasureLineNum();
        boolean zero = false;
        while (songPlaying) {
            if (zero) {
                queue++;
                zeroEverything();
                while (queue > 0)
                    ;
                zero = false;
            }
            queue++;
            playNextLine();
            counter++;
            if (counter > lastLine && counter % 4 == 0) {
                if (StateMachine.isLoopPressed()) {
                    counter = 0;
                    index = 0;
                    advance = false;
                    zero = true;
                } else {
                    songPlaying = false;
                }
            }
            try {
                Thread.sleep(delayMillis, delayNanos);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        highlightsOff();
        hitStop();
        return null;
    }

    /**
     * Plays the next line of notes in the queue. For
     * ease-of-programming purposes, we'll not care about efficiency and
     * just play things as they are.
     */
    protected void playNextLine() {
        runUI(index, advance);
        advance = !(index < Values.NOTELINES_IN_THE_WINDOW - 1);
        int remain = (int) (theSequence.get().getTheLinesSize().get() - measureLineNum.intValue());
        if (Values.NOTELINES_IN_THE_WINDOW > remain && advance) {
            index -= (remain - 1);
        } else {
            index = advance ? 0 : (index + 1);
        }
    }

    /**
     * Bumps the highlight of the notes to the next play bar.
     *
     * @param playBars
     *            The list of the measure highlights.
     * @param index
     *            The current index of the measure that we're on.
     * @param advance
     *            Whether we need to move the staff by some bit.
     */
    private void runUI(final int index, final boolean advance) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                bumpHighlights(playBars, index);
                if (advance) {
                    int loc = (int) currVal.getValue().doubleValue()
                            + Values.NOTELINES_IN_THE_WINDOW;
                    setLocation(loc);
                    currVal.setValue(loc);
                }
                playSoundLine(index);
                queue--;
            }
        });
    }

    /**
     * Plays a sound line at the index specified. Or rather, tells the
     * SoundPlayer thread to do that.
     *
     * @param index
     *            The index to play.
     */
    private void playSoundLine(int index) {
        soundPlayer.playSoundLine(index);
    }

    /**
     * Bumps the highlights on the staff by a certain amount.
     *
     * @param playBars
     *            The list of playbar objects
     * @param index
     *            The index that we are currently at
     */
    private void bumpHighlights(ArrayList<ImageView> playBars,
            int index) {
        playBars.get(index).setVisible(true);
        for (int i = 0; i < playBars.size(); i++)
            if (i != index)
                playBars.get(i).setVisible(false);

    }

}
