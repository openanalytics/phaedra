package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseClickAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.misc.AsyncColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.DefaultMouseClickAction;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.ILinkedColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NatTableToolTip.ITooltipColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.ProgressCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionManager.SelectedCell;
import eu.openanalytics.phaedra.base.ui.nattable.selection.SelectionTransformer;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateSummary;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowseWells;
import eu.openanalytics.phaedra.ui.plate.table.NatTableStatePersister;
import eu.openanalytics.phaedra.ui.plate.util.HeatmapImageFactory;
import eu.openanalytics.phaedra.ui.plate.util.HeatmapImageFactory.HeatmapConfig;
import eu.openanalytics.phaedra.ui.plate.util.PlateSummaryWithStats;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateUploadStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class PlateFeatureHeatmapsTab extends Composite {

	private NatTable table;
	private ColumnAccessor columnAccessor;
	private MenuManager menuMgr;

	private List<Plate> plates;
	private List<Feature> features;
	private List<String> normalizations;

	private IUIEventListener uiListener;
	private IModelEventListener colorMethodListener;
	private SelectionProviderIntermediate selectionProvider;

	public PlateFeatureHeatmapsTab(Composite parent, int style, List<Plate> plates) {
		super(parent, style);

		this.plates = plates;
		this.features = new ArrayList<>();
		this.normalizations = new ArrayList<>();

		GridLayoutFactory.fillDefaults().applyTo(this);

		menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		selectionProvider = new SelectionProviderIntermediate();
		
		// Listen to FeatureGroup, Normalization and PlateExpLimit selections
		uiListener = e -> {
			if (table == null || plates.isEmpty()) return;
			
			// Ignore events from other protocol classes
			ProtocolClass pc = ProtocolUtils.getProtocolClass(plates.get(0));
			if (!pc.equals(ProtocolUIService.getInstance().getCurrentProtocolClass())) return;

			if (e.type == UIEvent.EventType.FeatureGroupSelectionChanged) {
				rebuildTable();
				loadHeatmaps(-1, -1, -1);
			} else if (e.type == UIEvent.EventType.NormalizationSelectionChanged) {
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				String norm = ProtocolUIService.getInstance().getCurrentNormalization();
				int featureIndex = features.indexOf(f);
				if (featureIndex != -1 && !normalizations.get(featureIndex).equals(norm)) {
					normalizations.set(featureIndex, norm);
					table.refresh();
					loadHeatmaps(-1, -1, columnAccessor.getColumnIndex(f.getDisplayName()));
				}
			} else if (e.type == UIEvent.EventType.ColorMethodChanged) {
				table.refresh();
				loadHeatmaps(-1, -1, -1);
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(uiListener);

		// Listen to color method changes
		colorMethodListener = event -> {
			if (table == null || features.isEmpty()) return;
			ProtocolClass pc = features.get(0).getProtocolClass();
			if (event.type == ModelEventType.ObjectChanged && pc.equals(event.source)) {
				table.refresh();
				loadHeatmaps(-1, -1, -1);
			}
		};
		ModelEventService.getInstance().addEventListener(colorMethodListener);
	}

	@Override
	public void dispose() {
		ProtocolUIService.getInstance().removeUIEventListener(uiListener);
		ModelEventService.getInstance().removeEventListener(colorMethodListener);
		if (columnAccessor != null) columnAccessor.dispose();
		super.dispose();
	}

	public MenuManager getMenuMgr() {
		return menuMgr;
	}

	public ISelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	public void startLoading() {
		rebuildTable();
	}

	/*
	 * Non-public
	 * **********
	 */

	private void rebuildTable() {
		features = ProtocolService.getInstance().getMembers(ProtocolUIService.getInstance().getCurrentFeatureGroup());
		normalizations = features.stream().map(f -> f.getNormalization()).collect(Collectors.toList());
		
		if (table != null && !table.isDisposed()) table.dispose();
		
		float aspectRatio = plates.isEmpty() ? 4.0f/3.0f : ((float)plates.get(0).getColumns()) / plates.get(0).getRows();
		if (aspectRatio == 1.5f) aspectRatio = 4.0f/3.0f;
		
		columnAccessor = new ColumnAccessor();
		NatTableBuilder<Plate> builder = new NatTableBuilder<Plate>(columnAccessor, plates)
				.addSelectionProvider(new SelectionTransformer<Plate>(Plate.class))
				.addSelectionListener(sel -> selectFeature(sel))
				.addCustomCellPainters(columnAccessor.getCustomPainters())
				.addConfiguration(createMouseActions())
				.addLinkedResizeSupport(aspectRatio, (w, h) -> loadHeatmaps(w, h, -1), columnAccessor)
				.addPersistentStateSupport(getTablePersistenceKey(), new NatTableStatePersister())
				.resizeColumns(columnAccessor.getColumnWidths());

		table = builder.build(this, true, menuMgr);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(table);
		this.layout();
		
		columnAccessor.setTable(table);
		columnAccessor.start(plates);
		
		selectionProvider.setSelectionProviderDelegate(builder.getSelectionProvider());
		
		// Refresh the table and apply the configured columns widths.
		table.refresh();
		NatTableUtils.resizeColumns(table, columnAccessor.getColumnWidths());
	}
	
	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator("plateMenu"));
	}

	private void selectFeature(SelectedCell[] selection) {
		if (selection.length == 0) return;
		int index = selection[selection.length-1].columnIndex;
		//Fix: do not fire a feature selection if this table is not active.
		if (table.isFocusControl()) {
			Feature f = columnAccessor.getFeature(index);
			ProtocolUIService.getInstance().setCurrentFeature(f);
		}
	}

	private AbstractUiBindingConfiguration createMouseActions() {
		IMouseClickAction doubleClickAction = new DefaultMouseClickAction() {
			@Override
			public void run(NatTable table, MouseEvent event) {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				ISelection selection = page.getSelection();
				Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
				BrowseWells.execute(plate);
			}
		};
		return new AbstractUiBindingConfiguration() {
			@Override
			public void configureUiBindings(UiBindingRegistry reg) {
				reg.registerDoubleClickBinding(MouseEventMatcher.bodyLeftClick(SWT.NONE), doubleClickAction);
			}
		};
	}

	private String getTablePersistenceKey() {
		String key = this.getClass().getSimpleName();
		if (!plates.isEmpty()) key += "#pclass" + ProtocolUtils.getProtocolClass(plates.get(0)).getId();
		return key;
	}

	private void loadHeatmaps(int w, int h, int colIndex) {
		// Leave one pixel for the border.
		if (w > 0 && h > 0) columnAccessor.currentHeatmapSize = new Point(w-1, h-1);
		if (colIndex == -1) columnAccessor.reset();
		else columnAccessor.reset(colIndex);
	}

	private class ColumnAccessor extends AsyncColumnAccessor<Plate> implements ILinkedColumnAccessor<Plate>, ITooltipColumnAccessor<Plate> {

		private AsyncDataLoader<Plate> dataLoader;
		private AsyncDataLoader<Plate>.DataAccessor<PlateSummary> summaryAccessor;
		
		private int baseColumnCount = 7;
		
		private Point currentHeatmapSize = new Point(0,0);
		
		
		public ColumnAccessor() {
			// Pass null because the DataLoader shouldn't do refreshes; AsyncColumnAccessor takes care of that.
			this.dataLoader = new AsyncDataLoader<Plate>("data for plate browser");
			this.summaryAccessor = this.dataLoader.addDataRequest((plate) -> PlateSummaryWithStats.loadSummary(plate));
		}
		
		
		@Override
		public int getColumnCount() {
			return baseColumnCount + 2*features.size();
		}
		
		@Override
		public Object getDataValue(Plate plate, int index) {
			if (index == 0) return plate.getBarcode();
			if (index == 1) return plate.getValidationStatus();
			if (index == 2) return plate.getApprovalStatus();
			if (index == 3) return plate.getUploadStatus();
			if (index == 4) {
				final Object value = summaryAccessor.getData(plate);
				return (value instanceof DataLoadStatus) ? value : ((PlateSummary)value).crcCount;
			}
			if (index == 5) {
				final Object value = summaryAccessor.getData(plate);
				return (value instanceof DataLoadStatus) ? value : ((PlateSummary)value).screenCount;
			}
			return super.getDataValue(plate, index);
		}

		@Override
		protected Object loadDataValue(Plate plate, int index) {
			if (index == 6) {
				HeatmapConfig cfg = new HeatmapConfig(plate, null, currentHeatmapSize.x, currentHeatmapSize.y);
				cfg.welltypeMap = true;
				return HeatmapImageFactory.drawHeatmap(cfg);
			}
			if (index > 6) {
				int featureIndex = (index - baseColumnCount)/2;
				Feature f = features.get(featureIndex);
				if (index%2 == 0) {
					HeatmapConfig cfg = new HeatmapConfig(plate, f, currentHeatmapSize.x, currentHeatmapSize.y);
					cfg.normalization = normalizations.get(featureIndex);
					cfg.experimentLimit = ProtocolUIService.getInstance().isExperimentLimit();
					return HeatmapImageFactory.drawHeatmap(cfg);
				}
				else return StatService.getInstance().calculate("zprime", plate, f, null, f.getNormalization());
			}
			return null;
		}

		@Override
		public String getColumnProperty(int index) {
			if (index == 0) return "Barcode";
			if (index == 1) return "V";
			if (index == 2) return "A";
			if (index == 3) return "U";
			if (index == 4) return "#DRC";
			if (index == 5) return "#SDP";
			if (index == 6) return "Welltypes";
			else {
				Feature f = features.get((index - baseColumnCount)/2);
				if (index%2 == 0) return f.getDisplayName();
				else return f.getDisplayName() + "\nZ-Prime";
			}
		}

		@Override
		public int getColumnIndex(String colName) {
			for (int i=0; i<getColumnCount(); i++) {
				if (colName.equals(getColumnProperty(i))) return i;
			}
			return -1;
		}

		@Override
		public int[] getLinkedColumns() {
			int[] heatmapColumns = new int[features.size() + 1];
			heatmapColumns[heatmapColumns.length-1] = 6;
			for (int i=0; i<features.size(); i++) heatmapColumns[i] = baseColumnCount+1+i*2;
			return heatmapColumns;
		}

		@Override
		public String getTooltipText(Plate plate, int colIndex) {
			String[] headerTooltips = { null, "Validation Status", "Approval Status", "Upload Status", "#Dose-Response Curves", "#Single-Dose Points" };
			if (plate == null && colIndex < headerTooltips.length) {
				return headerTooltips[colIndex];
			}
			if (plate != null) {
				if (colIndex == 1) return PlateValidationStatus.getByCode(plate.getValidationStatus()).toString();
				if (colIndex == 2) return PlateApprovalStatus.getByCode(plate.getApprovalStatus()).toString();
				if (colIndex == 3) return PlateUploadStatus.getByCode(plate.getUploadStatus()).toString();
			}
			return null;
		}

		private void start(List<Plate> rowObjects) {
			this.dataLoader.asyncLoad(rowObjects, null);
		}

		private int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			columnWidths[0] = 120;
			columnWidths[1] = 20;
			columnWidths[2] = 20;
			columnWidths[3] = 20;
			columnWidths[4] = 35;
			columnWidths[5] = 35;
			columnWidths[6] = 52;
			for (int i=0; i<features.size(); i++) {
				columnWidths[baseColumnCount+i*2] = 60;
				columnWidths[baseColumnCount+1+i*2] = 52;
			}
			return columnWidths;
		}

		private Feature getFeature(int columnIndex) {
			if (columnIndex < baseColumnCount) return null;
			return features.get((columnIndex - baseColumnCount)/2);
		}

		private Map<int[], AbstractCellPainter> getCustomPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			int[] flagColumns = new int[] {1, 2, 3};
			painters.put(flagColumns, new FlagCellPainter());

			int[] progressColumns = new int[features.size()];
			for (int i=0; i<features.size(); i++) progressColumns[i] = baseColumnCount+i*2;
			painters.put(progressColumns, new ProgressCellPainter());

			return painters;
		}
	}
}
