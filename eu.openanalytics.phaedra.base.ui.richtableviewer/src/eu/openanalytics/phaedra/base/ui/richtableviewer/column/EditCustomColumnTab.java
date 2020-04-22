package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;


public abstract class EditCustomColumnTab {
	
	
	private final String label;
	
	private EditCustomColumnDialog dialog;
	
	protected TabItem tabItem;
	
	
	protected EditCustomColumnTab(final String label) {
		this.label = label;
	}
	
	
	void setContainer(final EditCustomColumnDialog dialog) {
		this.dialog = dialog;
	}
	
	protected ColumnConfiguration getColumnConfig() {
		return this.dialog.getColumnConfig();
	}
	
	public EditCustomColumnDialog getDialog() {
		return this.dialog;
	}
	
	protected Shell getShell() {
		return this.dialog.getShell();
	}
	
	protected int getDetailHorizontalIndent() {
		return this.dialog.convertHorizontalDLUsToPixels(IDialogConstants.LEFT_MARGIN);
	}
	
	
	protected TabItem getTabItem() {
		return this.tabItem;
	}
	
	protected void createTabItem(final TabFolder tabFolder) {
		this.tabItem = new TabItem(tabFolder, SWT.NONE);
		this.tabItem.setText(this.label);
		this.tabItem.setControl(createContent(tabFolder));
	}
	
	protected abstract Composite createContent(final Composite parent);
	
	protected GridLayout createContentGridLayout(final int numColumns) {
		final GridLayout layout = new GridLayout(numColumns, false);
		layout.marginLeft = layout.marginRight = 5;
		layout.marginTop = layout.marginBottom = 5;
		return layout;
	}
	
	protected void initDataBinding(final DataBindingContext dbc) {
	}
	
	protected void updateConfig(final Map<String, Object> customData) {
	}
	
	protected void updateTargets(final Map<String, Object> customData) {
	}
	
	
	protected static void bindEnabled(final List<Control> targets, final IObservableValue<Boolean> enabledValue) {
		enabledValue.addValueChangeListener(new IValueChangeListener<Boolean>() {
			@Override
			public void handleValueChange(final ValueChangeEvent<? extends Boolean> event) {
				final boolean enabled = event.diff.getNewValue();
				for (final Control widget : targets) {
					widget.setEnabled(enabled);
				}
			}
		});
	}
	
}
