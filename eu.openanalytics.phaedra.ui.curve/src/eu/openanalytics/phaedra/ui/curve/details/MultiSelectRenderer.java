package eu.openanalytics.phaedra.ui.curve.details;

import java.util.List;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class MultiSelectRenderer extends XYLineAndShapeRenderer {

	private static final long serialVersionUID = 364288029881506932L;

	private List<int[]> selection;
	
	public MultiSelectRenderer(boolean lines, boolean shapes) {
		super(lines, shapes);
	}
	
	@Override
	public boolean getItemShapeFilled(int series, int item) {
		if (selection != null) {
			for (int[] selectionItem: selection) {
				if (selectionItem[0] == series && selectionItem[1] == item) {
					return true;
				}
			}
		}
		return false;
	}

	public List<int[]> getSelection() {
		return selection;
	}

	public void setSelection(List<int[]> selection) {
		this.selection = selection;
		fireChangeEvent();
	}
}
