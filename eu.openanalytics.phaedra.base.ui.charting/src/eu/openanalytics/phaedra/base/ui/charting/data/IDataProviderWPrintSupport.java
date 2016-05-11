package eu.openanalytics.phaedra.base.ui.charting.data;

import eu.openanalytics.phaedra.base.ui.charting.render.IRenderCustomizer;

public abstract class IDataProviderWPrintSupport<E> implements IDataProvider<E> {

	public abstract IRenderCustomizer createRenderCustomizer();

	@Override
	public double[] getGlobalMinMax(String[] parameters) {
		return null;
	}

}
