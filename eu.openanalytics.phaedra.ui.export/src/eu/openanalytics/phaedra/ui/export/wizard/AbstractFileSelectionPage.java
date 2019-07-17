package eu.openanalytics.phaedra.ui.export.wizard;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.ISettingsOption;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public abstract class AbstractFileSelectionPage extends BaseExportWizardPage {

	private final IExportExperimentsSettings settings;
	
	private Protocol protocol;
	
	private String[] fileTypes;
	private String fileType;
	
	private String fileNameProposal;
	
	private Text pathTxt;
	private Button browseBtn;

	private List<? extends ISettingsOption> includeOptions;
	private Collection/*<Includes>*/ includeOptionSelection;
	private CheckboxTableViewer includeOptionTableViewer;
	
	public AbstractFileSelectionPage(String pageName, IExportExperimentsSettings settings,
			/*@Nullable*/String fileNameProposal) {
		super(pageName);
		
		this.settings = settings;
		this.fileNameProposal = fileNameProposal;
		
		setFileTypes(WriterFactory.getAvailableFileTypes());
	}
	
	
	protected void setFileTypes(String[] availableFileTypes) {
		this.fileTypes = availableFileTypes;
	}
	
	protected Protocol getProtocol() {
		return protocol;
	}
	
	
	protected String getFileNameProposal() {
		if (settings.getDestinationPath() != null) {
			return WriterFactory.removeFileType(settings.getDestinationPath());
		}
		if (protocol != null) {
			return (fileNameProposal != null) ? protocol.getName() + ' ' + fileNameProposal : protocol.getName();
		}
		return (fileNameProposal != null) ? fileNameProposal : "export";
	}
	
	public Composite createFileSelection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		
		Label label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
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
		pathTxt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(pathTxt);
		
		return container;
	}
	
	private void promptFileSelector() {
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setFilterExtensions(getFileTypeExtensions());
		dialog.setFilterNames(getFileTypeNames());
		dialog.setFileName(getFileNameProposal());
		if (fileType != null) dialog.setFilterIndex(CollectionUtils.find(fileTypes, fileType));
		dialog.setText("Select a destination for the export");
		String selectedPath = dialog.open();
		
		if (selectedPath == null) return;
		
		fileType = fileTypes[dialog.getFilterIndex()];
		selectedPath = WriterFactory.applyFileType(selectedPath, fileType);
		
		if (new File(selectedPath).exists()) {
			boolean confirmed = MessageDialog.openConfirm(getShell(), "File exists",
					"Are you sure you want to overwrite this file?\n" + selectedPath);
			if (!confirmed) {
				promptFileSelector();
				return;
			}
		}
		
		settings.setFileType(fileType);
		settings.setDestinationPath(selectedPath);
		pathTxt.setText(selectedPath);
		checkPageComplete();
	}
	
	
	private String[] getFileTypeNames() {
		String[] names = new String[fileTypes.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = WriterFactory.getFileTypeName(fileTypes[i]) + " (*." + fileTypes[i] + ")";
		}
		return names;
	}
	
	private String[] getFileTypeExtensions() {
		String[] extensions = new String[fileTypes.length];
		for (int i = 0; i < extensions.length; i++) {
			extensions[i] = "*." + fileTypes[i];
		}
		return extensions;
	}
	
	
	protected <TOption extends ISettingsOption> Composite createIncludesSelection(Composite parent,
			List<TOption> options, Collection<? super TOption> model) {
		this.includeOptions = options;
		this.includeOptionSelection = model;
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Include columns:");
		GridDataFactory.defaultsFor(label).span(2, 1).applyTo(label);
		
		includeOptionTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		includeOptionTableViewer.setContentProvider(new ArrayContentProvider());
		includeOptionTableViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(Object element) {
				return (includeOptionSelection.contains(element));
			}
			@Override
			public boolean isGrayed(Object element) {
				return false;
			}
		});
		includeOptionTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				CollectionUtils.setContains(includeOptionSelection, event.getElement(), event.getChecked());
				checkPageComplete();
			}
		});
		includeOptionTableViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((ISettingsOption)element).getLabel();
			}
		});
		includeOptionTableViewer.setInput(includeOptions);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 150).applyTo(includeOptionTableViewer.getControl());
		
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().applyTo(buttonContainer);
		addButton(buttonContainer, "Select All", (option) -> true);
		addButton(buttonContainer, "Select None", (option) -> false);
		addButton(buttonContainer, "Restore Defaults", (option) -> option.getDefaultValue());
		
		return container;
	}
	
	private Button addButton(Composite parent, String label, Predicate<ISettingsOption> select) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				for (ISettingsOption option: includeOptions) {
					CollectionUtils.setContains(includeOptionSelection, option, select.test(option));
				}
				includeOptionTableViewer.refresh();
				checkPageComplete();
			}
		});
		GridDataFactory.defaultsFor(button).applyTo(button);
		return button;
	}
	
	
	@Override
	protected void pageAboutToShow(boolean firstTime) {
		super.pageAboutToShow(firstTime);
		
		List<Experiment> experiments = settings.getExperiments();
		protocol = (experiments != null && !experiments.isEmpty()) ? experiments.get(0).getProtocol() : null;
	}
	
	@Override
	protected boolean validateSettings() {
		return super.validateSettings()
				&& (settings.getDestinationPath() != null);
	}
	
	protected void loadDialogSettings() {
		includeOptionTableViewer.refresh();
	}
	
	@Override
	public void saveDialogSettings() {
		super.saveDialogSettings();
	}
	
}
