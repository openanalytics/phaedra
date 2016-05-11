package eu.openanalytics.phaedra.ui.protocol.provider;

import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;

public interface IFeatureProvider {

	public ProtocolClass getCurrentProtocolClass();

	public Feature getCurrentFeature();
	
	public String getCurrentNormalization();
	
	public boolean isExperimentLimit();
	
	public IColorMethod getCurrentColorMethod();

	public void addUIEventListener(IUIEventListener listener);
	
	public void removeUIEventListener(IUIEventListener listener);
	
}