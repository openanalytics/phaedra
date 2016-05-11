package eu.openanalytics.phaedra.base.ui.util.tooltip;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;

public abstract class ToolTipLabelProvider extends LabelProvider {

	public boolean isHideOnMouseDown() {
		return !hasAdvancedControls();
	}

	public boolean hasAdvancedControls() {
		return false;
	}

	public void fillAdvancedControls(Composite parent, Object element, IToolTipUpdate update) {
		// Do nothing.
	}

}
