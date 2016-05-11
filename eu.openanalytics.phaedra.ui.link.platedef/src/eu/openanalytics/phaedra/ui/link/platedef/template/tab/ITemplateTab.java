package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public interface ITemplateTab {

	public String getName();
	
	public IGridCellRenderer createCellRenderer();
	
	public String getValue(WellTemplate well);
	
	public boolean applyValue(WellTemplate well, String value);
	
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher);
	
	public void selectionChanged(List<WellTemplate> newSelection);
	
	public void protocolClassChanged(ProtocolClass newPClass);
}
