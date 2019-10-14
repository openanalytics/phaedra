package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.SerializationUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.FeaturePlateLayer;

public abstract class WellChartLayer extends FeaturePlateLayer {

	public static final String PROPERTY_CONFIG = "layer_config";
	public static final String FILL_OPTION = "FILL_OPTION";
	private static final String CHART_LAYER_SETTINGS = "CHART_LAYER_SETTINGS";

	private ChartRenderer renderer;
	private LayerSettings<Plate, Well> config;
	private WellDataProvider dataProvider;
	private boolean dataLoadingJobFinished = true;
	private boolean requiresDataLoad = false;
	private long protocolId;

	private Job loadDataJob;

	@SuppressWarnings("unchecked")
	@Override
	protected void doInitialize() {
		protocolId = getEntities().get(0).getPlate().getExperiment().getProtocol().getId();
		dataProvider = new WellDataProvider(getLayerSupport().getDataFormatSupport());
		dataProvider.initialize();
		dataProvider.loadData(getEntities(), getDimensionCount());

		loadDataJob = new Job("Loading Well Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return loadData(getEntities(), monitor);
			}
		};

		Boolean isHidden = (Boolean) getLayerSupport().getAttribute(GridLayerSupport.IS_HIDDEN);
		if (isHidden == null || isHidden == false) {
			String fillOption = GridState.getStringValue(protocolId, getId(), FILL_OPTION);
			if (fillOption == null) {
				setFillOption(getDefaultFillOption());
			} else {
				setFillOption(fillOption);
			}
			config = (LayerSettings<Plate, Well>) GridState.getValue(protocolId, getId(), PROPERTY_CONFIG);
			if (config == null) {
				config = createConfig();
				List<String> features = new ArrayList<>();
				for (int i = 0; i < getDimensionCount(); i++) {
					features.add(getFeatures().get(i).getDisplayName());
				}
				config.getDataProviderSettings().setFeatures(features);

				configureDataProvider();
				requiresDataLoad = true;
				createConfigDialog(Display.getDefault().getActiveShell()).open();
			} else {
				updateDataProvider();
				triggerLoadDataJob();
			}
		}
	}

	private void configureDataProvider() {
		dataProvider.setDataProviderSettings(config.getDataProviderSettings());
	}

	void triggerLoadDataJob() {
		dataLoadingJobFinished = false;
		requiresDataLoad = false;
		loadDataJob.cancel();
		loadDataJob.schedule(100);
		loadDataJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult() != Status.CANCEL_STATUS) {
					dataLoadingJobFinished = true;
					Display.getDefault().asyncExec(() -> {
						if (!getLayerSupport().getViewer().getGrid().isDisposed())
							getRenderer().resetRendering();
							getLayerSupport().getViewer().getGrid().redraw();
					});
				}
			}
		});
	}

	/**
	 * Load plate data.
	 *
	 * @param plate
	 * @param monitor
	 * @return
	 */
	private IStatus loadData(List<Well> wells, final IProgressMonitor monitor) {
		configureDataProvider();

		dataProvider.loadData(wells, getDimensionCount(), monitor);

		monitor.done();
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	public abstract int getDimensionCount();

	@Override
	public void dispose() {
		if (renderer != null) renderer.resetRendering();
	}

	private class ChartRenderer extends BaseConcurrentGridCellRenderer {

		public ChartRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}

		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			if (cell.getData() == null) {
				return null;
			}

			WellChartRenderTask task = createRenderTask(cell, w, h);
			task.setConfig(config);
			return task;
		}

		@Override
		public void prerender(Grid grid) {
			if (isEnabled() && hasSelection() && dataLoadingJobFinished && config != null
					&& config.getDataProviderSettings().getFeatures() != null) {
				super.prerender(grid);
			}
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (isEnabled() && hasSelection() && dataLoadingJobFinished) {
				super.render(cell, gc, x, y, w, h);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setConfig(Object config) {
		renderer.resetRendering();
		Map<String, Object> configMap = (Map<String, Object>) config;
		setFillOption((String) configMap.get(FILL_OPTION));
		try {
			this.config = (LayerSettings<Plate, Well>) SerializationUtils.fromString((String) configMap.get(CHART_LAYER_SETTINGS));
		} catch (ClassNotFoundException | IOException e) {
			// Do nothing. Leave config in its current state (default).
		}
		protocolId = getEntities().get(0).getAdapter(Protocol.class).getId();
		GridState.saveValue(protocolId, getId(), PROPERTY_CONFIG, this.config);
	}

	public abstract WellChartRenderTask createRenderTask(GridCell cell, int w, int h);

	@Override
	public Object getConfig() {
		Map<String, Object> configMap = new HashMap<>();
		try {
			configMap.put(CHART_LAYER_SETTINGS, SerializationUtils.toString(config));
		} catch (IOException e) {
			// Failed to serialize config, return empty String.
			configMap.put(CHART_LAYER_SETTINGS, "");
		}
		configMap.put(FILL_OPTION, getFillOption());
		return configMap;
	}

	public LayerSettings<Plate, Well> getLayerSettings() {
		return config;
	}

	@Override
	public boolean isRendering() {
		return renderer.isRendering();
	}

	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new ChartRenderer();
		return renderer;
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	public abstract LayerSettings<Plate, Well> createConfig();

	public abstract void updateDataProvider();

	public BaseConcurrentGridCellRenderer getRenderer() {
		return renderer;
	}

	public void setRenderer(ChartRenderer renderer) {
		this.renderer = renderer;
	}

	public void setConfig(LayerSettings<Plate, Well> config) {
		this.config = config;
	}

	public WellDataProvider getDataProvider() {
		return dataProvider;
	}

	public boolean isDataLoadingJobFinished() {
		return dataLoadingJobFinished;
	}

	public boolean isRequiresDataLoad() {
		return requiresDataLoad;
	}

	public void setRequiresDataLoad(boolean requiresDataLoad) {
		this.requiresDataLoad = requiresDataLoad;
	}

	public long getProtocolId() {
		return protocolId;
	}

	public AbstractChartLayer<Plate, Well> getLegendLayer() {
		return new AbstractChartLayer<Plate, Well>(new Scatter2DChart<Plate, Well>(), new Scatter2DLegend<Plate, Well>(), dataProvider);
	}

}
