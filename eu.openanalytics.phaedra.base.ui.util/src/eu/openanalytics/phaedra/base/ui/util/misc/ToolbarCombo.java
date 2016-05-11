package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class ToolbarCombo extends ControlContribution {

	private String lbl;
	private CCombo combo;
	private ComboInitializer initializer;
	
	public ToolbarCombo(String lbl, String id, ComboInitializer initializer) {
		super(id);
		this.lbl = lbl;
		this.initializer = initializer;
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,0).applyTo(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(lbl);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
		
		combo = new CCombo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.setVisibleItemCount(10);
		GridDataFactory.fillDefaults().applyTo(combo);
		
		if (initializer != null) initializer.initialize(combo);
		return container;
	}
	
	public CCombo getCombo() {
		return combo;
	}

	public void addSelectionListener(SelectionListener listener) {
		combo.addSelectionListener(listener);
	}
	
	public void removeSelectionListener(SelectionListener listener) {
		combo.removeSelectionListener(listener);
	}
	
	public static class ComboInitializer {
		public void initialize(CCombo combo) {
			// Default behaviour: do nothing.
		}
	}
}
