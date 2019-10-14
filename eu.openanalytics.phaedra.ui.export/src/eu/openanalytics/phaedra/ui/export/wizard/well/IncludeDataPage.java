package eu.openanalytics.phaedra.ui.export.wizard.well;

import static java.util.Arrays.asList;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.ui.export.wizard.AbstractFileSelectionPage;

public class IncludeDataPage extends AbstractFileSelectionPage {
	
	
	private static final String CENSORED_DATA_SPLIT_KEY = "format.CensoredData.split";
	
	
	private final ExportSettings settings;
	
	private Button compoundJoinedChk;
	private Button compoundSplitChk;
	private Button censoredJoinedChk;
	private Button censoredSplitChk;
	
	public IncludeDataPage(ExportSettings settings, int stepNum, int stepTotal) {
		super("Data to Include", settings, "Welldata");
		setDescription(String.format("Step %1$s/%2$s: Select additional data to export.", stepNum, stepTotal));
		
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(1).applyTo(container);
		setControl(container);
		
		Composite includes = createIncludesSelection(container, asList(Includes.values()), settings.getIncludes());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(includes);
		
		Label label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().applyTo(label);
		
		Composite tableConfig = createFormatConfig(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(tableConfig);
		
		Composite fileSelection = createFileSelection(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fileSelection);
		
		setPageComplete(false);
		loadDialogSettings();
	}
	
	private Composite createFormatConfig(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		{	Label label = new Label(composite, SWT.NONE);
			label.setText("Compound numbers:");
			GridDataFactory.fillDefaults().applyTo(label);
			
			Composite radioGroup = new Composite(composite, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(radioGroup);
			GridDataFactory.fillDefaults().applyTo(radioGroup);
			
			compoundJoinedChk = new Button(radioGroup, SWT.RADIO);
			compoundJoinedChk.setText("In one column ('TypeNumber')");
			GridDataFactory.fillDefaults().applyTo(compoundJoinedChk);
			
			compoundSplitChk = new Button(radioGroup, SWT.RADIO);
			compoundSplitChk.setText("In two columns ('Type' 'Number')");
			GridDataFactory.fillDefaults().applyTo(compoundSplitChk);
		}
		{	Label label = new Label(composite, SWT.NONE);
			label.setText("Censored data:");
			GridDataFactory.fillDefaults().applyTo(label);
			
			Composite radioGroup = new Composite(composite, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(radioGroup);
			GridDataFactory.fillDefaults().applyTo(radioGroup);
			
			censoredJoinedChk = new Button(radioGroup, SWT.RADIO);
			censoredJoinedChk.setText("In one column ('>1.5e-3')");
			GridDataFactory.fillDefaults().applyTo(censoredJoinedChk);
			
			censoredSplitChk = new Button(radioGroup, SWT.RADIO);
			censoredSplitChk.setText("In two columns ('>' 1.5e-3)");
			GridDataFactory.fillDefaults().applyTo(censoredSplitChk);
		}
		return composite;
	}

	@Override
	public void collectSettings() {
		super.collectSettings();
		
		settings.setCompoundNameSplit(compoundSplitChk.getSelection());
		settings.setCensoredValueSplit(censoredSplitChk.getSelection());
	}

	@Override
	protected void loadDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		
		compoundJoinedChk.setSelection(dialogSettings.getBoolean("compoundJoinedChk"));
		compoundSplitChk.setSelection(dialogSettings.getBoolean("compoundSplitChk"));
		if (!compoundJoinedChk.getSelection() && !compoundSplitChk.getSelection()) compoundJoinedChk.setSelection(true);
		
		if (dialogSettings.get(CENSORED_DATA_SPLIT_KEY) != null && dialogSettings.getBoolean(CENSORED_DATA_SPLIT_KEY)) {
			censoredSplitChk.setSelection(true);
		}
		else {
			censoredJoinedChk.setSelection(true);
		}
		
		for (Includes inc: Includes.values()) {
			String key = "include" + inc.name();
			boolean state = dialogSettings.get(key) == null ? inc.getDefaultValue() : dialogSettings.getBoolean(key);
			CollectionUtils.setContains(settings.getIncludes(), inc, state);
		}
		super.loadDialogSettings();
	}

	@Override
	public void saveDialogSettings() {
		super.saveDialogSettings();
		
		IDialogSettings dialogSettings = getDialogSettings();
		
		dialogSettings.put("compoundJoinedChk", !settings.compoundNameSplit);
		dialogSettings.put("compoundSplitChk", settings.compoundNameSplit);
		
		dialogSettings.put(CENSORED_DATA_SPLIT_KEY, settings.getCensoredValueSplit());
		
		for (Includes inc: Includes.values()) {
			String key = "include" + inc.name();
			boolean state = settings.getIncludes().contains(inc);
			dialogSettings.put(key, state);
		}
	}
	
}
