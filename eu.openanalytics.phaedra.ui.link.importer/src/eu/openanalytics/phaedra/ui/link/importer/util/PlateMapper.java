package eu.openanalytics.phaedra.ui.link.importer.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import eu.openanalytics.phaedra.base.ui.util.table.TableViewerSorter;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

/**
 * Widget that allows plate mapping.
 * Plate mapping in this context means making an association
 * between plate folders on a disk and plate objects in Phaedra.
 */
public class PlateMapper extends Composite {

	private TableViewer leftTableViewer;
	private TableViewer rightTableViewer;
		
	private Button resetBtn;
	private Button mapAllBtn;
	private Button undoMapBtn;

	private PlateMapListenerManager listenerManager;
	
	private Map<Plate, PlateReading> mapping;
	
	public PlateMapper(Composite parent, int style) {
		super(parent, style);
		
		listenerManager = new PlateMapListenerManager();
		mapping = new HashMap<Plate, PlateReading>();
		
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(this);
		
		Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Readings:");
		
		lbl = new Label(this, SWT.NONE);
		lbl.setText("Plates:");
		
		Table leftTable = new Table(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		leftTable.setHeaderVisible(true);
		leftTable.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(leftTable);
		leftTableViewer = new TableViewer(leftTable);		
		leftTableViewer.setContentProvider(new ArrayContentProvider());
		
		TableViewerColumn col = new TableViewerColumn(leftTableViewer, SWT.NONE);
		col.getColumn().setText("Reading");
		col.getColumn().setWidth(100);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				PlateReading reading = (PlateReading)cell.getElement();
				cell.setText(reading.getBarcode());
			}
		});
		new TableViewerSorter(leftTableViewer, col){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null || ((PlateReading)e1).getBarcode() == null) return -1;
				if (e2 == null || ((PlateReading)e2).getBarcode() == null) return 1;
				return ((PlateReading)e1).getBarcode().compareTo(((PlateReading)e2).getBarcode());
			}
		};
		
		col = new TableViewerColumn(leftTableViewer, SWT.NONE);
		col.getColumn().setText("Location");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				PlateReading reading = (PlateReading)cell.getElement();
				cell.setText(reading.getSourcePath());
			}
		});
		new TableViewerSorter(leftTableViewer, col){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null || ((PlateReading)e1).getSourcePath() == null) return -1;
				if (e2 == null || ((PlateReading)e2).getSourcePath() == null) return 1;
				return ((PlateReading)e1).getSourcePath().compareTo(((PlateReading)e2).getSourcePath());
			}
		};
		
		Table rightTable = new Table(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		rightTable.setHeaderVisible(true);
		rightTable.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(rightTable);
		rightTableViewer = new TableViewer(rightTable);
		rightTableViewer.setContentProvider(new ArrayContentProvider());
		
		col = new TableViewerColumn(rightTableViewer, SWT.NONE);
		col.getColumn().setText("Barcode");
		col.getColumn().setWidth(100);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Plate plate = (Plate)cell.getElement();
				cell.setText(plate.getBarcode());
			}
		});
		new TableViewerSorter(rightTableViewer, col){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null || ((Plate)e1).getBarcode() == null) return -1;
				if (e2 == null || ((Plate)e2).getBarcode() == null) return 1;
				return ((Plate)e1).getBarcode().compareTo(((Plate)e2).getBarcode());
			}
		};
		
		col = new TableViewerColumn(rightTableViewer, SWT.NONE);
		col.getColumn().setText("Mapped to Reading");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Plate plate = (Plate)cell.getElement();
				PlateReading reading = mapping.get(plate);
				if (reading != null) cell.setText(reading.getBarcode());
				else cell.setText("");
			}
		});
		new TableViewerSorter(rightTableViewer, col){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null) return -1;
				if (e2 == null) return 1;
				PlateReading reading1 = mapping.get((Plate)e1);
				PlateReading reading2 = mapping.get((Plate)e2);
				if (reading1 == null || reading1.getBarcode() == null) return -1;
				if (reading2 == null || reading2.getBarcode() == null) return 1;
				return reading1.getBarcode().compareTo(reading2.getBarcode());
			}
		};
		
		// Drag & drop
		
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DragSource dragSource = new DragSource(leftTable, DND.DROP_MOVE | DND.DROP_COPY);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(leftTableViewer.getSelection());
			}
		});

		DropTarget dropTarget = new DropTarget(rightTable, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
		dropTarget.setTransfer(types);
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dropAccept(DropTargetEvent event) {
				TableItem targetItem = (TableItem)event.item;
				if (targetItem != null) {
					IStructuredSelection sel = (IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
					PlateReading reading = (PlateReading)sel.getFirstElement();
					mapItem(reading, targetItem);
				}
			}
		});
		
		Composite buttonContainer = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().span(2,1).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(buttonContainer);
		
		resetBtn = new Button(buttonContainer, SWT.PUSH);
		resetBtn.setText("Reset");
		resetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				reset();
			}
		});
		
		mapAllBtn = new Button(buttonContainer, SWT.PUSH);
		mapAllBtn.setText("Auto Map");
		mapAllBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mapAll();
			}
		});
		
		undoMapBtn = new Button(buttonContainer, SWT.PUSH);
		undoMapBtn.setText("Undo Selected Mapping(s)");
		undoMapBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				undoMapping();
			}
		});
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		leftTableViewer.getTable().setEnabled(enabled);
		rightTableViewer.getTable().setEnabled(enabled);
		resetBtn.setEnabled(enabled);
		mapAllBtn.setEnabled(enabled);
		undoMapBtn.setEnabled(enabled);
		super.setEnabled(enabled);
	}
	
	public void setReadings(PlateReading[] sources) {
		leftTableViewer.setInput(sources);
	}
	
	public void setPlates(Plate[] plates) {
		rightTableViewer.setInput(plates);
	}
	
	public Map<PlateReading, Plate> getMapping() {
		Map<PlateReading, Plate> sourceMapping = new HashMap<PlateReading, Plate>();
		Plate[] plates = (Plate[])rightTableViewer.getInput();
		if (plates != null) {
			for (int i=0; i<plates.length; i++) {
				PlateReading source = mapping.get(plates[i]);
				if (source != null) {
					sourceMapping.put(source, plates[i]);
				}
			}
		}
		return sourceMapping;
	}
	
	public void mapAll() {
		// Try to map all sources by matching their names to the plate barcodes.
		PlateReading[] sources = (PlateReading[])leftTableViewer.getInput();
		Plate[] plates = (Plate[])rightTableViewer.getInput();
		for (PlateReading source: sources) {
			for (int i=0; i<plates.length; i++) {
				Plate plate = plates[i];
				if (mapping.get(plate) == null
						&& (source.getBarcode().toLowerCase()).equals(plate.getBarcode().toLowerCase())) {
					mapping.put(plate, source);
				}
			}
		}
		rightTableViewer.refresh();
	}

	public void addListener(IPlateMapListener listener) {
		listenerManager.addListener(listener);
	}
	
	public void removeListener(IPlateMapListener listener) {
		listenerManager.removeListener(listener);
	}
	
	/*
	 * ******************
	 * Non-public helpers
	 * ******************
	 */
	
	private void reset() {
		mapping.clear();
		rightTableViewer.refresh();
	}
	
	private void mapItem(PlateReading source, TableItem target) {
		Plate plate = (Plate)target.getData();
		mapping.put(plate, source);
		rightTableViewer.refresh();
		listenerManager.notifyPlateMapped(source, plate);
	}
	
	private void undoMapping() {
		StructuredSelection sel = (StructuredSelection)rightTableViewer.getSelection();
		for (Iterator<?> it = sel.iterator(); it.hasNext();) {
			Plate plate = (Plate)it.next();
			mapping.remove(plate);
		}
		rightTableViewer.refresh();
	}
	
	private class PlateMapListenerManager extends EventManager {
		
		public void addListener(IPlateMapListener listener) {
			addListenerObject(listener);
		}
		
		public void removeListener(IPlateMapListener listener) {
			removeListenerObject(listener);
		}
		
		public void notifyPlateMapped(PlateReading source, Plate plate) {
			for (Object listener: getListeners()) {
				IPlateMapListener l = (IPlateMapListener)listener;
				l.plateMapped(source, plate);
			}
		}
	}
}
