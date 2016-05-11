package eu.openanalytics.phaedra.ui.export.wizard.pages;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.export.Activator;
import eu.openanalytics.phaedra.ui.export.wizard.BaseExportWizardPage;

public class IncludeDataPage extends BaseExportWizardPage {

	private String path;
	private String fileType;
	private String protocolName;
	private String[] fileTypes;
	private String[] fileTypeNames;
		
	private CheckboxTableViewer includesTableViewer;
	
	private Button compoundJoinedChk;
	private Button compoundSplitChk;
	
	private Text pathTxt;
	private Button browseBtn;
	
	public IncludeDataPage() {
		super("Data to Include");
		setDescription("Step 4/4: Select additional data to export.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(1).applyTo(container);
		setControl(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Include columns:");
		
		Table table = new Table(container, SWT.BORDER | SWT.CHECK);
		table.setLinesVisible(true);
		includesTableViewer = new CheckboxTableViewer(table);
		includesTableViewer.setContentProvider(new ArrayContentProvider());
		includesTableViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Includes)element).getLabel();
			}
		});
		includesTableViewer.setInput(Includes.values());
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 150).applyTo(table);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().applyTo(label);
		
		label = new Label(container, SWT.NONE);
		label.setText("Compound numbers:");
		
		compoundJoinedChk = new Button(container, SWT.RADIO);
		compoundJoinedChk.setText("In one column ('TypeNumber')");
		GridDataFactory.fillDefaults().applyTo(compoundJoinedChk);
		
		compoundSplitChk = new Button(container, SWT.RADIO);
		compoundSplitChk.setText("In two columns ('Type' 'Number')");
		GridDataFactory.fillDefaults().applyTo(compoundSplitChk);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().applyTo(label);
		
		label = new Label(container, SWT.NONE);
		label.setText("Save As:");
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		Composite subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(subContainer);
		
		browseBtn = new Button(subContainer, SWT.PUSH);
		browseBtn.setText("Select Destination...");
		browseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				promptFileSelector();
			}
		});
		GridDataFactory.fillDefaults().applyTo(browseBtn);
		
		pathTxt = new Text(subContainer, SWT.BORDER);
		pathTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				path = pathTxt.getText();
				checkPageComplete();
			}
		});
		pathTxt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(pathTxt);

		setPageComplete(false);
		
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		compoundJoinedChk.setSelection(settings.getBoolean("compoundJoinedChk"));
		compoundSplitChk.setSelection(settings.getBoolean("compoundSplitChk"));
		if (!compoundJoinedChk.getSelection() && !compoundSplitChk.getSelection()) compoundJoinedChk.setSelection(true);
		for (Includes inc: Includes.values()) {
			String name = inc.name();
			boolean state = settings.get("include" + name) == null ? true : settings.getBoolean("include" + name);
			includesTableViewer.setChecked(inc, state);
		}
	}

	@Override
	protected void pageAboutToShow(ExportSettings settings, boolean firstTime) {
		fileTypes = WriterFactory.getAvailableFileTypes();
		fileTypeNames = WriterFactory.getAvailableFileTypeNames();
		if (settings.experiments != null && !settings.experiments.isEmpty()) {
			Protocol p = settings.experiments.get(0).getProtocol();
			protocolName = p.getName();
		}
	}
	
	private void checkPageComplete() {
		boolean complete = false;
		if (path != null && !path.isEmpty()) {
			complete = true;
		}
		setPageComplete(complete);
	}
	
	private void promptFileSelector() {
		String fileName = "export";
		if (protocolName != null) fileName = protocolName;
		if (path != null) fileName = path;
		
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setFilterExtensions(fileTypes);
		dialog.setFilterNames(fileTypeNames);
		dialog.setFileName(fileName);
		dialog.setText("Select a destination for the export");
		String selectedPath = dialog.open();
		
		if (selectedPath == null) return;
		
		fileType = fileTypes[dialog.getFilterIndex()];
		if (!selectedPath.endsWith(fileType)) selectedPath += "." + fileType;
		
		if (new File(selectedPath).exists()) {
			boolean confirmed = MessageDialog.openConfirm(getShell(), "File exists",
					"Are you sure you want to overwrite this file?\n" + selectedPath);
			if (!confirmed) {
				promptFileSelector();
				return;
			}
		}
		
		path = selectedPath;
		pathTxt.setText(path);
		checkPageComplete();
	}
	
	@Override
	public void collectSettings(ExportSettings settings) {
		settings.compoundNameSplit = compoundSplitChk.getSelection();
		settings.destinationPath = path;
		settings.fileType = fileType;
		
		IDialogSettings dialogSettings = Activator.getDefault().getDialogSettings();
		dialogSettings.put("compoundJoinedChk", !settings.compoundNameSplit);
		dialogSettings.put("compoundSplitChk", settings.compoundNameSplit);
		
		for (Includes inc: Includes.values()) {
			String name = inc.name();
			boolean state = includesTableViewer.getChecked(inc);
			dialogSettings.put("include" + name, state);
			if (state) CollectionUtils.addUnique(settings.includes, inc);
			else settings.includes.remove(inc);
		}
	}
}
