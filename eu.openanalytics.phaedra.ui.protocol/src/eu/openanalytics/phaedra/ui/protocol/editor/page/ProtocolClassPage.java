package eu.openanalytics.phaedra.ui.protocol.editor.page;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.admin.fs.EditFSFileCmd;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.autocomplete.ComboAutoCompleteField;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;
import eu.openanalytics.phaedra.ui.protocol.editor.ProtocolClassEditor;
import eu.openanalytics.phaedra.ui.protocol.editor.page.security.EditorsContentProvider;

public class ProtocolClassPage extends FormPage {

	private Text textName;
	private Text textDescription;

	private Label labelId;

	private Button checkIsEditable;
	private Button checkIsInDevelopment;

	private CCombo comboDefaultLinkSource;
	private CCombo comboDefaultCaptureConfig;
	
	private ComboViewer defaultFeatureCmbViewer;
	private ComboAutoCompleteField defaultFeatureAutoComplete;
	
	private CCombo comboHigerBound;
	private CCombo comboLowerBound;

	private ProtocolClass protocolClass;

	private TreeViewer permissionsTreeViewer;
	private IMessageManager msgManager;

	private ProtocolClassEditor pceditor;

	private boolean dirty = false;

	public ProtocolClassPage(FormEditor editor, String id, String title) {
		super(editor, id, title);

		if (editor instanceof ProtocolClassEditor) {
			pceditor = (ProtocolClassEditor) editor;
			protocolClass = pceditor.getProtocolClass();
		}
	}

	private KeyAdapter dirtyKeyListener = new KeyAdapter() {
		@Override
		public void keyPressed(final KeyEvent e) {
			markDirty();
		}
	};

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();

		String suffix = "";
		if (!getEditor().isSaveAsAllowed()) suffix += " (Locked)";

		form.setText("Protocol Class: " + protocolClass.getName() + suffix);
		form.setImage(IconManager.getIconImage("struct.png"));

		Composite body = form.getBody();
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(body);
		toolkit.paintBordersFor(body);

		toolkit.decorateFormHeading(managedForm.getForm().getForm());
		msgManager = getManagedForm().getMessageManager();
		msgManager.setDecorationPosition(SWT.TOP);

		Section section = toolkit.createSection(body, Section.TITLE_BAR | Section.DESCRIPTION);
		section.setDescription("This section contains the general settings of the protocol class.");
		section.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		section.setText("General");

