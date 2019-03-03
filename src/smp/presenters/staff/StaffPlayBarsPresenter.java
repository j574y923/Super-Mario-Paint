package smp.presenters.staff;

import java.util.ArrayList;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import smp.ImageIndex;
import smp.ImageLoader;
import smp.models.stateMachine.StateMachine;
import smp.models.stateMachine.Variables;

public class StaffPlayBarsPresenter {

	// TODO: auto-add these model comments
	// ====Models====
	private IntegerProperty playIndex;

	private HBox staffPlayBars;

	private ArrayList<ImageView> staffPlayBarsIV;

	// TODO: set
	private ImageLoader il;

	public StaffPlayBarsPresenter(HBox staffPlayBars) {
		this.staffPlayBars = staffPlayBars;
		initializeStaffPlayBars(this.staffPlayBars);

		this.measureLineNumber = StateMachine.getMeasureLineNum();
		this.playIndex = Variables.playIndex;
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
	
	private void setupViewUpdater() {
		this.playIndex.addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				bumpHighlights(newValue.intValue());
			}
		});
	}
}
