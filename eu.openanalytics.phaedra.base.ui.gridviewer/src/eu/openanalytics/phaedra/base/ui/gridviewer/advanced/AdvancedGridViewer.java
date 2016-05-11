package eu.openanalytics.phaedra.base.ui.gridviewer.advanced;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.advanced.AdvancedGrid.GridFillLayout;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.IGridContentProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

public class AdvancedGridViewer extends GridViewer {

	private int selConf;
	
	public AdvancedGridViewer(Composite parent) {
		this(parent, 8, 12);
	}

	public AdvancedGridViewer(Composite parent, int rows, int columns) {
		super(parent, rows, columns);
	}

	public AdvancedGrid getAdvancedGrid() {
		return (AdvancedGrid) getGrid();
	}

	public void setSelectionConfiguration(int selConf) {
		this.selConf = selConf;
	}
	
	@Override
	protected Grid createGrid(Composite parent, int rows, int columns) {
		return new AdvancedGrid(parent, SWT.NONE, rows, columns);
	}

	@Override
	public void setContentProvider(IContentProvider provider) {
		if (provider instanceof ModifyableGridContentProvider) {
			getAdvancedGrid().setGridFillLayout(GridFillLayout.SPECIFY_ROW);
		}
		super.setContentProvider(provider);
	}

	@Override
	public ISelection getSelection() {
		Control control = getControl();
		if (control == null || control.isDisposed()) return StructuredSelection.EMPTY;
		ConfigurableStructuredSelection sel = new ConfigurableStructuredSelection(getSelectionFromWidget(), getComparer());
		sel.setConfiguration(selConf);
		return sel;
	}
	
	@Override
	protected void internalRefresh(Object element) {
		if (getContentProvider() instanceof ModifyableGridContentProvider) {
			AdvancedGrid grid = getAdvancedGrid();
			ModifyableGridContentProvider contentProvider = (ModifyableGridContentProvider) getContentProvider();

			grid.resetGrid(contentProvider.getSize());
			contentProvider.resetGrid(grid.getRows(), grid.getColumns(), grid.isFillColumnWise());

			for (int row=0; row < grid.getRows(); row++) {
				for (int col=0; col < grid.getColumns(); col++) {
					Object e = contentProvider.getElement(row, col);
					grid.getCell(row, col).setData(e);
					updateItem(grid, e);
					if (e != null) update(e, null);
				}
			}
		} else {
			super.internalRefresh(element);
		}
	}

	public Dialog createDialog() {
		return new AdvancedGridViewerSettingsDialog(Display.getDefault().getActiveShell(), this);
	}

	private class AdvancedGridViewerSettingsDialog extends TitleAreaDialog {

		private AdvancedGridViewer gridViewer;

		private Combo gridFillCombo;
		private Button keepSquareButton;
		private Spinner cellWidthSpinner;
		private Spinner cellHeightSpinner;
		private Spinner columnSpinner;
		private Spinner rowSpinner;

		private GridFillLayout originalGridFillLayout;
		private boolean originalKeepSquare;
		private Point originalCellSize;
		private int originalColumns;
		private int originalRows;

