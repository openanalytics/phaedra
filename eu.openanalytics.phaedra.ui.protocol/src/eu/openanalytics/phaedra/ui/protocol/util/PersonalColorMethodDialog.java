package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PersonalColorMethodDialog extends TitleAreaDialog {

	private Feature feature;
	private Combo colorMethods;
	
	private String currentColorMethodId;
	private Map<String,Map<String,String>> personalSettings;
	
	public PersonalColorMethodDialog(Shell parentShell, Feature feature) {
		super(parentShell);
		this.feature = feature;
		this.personalSettings = new HashMap<>();
		
		// Retrieve the current color method id and settings (personal or default).
		IColorMethod cm = ColorMethodFactory.createColorMethod(feature);
		currentColorMethodId = cm.getId();
		cm.getConfiguration(getPersonalSettings(cm.getId()));
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Personalize Color Method");
	}
		
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(parentComposite, SWT.NONE);
		
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(container);

		new Label(container, SWT.NONE).setText("Feature:");
		
		Text text = new Text(container, SWT.BORDER);
		text.setText(feature.getName());
		text.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
		
		new Label(container, SWT.NONE).setText("Color Method:");
		
		colorMethods = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		colorMethods.setLayoutData(new GridData(140, SWT.DEFAULT));
		colorMethods.setItems(ColorMethodRegistry.getInstance().getNames());
		colorMethods.setVisibleItemCount(10);
		colorMethods.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] ids = ColorMethodRegistry.getInstance().getIds();
				currentColorMethodId = ids[colorMethods.getSelectionIndex()];
			}
		});
		String[] ids = ColorMethodRegistry.getInstance().getIds();
		colorMethods.select(CollectionUtils.find(ids, currentColorMethodId));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorMethods);
		
		new Label(container, SWT.NONE);
		
		Button browseBtn = new Button(container, SWT.PUSH);
		browseBtn.setText("Configure...");
		browseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IColorMethod cm = createColorMethod(currentColorMethodId);
				Dialog dialog = cm.createDialog(Display.getCurrent().getActiveShell());
				if (dialog == null) return;
				if (dialog.open() == Dialog.OK) cm.getConfiguration(getPersonalSettings(currentColorMethodId));
			}
		});
		
		setTitle("Personalize Color Method");
		setMessage("You can personalize the Color Method of this feature below."
				+ "\nThese settings will override the Color Method settings in the Protocol Class.");
		return parentComposite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.ABORT_ID, "Revert", true);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.ABORT_ID) {
			boolean confirmed = MessageDialog.openConfirm(getShell(), "Revert Color Method", 
					"This will revert the Color Method to the settings specified in the Protocol Class."
					+ "\nAre you sure you want to delete your personalized Color Method?");
			if (!confirmed) return;
			PersonalColorMethodFactory.revertColorMethod(feature);
			super.okPressed();
		}
		else super.buttonPressed(buttonId);
	}
	
	@Override
	protected void okPressed() {
		IColorMethod newPersonalCM = createColorMethod(currentColorMethodId);
		PersonalColorMethodFactory.saveColorMethod(feature, newPersonalCM);
		super.okPressed();
	}

	private Map<String, String> getPersonalSettings(String id) {
		Map<String, String> settings = personalSettings.get(id);
		if (settings == null) {
			settings = new HashMap<>();
			personalSettings.put(id, settings);
		}
		return settings;
	}
	
	private IColorMethod createColorMethod(String id) {
		// Create a color method based on 'currentColorMethodId' but with no personal settings.
		IColorMethod cm = null;
		String oldId = feature.getColorMethodSettings().get(ColorMethodFactory.SETTING_METHOD_ID);
		feature.getColorMethodSettings().put(ColorMethodFactory.SETTING_METHOD_ID, currentColorMethodId);
		try {
			cm = ColorMethodFactory.createColorMethod(feature, false);
		} finally {
			if (oldId != null) feature.getColorMethodSettings().put(ColorMethodFactory.SETTING_METHOD_ID, oldId);
		}
		
		// Apply the transient personal settings, if any.
		Map<String, String> settings = getPersonalSettings(cm.getId());
		if (!settings.isEmpty()) cm.configure(settings);
		
		return cm;
	}
}
