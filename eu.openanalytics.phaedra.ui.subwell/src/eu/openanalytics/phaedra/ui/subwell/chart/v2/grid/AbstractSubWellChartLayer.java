package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.SerializationUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.data.SubWellDataProvider;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.render.SubWellChartRenderTask;

@SuppressWarnings({ "unchecked" })
public abstract class AbstractSubWellChartLayer extends PlatesLayer {

	public static final String PROPERTY_CONFIG = "layer_config";

	private ChartRenderer renderer;
	private Map<String, double[][]> plateBoundsMap;
	private SubWellDataProvider dataProvider;
	private List<LayerSettings<Well, Well>> configs;
	private boolean dataLoadingJobFinished = false;
	private long protocolId;

	private Job loadDataJob;

	public AbstractSubWellChartLayer() {
		loadDataJob = new Job("Loading Subwell Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return loadData(monitor);
			}
		};
		loadDataJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult() != Status.CANCEL_STATUS) {
					dataLoadingJobFinished = true;
					Display.getDefault().asyncExec(() -> {
						if (!getLayerSupport().getViewer().getGrid().isDisposed()) {
							getRenderer().resetRendering();
							getLayerSupport().getViewer().getGrid().redraw();
						}
					});
				}
			}
		});
	}

	@Override
	protected void doInitialize() {
		protocolId = getPlate().getExperiment().getProtocol().getId();

		Boolean isHidden = (Boolean) getLayerSupport().getAttribute(GridLayerSupport.IS_HIDDEN);
		if (isHidden == null || isHidden == false) {
			if (configs == null) configs = (List<LayerSettings<Well, Well>>) GridState.getValue(protocolId, getId(), PROPERTY_CONFIG);
			if (configs == null) {
				configs = Collections.synchronizedList(new ArrayList<>());
				GridState.saveValue(protocolId, getId(), PROPERTY_CONFIG, configs);
				createConfigDialog(Display.getDefault().getActiveShell()).open();
			} else {
				updateDataProvider();
				triggerLoadDataJob();
			}
		}
	}

	void triggerLoadDataJob() {
		dataLoadingJobFinished = false;
		loadDataJob.cancel();
		loadDataJob.schedule(100);
	}

	/**
	 * load plate data and calculate the plate limits to pass to individual
	 * cells afterwards.
	 *
	 * @param plate
	 * @param monitor
	 * @return
	 */
	private IStatus loadData(final IProgressMonitor monitor) {
		getRenderer().resetRendering();
		Object input = getCurrentInput();
		List<Well> wells = getWells(input);

		if (monitor.isCanceled()) return Status.CANCEL_STATUS;

		// Get the chart dimension count.
		int dimensionCount = getDimensionCount();
		dataProvider = new SubWellDataProvider();
		dataProvider.initialize();
		dataProvider.loadData(wells, dimensionCount, monitor);

		plateBoundsMap = new HashMap<>();
		for (LayerSettings<Well, Well> config : configs) {
			dataProvider.setDataProviderSettings(config.getDataProviderSettings());
			// All wells were given as input, so it will be the limits of all shown wells (which can be the same as the plate limits).
			dataProvider.setUsePlateLimits(false); // config.getChartSettings().isUsePlateLimits()
			dataProvider.setDataBounds(null);

			double[][] bounds = null;
			String key = SubWellChartRenderTask.getKey(dataProvider);
			if (plateBoundsMap.containsKey(key)) {
				bounds = plateBoundsMap.get(key);
			} else {
				dimensionCount = dataProvider.getSelectedFeatures().size();
				bounds = new double[dimensionCount][2];
				for (int i = 0; i < dimensionCount; i++) {
					bounds[i][0] = Float.MAX_VALUE;
					bounds[i][1] = -Float.MAX_VALUE;
				}
				plateBoundsMap.put(key, bounds);
			}

			double[][] tempBounds = dataProvider.getDataBounds();
			for (int i = 0; i < dimensionCount; i++) {
				bounds[i][0] = Math.min(tempBounds[i][0], bounds[i][0]);
				bounds[i][1] = Math.max(tempBounds[i][1], bounds[i][1]);
			}
		}

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
		public boolean isRendering() {
			if (!isDataLoadingJobFinished()) return true;
			return super.isRendering();
		}

		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			if (cell.getData() == null) return null;

			SubWellChartRenderTask task = createRenderTask(cell, w, h);
			task.setPlateBoundsMap(plateBoundsMap);
			task.setConfigs(configs);
			return task;
		}

		@Override
		public void prerender(Grid grid) {
			if (isEnabled() && hasPlates() && dataLoadingJobFinished && configs != null
					&& !configs.isEmpty() && configs.get(0).getDataProviderSettings().getFeatures() != null) {
				super.prerender(grid);
			}
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (isEnabled() && hasPlates() && dataLoadingJobFinished) {
				if (cell.getData() instanceof Well) {
					super.render(cell, gc, x, y, w, h);
				}
			}
		}
	}

	@Override
	public void setConfig(Object config) {
		configs = Collections.synchronizedList(new ArrayList<>());
		Map<Integer, String> configMap = (Map<Integer, String>) config;
		for (int i = 0; i < configMap.size(); i++) {
			if (!configMap.containsKey(i)) break;
			String configStr = configMap.get(i);
			try {
				configs.add((LayerSettings<Well, Well>) SerializationUtils.fromString(configStr));
			} catch (ClassNotFoundException | IOException e) {
				// Do nothing. Skip layer.
			}
		}
		if (hasPlates()) {
			protocolId = getPlate().getExperiment().getProtocol().getId();
			GridState.saveValue(protocolId, getId(), PROPERTY_CONFIG, configs);
			updateDataProvider();
			triggerLoadDataJob();
		}
	}

	@Override
	public Object getConfig() {
		Map<Integer, String> configMap = new HashMap<>();
		if (configs != null) {
			for (int i = 0; i < configs.size(); i++) {
				String config;
				try {
					config = SerializationUtils.toString(configs.get(i));
				} catch (IOException e) {
					// Failed to serialize config, return empty String.
					config = "";
				}
				configMap.put(i, config);
			}
		}
		return configMap;
	}

	public SubWellChartRenderTask createRenderTask(GridCell cell, int w, int h) {
		return new SubWellChartRenderTask((Well) cell.getData(), w, h, cell.getRow(), cell.getColumn());
	}

	public List<LayerSettings<Well, Well>> getLayerSettings() {
		return configs;
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

	public abstract void updateDataProvider();

	public BaseConcurrentGridCellRenderer getRenderer() {
		return renderer;
	}

	public void setRenderer(ChartRenderer renderer) {
		this.renderer = renderer;
	}

	public SubWellDataProvider getDataProvider() {
		return dataProvider;
	}

	public long getProtocolId() {
		return protocolId;
	}

	public boolean isDataLoadingJobFinished() {
		return dataLoadingJobFinished;
	}

	public void setDataLoadingJobFinished(boolean dataLoadingJobFinished) {
		this.dataLoadingJobFinished = dataLoadingJobFinished;
	}

}