		public AdvancedGridViewerSettingsDialog(Shell parentShell, AdvancedGridViewer gridViewer) {
			super(parentShell);
			this.gridViewer = gridViewer;

			AdvancedGrid grid = gridViewer.getAdvancedGrid();
			this.originalGridFillLayout = grid.getGridFillLayout();
			this.originalKeepSquare = grid.isKeepSquare();
			this.originalCellSize = grid.getCurrentCellSize();
			this.originalColumns = grid.getPreferredColumns();
			this.originalRows = grid.getPreferredRows();
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Configure Grid");
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle("Configure Grid");
			setMessage("Change the layout of the grid");
			Composite area = (Composite) super.createDialogArea(parent);
			Composite comp = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(comp);

			Label lbl = new Label(comp, SWT.NONE);
			lbl.setText("Grid Fill Layout:");

			gridFillCombo = new Combo(comp, SWT.READ_ONLY | SWT.BORDER);
			GridFillLayout[] values = GridFillLayout.values();
			String[] items = new String[values.length];

			int selectedIndex = 0;
			for (int i = 0; i < items.length; i++) {
				items[i] = values[i].getName();
				if (values[i] == originalGridFillLayout) {
					selectedIndex = i;
				}
			}
			gridFillCombo.setItems(items);
			gridFillCombo.select(selectedIndex);
			gridFillCombo.addListener(SWT.Selection, e -> {
				int selectionIndex = gridFillCombo.getSelectionIndex();
				GridFillLayout newGridFillLayout = GridFillLayout.values()[selectionIndex];
				AdvancedGrid advancedGrid = gridViewer.getAdvancedGrid();
				advancedGrid.setGridFillLayout(newGridFillLayout);
				updateContainer();
				gridViewer.internalRefresh(null);
			});

			Listener cellSizeListener = e -> {
				if (keepSquareButton.getSelection()) {
					if (e.widget == cellWidthSpinner) {
						cellHeightSpinner.setSelection(cellWidthSpinner.getSelection());
					} else {
						cellWidthSpinner.setSelection(cellHeightSpinner.getSelection());
					}
				}
				int cellWidth = cellWidthSpinner.getSelection();
				int cellHeight = cellHeightSpinner.getSelection();
				gridViewer.getAdvancedGrid().setCurrentCellSize(cellWidth, cellHeight);
				gridViewer.internalRefresh(null);
			};

			lbl = new Label(comp, SWT.NONE);
			lbl.setText("Cell Width:");

			cellWidthSpinner = new Spinner(comp, SWT.BORDER);
			cellWidthSpinner.setMinimum(10);
			cellWidthSpinner.setMaximum(2500);
			cellWidthSpinner.setIncrement(10);
			cellWidthSpinner.setPageIncrement(100);
			cellWidthSpinner.setSelection(originalCellSize.x);
			cellWidthSpinner.addListener(SWT.Selection, cellSizeListener);

			lbl = new Label(comp, SWT.NONE);
			lbl.setText("Cell Height:");

			cellHeightSpinner = new Spinner(comp, SWT.BORDER);
			cellHeightSpinner.setMinimum(10);
			cellHeightSpinner.setMaximum(2500);
			cellHeightSpinner.setIncrement(10);
			cellHeightSpinner.setPageIncrement(100);
			cellHeightSpinner.setSelection(originalCellSize.y);
			cellHeightSpinner.addListener(SWT.Selection, cellSizeListener);

			lbl = new Label(comp, SWT.NONE);

			keepSquareButton = new Button(comp, SWT.CHECK);
			keepSquareButton.setText("Keep Square Aspect Ratio");
			keepSquareButton.setSelection(originalKeepSquare);
			keepSquareButton.addListener(SWT.Selection, e -> {
				gridViewer.getAdvancedGrid().setKeepSquare(keepSquareButton.getSelection());
				cellSizeListener.handleEvent(e);
				updateContainer();
				gridViewer.internalRefresh(null);
			});

			lbl = new Label(comp, SWT.NONE);
			lbl.setText("# of Columns:");

			columnSpinner = new Spinner(comp, SWT.BORDER);
			columnSpinner.setMinimum(1);
			columnSpinner.setMaximum(Integer.MAX_VALUE);
			columnSpinner.setIncrement(1);
			columnSpinner.setPageIncrement(10);
			columnSpinner.setSelection(originalColumns);
			columnSpinner.addListener(SWT.Selection, e -> {
				IContentProvider contentProvider = gridViewer.getContentProvider();
				if (contentProvider instanceof IGridContentProvider) {
					int size = ((IGridContentProvider) contentProvider).getRows(null) * ((IGridContentProvider) contentProvider).getColumns(null);
					int newColumns = columnSpinner.getSelection();
					int newRows = size / newColumns;
					if (size % newColumns != 0) newRows++;
					rowSpinner.setSelection(newRows);
					AdvancedGrid grid = gridViewer.getAdvancedGrid();
					grid.setPreferredColumns(newColumns);
					grid.setPreferredRows(newRows);
					updateGrid(grid);
				}
			});

			lbl = new Label(comp, SWT.NONE);
			lbl.setText("# of Rows:");

			rowSpinner = new Spinner(comp, SWT.BORDER);
			rowSpinner.setMinimum(1);
			rowSpinner.setMaximum(Integer.MAX_VALUE);
			rowSpinner.setIncrement(1);
			rowSpinner.setPageIncrement(10);
			rowSpinner.setSelection(originalRows);
			rowSpinner.addListener(SWT.Selection, e -> {
				IContentProvider contentProvider = gridViewer.getContentProvider();
				if (contentProvider instanceof IStructuredContentProvider) {
					int size = ((IGridContentProvider) contentProvider).getRows(null) * ((IGridContentProvider) contentProvider).getColumns(null);
					int newRows = rowSpinner.getSelection();
					int newColumns = size / newRows;
					if (size % newRows != 0) newColumns++;
					columnSpinner.setSelection(newColumns);
					AdvancedGrid grid = gridViewer.getAdvancedGrid();
					grid.setPreferredColumns(newColumns);
					grid.setPreferredRows(newRows);
					updateGrid(grid);
				}
			});

			updateContainer();

			return comp;
		}

		@Override
		protected void cancelPressed() {
			AdvancedGrid grid = gridViewer.getAdvancedGrid();
			grid.setGridFillLayout(originalGridFillLayout);
			grid.setKeepSquare(originalKeepSquare);
			grid.setCurrentCellSize(originalCellSize.x, originalCellSize.y);
			grid.setPreferredColumns(originalColumns);
			grid.setPreferredRows(originalRows);
			gridViewer.internalRefresh(null);
			super.cancelPressed();
		}

		private void updateGrid(AdvancedGrid grid) {
			JobUtils.runJob(monitor -> {
				Display.getDefault().asyncExec(() -> {
					gridViewer.internalRefresh(null);
					grid.redraw();
				});
			}, true, "Updating Grid Layout", 100, toString(), null, 500);
		}

		private void updateContainer() {
			GridFillLayout newGridFillLayout = gridViewer.getAdvancedGrid().getGridFillLayout();
			switch (newGridFillLayout) {
			case HORIZONTAL_FILL:
			case VERTICAL_FILL:
				cellWidthSpinner.setEnabled(true);
				cellHeightSpinner.setEnabled(true);
				columnSpinner.setEnabled(false);
				rowSpinner.setEnabled(false);
				break;
			case SPECIFY_COLUMN:
				cellWidthSpinner.setEnabled(false);
				cellHeightSpinner.setEnabled(!keepSquareButton.getSelection());
				columnSpinner.setEnabled(true);
				rowSpinner.setEnabled(false);
				break;
			case SPECIFY_ROW:
				cellWidthSpinner.setEnabled(!keepSquareButton.getSelection());
				cellHeightSpinner.setEnabled(false);
				columnSpinner.setEnabled(false);
				rowSpinner.setEnabled(true);
				break;
			default:
				break;
			}
		}
	}

}
