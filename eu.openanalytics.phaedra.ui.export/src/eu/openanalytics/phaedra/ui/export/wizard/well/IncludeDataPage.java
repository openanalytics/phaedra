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

	private final ExportSettings settings;
	
	private Button compoundJoinedChk;
	private Button compoundSplitChk;
	
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
		
		label = new Label(container, SWT.NONE);
		label.setText("Compound numbers:");
		
		compoundJoinedChk = new Button(container, SWT.RADIO);
		compoundJoinedChk.setText("In one column ('TypeNumber')");
		GridDataFactory.fillDefaults().applyTo(compoundJoinedChk);
		
		compoundSplitChk = new Button(container, SWT.RADIO);
		compoundSplitChk.setText("In two columns ('Type' 'Number')");
		GridDataFactory.fillDefaults().applyTo(compoundSplitChk);
		
		Composite fileSelection = createFileSelection(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fileSelection);
		
		setPageComplete(false);
		loadDialogSettings();
	}

	@Override
	public void collectSettings() {
		super.collectSettings();
		
		settings.compoundNameSplit = compoundSplitChk.getSelection();
	}

	@Override
	protected void loadDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		
		compoundJoinedChk.setSelection(dialogSettings.getBoolean("compoundJoinedChk"));
		compoundSplitChk.setSelection(dialogSettings.getBoolean("compoundSplitChk"));
		if (!compoundJoinedChk.getSelection() && !compoundSplitChk.getSelection()) compoundJoinedChk.setSelection(true);
		
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
		
		for (Includes inc: Includes.values()) {
			String key = "include" + inc.name();
			boolean state = settings.getIncludes().contains(inc);
			dialogSettings.put(key, state);
		}
	}
	
}
