package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.config.MultiFeatureConfig;

public class WellSpiderLayer extends PlatesLayer {

	private PlateDataAccessor dataAccessor;
	private List<Feature> allFeatures;
	private List<Feature> features;

	private long protocolId;
	private boolean initialized;

	private BaseConcurrentGridCellRenderer renderer;
	
	@Override
	public String getName() {
		return "Well Spider Plot";
	}

	@Override
	protected void doInitialize() {
		dataAccessor = CalculationService.getInstance().getAccessor(getPlate());

		allFeatures = PlateUtils.getFeatures(dataAccessor.getPlate());
		allFeatures = CollectionUtils.findAll(allFeatures, ProtocolUtils.KEY_FEATURES);
		allFeatures = CollectionUtils.findAll(allFeatures, ProtocolUtils.NUMERIC_FEATURES);

		protocolId = dataAccessor.getPlate().getExperiment().getProtocol().getId();

		if (features == null || features.isEmpty()) {
			features = new ArrayList<Feature>();
			insertDefaults();
		}
		if(features.isEmpty()) features = CollectionUtils.findAll(allFeatures, ProtocolUtils.KEY_FEATURES);
		initialized = true;
	}

	private void insertDefaults() {
		int featureCount = allFeatures.size();
		for (int i = 0; i < featureCount; i++) {
			String setting = GridState.getStringValue(protocolId, getId(), i + "");
			if (setting != "" && setting != null)
				features.add(allFeatures.get(Integer.parseInt(setting)));
		}
	}

	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new ChartRenderer();
		return renderer;
	}

	@Override
	public void dispose() {
		if (renderer != null) renderer.resetRendering();
	}

	private void doApplySettings(boolean[] enabledFeatures) {
		features.clear();
		for (int i = 0; i < allFeatures.size(); i++) {
			if (enabledFeatures[i] && i > 0) {
				features.add(allFeatures.get(i));
				GridState.saveValue(protocolId, getId(), i + "", i + "");
			} else {
				GridState.removeValue(protocolId, getId(), i + "");
			}

		}
		renderer.resetRendering();
	}

	@Override
	public void setConfig(Object config) {
		renderer.resetRendering();
		features = ((MultiFeatureConfig)config).getFeatures();
	}

	@Override
	public Object getConfig() {
		return new MultiFeatureConfig(features);
	}

	private class ChartRenderer extends BaseConcurrentGridCellRenderer{

		public ChartRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}

		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {

			Well well = (Well)cell.getData();
			if (well == null) return null;

			WellSpiderRenderTask task = new WellSpiderRenderTask(well, w, h, cell.getRow(), cell.getColumn());
			task.setFeatures(features);
			return task;
		}

		@Override
		public void prerender(Grid grid) {
			if (!isEnabled() || !hasPlates()) return;
			if (!initialized) return;

			super.prerender(grid);
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;
			if (!initialized) return;

			Well well = (Well)cell.getData();
			if (well == null) return;

			super.render(cell, gc, x, y, w, h);
		}
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ConfigDialog(shell);
	}

	private class ConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

		private CheckboxTableViewer featureTable;

		private boolean[] selectedFeatures;

		public ConfigDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Configuration: " + getName());
		}

		@Override
		protected Point getInitialSize() {
			return new Point(400, 400);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			renderer.resetRendering();

			Composite area = (Composite) super.createDialogArea(parent);
			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

			featureTable = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			featureTable.getTable().setLinesVisible(true);
			featureTable.getTable().setHeaderVisible(true);
			featureTable.setContentProvider(new ArrayContentProvider());
			featureTable.setInput(allFeatures);
			featureTable.setAllChecked(false);
			createColumns();
			GridDataFactory.fillDefaults().span(2,1).align(SWT.FILL,SWT.FILL).grab(true,true).applyTo(featureTable.getTable());

			if (features != null && features.size() != 0) {
				for (int i = 0; i < features.size(); i++) {
					featureTable.setChecked(features.get(i), true);
				}
			} else {
				featureTable.setAllChecked(true);
			}

			setTitle(getName());
			setMessage("Select the features to display.");
			return area;
		}

		@Override
		protected void okPressed() {
			selectedFeatures = new boolean[allFeatures.size()];
			for (int i = 0; i < allFeatures.size(); i++) {
				selectedFeatures[i] = featureTable.getChecked(allFeatures.get(i));
			}
			applySettings(getLayerSupport().getViewer(), WellSpiderLayer.this);
			super.okPressed();
		}

		@Override
		public void applySettings(GridViewer viewer, IGridLayer layer) {
			((WellSpiderLayer)layer).doApplySettings(selectedFeatures);
			viewer.getGrid().redraw();
		}

		private void createColumns() {

			TableViewerColumn column = new TableViewerColumn(featureTable, SWT.BORDER);
			column.getColumn().setWidth(200);
			column.getColumn().setText("Feature");
			column.getColumn().setAlignment(SWT.CENTER);
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Feature)element).getName();
				}
			});

			column = new TableViewerColumn(featureTable, SWT.BORDER);
			column.getColumn().setWidth(200);
			column.getColumn().setText("Description");
			column.getColumn().setAlignment(SWT.LEFT);
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Feature)element).getDescription();
				}
			});
		}
	}
}
