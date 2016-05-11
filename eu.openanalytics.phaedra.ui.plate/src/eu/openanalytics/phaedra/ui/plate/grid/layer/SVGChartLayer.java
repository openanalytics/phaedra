package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.io.IOException;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.ConcurrentTaskResult;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.chart.svg.SVGChartSupport;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.config.SVGChartConfig;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public class SVGChartLayer extends PlatesLayer {

	private final static String SETTING_CHART = "SETTING_CHART";
	private final static String SETTING_NO_BG = "NO_BG";
	private final static String SETTING_BG_COLOR = "BG_COLOR";

	private SVGChartSupport chartSupport;
	private SVGChartConfig config;

	private long protocolId;
	private boolean initialized;
	private IPropertyChangeListener prefListener;
	
	private BaseConcurrentGridCellRenderer renderer;

	@Override
	public String getName() {
		return "SVG Chart";
	}

	@Override
	protected void doInitialize() {
		PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(getPlate());
		protocolId = dataAccessor.getPlate().getExperiment().getProtocol().getId();

		chartSupport = new SVGChartSupport(dataAccessor.getPlate());
		String[] charts = chartSupport.getAvailableCharts();

		if (config == null) {
			config = new SVGChartConfig();
			String chartName = GridState.getStringValue(protocolId, getId(), SETTING_CHART);
			if (charts.length == 0) {
				// No charts available.
				chartName = null;
			} else {
				if (chartName != null) {
					int index = CollectionUtils.find(charts, chartName);
					if (index == -1) chartName = charts[0];
				} else {
					chartName = charts[0];
				}
			}
			config.setChartName(chartName);

			Boolean noBg = GridState.getBooleanValue(GridState.ALL_PROTOCOLS, getId(), SETTING_NO_BG);
			if (noBg == null) noBg = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SVG_NO_BG);
			config.setNoBg(noBg);
			
			RGB bgColor = ColorUtils.parseColorString(GridState.getStringValue(GridState.ALL_PROTOCOLS, getId(), SETTING_BG_COLOR));
			if (bgColor == null) bgColor = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), Prefs.SVG_BG_COLOR);
			config.setBgColor(ColorUtils.rgbToHex(bgColor));
		}

		prefListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				boolean renderRequired = false;
				if (event.getProperty().equals(Prefs.SVG_BG_COLOR)) {
					config.setBgColor(ColorUtils.rgbToHex((RGB) event.getNewValue()));
					renderRequired = true;
				}
				if (event.getProperty().equals(Prefs.SVG_NO_BG)) {
					config.setNoBg(Boolean.valueOf(event.getNewValue().toString()));
					renderRequired = true;
				}
				if (renderRequired) {
					if (config.isNoBg()) chartSupport.setBgColor(null);
					else chartSupport.setBgColor(new Color(null, ColorUtils.hexToRgb(config.getBgColor())));
					renderer.resetRendering();
					getLayerSupport().getViewer().getGrid().redraw();
				}
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);

		refreshSettings();
		initialized = true;
	}

	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new SVGChartRenderer();
		return renderer;
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ConfigDialog(shell);
	}

	@Override
	public void dispose() {
		if (prefListener != null) Activator.getDefault().getPreferenceStore().removePropertyChangeListener(prefListener);
		if (chartSupport != null) chartSupport.dispose();
		if (renderer != null) renderer.resetRendering();
	}

	@Override
	public void setConfig(Object config) {
		this.config = (SVGChartConfig) config;
		refreshSettings();
	}

	@Override
	public Object getConfig() {
		return config;
	}

	@Override
	public boolean isRendering() {
		return renderer.isRendering();
	}

	private void doApplySettings(String chartName, RGB bgColor) {
		config.setChartName(chartName);
		GridState.saveValue(protocolId, getId(), SETTING_CHART, chartName);

		config.setNoBg(bgColor == null);
		GridState.saveValue(-1, getId(), SETTING_NO_BG, config.isNoBg());

		config.setBgColor(ColorUtils.rgbToHex(bgColor));
		if (bgColor == null) GridState.removeValue(GridState.ALL_PROTOCOLS, getId(), SETTING_BG_COLOR);
		else GridState.saveValue(GridState.ALL_PROTOCOLS, getId(), SETTING_BG_COLOR, ColorUtils.createRGBString(bgColor));

		refreshSettings();
	}
	
	private void refreshSettings() {
		if (chartSupport == null) return;
		if (config.isNoBg()) chartSupport.setBgColor(null);
		else chartSupport.setBgColor(new Color(null, ColorUtils.hexToRgb(config.getBgColor())));
		renderer.resetRendering();
	}
	
	private class SVGChartRenderer extends BaseConcurrentGridCellRenderer {
		
		public SVGChartRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}
		
		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			Well well = (Well)cell.getData();
			if (well == null) return null;
			if (chartSupport.getAvailableCharts().length == 0) return null;
			return new SVGChartRenderTask(well, w, h, cell.getRow(), cell.getColumn());
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
	
	private class SVGChartRenderTask extends ConcurrentTask {
	
		private Well well;
		private int w;
		private int h;
		private int row;
		private int col;
		private int border;
		
		public SVGChartRenderTask(Well well, int w, int h, int row, int col) {
			this.well = well;
			this.w = w;
			this.h = h;
			this.row = row;
			this.col = col;
			this.border = Activator.getDefault().getPreferenceStore().getInt(eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs.PADDING);
		}
		
		@Override
		public void run() {
			int[] padding = { (int) (w*border/100), (int) (h*border/100) };
			Image image = null;
			ImageData imageData = null;
			try {
				image = chartSupport.getChart(config.getChartName(), well, w-2*padding[0], h-2*padding[1]);
				image = ImageUtils.addPadding(image, padding[0], padding[1]);
				imageData = image.getImageData();
			} catch (IOException e) {
				// Will return an empty result.
			} finally {
				if (image != null) image.dispose();
			}
			setResult(new ConcurrentTaskResult(this.row, this.col, imageData));		
		}
	}

	private class ConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

		private Combo chartCombo;
		private Button bgNoneBtn;
		private Button bgColorBtn;
		private ColorSelector colorSelector;

		private String selectedChart;
		private RGB selectedBgColor;

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
			return super.getInitialSize();//new Point(400, 400);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(container);

			String[] charts = chartSupport.getAvailableCharts();

			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Chart:");
			chartCombo = new Combo(container, SWT.READ_ONLY);
			chartCombo.setItems(charts);
			GridDataFactory.fillDefaults().grab(true,false).applyTo(chartCombo);

			String chartName = config.getChartName();
			if (chartName != null && !chartName.isEmpty()) {
				int index = CollectionUtils.find(charts, chartName);
				chartCombo.select(index);
			} else {
				chartCombo.select(0);
			}

			bgNoneBtn = new Button(container, SWT.RADIO);
			bgNoneBtn.setText("No background color (transparent)");
			bgNoneBtn.setSelection(config.isNoBg());
			GridDataFactory.fillDefaults().span(2,1).applyTo(bgNoneBtn);

			bgColorBtn = new Button(container, SWT.RADIO);
			bgColorBtn.setText("Background color:");
			bgColorBtn.setSelection(!config.isNoBg());

			colorSelector = new ColorSelector(container);
			if (!config.isNoBg()) colorSelector.setColorValue(ColorUtils.hexToRgb(config.getBgColor()));
			else colorSelector.setColorValue(new RGB(255,255,255));

			setTitle(getName());
			setMessage("Select the chart to display on the grid.");
			return area;
		}

		@Override
		protected void okPressed() {
			int chartIndex = chartCombo.getSelectionIndex();
			String[] charts = chartSupport.getAvailableCharts();
			if (chartIndex >= 0 && charts.length > 0) selectedChart = charts[chartIndex];
			if (bgColorBtn.getSelection()) selectedBgColor = colorSelector.getColorValue();

			applySettings(getLayerSupport().getViewer(), SVGChartLayer.this);
			super.okPressed();
		}

		@Override
		public void applySettings(GridViewer viewer, IGridLayer layer) {
			((SVGChartLayer)layer).doApplySettings(selectedChart, selectedBgColor);
			viewer.getGrid().redraw();
		}
	}
}