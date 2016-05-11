package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SCATTER_3D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.THREE_DIMENSIONAL;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.TopcatViewStyles;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseChartView;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.CartesianPlot3D;
import uk.ac.starlink.ttools.plot.Plot3D;
import uk.ac.starlink.ttools.plot.Plot3DState;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.TablePlot;

public class Scatter3DChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	private double zoom = 1.0;
	double[] rotation;

	public Scatter3DChart() {
		setType(THREE_DIMENSIONAL);
		setName(SCATTER_3D);
		rotation = TopcatViewStyles.getView(TopcatViewStyles.DEFAULT);
	}

	@Override
	public TablePlot createPlot() {
		return new CartesianPlot3D();
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}

		/* Set per-axis characteristics. */
		int axesCount = getType().getNumberOfDimensions();
		ColumnInfo[] axinfos = new ColumnInfo[axesCount];
		ValueConverter[] converters = new ValueConverter[axesCount];
		for (int i = 0; i < axesCount; i++) {
			int index = getDataProvider().getFeatureIndex(getDataProvider().getSelectedFeature(i));
			ColumnInfo cinfo = getDataProvider().getColumnInfo(index);
			axinfos[i] = cinfo;
			converters[i] = (ValueConverter) cinfo.getAuxDatumValue(TopcatUtils.NUMERIC_CONVERTER_INFO,
					ValueConverter.class);
		}

		Plot3DState state = new Plot3DState();
		setPlotState(state);
		state.setPlotData(createPlotData());

		state.setMainNdim(getType().getNumberOfDimensions());
		state.setGrid(true);
		state.setAntialias(true);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setFlipFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setShowAxes(true);

		/* Set axis labels configured in the axis editor window. */
		state.setAxisLabels(getDataProvider().getAxisLabels());

		state.setRanges(getRangeBounds());

		/*
		 * Configure the state with this window's current viewing angles and zoom state.
		 */
		state.setRotation(getRotation());
		state.setRotating( false );
		state.setZoomScale( zoom );

		/* Configure rendering options. */
		state.setFogginess( getChartSettings().isShowFog() ? 2.0 : 0.0 );
		state.setAntialias(true);
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void settingsChanged() {
		((CartesianPlot3D) getPlot()).setOpacity(getChartSettings().getOpacity());
		((Plot3DState)getPlotState()).setZoomScale(getZoom());
		((Plot3DState)getPlotState()).setRotation(getRotation());
		((Plot3DState)getPlotState()).setFogginess( getChartSettings().isShowFog() ? 2.0 : 0.0 );

		// handle aux settings
		if (getChartSettings().getAuxiliaryChartSettings() != null) {
			List<Shader> shaders = new ArrayList<Shader>();
			for (AuxiliaryChartSettings auxSettings : getChartSettings().getAuxiliaryChartSettings()) {
				if (auxSettings.getWeightFeature() != null) {
					shaders.add(auxSettings.getShader());
				}
			}
			getPlotState().setShaders(shaders.toArray(new Shader[shaders.size()]));
		}
		super.settingsChanged();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			((CartesianPlot3D) getPlot()).setSelectionBitSet(bitSet);
		}
	}

	@Override
	public BitSet getActiveSelection() {
		return ((CartesianPlot3D) getPlot()).getSelectionBitSet();
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		if (isSingleSelection) {
			BitSet selection = new BitSet();
			Rectangle bounds = shape.getBounds();
			Point point = new Point(bounds.x, bounds.y);
			int index = ((CartesianPlot3D) getPlot()).getPlottedPointIterator().getClosestPoint(point, 10);
			if (index >= 0) {
				selection.set(index);
			}
			return selection;
		}
		return ((CartesianPlot3D) getPlot()).getPlottedPointIterator().getContainedPoints(shape);
	}

	/*getters and setters*/
	public void setViewStyle(String viewStyle) {
		setRotation(TopcatViewStyles.getView(getChartSettings().getViewStyle()));
	}

	public double[] getRotation() {
		return rotation;
	}

	public void setRotation(double[] rotation) {
		this.rotation = rotation;
	}

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	@Override
	public boolean isSupportSVG() {
		return true;
	}

	@Override
	public MouseAdapter getDragListener(BaseChartView<ENTITY, ITEM> chartView) {
		return new Drag3DListener(chartView);
	}

	protected void stopRotating(BaseChartView<ENTITY,ITEM> chartView) {
		// Do nothing.
	}

	/**
	 * Listener which interprets drag gestures on the plotting surface as
	 * requests to rotate the viewing angles.
	 */
	private class Drag3DListener extends MouseAdapter implements MouseMotionListener {

		private Point posBase_;
		private double[] rotBase_;
		private boolean relevant_;
		private boolean rotating;
		private BaseChartView<ENTITY, ITEM> chartView;

		public Drag3DListener(BaseChartView<ENTITY, ITEM> chartView) {
			this.chartView = chartView;
		}

		@Override
		public void mousePressed(MouseEvent evt) {
			relevant_ = getPlot().getPlotBounds().contains(evt.getPoint());
		}

		@Override
		public void mouseDragged(MouseEvent evt) {
			/*
			 * if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) !=
			 * InputEvent.BUTTON3_MASK) { return; }
			 */
			if (!relevant_) {
				return;
			}
			rotating = true;
			Point pos = evt.getPoint();
			if (posBase_ == null) {
				posBase_ = pos;
				rotBase_ = getRotation();
			} else {
				/*
				 * Work out the amounts by which the user wants to rotate in the
				 * 'horizontal' and 'vertical' directions respectively (these
				 * directions are relative to the current orientation of the
				 * view).
				 */
				double scale = Math.min(getPlot().getWidth(), getPlot().getHeight());
				double xf = -(pos.x - posBase_.x) / scale / getZoom();
				double yf = -(pos.y - posBase_.y) / scale / getZoom();

				/*
				 * Turn these into angles. Phi and Psi are the rotation angles
				 * around the screen vertical and horizontal axes respectively.
				 */
				double yRotation = xf * Math.PI / 2.;
				double xRotation = yf * Math.PI / 2.;
				for (AbstractChartLayer<ENTITY, ITEM> layer : chartView.getChartLayers()) {
					if (!layer.isSelectionLayer()) {
						((Plot3DState) layer.getChart().getPlot().getState()).setRotating(rotating);
						Scatter3DChart<ENTITY, ITEM> chart = (Scatter3DChart<ENTITY, ITEM>) layer.getChart();
						chart.setRotation(Plot3D.rotateXY(rotBase_, yRotation, xRotation));
						chart.settingsChanged();
					}
				}
			}
		}

		@Override
		public void mouseMoved(MouseEvent evt) {
			posBase_ = null;
			rotBase_ = null;
		}

		@Override
		public void mouseReleased(MouseEvent evt) {
			if (rotating) {
				rotating = false;
				for (AbstractChartLayer<ENTITY, ITEM> layer : chartView.getChartLayers()) {
					if (!layer.isSelectionLayer()) {
						((Plot3DState) layer.getChart().getPlot().getState()).setRotating(rotating);
						layer.getChart().settingsChanged();
					}
				}
				stopRotating(chartView);
			}
		}
	}

}