package eu.openanalytics.phaedra.ui.export.wizard.plate;

import static java.util.Arrays.asList;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.export.core.ExportPlateTableSettings;
import eu.openanalytics.phaedra.export.core.ExportPlateTableSettings.Includes;
import eu.openanalytics.phaedra.ui.export.wizard.AbstractFileSelectionPage;

public class IncludeDataPage extends AbstractFileSelectionPage {
	
	private ExportPlateTableSettings settings;
	
	public IncludeDataPage(ExportPlateTableSettings settings, int stepNum, int stepTotal) {
		super("Statistics to Include", settings, "Platelist");
		setDescription(String.format("Step %1$s/%2$s: Select the plate statistics to export.", stepNum, stepTotal));
		
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
		
		Composite fileSelection = createFileSelection(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fileSelection);
		
		setPageComplete(false);
		loadDialogSettings();
	}

	@Override
	protected void loadDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		
		for (Includes inc: Includes.values()) {
			String key= "include." + inc.name();
			boolean state = dialogSettings.get(key) == null ? inc.getDefaultValue() : dialogSettings.getBoolean(key);
			CollectionUtils.setContains(settings.getIncludes(), inc, state);
		}
		super.loadDialogSettings();
	}

	@Override
	public void saveDialogSettings() {
		super.saveDialogSettings();
		
		IDialogSettings dialogSettings = getDialogSettings();
		
		for (Includes inc: Includes.values()) {
			String key = "include." + inc.name();
			boolean state = settings.getIncludes().contains(inc);
			dialogSettings.put(key, state);
		}
	}
	
}
