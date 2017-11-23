package smp.clipboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import smp.components.InstrumentIndex;

public class InstrumentFilter extends HashSet<InstrumentIndex> {

	/**
	 * wot
	 */
	private static final long serialVersionUID = 1L;
	
	private HBox instrumentLine;
	private List<Text> filterTexts = new ArrayList<>();
	private List<FadeTransition> filterTextsFades = new ArrayList<>();
	
	private Node prevFocus;
	
	public InstrumentFilter(HBox instLine){
		super();
		instrumentLine = instLine;
		ObservableList<Node> instrumentImages = instLine.getChildren();
		for (int i = 0; i < instrumentImages.size(); i++) {
			final int index = i;
			final Node instrumentImage = instrumentImages.get(i);
			addFilterText(instrumentImage);
			
			instrumentImage.setOnMouseEntered(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					prevFocus = instrumentImage.getScene().focusOwnerProperty().get();
					instrumentImage.requestFocus();
					fadeFilterTexts(false);
					for(Text filterText : filterTexts)
						filterText.setOpacity(1.0);
				}});
			instrumentImage.setOnMouseExited(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					prevFocus.requestFocus();
					fadeFilterTexts(true);
				}});
			
			instrumentImage.setOnKeyPressed(new EventHandler<KeyEvent>() {

				@Override
				public void handle(KeyEvent event) {
					// TODO Auto-generated method stub
					if(event.getCode() == KeyCode.F) {
						toggleInstrumentNoText(InstrumentIndex.values()[index]);
					} 
				}
			});
		}
	}
	
	private void addFilterText(Node instrumentImage) {
		Text filterText = new Text("[_]");
		filterText.setFill(Color.RED);
		filterTexts.add(filterText);
		Pane instLinePane = (Pane) instrumentLine.getParent();
		instLinePane.getChildren().add(filterText);
		
		Bounds instrumentImageBounds = instrumentImage.localToScene(instrumentImage.getBoundsInLocal());
		filterText.setTranslateX(instrumentImageBounds.getMinX());
		filterText.setTranslateY(instrumentImageBounds.getMinY() - 5);
		filterText.setOpacity(0.0);
		
		FadeTransition ft = new FadeTransition(Duration.millis(2000), filterText);
		ft.setFromValue(1.0);
		ft.setToValue(0.0);
		filterTextsFades.add(ft);
	}
	
	private void fadeFilterTexts(boolean fadeThem) {
		for (FadeTransition fade : filterTextsFades)
			if(fadeThem)
				fade.playFromStart();
			else
				fade.pause();
	}
	
	/**
	 * @param ind
	 *            the instrument
	 * @return if instrument is allowed copying, deleting, etc.
	 */
	public boolean isFiltered(InstrumentIndex ind) {
		return this.isEmpty() || this.contains(ind);
	}
	
	/**
	 * turn instrument on/off in filter, display and fade the filter text
	 * 
	 * @param ind
	 *            instrument to filter
	 * @return true if it now contains ind, false if it doesn't
	 */
	public boolean toggleInstrument(InstrumentIndex ind) {
		if(toggleInstrumentNoText(ind)) {
			fadeFilterTexts(true);
			return true;
		} else {
			fadeFilterTexts(true);
			return false;
		}
	}
	
	/**
	 * toggleInstrument but don't display text
	 * 
	 * @param ind
	 *            instrument to filter
	 * @return true if it now contains ind, false if it doesn't
	 */
	public boolean toggleInstrumentNoText(InstrumentIndex ind) {
		if(!this.contains(ind)){
			this.add(ind);
			filterTexts.get(ind.ordinal()).setText("[f]");
			return true;
		}
		else{
			this.remove(ind);
			filterTexts.get(ind.ordinal()).setText("[_]");
			return false;
		}
	}
}
