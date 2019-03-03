package smp.presenters.buttons.PlayPresenter;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import smp.components.Values;
import smp.models.staff.StaffNoteLine;
import smp.models.staff.StaffSequence;
import smp.models.stateMachine.ProgramState;
import smp.models.stateMachine.StateMachine;
import smp.models.stateMachine.Variables;

/**
 * This class keeps track of animation and sound. Note to self: While
 * running a service or a task, crashes do not print stack traces.
 * Therefore, debug like crazy!
 */
class AnimationTask extends Task<Void> {

	// TODO: auto-add these model comments
	// ====Models====
	private DoubleProperty measureLineNum;
	private ObjectProperty<StaffSequence> theSequence;
	private IntegerProperty playIndex;
	private ObjectProperty<ProgramState> programState;	
	private BooleanProperty loopPressed;

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
    
    /** Milliseconds to delay between updating the play bars. */
    protected long delayMillis;

    /** Nanoseconds to delay in addition to the milliseconds delay. */
    protected int delayNanos;

    /** Whether we are playing a song. */
    protected boolean songPlaying = false;
    
    /** This is the last line of notes in the song. */
    protected int lastLine;
    
    public AnimationTask() {
    	this.theSequence = Variables.theSequence;
    	this.measureLineNum = StateMachine.getMeasureLineNum();
    	this.playIndex = Variables.playIndex;
		this.programState = StateMachine.getState();
		this.loopPressed = StateMachine.getLoopPressed();
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
    	startSong();
        int counter = measureLineNum.intValue();
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
                if (loopPressed.get()) {
                    counter = 0;
                    index = 0;
                    playIndex.set(0);
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
		hitStop();
		return null;
	}

	/**
	 * Plays the next line of notes in the queue. For ease-of-programming purposes,
	 * we'll not care about efficiency and just play things as they are.
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
        playIndex.set(index);
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
                if (advance) {
                    int loc = measureLineNum.intValue() + Values.NOTELINES_IN_THE_WINDOW;
                    measureLineNum.set(loc);
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
    	//TODO:
//        soundPlayer.playSoundLine(index);
    }
    
    /**
     * Finds the last line in the sequence that we are playing.
     */
    protected int findLastLine() {
        ObservableList<StaffNoteLine> lines = theSequence.get().getTheLines();
        for (int i = lines.size() - 1; i >= 0; i--)
            if (!lines.get(i).isEmpty()) {
                return i;
            }
        return 0;
    }
    
    /**
     * Hits the stop button.
     */
    public void hitStop() {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
            	programState.set(ProgramState.EDITING);
            }
        });
    }

    /** Begins animation of the Staff. (Starts a song) */
    public synchronized void startSong() {
//        soundPlayer.setRun(true);//TODO
        lastLine = findLastLine();
        if ((lastLine == 0 && theSequence.get().getLine(0).isEmpty())
                || (lastLine < measureLineNum.intValue())) {
        	programState.set(ProgramState.EDITING);
            return;
        }
//        soundPlayerThread.start();//TODO
        songPlaying = true;
        setTempo(theSequence.get().getTempo().get());
//        animationService.restart();//TODO
    }
    
    /**
     * @param tempo
     *            The tempo we want to set this staff to run at, in BPM. Beats
     *            per minute * 60 = beats per second <br>
     *            Beats per second ^ -1 = seconds per beat <br>
     *            Seconds per beat * 1000 = Milliseconds per beat <br>
     *            (int) Milliseconds per beat = Milliseconds <br>
     *            Milliseconds per beat - milliseconds = remainder <br>
     *            (int) (Remainder * 1e6) = Nanoseconds <br>
     *
     */
    public void setTempo(double tempo) {
        double mill = (60.0 / tempo) * 1000;
        delayMillis = (int) mill;
        double nano = (mill - delayMillis) * Math.pow(10, 6);
        delayNanos = (int) nano;
    }
}
