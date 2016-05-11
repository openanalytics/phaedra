package eu.openanalytics.phaedra.base.ui.nattable.columnChooser.command;


import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.nebula.widgets.nattable.columnChooser.command.DisplayColumnChooserCommand;
import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;

import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.ColumnChooser;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;

public class DisplayColumnChooserCommandHandler extends AbstractLayerCommandHandler<DisplayColumnChooserCommand> {

	private final ColumnHideShowLayer columnHideShowLayer;
	private final ColumnGroupHeaderLayer columnGroupHeaderLayer;
	private final ColumnGroupModel columnGroupModel;
	private final SelectionLayer selectionLayer;
	private final DataLayer columnHeaderDataLayer;
	private final ColumnHeaderLayer columnHeaderLayer;
	private final boolean sortAvailableColumns;
	private IDialogSettings dialogSettings;

	private Map<String, IColumnMatcher> groupMatchers;

	public DisplayColumnChooserCommandHandler(
			SelectionLayer selectionLayer,
			ColumnHideShowLayer columnHideShowLayer,
			ColumnHeaderLayer columnHeaderLayer,
			DataLayer columnHeaderDataLayer,
			ColumnGroupHeaderLayer cgHeader,
			ColumnGroupModel columnGroupModel,
			Map<String, IColumnMatcher> groupMatchers) {

		this(selectionLayer, columnHideShowLayer, columnHeaderLayer, columnHeaderDataLayer, cgHeader, columnGroupModel, false, groupMatchers);
	}

	public DisplayColumnChooserCommandHandler(
			SelectionLayer selectionLayer,
			ColumnHideShowLayer columnHideShowLayer,
			ColumnHeaderLayer columnHeaderLayer,
			DataLayer columnHeaderDataLayer,
			ColumnGroupHeaderLayer cgHeader,
			ColumnGroupModel columnGroupModel,
			boolean sortAvalableColumns,
			Map<String, IColumnMatcher> groupMatchers) {

		this.selectionLayer = selectionLayer;
		this.columnHideShowLayer = columnHideShowLayer;
		this.columnHeaderLayer = columnHeaderLayer;
		this.columnHeaderDataLayer = columnHeaderDataLayer;
		this.columnGroupHeaderLayer = cgHeader;
		this.columnGroupModel = columnGroupModel;
		this.sortAvailableColumns = sortAvalableColumns;
		this.groupMatchers = groupMatchers;
	}

	@Override
	public boolean doCommand(DisplayColumnChooserCommand command) {
		ColumnChooser columnChooser = new ColumnChooser(
				command.getNatTable().getShell(),
				selectionLayer,
				columnHideShowLayer,
				columnHeaderLayer,
				columnHeaderDataLayer,
				columnGroupHeaderLayer,
				columnGroupModel,
				sortAvailableColumns,
				groupMatchers
		);

		columnChooser.setDialogSettings(dialogSettings);
		columnChooser.openDialog();
		return true;
	}

	public void setDialogSettings(IDialogSettings dialogSettings) {
		this.dialogSettings = dialogSettings;
	}

	@Override
	public Class<DisplayColumnChooserCommand> getCommandClass() {
		return DisplayColumnChooserCommand.class;
	}

}
