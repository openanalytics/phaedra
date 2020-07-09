package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.dialog.TitleAreaDataBindingDialog;


public class EditCustomColumnDialog extends TitleAreaDataBindingDialog {
	
	
	private final ColumnConfiguration columnConfig;
	
	private Text nameControl;
	private Text descriptionControl;
	
	private TabFolder tabFolder;
	private List<EditCustomColumnTab> tabs;
	
	
	protected EditCustomColumnDialog(final Shell shell, final ColumnConfiguration columnConfig) {
		super(shell);
		
		this.columnConfig= columnConfig;
		setDialogTitle((columnConfig.getKey() == null) ? "Add New Custom Column" : "Edit Custom Column");
		setDialogMessage("Edit the properties of the column.");
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	public EditCustomColumnDialog(final Shell shell, final ColumnConfiguration columnConfig,
			final EditCustomColumnTab... tabs) {
		this(shell, columnConfig);
		setTabs(tabs);
	}
	
	protected void setTabs(final EditCustomColumnTab... tabs) {
		for (final EditCustomColumnTab tab : tabs) {
			tab.setContainer(this);
		}
		this.tabs = Arrays.asList(tabs);
	}
	
	public ColumnConfiguration getColumnConfig() {
		return this.columnConfig;
	}
	
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite dialogArea= super.createDialogAreaComposite(parent);
		
		final Composite composite= new Composite(dialogArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		GridDataFactory.fillDefaults()
				.grab(true, true)
				.applyTo(composite);
		
		addBasicControls(composite);
		
		this.tabFolder = new TabFolder(composite, SWT.TOP);
		this.tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		for (final EditCustomColumnTab tab : this.tabs) {
			tab.createTabItem(this.tabFolder);
		}
		this.tabFolder.setSelection(0);
		
		return dialogArea;
	}
	
	protected void addBasicControls(final Composite composite) {
		{	final Label label= new Label(composite, SWT.NONE);
			label.setText("&Name:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			final Text text= new Text(composite, SWT.SINGLE | SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			this.nameControl= text;
		}
		{	final Label label= new Label(composite, SWT.NONE);
			label.setText("D&escription:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			final Text text= new Text(composite, SWT.SINGLE | SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			this.descriptionControl= text;
		}
	}
	
	@Override
	protected int convertHorizontalDLUsToPixels(final int dlus) {
		return super.convertHorizontalDLUsToPixels(dlus);
	}
	
	@Override
	protected int convertVerticalDLUsToPixels(final int dlus) {
		return super.convertVerticalDLUsToPixels(dlus);
	}
	
	
	@Override
	protected void initDataBinding(final DataBindingContext dbc) {
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(this.nameControl),
				PojoProperties.value("name", String.class).observe(this.columnConfig), //$NON-NLS-1$
				new UpdateValueStrategy().setAfterGetValidator((value) -> {
					final String s = (String) value;
					if (s.isEmpty()) {
						return ValidationStatus.error("Enter a name for the column.");
					}
					return ValidationStatus.ok();
				}),
				null );
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(this.descriptionControl),
				PojoProperties.value("tooltip", String.class).observe(this.columnConfig) );
		
		for (final EditCustomColumnTab tab : this.tabs) {
			tab.initDataBinding(dbc);
		}
	}
	
	
	@Override
	protected void updateTargets() {
		final Map<String, Object> customData = getColumnConfig().getCustomData();
		for (final EditCustomColumnTab tab : this.tabs) {
			tab.updateTargets(customData);
		}
		getColumnConfig().setCustomData(customData);
		
		super.updateTargets();
	}
	
	@Override
	protected void okPressed() {
		final Map<String, Object> customData = new HashMap<String, Object>();
		for (final EditCustomColumnTab tab : this.tabs) {
			tab.updateConfig(customData);
		}
		getColumnConfig().setCustomData(customData);
		
		super.okPressed();
	}
	
}
