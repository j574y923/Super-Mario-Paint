package smp.presenters.buttons.PlayPresenter;

import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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
}