package eu.openanalytics.phaedra.ui.link.platedef.template;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridContentProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridLabelProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.link.platedef.Activator;
import eu.openanalytics.phaedra.ui.link.platedef.template.copypaste.GridToTemplate;
import eu.openanalytics.phaedra.ui.link.platedef.template.copypaste.PastedGrid;
import eu.openanalytics.phaedra.ui.link.platedef.template.copypaste.SelectionToGrid;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.AnnotationTab;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.CompoundTab;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.ConcentrationTab;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.ITemplateTab;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.OverviewTab;
import eu.openanalytics.phaedra.ui.link.platedef.template.tab.WellTypeTab;


public class PlateTemplateEditor extends EditorPart {

	private PlateTemplate plateTemplate;
	private ProtocolClass protocolClass;
	private boolean isNewTemplate;
	
	private DataFormatSupport dataFormatSupport;
	
	private List<WellTemplate> currentSelection;
	private boolean dirty = false;

	private Text plateIdText;
	private Text plateCreatorText;
	private Text plateRowsText;
	private Text plateColumnsText;
	
	private ComboViewer protocolClassComboViewer;
	
	private Button saveBtn;
	private Button makeCopyBtn;
	private Button deleteBtn;

	private CTabFolder tabFolder;
	private CTabItem[] tabItems;
	private ITemplateTab[] tabs;
	private GridViewer[] gridViewers;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}
	
	@Override
	public void dispose() {
		if (this.dataFormatSupport != null) this.dataFormatSupport.dispose();
		super.dispose();
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		this.dataFormatSupport = new DataFormatSupport(this::reloadData);

		plateTemplate = ((PlateTemplateEditorInput)getEditorInput()).getPlateTemplate();
		isNewTemplate = ((PlateTemplateEditorInput)getEditorInput()).isNewTemplate();

		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(parent);

		/*
		 * The template properties group
		 * *****************************
		 */

		Group plateGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		plateGroup.setText("Template Properties");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(plateGroup);
		GridLayoutFactory.fillDefaults().numColumns(4).margins(5,5).applyTo(plateGroup);

		Label lbl = new Label(plateGroup, SWT.NONE);
		lbl.setText("ID:");

		plateIdText = new Text(plateGroup, SWT.BORDER);
		plateIdText.setText(plateTemplate.getId());
		plateIdText.setEnabled(isNewTemplate);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(plateIdText);

		lbl = new Label(plateGroup, SWT.NONE);
		lbl.setText("Protocol Class:");

		protocolClassComboViewer = new ComboViewer(plateGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		protocolClassComboViewer.addSelectionChangedListener(e -> updateProtocolClass(SelectionUtils.getFirstObject(e.getSelection(), ProtocolClass.class)));
		protocolClassComboViewer.setContentProvider(new ArrayContentProvider());
		protocolClassComboViewer.setLabelProvider(new LabelProvider());
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).applyTo(protocolClassComboViewer.getControl());
		
		lbl = new Label(plateGroup, SWT.NONE);
		lbl.setText("Creator:");
		
		plateCreatorText = new Text(plateGroup, SWT.BORDER);
		plateCreatorText.setText(plateTemplate.getCreator());
		plateCreatorText.setEnabled(false);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(plateCreatorText);
		
		lbl = new Label(plateGroup, SWT.NONE);
		lbl.setText("Dimensions:");

		Composite dimComp = new Composite(plateGroup, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(dimComp);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(dimComp);
		
		plateRowsText = new Text(dimComp, SWT.BORDER);
		plateRowsText.setText(""+plateTemplate.getRows());
		GridDataFactory.fillDefaults().hint(30, SWT.DEFAULT).applyTo(plateRowsText);
		plateRowsText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				plateRowsText.selectAll();
			}
		});
		plateRowsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				changePlateDimensions();
			}
		});

		lbl = new Label(dimComp, SWT.NONE);
		lbl.setText(" x ");

		plateColumnsText = new Text(dimComp, SWT.BORDER);
		plateColumnsText.setText(""+plateTemplate.getColumns());
		GridDataFactory.fillDefaults().hint(30, SWT.DEFAULT).applyTo(plateColumnsText);
		plateColumnsText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				plateColumnsText.selectAll();
			}
		});
		plateColumnsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				changePlateDimensions();
			}
		});

		/*
		 * The Actions group
		 * *****************
		 */

		Group actionsGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		actionsGroup.setText("Actions");
		GridDataFactory.fillDefaults().grab(false, false).applyTo(actionsGroup);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,5).applyTo(actionsGroup);

		saveBtn = new Button(actionsGroup, SWT.PUSH);
		saveBtn.setText("Save");
		saveBtn.setImage(IconManager.getIconImage("disk.png"));
		saveBtn.setToolTipText("Save all changes to this template");
		saveBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doSave(null);
			}
		});
		GridDataFactory.fillDefaults().applyTo(saveBtn);

		makeCopyBtn = new Button(actionsGroup, SWT.PUSH);
		makeCopyBtn.setText("Make Copy");
		makeCopyBtn.setImage(IconManager.getIconImage("disk_multiple.png"));
		makeCopyBtn.setToolTipText("Make a copy of this template with another ID");
		makeCopyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doCopy();
			}
		});
		GridDataFactory.fillDefaults().applyTo(makeCopyBtn);

		deleteBtn = new Button(actionsGroup, SWT.PUSH);
		deleteBtn.setText("Delete");
		deleteBtn.setImage(IconManager.getIconImage("bin_empty.png"));
		deleteBtn.setToolTipText("Delete this template");
		deleteBtn.setEnabled(PlateDefinitionService.getInstance().getTemplateManager().canDelete(plateTemplate));
		deleteBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doDelete();
			}
		});
		GridDataFactory.fillDefaults().applyTo(deleteBtn);
		
		/*
		 * The Well layout group
		 * *********************
		 */
		
		tabFolder = new CTabFolder(parent, SWT.NONE);
		tabFolder.setSelectionBackground(tabFolder.getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		GridDataFactory.fillDefaults().grab(true,true).span(3, 1).applyTo(tabFolder);
		
		tabs = new ITemplateTab[] {
				new OverviewTab(),
				new WellTypeTab(),
				new CompoundTab(),
				new ConcentrationTab(this.dataFormatSupport),
				new AnnotationTab()
		};
		
		Supplier<List<WellTemplate>> selectionSupplier = () -> currentSelection;
		Runnable templateRefresher = () -> {
			setDirty(true);
			resetHeatmap();
		};
		
		tabItems = new CTabItem[5];
		gridViewers = new GridViewer[tabItems.length];
		for (int i = 0; i < tabItems.length; i++) {
			tabItems[i] = new CTabItem(tabFolder, SWT.NONE);
			Composite container = new Composite(tabFolder, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
			tabItems[i].setText(tabs[i].getName());
			tabItems[i].setControl(container);
			
			Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
			GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(group);
			tabs[i].createEditingFields(group, plateTemplate, selectionSupplier, templateRefresher);
			
			gridViewers[i] = new GridViewer(container, plateTemplate.getRows(), plateTemplate.getColumns());
			gridViewers[i].setContentProvider(new AbstractGridContentProvider() {
				@Override
				public int getColumns(Object inputElement) {
					return plateTemplate.getColumns();
				}
				@Override
				public int getRows(Object inputElement) {
					return plateTemplate.getRows();
				}
				@Override
				public Object getElement(int row, int column) {
					int wellNr = NumberUtils.getWellNr(row+1, column+1, plateTemplate.getColumns());
					return plateTemplate.getWells().get(wellNr);
				}
			});
			final IGridCellRenderer r = tabs[i].createCellRenderer();
			gridViewers[i].setLabelProvider(new AbstractGridLabelProvider() {
				private IGridCellRenderer renderer = r;
				@Override
				public IGridCellRenderer createCellRenderer() {
					return renderer;
				}
			});
			gridViewers[i].setInput("root");
			GridDataFactory.fillDefaults().grab(true, true).applyTo(gridViewers[i].getGrid());

			gridViewers[i].addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					PlateTemplateEditor.this.selectionChanged(event);
				}
			});
			
			gridViewers[i].getGrid().addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					int modifier = e.stateMask & SWT.MODIFIER_MASK;
					if (modifier == SWT.CTRL && e.keyCode == 'c') copyToClipboard();
					if (modifier == SWT.CTRL && e.keyCode == 'v') pasteFromClipboard();
				}
			});
		}
		tabFolder.setSelection(0);
		
		/*
		 * Select an appropriate protocol class
		 */
		
		List<ProtocolClass> pClasses = ProtocolService.getInstance().getProtocolClasses();
		Collections.sort(pClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
		protocolClassComboViewer.setInput(pClasses);
		
		if (plateTemplate.getProtocolClassId() != 0) {
			protocolClass = ProtocolService.getInstance().getProtocolClass(plateTemplate.getProtocolClassId());
		} else {
			ISelection startingSelection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
			protocolClass = SelectionUtils.getFirstObject(startingSelection, ProtocolClass.class);
		}
		if (protocolClass == null) protocolClass = pClasses.get(0);
		protocolClassComboViewer.setSelection(new StructuredSelection(protocolClass));
		updateProtocolClass(protocolClass);

	}

	@Override
	public void setFocus() {
		plateIdText.setFocus();
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			// First apply the plate properties, and abort if they're invalid.
			if (!applyPlateChanges()) return;

			// New templates are not allowed to overwrite existing templates.
			PlateDefinitionService.getInstance().getTemplateManager().save(plateTemplate, monitor, isNewTemplate);
			setDirty(false);
			// Update the editor's title.
			setPartName(plateTemplate.getId());
			// The template is no longer new, and is allowed to overwrite itself.
			isNewTemplate = false;
			plateIdText.setEnabled(isNewTemplate);
			((PlateTemplateEditorInput)getEditorInput()).setNewTemplate(false);
			refreshTemplateBrowser();
		} catch (Exception e) {
			ErrorDialog.openError(getSite().getShell(),
					"Error saving template",
					"An error occured while saving the template.",
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
	}

	@Override
	public void doSaveAs() {
		// Do nothing.
	}

	/*
	 * Non-public
	 * **********
	 */

	private void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		currentSelection = SelectionUtils.getObjects(selection, WellTemplate.class);
		if (currentSelection.isEmpty()) return;
		for (int i = 0; i < gridViewers.length; i++) {
			if (selection.equals(gridViewers[i].getSelection())) continue;
			gridViewers[i].setSelection(selection);
		}
		for (int i = 0; i < tabs.length; i++) tabs[i].selectionChanged(currentSelection);
	}

	private void copyToClipboard() {
		ITemplateTab tab = tabs[tabFolder.getSelectionIndex()];
		String value = new SelectionToGrid().apply(plateTemplate, tab, currentSelection);
		if (value == null || value.trim().isEmpty()) return;
		Clipboard cb = new Clipboard(Display.getCurrent());
		try {
			Object[] datas = { value };
			Transfer[] transfers = { TextTransfer.getInstance() };
			cb.setContents(datas, transfers);
		} finally {
			cb.dispose();
		}
	}
	
	private void pasteFromClipboard() {
		Clipboard cb = new Clipboard(Display.getCurrent());
		try {
			TextTransfer textTransfer = TextTransfer.getInstance();
			String textData = (String)cb.getContents(textTransfer);
			if (textData != null && !textData.trim().isEmpty() && tabFolder.getSelectionIndex() != -1) {
				ITemplateTab tab = tabs[tabFolder.getSelectionIndex()];
				boolean modified = new GridToTemplate().apply(new PastedGrid(textData), plateTemplate, tab, currentSelection);
				setDirty(modified);
				if (modified) resetHeatmap();
			}
		} finally {
			cb.dispose();
		}
	}
	
	private void doCopy() {
		Shell shell = Display.getCurrent().getActiveShell();
		if (isDirty()) {
			MessageDialog.openWarning(shell, "Unsaved changes", "You must save the template before making a copy.");
			return;
		}
		InputDialog dialog = new InputDialog(shell, "Enter a new template ID", "Please enter a unique ID for the copy:",
				"Copy of " + plateTemplate.getId(), null);
		int retCode = dialog.open();
		if (retCode == Window.CANCEL) return;

		String newId = dialog.getValue();

		if (!FileUtils.isValidFilename(newId)) {
			MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
					"Invalid template ID",
					"The following characters are not allowed:\n"
					+ new String(FileUtils.ILLEGAL_FILENAME_CHARS));
			return;
		}
		
		try {
			PlateTemplateEditorInput input = new PlateTemplateEditorInput();
			PlateTemplate copy = plateTemplate.clone();
			copy.setId(newId);
			copy.setCreator(SecurityService.getInstance().getCurrentUserName());
			input.setPlateTemplate(copy);
			IEditorPart editor = getSite().getPage().openEditor(input, PlateTemplateEditor.class.getName());
			((PlateTemplateEditor)editor).setDirty(true);
		} catch (Exception e) {
			MessageDialog.openError(shell, "Cannot create copy", "The copy failed: " + e.getMessage());
		}
	}

	private void doDelete() {
		boolean confirmed = MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				"Delete template",
				"Are you sure you want to delete this plate template?");
		if (confirmed) {
			try {
				PlateDefinitionService.getInstance().getTemplateManager().delete(plateTemplate);
				getSite().getPage().closeEditor(this, false);
				refreshTemplateBrowser();
			} catch (IOException e) {
				ErrorDialog.openError(getSite().getShell(),
						"Error deleting template",
						"An error occured while deleting the template.",
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
			}
		}
	}

	private boolean changePlateDimensions() {
		setDirty(true);

		if (plateRowsText.getText().isEmpty()) return false;
		if (plateColumnsText.getText().isEmpty()) return false;
		int rows = Integer.parseInt(plateRowsText.getText());
		int cols = Integer.parseInt(plateColumnsText.getText());
		if (rows < 1 || cols < 1) return false;

		plateTemplate.setRows(rows);
		plateTemplate.setColumns(cols);
		for (int i = 0; i < gridViewers.length; i++) gridViewers[i].getGrid().resetGrid(rows, cols);
		resetHeatmap();
		return true;
	}

	private void updateProtocolClass(ProtocolClass pClass) {
		protocolClass = pClass;
		for (int i = 0; i < tabs.length; i++) {
			tabs[i].protocolClassChanged(pClass);
		}
	}
	
	private boolean applyPlateChanges() {
		String id = plateIdText.getText();

		if (!FileUtils.isValidFilename(id)) {
			MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
					"Invalid template ID",
					"The following characters are not allowed:\n"
					+ new String(FileUtils.ILLEGAL_FILENAME_CHARS));
			return false;
		}
		if (isNewTemplate && PlateDefinitionService.getInstance().getTemplateManager().exists(id)) {
			MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
					"Invalid template ID",
					"A template with ID '" + id + "' already exists.\n"
					+ "Please use another ID.");
			return false;
		}

		plateTemplate.setId(id);
		plateTemplate.setProtocolClassId(protocolClass.getId());
		return true;
	}

	private void resetHeatmap() {
		for (int i = 0; i < gridViewers.length; i++) gridViewers[i].setInput("root");
	}
	
	private void reloadData() {
		if (this.tabFolder == null || this.tabFolder.isDisposed()
				|| this.currentSelection == null) {
			return;
		}
		for (int i = 0; i < tabs.length; i++) {
			tabs[i].selectionChanged(this.currentSelection); // edit field
		}
		resetHeatmap();
	}

	private void refreshTemplateBrowser() {
		// Refresh the templates list, if it's currently open.
		PlateTemplateBrowser view = (PlateTemplateBrowser)getSite().getPage().findView(PlateTemplateBrowser.class.getName());
		if (view != null) view.reloadTemplates();
	}
}
