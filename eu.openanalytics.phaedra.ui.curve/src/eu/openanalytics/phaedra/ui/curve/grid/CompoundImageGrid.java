package eu.openanalytics.phaedra.ui.curve.grid;

import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import ca.odell.glazedlists.matchers.Matcher;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.SizeManager;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundGridInput;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundImageContentProvider;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundImageSelectionTransformer;
import eu.openanalytics.phaedra.ui.curve.grid.tooltip.CompoundImageToolTip;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class CompoundImageGrid extends Composite {

	private NatTable table;
	private NatTableSelectionProvider<CompoundWithGrouping> selectionProvider;
	private CompoundImageContentProvider columnAccessor;
	private FullFeaturedColumnHeaderLayerStack<CompoundWithGrouping> columnHeaderLayer;
	private Matcher<CompoundWithGrouping> selectedMatcher;
	private boolean isShowSelectedOnly;

	private IUIEventListener imageSettingListener;

	public CompoundImageGrid(Composite parent, CompoundGridInput gridInput, MenuManager menuMgr) {
		super(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(this);

		columnAccessor = new CompoundImageContentProvider(gridInput);

		menuMgr.addMenuListener(manager -> {
			if (CompoundImageGrid.this.isVisible()) {
				ISelection sel = selectionProvider.getSelection();
				final Well selWell = SelectionUtils.getFirstObject(sel, Well.class);
				// Only show the Image menu entry when there is a Well Selection.
				if (selWell == null) return;
				manager.add(new ContributionItem() {
					@Override
					public void fill(Menu menu, int index) {
						final Menu imageMenu = new Menu(menu);
						imageMenu.addListener(SWT.Show, e -> {
							for (MenuItem item : imageMenu.getItems()) item.dispose();
							List<Well> wells = columnAccessor.getPossibleWells(selWell);
							final Well currentWell = columnAccessor.getCurrentWell(selWell);
							for (final Well well : wells) {
								MenuItem menuItem = new MenuItem(imageMenu, SWT.RADIO);
								menuItem.setText("Well " + NumberUtils.getWellCoordinate(well.getRow(), well.getColumn()));
								menuItem.addListener(SWT.Selection, event -> {
									columnAccessor.replaceCurrentUsedWell(well);
									selectionProvider.fireUpdatedSelection();
								});
								if (well.equals(currentWell)) menuItem.setSelection(true);
							}
						});

						new MenuItem(menu, SWT.SEPARATOR);
						MenuItem item = new MenuItem(menu, SWT.CASCADE);
						item.setMenu(imageMenu);
						item.setText("Select Well Image");
						item.setImage(IconManager.getIconImage("images.png"));
					}
					@Override
					public boolean isDynamic() {
						return true;
					}
				});
			}
		});

		NatTableBuilder<CompoundWithGrouping> builder = new NatTableBuilder<>(columnAccessor, gridInput.getGridCompounds());
		table = builder
				.resizeColumns(columnAccessor.getColumnWidths())
				.hideColumns(columnAccessor.getDefaultHiddenColumns())
				.addLinkedResizeSupport(1f, (w, h) -> loadWellImages(w, h), columnAccessor)
				.addCustomCellPainters(columnAccessor.getCustomPainters())
				.addConfiguration(columnAccessor.getCustomConfiguration())
				.addSelectionProvider(new CompoundImageSelectionTransformer(gridInput, columnAccessor))
				.build(this, false, menuMgr);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(table);

		selectionProvider = builder.getSelectionProvider();
		columnHeaderLayer = builder.getColumnHeaderLayer();

		columnAccessor.setTable(table);

		new CompoundImageToolTip(table, columnAccessor, selectionProvider.getRowDataProvider());

		selectedMatcher = row -> selectionProvider.getCurrentListselection().contains(row);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.KEYPAD_ADD) {
					columnAccessor.loadNextAvailableImage();
					selectionProvider.fireUpdatedSelection();
				}
				if (e.keyCode == SWT.KEYPAD_SUBTRACT) {
					columnAccessor.loadPreviousAvailableImage();
					selectionProvider.fireUpdatedSelection();
				}
			}
		});

		imageSettingListener = event -> {
			if (event.type == EventType.ImageSettingsChanged) {
				table.doCommand(new VisualRefreshCommand());
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingListener);

		JobUtils.runBackgroundJob(() -> {
			Display.getDefault().asyncExec(() -> {
				// SizeManager isn't supposed to be accessible, but getImageAspectRatio is so slow (TAU: > 4 sec)
				// it must be delayed until actually needed.
				SizeManager sm = (SizeManager) table.getData("sizeManager");
				if (sm != null) {
					float newAspectRatio = columnAccessor.getImageAspectRatio();
					if (Math.abs(sm.aspectRatio - newAspectRatio) > 0.001) {
						sm.aspectRatio = newAspectRatio;
						NatTableUtils.resizeColumn(table, columnAccessor.getColumnCount()-1, columnAccessor.getImageWidth());
					}
				}
			});
		});
	}

	private void loadWellImages(int w, int h) {
		columnAccessor.setImageSize(w-1, h-1);
	}

	public void startPreLoading() {
		columnAccessor.loadImagesJob();
	}

	public ISelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	public CompoundImageContentProvider getCompoundImageContentProvider() {
		return columnAccessor;
	}

	public NatTable getTable() {
		return table;
	}

	@Override
	public void dispose() {
		ProtocolUIService.getInstance().removeUIEventListener(imageSettingListener);
		table.dispose();
		super.dispose();
	}

	public void createButtons(ToolBar toolbar) {
		// Image channels
		ToolItem item = DropdownToolItemFactory.createDropdown(toolbar);
		item.setImage(IconManager.getIconImage("image.png"));
		item.setToolTipText("Select Image Channels");

		columnAccessor.fillChannelDropdown(item);

		new ToolItem(toolbar, SWT.SEPARATOR);

		item = new ToolItem(toolbar, SWT.CHECK);
		item.setImage(IconManager.getIconImage("wand.png"));
		item.setToolTipText("Show selected items / all items");
		item.addListener(SWT.Selection, e -> {
			isShowSelectedOnly = ((ToolItem) e.widget).getSelection();
			if (isShowSelectedOnly) {
				columnHeaderLayer.addStaticFilter(selectedMatcher);
			} else {
				columnHeaderLayer.removeStaticFilter(selectedMatcher);
			}
		});
		item.setSelection(isShowSelectedOnly);
	}

	public void preSelection() {
		if (isShowSelectedOnly) columnHeaderLayer.removeStaticFilter(selectedMatcher);
	}

	public void postSelection() {
		if (isShowSelectedOnly) columnHeaderLayer.addStaticFilter(selectedMatcher);
	}

}