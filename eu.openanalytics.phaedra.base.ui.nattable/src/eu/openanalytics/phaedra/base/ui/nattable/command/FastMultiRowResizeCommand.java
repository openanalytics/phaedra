package eu.openanalytics.phaedra.base.ui.nattable.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.nebula.widgets.nattable.command.AbstractMultiRowCommand;
import org.eclipse.nebula.widgets.nattable.command.ILayerCommand;
import org.eclipse.nebula.widgets.nattable.command.LayerCommandUtil;
import org.eclipse.nebula.widgets.nattable.coordinate.RowPositionCoordinate;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;

public class FastMultiRowResizeCommand extends AbstractMultiRowCommand {

	private ILayer currentLayer;
	private boolean isResizeAllRows;
	private int commonRowHeight = -1;

	private Map<Integer, Integer> rowPositionToHeight;

	/**
	 * All rows are being resized to the same height.
	 */
	public FastMultiRowResizeCommand(ILayer layer, int commonRowHeight) {
		this(layer, new int[0], commonRowHeight);
	}

	/**
	 * All given rows are being resized to the same height e.g. during a drag resize
	 */
	public FastMultiRowResizeCommand(ILayer layer, int[] rowPositions, int commonRowHeight) {
		super(layer, rowPositions);
		this.currentLayer = layer;
		this.isResizeAllRows = rowPositions.length == 0;
		this.commonRowHeight = commonRowHeight;

		this.rowPositionToHeight = new HashMap<>();
		for (int i = 0; i < rowPositions.length; i++) {
			rowPositionToHeight.put(rowPositions[i], commonRowHeight);
		}
	}

	/**
	 * Each row is being resized to a different size e.g. during auto resize
	 */
	public FastMultiRowResizeCommand(ILayer layer, int[] rowPositions, int[] rowHeights) {
		super(layer, rowPositions);
		this.currentLayer = layer;

		this.rowPositionToHeight = new HashMap<>();
		for (int i = 0; i < rowPositions.length; i++) {
			rowPositionToHeight.put(rowPositions[i], rowHeights[i]);
		}
	}

	protected FastMultiRowResizeCommand(FastMultiRowResizeCommand command) {
		super(command);
		this.currentLayer = command.currentLayer;
		this.isResizeAllRows = command.isResizeAllRows;
		this.commonRowHeight = command.commonRowHeight;

		this.rowPositionToHeight = command.rowPositionToHeight;
	}

	@Override
	public boolean convertToTargetLayer(ILayer targetLayer) {
		if (isResizeAllRows) return true;

		Map<Integer, Integer> newRowPositionToHeight = new HashMap<>();

		for (Integer rowPosition : rowPositionToHeight.keySet()) {
			RowPositionCoordinate convertedRowPositionCoordinate = LayerCommandUtil.convertRowPositionToTargetContext(new RowPositionCoordinate(currentLayer, rowPosition), targetLayer);
			if (convertedRowPositionCoordinate != null) {
				Integer value = rowPositionToHeight.get(rowPosition);
				newRowPositionToHeight.put(convertedRowPositionCoordinate.getRowPosition(), value);
			}
		}

		if (super.convertToTargetLayer(targetLayer)) {
			rowPositionToHeight = newRowPositionToHeight;
			currentLayer = targetLayer;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ILayerCommand cloneCommand() {
		return new FastMultiRowResizeCommand(this);
	}

	public int getRowHeight(int rowPosition) {
		Integer height = rowPositionToHeight.get(rowPosition);
		if (height == null) return commonRowHeight;
		return height;
	}

	public int getCommonRowHeight() {
		return commonRowHeight;
	}

	public boolean isResizeAllRows() {
		return isResizeAllRows;
	}

}
