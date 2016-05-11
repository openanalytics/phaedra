package eu.openanalytics.phaedra.base.ui.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.nebula.widgets.nattable.NatTable;

import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NullRichColumnAccessor;

public abstract class AbstractQueryEditorSupport implements IQueryEditorSupport {

	private String id;

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(IQueryEditorSupport.ATTR_ID);
	}

	@Override
	public String getLabel() {
		return getSupportedClass().getSimpleName();
	}

	@Override
	public String getLabelForField(String fieldName) {
		return fieldName;
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new NullRichColumnAccessor<Object>();
	}

	@Override
	public void customize(NatTable table) {
		// Default: do nothing.
	}
}
