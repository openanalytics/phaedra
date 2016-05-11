package eu.openanalytics.phaedra.base.ui.nattable.copy.command;

import org.eclipse.nebula.widgets.nattable.copy.command.CopyDataCommandHandler;
import org.eclipse.nebula.widgets.nattable.copy.command.CopyDataToClipboardCommand;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

/**
 * <p>Based on the original {@link CopyDataCommandHandler}.
 * </p><p>
 * Limits the amount of cells that can be copied into the clipboard as text to prevent the application from hanging for several minutes.
 * </p>
 * @see {@link CopyDataCommandHandler}
 */
public class LimitedCopyDataCommandHandler extends CopyDataCommandHandler {

	private static final int MAX_CELLS = 1_000_000;

	private final SelectionLayer selectionLayer;

    /**
     * Creates an instance that only checks the {@link SelectionLayer} for data
     * to add to the clipboard.
     *
     * @param selectionLayer
     *            The {@link SelectionLayer} within the NatTable. Can not be
     *            <code>null</code>.
     */
    public LimitedCopyDataCommandHandler(SelectionLayer selectionLayer) {
        super(selectionLayer, null, null);
        this.selectionLayer = selectionLayer;
    }

    /**
     * Creates an instance that checks the {@link SelectionLayer} and the header
     * layers if they are given.
     *
     * @param selectionLayer
     *            The {@link SelectionLayer} within the NatTable. Can not be
     *            <code>null</code>.
     * @param columnHeaderDataLayer
     *            The column header data layer within the NatTable grid. Can be
     *            <code>null</code>.
     * @param rowHeaderDataLayer
     *            The row header data layer within the NatTable grid. Can be
     *            <code>null</code>.
     */
    public LimitedCopyDataCommandHandler(SelectionLayer selectionLayer, ILayer columnHeaderDataLayer, ILayer rowHeaderDataLayer) {
       super(selectionLayer, columnHeaderDataLayer, rowHeaderDataLayer);
       this.selectionLayer = selectionLayer;
    }

    @Override
    public boolean doCommand(CopyDataToClipboardCommand command) {
    	int nrOfSelectedRows = selectionLayer.getSelectionModel().getSelectedRowCount();
    	int nrOfSelectedColumns = selectionLayer.getSelectionModel().getSelectedColumnPositions().length;
    	int totalCells = nrOfSelectedRows * nrOfSelectedColumns;
		if (totalCells > MAX_CELLS) {
    		copyWarningMessage();
    		return true;
    	}
        return super.doCommand(command);
    }

	private void copyWarningMessage() {
		final TextTransfer textTransfer = TextTransfer.getInstance();
		final Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			clipboard.setContents(new Object[] { "Cannot copy such a large selection in text format." }, new Transfer[] { textTransfer });
		} finally {
			clipboard.dispose();
		}
	}

}
