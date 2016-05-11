package eu.openanalytics.phaedra.ui.plate.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.ScrolledComposite2;
import eu.openanalytics.phaedra.base.ui.util.misc.ToolbarCombo;
import eu.openanalytics.phaedra.base.ui.util.misc.ToolbarCombo.ComboInitializer;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationUtils;

/**
 * A convenience class that wraps a set of plate heatmaps.
 */
public class PlateGrid extends Composite implements ISelectionProvider {

	private ToolBarManager toolBarManager;
	private ScrolledComposite2 scrollContainer;

	private ToolbarCombo gridsPerRowToolItem;
	private ToolbarCombo sortByToolItem;

	private int gridsPerRow;
	private boolean isFirst;

	private Map<String, Comparator<GridViewer>> sorters;
	private List<GridViewer> sortedOrder;

	private List<HeaderBar> headerBars;
	private List<GridViewer> gridViewers;

	public PlateGrid(Composite parent, int style) {
		super(parent, style);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(this);

		this.gridsPerRow = 3;
		this.isFirst = true;

		gridsPerRowToolItem = new ToolbarCombo("Items per row:", "itemsPerRow", new ComboInitializer() {
			@Override
			public void initialize(final CCombo combo) {
				combo.setItems(new String[]{"1","2","3","4","5","6","7","8","9","10","11","12","Automatic"});
				combo.addListener(SWT.Selection, e -> {
					String s = combo.getItem(combo.getSelectionIndex());
					if (NumberUtils.isDigit(s)) {
						gridsPerRow = Integer.parseInt(s);
					} else {
						gridsPerRow = -1;
					}
					adjustSize();
				});
				combo.select(gridsPerRow - 1);
			}
		});

		sortByToolItem = new ToolbarCombo("Sort by:", "sortBy", new ComboInitializer() {
			@Override
			public void initialize(final CCombo combo) {
				combo.addListener(SWT.Selection, e -> applyGridOrder());
			}
		});

		toolBarManager = new ToolBarManager(SWT.FLAT);
		toolBarManager.add(gridsPerRowToolItem);
		// Prevent the ToolBar from going to size 7 after a manager update.
		toolBarManager.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				final Image img = new Image(Display.getDefault(), 16, 16);
				ToolItem item = new ToolItem(parent, SWT.PUSH);
				item.setImage(img);
				item.setEnabled(false);
				item.addListener(SWT.Dispose, e -> img.dispose());
			}
		});
		toolBarManager.add(sortByToolItem);
		ToolBar toolbar = toolBarManager.createControl(this);
		GridDataFactory.fillDefaults().indent(5, 5).grab(true, false).hint(SWT.DEFAULT, 25).applyTo(toolbar);

		sorters = new HashMap<String, Comparator<GridViewer>>();
		sortedOrder = new ArrayList<>();
		addSorter("", null);

		Label lbl = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(lbl);

		scrollContainer = new ScrolledComposite2(this, style | SWT.V_SCROLL);
		scrollContainer.getVerticalBar().setIncrement(10);
		scrollContainer.getVerticalBar().setPageIncrement(200);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(scrollContainer);

		headerBars = new ArrayList<HeaderBar>();
		gridViewers = new ArrayList<GridViewer>();

		GridLayoutFactory.fillDefaults().numColumns(gridsPerRow == -1 ? 1 : gridsPerRow).margins(5,5).applyTo(scrollContainer);
		addListener(SWT.Resize, e -> adjustSize());

		toolBarManager.getControl().layout();
	}

	@Override
	public boolean setFocus() {
		return scrollContainer.setFocus();
	}

	public void addSorter(String name, Comparator<GridViewer> comparator) {
		sorters.put(name, comparator);

		String[] names = sorters.keySet().toArray(new String[sorters.size()]);
		Arrays.sort(names);

		if (sortByToolItem.getCombo().getSelectionIndex() == -1) {
			// Select first item.
			sortByToolItem.getCombo().setItems(names);
			sortByToolItem.getCombo().select(0);
		} else {
			// Just retain selection.
			int selectedIndex = CollectionUtils.find(names, sortByToolItem.getCombo().getItem(sortByToolItem.getCombo().getSelectionIndex()));
			sortByToolItem.getCombo().setItems(names);
			sortByToolItem.getCombo().select(selectedIndex);
		}
	}

	public GridViewer createGridViewer(Plate plate) {

		String name = getPlateTitle(plate);
		int rows = plate.getRows();
		int columns = plate.getColumns();
		Image statusIcon = getPlateStatusIcon(plate);

		Composite c = new Composite(scrollContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(c);
		GridLayoutFactory.fillDefaults().margins(0,0).spacing(0,1).applyTo(c);

		final HeaderBar headerBar = new HeaderBar(c, SWT.NONE);
		headerBar.setText(name);
		headerBar.setIcon(IconManager.getIconImage("plate.png"));
		headerBar.setStatus(statusIcon);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(headerBar);
		headerBars.add(headerBar);

		final GridViewer gridViewer = new GridViewer(c, rows, columns);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(gridViewer.getControl());
		gridViewer.getGrid().setShowHeaders(false);
		gridViewer.getGrid().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				activateViewer(gridViewer);
				fireSelectionEvent(gridViewer);
			}
		});
		gridViewer.getGrid().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				fireSelectionEvent(gridViewer);
			}
		});
		gridViewers.add(gridViewer);
		sortedOrder.add(gridViewer);

		return gridViewer;
	}

	public GridViewer getGridViewer(int index) {
		if (index >= gridViewers.size()) return null;
		return gridViewers.get(index);
	}

	public ToolBarManager getToolBarManager() {
		return toolBarManager;
	}

	public Composite getContainer() {
		return scrollContainer;
	}

	@Override
	public void setBackground(Color color) {
		scrollContainer.setBackground(color);
		for (GridViewer gridViewer : gridViewers)
			gridViewer.getGrid().setBackground(color);
	}

	public void refreshSorting() {
		applyGridOrder();
	}

	public void refreshHeaders() {
		for (int i=0; i<headerBars.size(); i++) {
			Plate plate = (Plate)gridViewers.get(i).getInput();
			if (plate == null) continue;
			String name = plate.getSequence() + " - " + plate.getBarcode();
			headerBars.get(i).setText(name);
			headerBars.get(i).setStatus(IconManager.getIconImage(ValidationUtils.getIcon(PlateValidationStatus.getByCode(plate.getValidationStatus()))));
			headerBars.get(i).layout();
		}
	}

	public HashMap<String, Object> getConfig() {
		HashMap<String, Object> config = new HashMap<>();
		config.put("COLUMNS", gridsPerRow);
		config.put("SORT", sortByToolItem.getCombo().getSelectionIndex());
		return config;
	}

	public void setConfig(HashMap<String, Object> config) {
		if (config.get("COLUMNS") != null) {
			gridsPerRow = (int) config.get("COLUMNS");
			if (gridsPerRow == -1) gridsPerRowToolItem.getCombo().select(gridsPerRowToolItem.getCombo().getItemCount() - 1);
			else gridsPerRowToolItem.getCombo().select(gridsPerRow-1);
			adjustSize();
		}
		if (config.get("SORT") != null) {
			sortByToolItem.getCombo().select((int) config.get("SORT"));
			applyGridOrder();
		}
	}

	private void adjustSize() {
		if (scrollContainer == null) return;

		float aspectRatio = 4f/3f;
		int widthPadding = 10;
		int width = getSize().x;

		int nrOfCols;
		int widthPerGrid = 0;
		if (gridsPerRow == -1) {
			nrOfCols = Math.max(1, (int) Math.round(Math.sqrt(gridViewers.size())));
			while (widthPerGrid < 96) {
				if (nrOfCols == 1) break;
				if (widthPerGrid > 0) --nrOfCols;
				widthPerGrid = (width - nrOfCols*widthPadding) / nrOfCols;
				if (widthPerGrid < 1) break;
			}
		} else {
			nrOfCols = gridsPerRow;
			widthPerGrid = (width - nrOfCols*widthPadding) / nrOfCols;
		}

		int heightPerGrid = (int)(widthPerGrid / aspectRatio);
		int rows = gridViewers.size() / nrOfCols;
		if (gridViewers.size() % nrOfCols != 0) rows++;
		int height = rows * heightPerGrid;

		scrollContainer.setTotalHeight(height);

		int currentCols = ((GridLayout)scrollContainer.getLayout()).numColumns;
		if (isFirst || nrOfCols != currentCols) {
			isFirst = false;
			GridLayoutFactory.fillDefaults().numColumns(nrOfCols).margins(5,5).applyTo(scrollContainer);
			scrollContainer.layout();
		}
	}

	private void applyGridOrder() {
		if (sortByToolItem.getCombo().getSelectionIndex() == -1) return;
		if (gridViewers.isEmpty()) return;

		String s = sortByToolItem.getCombo().getItem(sortByToolItem.getCombo().getSelectionIndex());
		Comparator<GridViewer> sorter = sorters.get(s);
		if (sorter == null) return;

		Collections.sort(sortedOrder, sorter);

		// Re-arrange the grid widgets.
		Collections.reverse(sortedOrder);
		for (GridViewer gv: sortedOrder) {
			// Move that viewer to the top.
			Composite c = gv.getGrid().getParent();
			c.moveAbove(null);
		}
		Collections.reverse(sortedOrder);

		scrollContainer.layout();
	}

	/*
	 * Selection provider
	 * ******************
	 */

	private ListenerList listeners = new ListenerList();
	private GridViewer activeViewer;

	private void fireSelectionEvent(GridViewer viewer) {
		if (activeViewer == null) return;
		SelectionChangedEvent event = new SelectionChangedEvent(this, activeViewer.getSelection());
		for (Object l: listeners.getListeners()) {
			((ISelectionChangedListener)l).selectionChanged(event);
		}
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		if (activeViewer == null) return StructuredSelection.EMPTY;
		return activeViewer.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		// Find out which viewer has the appropriate plate
		Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
		if (plate == null) return;
		for (GridViewer v: gridViewers) {
			if (v.getInput() == plate) activateViewer(v);
		}
	}

	private void activateViewer(GridViewer viewer) {
		if (activeViewer == viewer) return;
		activeViewer = viewer;
		for (int i=0; i<gridViewers.size(); i++) {
			GridViewer v = gridViewers.get(i);
			if (v == activeViewer) {
				headerBars.get(i).setSelected(true);
			} else {
				v.setSelection(StructuredSelection.EMPTY);
				headerBars.get(i).setSelected(false);
			}
		}
	}

	private String getPlateTitle(Plate plate) {
		return plate.getSequence() + " - " + plate.getBarcode();
	}

	private Image getPlateStatusIcon(Plate plate) {
		String icon = ValidationUtils.getIcon(PlateValidationStatus.getByCode(plate.getValidationStatus()));
		return IconManager.getIconImage(icon);
	}

}