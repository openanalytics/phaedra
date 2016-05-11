package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public abstract class BaseTemplateTab implements ITemplateTab {

	@Override
	public String getValue(WellTemplate well) {
		return null;
	}
	
	@Override
	public boolean applyValue(WellTemplate well, String value) {
		// Do nothing
		return false;
	}

	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher) {
		// Do nothing
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		// Do nothing
	}
	
	@Override
	public void protocolClassChanged(ProtocolClass newPClass) {
		// Do nothing
	}
}