		Composite compositeBase = toolkit.createComposite(section, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginBottom = 20;
		gridLayout.horizontalSpacing = 10;
		gridLayout.numColumns = 2;
		compositeBase.setLayout(gridLayout);
		section.setClient(compositeBase);
		toolkit.paintBordersFor(compositeBase);

		compositeBase.setEnabled(getEditor().isSaveAsAllowed());

		Label label = toolkit.createLabel(compositeBase, "Name:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));

		textName = toolkit.createText(compositeBase, null, SWT.NONE);
		textName.addKeyListener(dirtyKeyListener);
		final GridData gd_textName = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textName.setLayoutData(gd_textName);
		textName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getEditor().setPartName(textName.getText());
			}
		});

		label = toolkit.createLabel(compositeBase, "Id:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));

		labelId = toolkit.createLabel(compositeBase, null, SWT.NONE);
		final GridData gd_labelId = new GridData(SWT.FILL, SWT.CENTER, true, false);
		labelId.setLayoutData(gd_labelId);
		if (protocolClass != null) {
			labelId.setText("" + protocolClass.getId());
		}

		label = toolkit.createLabel(compositeBase, "Description:", SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		textDescription = toolkit.createText(compositeBase, null, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		final GridData gd_textDescription = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd_textDescription.heightHint = 53;
		textDescription.setLayoutData(gd_textDescription);
		textDescription.addKeyListener(dirtyKeyListener);

		toolkit.createLabel(compositeBase, "Lock Status:", SWT.NONE);

		checkIsEditable = toolkit.createButton(compositeBase, "Editable" + " (team members with the manager role can edit this protocol class)", SWT.CHECK);
		checkIsEditable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		});

		toolkit.createLabel(compositeBase, "Development Status:", SWT.NONE);

		checkIsInDevelopment = toolkit.createButton(compositeBase, "In Development (team members without the manager role " + "can also edit this protocol class)", SWT.CHECK);
		checkIsInDevelopment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		});

		new Label(compositeBase, SWT.NONE);
		new Label(compositeBase, SWT.NONE);

		toolkit.createLabel(compositeBase, "Default Well Feature:", SWT.NONE);

		Composite subcomp = toolkit.createComposite(compositeBase, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subcomp);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(1, 1).applyTo(subcomp);
		toolkit.paintBordersFor(subcomp);

		defaultFeatureCmbViewer = new ComboViewer(subcomp, SWT.BORDER);
		defaultFeatureCmbViewer.setContentProvider(new ArrayContentProvider());
		defaultFeatureCmbViewer.setLabelProvider(new LabelProvider());
		defaultFeatureCmbViewer.addSelectionChangedListener(e -> {
			String name = defaultFeatureCmbViewer.getCombo().getText();
			if (name == null || name.isEmpty()) return;
			Feature defaultFeature = ProtocolUtils.getFeatures(protocolClass).stream()
					.filter(f -> f.getDisplayName().equals(name))
					.findAny().orElse(null);
			if (protocolClass.getDefaultFeature() != null && protocolClass.getDefaultFeature().equals(defaultFeature)) return;
			protocolClass.setDefaultFeature(defaultFeature);
			markDirty();
		});
		defaultFeatureAutoComplete = new ComboAutoCompleteField(defaultFeatureCmbViewer, new String[0]);
		GridDataFactory.fillDefaults().hint(280, SWT.DEFAULT).applyTo(defaultFeatureCmbViewer.getControl());
		fillComboDefaultFeature();

		Hyperlink refreshFeaturesLnk = toolkit.createHyperlink(subcomp, "Refresh", SWT.NONE);
		refreshFeaturesLnk.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				fillComboDefaultFeature();
			}
		});

		toolkit.createLabel(compositeBase, "Default Link Source:", SWT.NONE);

		comboDefaultLinkSource = new CCombo(compositeBase, SWT.BORDER | SWT.READ_ONLY);
		comboDefaultLinkSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				markDirty();
			}
		});
		GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(comboDefaultLinkSource);
		fillComboLinkSource();
		
		toolkit.createLabel(compositeBase, "Default Capture Configuration:", SWT.NONE);

		subcomp = toolkit.createComposite(compositeBase, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subcomp);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(1, 1).applyTo(subcomp);
		toolkit.paintBordersFor(subcomp);
		
		comboDefaultCaptureConfig = new CCombo(subcomp, SWT.BORDER | SWT.READ_ONLY);
		comboDefaultCaptureConfig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				markDirty();
			}
		});
		GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(comboDefaultCaptureConfig);
		fillComboDefaultCaptureConfig();
		
		Hyperlink editConfigLnk = toolkit.createHyperlink(subcomp, "Edit", SWT.NONE);
		editConfigLnk.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				EditFSFileCmd.execute(ProtocolService.getInstance().getDCConfigFile(comboDefaultCaptureConfig.getText()));
			}
		});
		
		new Label(compositeBase, SWT.NONE);
		new Label(compositeBase, SWT.NONE);

		toolkit.createLabel(compositeBase, "Low Control Type:", SWT.NONE);

		comboLowerBound = new CCombo(compositeBase, SWT.BORDER | SWT.READ_ONLY);
		final GridData gd_comboLowerBound = new GridData(150, SWT.DEFAULT);
		comboLowerBound.setLayoutData(gd_comboLowerBound);
		comboLowerBound.setVisibleItemCount(20);
		comboLowerBound.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		});

		toolkit.createLabel(compositeBase, "High Control Type:", SWT.NONE);

		comboHigerBound = new CCombo(compositeBase, SWT.BORDER | SWT.READ_ONLY);
		final GridData gd_comboHigerBound = new GridData(150, SWT.DEFAULT);
		comboHigerBound.setLayoutData(gd_comboHigerBound);
		comboHigerBound.setVisibleItemCount(20);
		toolkit.adapt(comboHigerBound, true, true);
		comboHigerBound.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		});

		fillComboHigherAndLowerBounds();

		final Section sectionTeamsAndUsers = toolkit.createSection(body, Section.TITLE_BAR | Section.DESCRIPTION | Section.EXPANDED | Section.TWISTIE);
		sectionTeamsAndUsers.setDescription("The following users can EDIT this protocol class:");
		final GridData gd_sectionTeamsAndUsers = new GridData(SWT.FILL, SWT.FILL, true, true);
		sectionTeamsAndUsers.setLayoutData(gd_sectionTeamsAndUsers);
		sectionTeamsAndUsers.setText("Protocol Class Permissions");

		final Composite compositeGroups = toolkit.createComposite(sectionTeamsAndUsers, SWT.NONE);
		compositeGroups.setLayout(new GridLayout());
		toolkit.paintBordersFor(compositeGroups);
		sectionTeamsAndUsers.setClient(compositeGroups);

		permissionsTreeViewer = new TreeViewer(compositeGroups, SWT.NONE);
		permissionsTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		permissionsTreeViewer.setContentProvider(new EditorsContentProvider());
		permissionsTreeViewer.setLabelProvider(new LabelProvider());
		permissionsTreeViewer.setInput(protocolClass);
		permissionsTreeViewer.expandToLevel(2);

		managedForm.getForm().getToolBarManager().add(getEditor().getSaveAction());
		managedForm.getForm().getToolBarManager().update(true);

		new Label(body, SWT.NONE);
		initDataBindings();
	}

	@Override
	public ProtocolClassEditor getEditor() {
		return (ProtocolClassEditor) super.getEditor();
	}

	private void fillComboLinkSource() {
		comboDefaultLinkSource.removeAll();
		comboDefaultLinkSource.setItems(PlateDefinitionService.getInstance().getSourceIds());
		if (comboDefaultLinkSource.getItemCount() > 0) {
			comboDefaultLinkSource.select(0);
		}
	}

	private void markDirty() {
		dirty = true;
		getEditor().markDirty();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	private void fillComboDefaultFeature() {
		List<Feature> features = protocolClass.getFeatures();
		if (features.isEmpty()) return;

		String[] names = features.stream().map(ProtocolUtils.FEATURE_NAMES).toArray(size -> new String[size]);
		defaultFeatureCmbViewer.setInput(names);
		defaultFeatureAutoComplete.setProposals(names);
		
		Feature defaultFeature = protocolClass.getDefaultFeature();
		if (defaultFeature == null) defaultFeature = CollectionUtils.find(features, ProtocolUtils.NUMERIC_FEATURES);
		if (defaultFeature != null) defaultFeatureCmbViewer.setSelection(new StructuredSelection(ProtocolUtils.FEATURE_NAMES.apply(defaultFeature)), true);
	}

	private void fillComboDefaultCaptureConfig() {
		try {
			comboDefaultCaptureConfig.setItems(DataCaptureService.getInstance().getAllCaptureConfigIds());
		} catch (IOException e) {}
		String defaultImporter = protocolClass.getDefaultCaptureConfig();
		if (defaultImporter != null) {
			comboDefaultCaptureConfig.setText(defaultImporter);
		} else if (comboDefaultCaptureConfig.getItemCount() > 0) {
			comboDefaultCaptureConfig.select(0);
		}
	}

	private void fillComboHigherAndLowerBounds() {
		List<WellType> types = ProtocolService.getInstance().getWellTypes();

		String[] welltypes = new String[types.size()];
		int i = 0;
		for (WellType type : types) {
			welltypes[i] = type.getCode();
			i++;
		}

		comboLowerBound.setItems(welltypes);
		comboHigerBound.setItems(welltypes);
	}

	protected DataBindingContext initDataBindings() {

		DataBindingContext ctx = new DataBindingContext();

		FormEditorUtils.bindText(textName, protocolClass, "name", ctx);
		FormEditorUtils.bindText(textDescription, protocolClass, "description", ctx);
		FormEditorUtils.bindSelection(checkIsEditable, protocolClass, "editable", ctx);
		FormEditorUtils.bindSelection(checkIsInDevelopment, protocolClass, "inDevelopment", ctx);
		FormEditorUtils.bindSelection(comboDefaultLinkSource, protocolClass, "defaultLinkSource", ctx);
		FormEditorUtils.bindSelection(comboDefaultCaptureConfig, protocolClass, "defaultCaptureConfig", ctx);
		FormEditorUtils.bindSelection(comboHigerBound, protocolClass, "highWellTypeCode", ctx);
		FormEditorUtils.bindSelection(comboLowerBound, protocolClass, "lowWellTypeCode", ctx);

		return ctx;
	}

}
