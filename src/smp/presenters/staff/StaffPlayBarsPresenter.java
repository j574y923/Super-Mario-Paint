package smp.presenters.staff;

import java.util.ArrayList;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import smp.ImageIndex;
import smp.ImageLoader;
import smp.models.stateMachine.ProgramState;
import smp.models.stateMachine.StateMachine;
import smp.models.stateMachine.Variables;

public class StaffPlayBarsPresenter {

	// TODO: auto-add these model comments
	// ====Models====
	private IntegerProperty playIndex;
	private ObjectProperty<ProgramState> programState;

	private HBox staffPlayBars;

	private ArrayList<ImageView> staffPlayBarsIV;

	// TODO: set
	private ImageLoader il;

	public StaffPlayBarsPresenter(HBox staffPlayBars) {
		this.staffPlayBars = staffPlayBars;
		initializeStaffPlayBars(this.staffPlayBars);

		this.playIndex = Variables.playIndex;
		this.programState = StateMachine.getState();
		setupViewUpdater();
	}

	/**
	 * Sets up the note highlighting functionality.
	 *
	 * @param staffPlayBars
	 *            The bars that move to highlight different notes.
	 */
	private void initializeStaffPlayBars(HBox playBars) {
		staffPlayBarsIV = new ArrayList<ImageView>();
		for (Node n : playBars.getChildren()) {
			ImageView i = (ImageView) n;
			i.setImage(il.getSpriteFX(ImageIndex.PLAY_BAR1));
			i.setVisible(false);
			staffPlayBarsIV.add(i);
		}
	}

    /**
     * Bumps the highlights on the staff by a certain amount.
     *
     * @param playBars
     *            The list of playbar objects
     * @param index
     *            The index that we are currently at
     */
    private void bumpHighlights(int index) {
    	if(index < 0 || index > staffPlayBarsIV.size())
    		return;
    	staffPlayBarsIV.get(index).setVisible(true);
        for (int i = 0; i < staffPlayBarsIV.size(); i++)
            if (i != index)
				staffPlayBarsIV.get(i).setVisible(false);
	}

	/** Turns off all highlights in the play bars in the staff. */
	private void highlightsOff() {
		for (ImageView i : staffPlayBarsIV) {
			i.setVisible(false);
		}
	}

	private void setupViewUpdater() {
		this.playIndex.addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				bumpHighlights(newValue.intValue());
			}
		});
		this.programState.addListener(new ChangeListener<ProgramState>() {

			@Override
			public void changed(ObservableValue<? extends ProgramState> arg0, ProgramState arg1, ProgramState arg2) {
				if (arg2.equals(ProgramState.EDITING) || arg2.equals(ProgramState.ARR_EDITING))
					highlightsOff();
			}
		});
	}
}
