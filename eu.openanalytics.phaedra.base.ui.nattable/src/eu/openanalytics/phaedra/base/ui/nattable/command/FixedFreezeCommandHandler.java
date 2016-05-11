package eu.openanalytics.phaedra.base.ui.nattable.command;

import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeHelper;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezeColumnCommand;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezeColumnStrategy;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezePositionCommand;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezePositionStrategy;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezeRowCommand;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezeRowStrategy;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezeSelectionCommand;
import org.eclipse.nebula.widgets.nattable.freeze.command.FreezeSelectionStrategy;
import org.eclipse.nebula.widgets.nattable.freeze.command.IFreezeCommand;
import org.eclipse.nebula.widgets.nattable.freeze.command.IFreezeCoordinatesProvider;
import org.eclipse.nebula.widgets.nattable.freeze.command.UnFreezeGridCommand;
import org.eclipse.nebula.widgets.nattable.freeze.event.UnfreezeEvent;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

/**
 * Fixed an issue with Unfreezing causing columns/rows to become hidden.
 * 
 * Originally, the FreezeCommandHandler would use the FreezeHelper class for unfreezing.
 * However, this FreezeHelper could reset the origins to negative values, which works fine as long as another object 
 * doesn't call the getWidth()/getHeight() method of the viewportLayer before getColumnCount()/getRowCount() was called.
 */
public class FixedFreezeCommandHandler extends AbstractLayerCommandHandler<IFreezeCommand> {

	protected final FreezeLayer freezeLayer;

	protected final ViewportLayer viewportLayer;

	protected final SelectionLayer selectionLayer;

	public FixedFreezeCommandHandler(FreezeLayer freezeLayer, ViewportLayer viewportLayer, SelectionLayer selectionLayer) {
		this.freezeLayer = freezeLayer;
		this.viewportLayer = viewportLayer;
		this.selectionLayer = selectionLayer;
	}

	@Override
	public Class<IFreezeCommand> getCommandClass() {
		return IFreezeCommand.class;
	}

	@Override
	public boolean doCommand(IFreezeCommand command) {

		if (command instanceof FreezeColumnCommand) {
			//freeze for a whole column
			FreezeColumnCommand freezeColumnCommand = (FreezeColumnCommand)command;
			IFreezeCoordinatesProvider coordinatesProvider = 
					new FreezeColumnStrategy(freezeLayer, freezeColumnCommand.getColumnPosition());
			handleFreezeCommand(coordinatesProvider, freezeColumnCommand.isToggle(), command.isOverrideFreeze());
			return true;
		} 
		else if (command instanceof FreezeRowCommand) {
			//freeze for a whole row
			FreezeRowCommand freezeRowCommand = (FreezeRowCommand) command;
			IFreezeCoordinatesProvider coordinatesProvider = 
					new FreezeRowStrategy(freezeLayer, freezeRowCommand.getRowPosition());
			handleFreezeCommand(coordinatesProvider, freezeRowCommand.isToggle(), command.isOverrideFreeze());
			return true;
		} 
		else if (command instanceof FreezePositionCommand) {
			//freeze for a given position
			FreezePositionCommand freezePositionCommand = (FreezePositionCommand) command;
			IFreezeCoordinatesProvider coordinatesProvider = 
					new FreezePositionStrategy(freezeLayer, freezePositionCommand.getColumnPosition(), freezePositionCommand.getRowPosition());
			handleFreezeCommand(coordinatesProvider, freezePositionCommand.isToggle(), command.isOverrideFreeze());
			return true;
		} 
		else if (command instanceof FreezeSelectionCommand) {
			//freeze at the current selection anchor
			IFreezeCoordinatesProvider coordinatesProvider = 
					new FreezeSelectionStrategy(freezeLayer, viewportLayer, selectionLayer);
			handleFreezeCommand(coordinatesProvider, command.isToggle(), command.isOverrideFreeze());
			return true;
		} 
		else if (command instanceof UnFreezeGridCommand) {
			//unfreeze
			handleUnfreeze();
			return true;
		}

		return false;
	}

	/**
	 * Performs freeze actions dependent on the coordinates specified by the given 
	 * {@link IFreezeCoordinatesProvider} and the configuration flags.
	 * If a freeze state is already active it is checked if this state should be overriden
	 * or toggled. Otherwise the freeze state is applied.
	 * @param coordinatesProvider The {@link IFreezeCoordinatesProvider} to retrieve the freeze
	 * 			coordinates from
	 * @param toggle whether to unfreeze if the freeze layer is already in a frozen state
	 * @param override whether to override a current frozen state.
	 */
	protected void handleFreezeCommand(IFreezeCoordinatesProvider coordinatesProvider, 
			boolean toggle, boolean override) {

		if (!freezeLayer.isFrozen() || override) {
			//if we are in a frozen state and be configured to override, reset the viewport first
			if (freezeLayer.isFrozen() && override) {
				FreezeHelper.resetViewport(freezeLayer, viewportLayer);
			}

			final PositionCoordinate topLeftPosition = coordinatesProvider.getTopLeftPosition();
			final PositionCoordinate bottomRightPosition = coordinatesProvider.getBottomRightPosition();

			FreezeHelper.freeze(freezeLayer, viewportLayer, topLeftPosition, bottomRightPosition);
		} 
		else if (toggle) {  
			// if frozen and toggle = true
			handleUnfreeze();
		}
	}

	/**
	 * Unfreeze a current frozen state.
	 */
	protected void handleUnfreeze() {
		//FreezeHelper.unfreeze(freezeLayer, viewportLayer);
		if (freezeLayer == null || viewportLayer == null) {
			throw new IllegalArgumentException("freezeLayer and viewportLayer can not be null!");
		}

		resetViewport(freezeLayer, viewportLayer);

		freezeLayer.setTopLeftPosition(-1, -1);
		freezeLayer.setBottomRightPosition(-1, -1);

		viewportLayer.fireLayerEvent(new UnfreezeEvent(viewportLayer));
	}

	/**
	 * Helper method to reset the origin coordinates of the viewport. Is needed to perform an 
	 * unfreeze or to override a current frozen state.
	 * @param freezeLayer The FreezeLayer of the grid to perform the freeze action.
	 * @param viewportLayer The ViewportLayer of the grid to perform the freeze action.
	 */
	public static void resetViewport(FreezeLayer freezeLayer, ViewportLayer viewportLayer) {
		PositionCoordinate topLeftPosition = freezeLayer.getTopLeftPosition();
		int startXOfColumnPosition = viewportLayer.getStartXOfColumnPosition(Math.max(0, topLeftPosition.columnPosition));
		int startYOfRowPosition = viewportLayer.getStartYOfRowPosition(Math.max(0,topLeftPosition.rowPosition));
		viewportLayer.resetOrigin(Math.max(startXOfColumnPosition, 0), Math.max(startYOfRowPosition, 0));
	}

}
