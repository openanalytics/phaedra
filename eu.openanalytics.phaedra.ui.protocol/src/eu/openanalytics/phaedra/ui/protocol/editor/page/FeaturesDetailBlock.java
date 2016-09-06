package eu.openanalytics.phaedra.ui.protocol.editor.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationLanguage;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService.NormalizationScope;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveMethod;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveModel;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveType;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;
import eu.openanalytics.phaedra.ui.protocol.dialog.ManageGroupsDialog;
import eu.openanalytics.phaedra.ui.protocol.util.ClassificationTableFactory;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;
import eu.openanalytics.phaedra.ui.protocol.util.ListenerHelper;

public class FeaturesDetailBlock implements IDetailsPage {

	private Feature feature;

	private boolean dirty;
	private FeaturesPage parentPage;

	private IManagedForm managedform;
	private DataBindingContext m_bindingContext;
	private FeaturesMasterBlock master;

	private Text textName;
	private Text textAlias;
	private Text textDescription;
	private CCombo comboFormatString;
	private CCombo comboGrouping;

	private Button checkKeyfeature;
	private Button checkNumeric;
	private Button checkAnnotation;
	private Button checkExport;

	private Text textCalcFormula;
	private CCombo comboFormulaLanguage;
	private CCombo comboFormulaTrigger;
	private Spinner spinnerSeq;

	private Button predefinedNormBtn;
	private Button customNormBtn;
	private CCombo comboNormalization;
	private CCombo customNormLanguage;
	private CCombo normScopeCmb;
	private Text customNormTxt;
	private Button editNormScriptBtn;
	private CCombo comboLowType;
	private CCombo comboHighType;

	private TableViewer classificationTableViewer;
	private WritableList classifications;
	private Button checkClassificationRestricted;
	
	private CCombo colorMethods;

	private CCombo comboCurveKind;
	private CCombo comboCurveModel;
	private CCombo comboCurveMethod;
	private CCombo comboCurveType;

	private CCombo comboCurveGroupBy1;
	private CCombo comboCurveGroupBy2;
	private CCombo comboCurveGroupBy3;
	
	private Text txtCurveThreshold;
	private Text txtCurveManualLB;
	private Text txtCurveManualUB;


	public FeaturesDetailBlock(FeaturesPage page, FeaturesMasterBlock master) {
		this.master = master;
		this.parentPage = page;
	}

	@Override
	public void initialize(IManagedForm form) {
		this.managedform = form;
	}

	@Override
	public void createContents(Composite parent) {

		FormToolkit toolkit = managedform.getToolkit();
		final GridLayout gridLayout = new GridLayout();
		parent.setLayout(gridLayout);

		/*
		 * Dirty & change listeners
		 * ************************
		 */

		final KeyAdapter dirtyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				parentPage.markDirty();
				master.refreshViewer();
			}
		};
		final SelectionAdapter selectionDirtyAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parentPage.markDirty();
				master.refreshViewer();
			}
		};
		SelectionAdapter curveChangedAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parentPage.setCurvesChanged(true);

			}
		};
		KeyAdapter curveChangedKeyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				parentPage.setCurvesChanged(true);
			}
		};

		/*
		 * Section: general
		 * ****************
		 */

		final Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText("General Feature Section");

		ImageHyperlink generalIcon = toolkit.createImageHyperlink(section, SWT.TRANSPARENT);
		generalIcon.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				section.setExpanded(true);
			}
		});
		generalIcon.setImage(IconManager.getIconImage("tag_blue.png"));
		generalIcon.setToolTipText(section.getDescription());
		section.setTextClient(generalIcon);
		toolkit.paintBordersFor(generalIcon);

		final Composite compositeGeneral = toolkit.createComposite(section, SWT.NONE);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.horizontalSpacing = 10;
		gridLayout_1.marginBottom = 10;
		gridLayout_1.numColumns = 2;
		compositeGeneral.setLayout(gridLayout_1);
		toolkit.paintBordersFor(compositeGeneral);
		section.setClient(compositeGeneral);

		Label label = toolkit.createLabel(compositeGeneral, "Name:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));

		textName = toolkit.createText(compositeGeneral, null, SWT.NONE);
		textName.addModifyListener(ListenerHelper.createTextNotEmptyListener("Name", managedform.getMessageManager()));
		textName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		toolkit.createLabel(compositeGeneral, "Alias:", SWT.NONE);
		textAlias = toolkit.createText(compositeGeneral, null, SWT.NONE);
		textAlias.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label label_1 = toolkit.createLabel(compositeGeneral, "Description:", SWT.NONE);
		label_1.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		textDescription = toolkit.createText(compositeGeneral, null, SWT.WRAP | SWT.V_SCROLL);
		final GridData gd_textDescription = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd_textDescription.heightHint = 41;
		textDescription.setLayoutData(gd_textDescription);

		label = toolkit.createLabel(compositeGeneral, "Format:", SWT.NONE);

		comboFormatString = new CCombo(compositeGeneral, SWT.BORDER);
		comboFormatString.setItems(new String[] { ".####", ".00", ".0000" });
		comboFormatString.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboFormatString.setVisibleItemCount(20);
		toolkit.adapt(comboFormatString, true, true);

		toolkit.createLabel(compositeGeneral, "Group:", SWT.NONE);
		comboGrouping = new CCombo(compositeGeneral, SWT.BORDER | SWT.READ_ONLY);
		comboGrouping.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboGrouping.setVisibleItemCount(20);
		toolkit.adapt(comboGrouping, true, true);

		toolkit.createLabel(compositeGeneral, "", SWT.NONE);
		Link manageGroupsBtn = new Link(compositeGeneral, SWT.NONE);
		manageGroupsBtn.setText("<a>Manage Groups</a>");
		manageGroupsBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				int buttonPressed = new ManageGroupsDialog(Display.getDefault().getActiveShell(), feature.getProtocolClass(), GroupType.WELL).open();
				if (buttonPressed == Window.OK) {
					fillGroupingCombo();
					parentPage.markDirty();
				}
			}
		});

		final Composite composite_1 = toolkit.createCompositeSeparator(compositeGeneral);
		final GridData gd_composite_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_composite_1.verticalIndent = 5;
		gd_composite_1.heightHint = 1;
		composite_1.setLayoutData(gd_composite_1);

		Label checkboxesLabel = toolkit.createLabel(compositeGeneral, "This is a:", SWT.NONE);
		checkboxesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		checkKeyfeature = toolkit.createButton(compositeGeneral, "Key feature (available in all tables and charts)", SWT.CHECK);
		checkKeyfeature.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(compositeGeneral, SWT.NONE);
		checkNumeric = toolkit.createButton(compositeGeneral, "Numeric feature (contains a numeric value)", SWT.CHECK);
		checkNumeric.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(compositeGeneral, SWT.NONE);
		checkAnnotation = toolkit.createButton(compositeGeneral, "Annotation feature (represents a well annotation rather than a feature value)", SWT.CHECK);
		checkAnnotation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		new Label(compositeGeneral, SWT.NONE);
		checkExport = toolkit.createButton(compositeGeneral, "Export feature (after approval, will be exported to warehouse)", SWT.CHECK);
		checkExport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		/*
		 * Section: Calculation
		 * ********************
		 */

		final Section section_1 = toolkit.createSection(parent, Section.TITLE_BAR | Section.DESCRIPTION | Section.TWISTIE);
		section_1.setDescription("To turn this feature into a Calculated Feature, enter a formula below.");
		section_1.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		section_1.setText("Calculation Section");

		ImageHyperlink formulaIcon = toolkit.createImageHyperlink(section_1, SWT.TRANSPARENT);
		formulaIcon.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				section_1.setExpanded(true);
			}
		});
		formulaIcon.setImage(IconManager.getIconImage("aggregation.gif"));
		formulaIcon.setToolTipText("");
		section_1.setTextClient(formulaIcon);
		toolkit.paintBordersFor(formulaIcon);

		final Composite compositeFormula = toolkit.createComposite(section_1, SWT.NONE);
		compositeFormula.setLayout(new GridLayout(3,false));
		toolkit.paintBordersFor(compositeFormula);
		section_1.setClient(compositeFormula);

		Label formulaLabel = toolkit.createLabel(compositeFormula, "Formula:", SWT.NONE);
		GridDataFactory.fillDefaults().hint(70, SWT.DEFAULT).align(SWT.BEGINNING, SWT.BEGINNING).applyTo(formulaLabel);

		textCalcFormula = toolkit.createText(compositeFormula, null, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		final GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd_text.heightHint = 60;
		textCalcFormula.setLayoutData(gd_text);

		final Button editBtn = new Button(compositeFormula, SWT.PUSH);
		final GridData gd_edit = new GridData(SWT.FILL, SWT.TOP, false, false);
		editBtn.setLayoutData(gd_edit);
		editBtn.setText("Edit expression...");
		editBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CalculationLanguage lang = CalculationLanguage.getLanguages()[comboFormulaLanguage.getSelectionIndex()];
				Shell shell = Display.getDefault().getActiveShell();
				String newFormula = lang.openEditor(shell, textCalcFormula.getText(), feature.getProtocolClass());
				if (newFormula != null) textCalcFormula.setText(newFormula);
				master.refreshViewer();
			}
		});

		toolkit.createLabel(compositeFormula, "Language:", SWT.NONE);

		String[] languageNames = Arrays.stream(CalculationLanguage.getLanguages()).map(l -> l.getLabel()).toArray(i -> new String[i]);
		comboFormulaLanguage = new CCombo(compositeFormula, SWT.BORDER | SWT.READ_ONLY | SWT.FILL);
		comboFormulaLanguage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboFormulaLanguage.setItems(languageNames);
		new Label(compositeFormula, SWT.NONE);

		toolkit.createLabel(compositeFormula, "Calculation:", SWT.NONE);

		String[] triggerNames = Arrays.stream(CalculationTrigger.values()).map(t -> t.getLabel()).toArray(i -> new String[i]);
		comboFormulaTrigger = new CCombo(compositeFormula, SWT.BORDER | SWT.READ_ONLY | SWT.FILL);
		comboFormulaTrigger.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboFormulaTrigger.setItems(triggerNames);
		new Label(compositeFormula, SWT.NONE);

		Label sequenceLabel = toolkit.createLabel(compositeFormula, "Sequence:", SWT.NONE);
		sequenceLabel.setLayoutData(new GridData(110, SWT.DEFAULT));

		spinnerSeq = new Spinner(compositeFormula, SWT.BORDER | SWT.FILL);
		spinnerSeq.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		spinnerSeq.setMinimum(0);
		spinnerSeq.setMaximum(Integer.MAX_VALUE);
		spinnerSeq.setIncrement(1);
		spinnerSeq.setPageIncrement(100);

		/*
		 * Section: Normalization
		 * **********************
		 */

		final Section normalizationSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.DESCRIPTION | Section.TWISTIE);
		normalizationSection.setDescription("To normalize this feature, choose a normalization method and the control types for normalization."
				+ "\nIf control types are not set, the protocol class defaults will be used.");
		normalizationSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		normalizationSection.setText("Normalization Section");

		ImageHyperlink icon = toolkit.createImageHyperlink(normalizationSection, SWT.TRANSPARENT);
		icon.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				normalizationSection.setExpanded(true);
			}
		});
		icon.setImage(IconManager.getIconImage("calculator.png"));
		icon.setToolTipText("");
		normalizationSection.setTextClient(icon);

		Composite normalizationCmp = toolkit.createComposite(normalizationSection, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).margins(5,5).spacing(5, 5).applyTo(normalizationCmp);
		normalizationSection.setClient(normalizationCmp);
		toolkit.paintBordersFor(normalizationCmp);

		predefinedNormBtn = toolkit.createButton(normalizationCmp, "Predefined method:", SWT.RADIO);
		predefinedNormBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleNormalizationButtons(customNormBtn.getSelection());
			}
		});
		predefinedNormBtn.setLayoutData(new GridData(140, SWT.DEFAULT));

		comboNormalization = new CCombo(normalizationCmp, SWT.BORDER | SWT.READ_ONLY);
		comboNormalization.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		comboNormalization.setItems(NormalizationService.getInstance().getNormalizations());
		comboNormalization.setVisibleItemCount(20);
		toolkit.adapt(comboNormalization, true, true);

		new Label(normalizationCmp, SWT.NONE);
		label = toolkit.createLabel(normalizationCmp, "Low Control Type:", SWT.NONE);
		comboLowType = new CCombo(normalizationCmp, SWT.BORDER | SWT.READ_ONLY);
		comboLowType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		toolkit.adapt(comboLowType, true, true);

		new Label(normalizationCmp, SWT.NONE);
		label = toolkit.createLabel(normalizationCmp, "High Control Type:", SWT.NONE);
		comboHighType = new CCombo(normalizationCmp, SWT.BORDER | SWT.READ_ONLY);
		comboHighType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		toolkit.adapt(comboHighType, true, true);

		customNormBtn = toolkit.createButton(normalizationCmp, "Custom method:", SWT.RADIO);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(customNormBtn);
		customNormBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleNormalizationButtons(customNormBtn.getSelection());
			}
		});

		customNormTxt = toolkit.createText(normalizationCmp, "", SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 60).grab(true,true).span(2, 1).applyTo(customNormTxt);

		editNormScriptBtn = toolkit.createButton(normalizationCmp, "Edit...", SWT.PUSH);
		editNormScriptBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CalculationLanguage lang = CalculationLanguage.getLanguages()[customNormLanguage.getSelectionIndex()];
				Shell shell = Display.getDefault().getActiveShell();
				String newFormula = lang.openEditor(shell, customNormTxt.getText(), feature.getProtocolClass());
				if (newFormula != null) customNormTxt.setText(newFormula);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(editNormScriptBtn);

		new Label(normalizationCmp, SWT.NONE);
		label = toolkit.createLabel(normalizationCmp, "Language:", SWT.NONE);
		customNormLanguage = new CCombo(normalizationCmp, SWT.BORDER | SWT.READ_ONLY | SWT.FILL);
		customNormLanguage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		customNormLanguage.setItems(languageNames);

		new Label(normalizationCmp, SWT.NONE);
		label = toolkit.createLabel(normalizationCmp, "Scope:", SWT.NONE);
		normScopeCmb = new CCombo(normalizationCmp, SWT.BORDER | SWT.READ_ONLY | SWT.FILL);
		normScopeCmb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		/*
		 * Section: classification
		 * ***********************
		 */

		final Section sectionClassification = toolkit.createSection(parent, Section.TITLE_BAR | Section.DESCRIPTION | Section.TWISTIE);
		sectionClassification.setDescription("Define feature classes to perform classification on this feature.");
		sectionClassification.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sectionClassification.setText("Classification Section");

		ImageHyperlink sectionIcon = toolkit.createImageHyperlink(sectionClassification, SWT.TRANSPARENT);
		sectionIcon.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				sectionClassification.setExpanded(true);
			}
		});
		sectionIcon.setImage(IconManager.getIconImage("tag_blue_edit.png"));
		sectionIcon.setToolTipText(sectionClassification.getDescription());
		sectionClassification.setTextClient(sectionIcon);
		toolkit.paintBordersFor(sectionIcon);

		Composite compositeClassification = toolkit.createComposite(sectionClassification, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).spacing(10, SWT.DEFAULT).applyTo(compositeClassification);
		sectionClassification.setClient(compositeClassification);
		toolkit.paintBordersFor(compositeClassification);

		Link link = ClassificationTableFactory.createAddClassLink(compositeClassification, new Listener() {
			@Override
			public void handleEvent(Event event) {
				FeatureClass newClass = ProtocolService.getInstance().createFeatureClass();
				classifications.add(newClass);
				classificationTableViewer.refresh();
				selectionDirtyAdapter.widgetSelected(null);
			}
		});
		link.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		link = ClassificationTableFactory.createRemoveClassLink(compositeClassification, new Listener() {
			@Override
			public void handleEvent(Event event) {
				StructuredSelection selection = (StructuredSelection) classificationTableViewer.getSelection();
				if (selection != null && !selection.isEmpty()) {
					// Ask for confirmation.
					Shell shell = Display.getDefault().getActiveShell();
					boolean confirmed = MessageDialog.openQuestion(shell, "Delete?",
							"Are you sure you want to delete the selected feature classes?");
					if (!confirmed) return;

					// Remove all selected elements.
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						FeatureClass classToRemove = (FeatureClass) iterator.next();
						classifications.remove(classToRemove);
					}
					classificationTableViewer.refresh();
					selectionDirtyAdapter.widgetSelected(null);
				}
			}
		});
		link.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		classificationTableViewer = ClassificationTableFactory.createTableViewer(compositeClassification, true, selectionDirtyAdapter);
		classificationTableViewer.setComparer(ClassificationTableFactory.createClassComparer());
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT,200).grab(true, false).indent(0, 5).span(4,1).applyTo(classificationTableViewer.getTable());

		checkClassificationRestricted = toolkit.createButton(compositeClassification, "Restrict values: only the values listed above are allowed", SWT.CHECK);
		GridDataFactory.fillDefaults().indent(0, 5).span(4, 1).applyTo(checkClassificationRestricted);

		/*
		 * Section: color method
		 * *********************
		 */

		final Section sectionColorMethod = toolkit.createSection(parent, Section.TITLE_BAR | Section.DESCRIPTION | Section.TWISTIE);
		sectionColorMethod.setDescription("The color method is used in plate heatmaps and other visual components.");
		sectionColorMethod.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sectionColorMethod.setText("Color Method Section");

		ImageHyperlink calibrationIcon = toolkit.createImageHyperlink(sectionColorMethod, SWT.TRANSPARENT);
		calibrationIcon.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				sectionColorMethod.setExpanded(true);
			}
		});
		calibrationIcon.setImage(IconManager.getIconImage("color_wheel.png"));
		formulaIcon.setToolTipText(sectionColorMethod.getDescription());
		sectionColorMethod.setTextClient(calibrationIcon);
		toolkit.paintBordersFor(calibrationIcon);

		final Composite compositeColorMethod = toolkit.createComposite(sectionColorMethod, SWT.NONE);
		final GridLayout gridLayout_2 = new GridLayout();
		gridLayout_2.numColumns = 3;
		gridLayout_2.horizontalSpacing = 10;
		compositeColorMethod.setLayout(gridLayout_2);
		sectionColorMethod.setClient(compositeColorMethod);
		toolkit.paintBordersFor(compositeColorMethod);

		Label colorMethodLabel = toolkit.createLabel(compositeColorMethod, "Color Method:", SWT.NONE);
		colorMethodLabel.setLayoutData(new GridData(110, SWT.DEFAULT));

		colorMethods = new CCombo(compositeColorMethod, SWT.BORDER | SWT.READ_ONLY);
		colorMethods.setLayoutData(new GridData(140, SWT.DEFAULT));
		colorMethods.setItems(ColorMethodRegistry.getInstance().getNames());
		colorMethods.setVisibleItemCount(10);
		colorMethods.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] ids = ColorMethodRegistry.getInstance().getIds();
				int index = colorMethods.getSelectionIndex();
				String newId = ids[index];
				feature.getColorMethodSettings().put(ColorMethodFactory.SETTING_METHOD_ID, newId);
			}
		});
		toolkit.adapt(colorMethods, true, true);

		Button browseBtn = new Button(compositeColorMethod, SWT.PUSH);
		browseBtn.setText("Settings...");
		browseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IColorMethod cm = ColorMethodFactory.createColorMethod(feature, false);
				Dialog dialog = cm.createDialog(Display.getCurrent().getActiveShell());
				if (dialog == null) return;
				int retCode = dialog.open();
				if (retCode == Dialog.OK) {
					// Obtain settings from cm and apply to feature
					cm.getConfiguration(feature.getColorMethodSettings());
					parentPage.markDirty();
				}
			}
		});
		toolkit.adapt(browseBtn, true, true);

		/*
		 * Section: curve settings
		 * ***********************
		 */

		final Section sectionCurveSettings = toolkit.createSection(parent, Section.TITLE_BAR | Section.DESCRIPTION | Section.TWISTIE);
		sectionCurveSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sectionCurveSettings.setDescription("Set the properties below to perform curve fitting on this feature.");
		sectionCurveSettings.setText("Curve Section");

		ImageHyperlink curveIcon = toolkit.createImageHyperlink(sectionCurveSettings, SWT.TRANSPARENT);
		curveIcon.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				sectionCurveSettings.setExpanded(true);
			}
		});
		curveIcon.setImage(IconManager.getIconImage("curve.png"));
		curveIcon.setToolTipText(sectionCurveSettings.getDescription());
		sectionCurveSettings.setTextClient(curveIcon);
		toolkit.paintBordersFor(curveIcon);

		final Composite compositeCurve = toolkit.createComposite(sectionCurveSettings, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(10, 5).numColumns(2).applyTo(compositeCurve);
		sectionCurveSettings.setClient(compositeCurve);
		toolkit.paintBordersFor(compositeCurve);

		link = new Link(compositeCurve, SWT.NONE);
		link.setText("<a>Clear settings</a>");
		link.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				clearCurveSettings();
			}
		});
		link.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		GridDataFactory.fillDefaults().span(2,1).applyTo(link);

		label = toolkit.createLabel(compositeCurve, "Curve Kind:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		comboCurveKind = new CCombo(compositeCurve, SWT.BORDER | SWT.READ_ONLY);
		comboCurveKind.setLayoutData(new GridData(180, SWT.DEFAULT));
		comboCurveKind.setItems(StringUtils.getEnumNames(CurveService.getInstance().getKinds()));
		comboCurveKind.setVisibleItemCount(20);
		toolkit.adapt(comboCurveKind, true, true);

		label = toolkit.createLabel(compositeCurve, "Curve Model:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		comboCurveModel = new CCombo(compositeCurve, SWT.BORDER);
		comboCurveModel.setLayoutData(new GridData(180, SWT.DEFAULT));
		comboCurveModel.setVisibleItemCount(20);
		toolkit.adapt(comboCurveModel, true, true);

		label = toolkit.createLabel(compositeCurve, "Curve Method:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		comboCurveMethod = new CCombo(compositeCurve, SWT.BORDER);
		comboCurveMethod.setLayoutData(new GridData(180, SWT.DEFAULT));
		comboCurveMethod.setVisibleItemCount(20);
		toolkit.adapt(comboCurveMethod, true, true);

		label = toolkit.createLabel(compositeCurve, "Curve Type:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		comboCurveType = new CCombo(compositeCurve, SWT.BORDER);
		comboCurveType.setLayoutData(new GridData(180, SWT.DEFAULT));
		comboCurveType.setVisibleItemCount(20);
		toolkit.adapt(comboCurveType, true, true);

		label = toolkit.createLabel(compositeCurve, "Curve Threshold:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		txtCurveThreshold = new Text(compositeCurve, SWT.BORDER);
		txtCurveThreshold.setLayoutData(new GridData(172, SWT.DEFAULT));
		toolkit.adapt(txtCurveThreshold, true, true);

		label = toolkit.createLabel(compositeCurve, "Manual LB:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		txtCurveManualLB = new Text(compositeCurve, SWT.BORDER);
		txtCurveManualLB.setLayoutData(new GridData(172, SWT.DEFAULT));
		toolkit.adapt(txtCurveManualLB, true, true);

		label = toolkit.createLabel(compositeCurve, "Manual UB:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		txtCurveManualUB = new Text(compositeCurve, SWT.BORDER);
		txtCurveManualUB.setLayoutData(new GridData(172, SWT.DEFAULT));
		toolkit.adapt(txtCurveManualUB, true, true);

		label = toolkit.createLabel(compositeCurve, "Group By:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));
		
		Composite c = toolkit.createComposite(compositeCurve, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(5).applyTo(c);
	
		comboCurveGroupBy1 = new CCombo(c, SWT.BORDER | SWT.READ_ONLY);
		comboCurveGroupBy1.setLayoutData(new GridData(100, SWT.DEFAULT));
		toolkit.adapt(comboCurveGroupBy1, true, true);
		
		toolkit.createLabel(c, " > ", SWT.NONE);
		
		comboCurveGroupBy2 = new CCombo(c, SWT.BORDER | SWT.READ_ONLY);
		comboCurveGroupBy2.setLayoutData(new GridData(100, SWT.DEFAULT));
		toolkit.adapt(comboCurveGroupBy2, true, true);
		
		toolkit.createLabel(c, " > ", SWT.NONE);
		
		comboCurveGroupBy3 = new CCombo(c, SWT.BORDER | SWT.READ_ONLY);
		comboCurveGroupBy3.setLayoutData(new GridData(100, SWT.DEFAULT));
		toolkit.adapt(comboCurveGroupBy3, true, true);
		
		textName.addKeyListener(dirtyAdapter);
		textAlias.addKeyListener(dirtyAdapter);
		textDescription.addKeyListener(dirtyAdapter);

		checkAnnotation.addSelectionListener(selectionDirtyAdapter);
		checkKeyfeature.addSelectionListener(selectionDirtyAdapter);
		checkExport.addSelectionListener(selectionDirtyAdapter);
		checkNumeric.addSelectionListener(selectionDirtyAdapter);
		checkClassificationRestricted.addSelectionListener(selectionDirtyAdapter);
		
		textCalcFormula.addKeyListener(dirtyAdapter);
		comboFormulaLanguage.addSelectionListener(selectionDirtyAdapter);
		comboFormulaTrigger.addSelectionListener(selectionDirtyAdapter);
		spinnerSeq.addSelectionListener(selectionDirtyAdapter);

		comboCurveKind.addSelectionListener(selectionDirtyAdapter);
		comboCurveKind.addSelectionListener(curveChangedAdapter);
		comboCurveKind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filterCurveCombos();
				if (comboCurveModel.getItemCount() > 0) comboCurveModel.select(0);
				if (comboCurveMethod.getItemCount() > 0) comboCurveMethod.select(0);
				if (comboCurveType.getItemCount() > 0) comboCurveType.select(0);
				txtCurveThreshold.setText("");
				txtCurveManualLB.setText("");
				txtCurveManualUB.setText("");
			}
		});

		comboCurveModel.addSelectionListener(selectionDirtyAdapter);
		comboCurveModel.addSelectionListener(curveChangedAdapter);
		comboCurveMethod.addSelectionListener(selectionDirtyAdapter);
		comboCurveMethod.addSelectionListener(curveChangedAdapter);
		comboCurveType.addSelectionListener(selectionDirtyAdapter);
		comboCurveType.addSelectionListener(curveChangedAdapter);
		txtCurveThreshold.addKeyListener(dirtyAdapter);
		txtCurveThreshold.addKeyListener(curveChangedKeyAdapter);
		txtCurveManualLB.addKeyListener(dirtyAdapter);
		txtCurveManualLB.addKeyListener(curveChangedKeyAdapter);
		txtCurveManualUB.addKeyListener(dirtyAdapter);
		txtCurveManualUB.addKeyListener(curveChangedKeyAdapter);
		comboCurveGroupBy1.addSelectionListener(selectionDirtyAdapter);
		comboCurveGroupBy1.addSelectionListener(curveChangedAdapter);
		comboCurveGroupBy2.addSelectionListener(selectionDirtyAdapter);
		comboCurveGroupBy2.addSelectionListener(curveChangedAdapter);
		comboCurveGroupBy3.addSelectionListener(selectionDirtyAdapter);
		comboCurveGroupBy3.addSelectionListener(curveChangedAdapter);
		
		comboFormatString.addSelectionListener(selectionDirtyAdapter);
		comboNormalization.addSelectionListener(selectionDirtyAdapter);
		customNormLanguage.addSelectionListener(selectionDirtyAdapter);
		normScopeCmb.addSelectionListener(selectionDirtyAdapter);
		comboGrouping.addSelectionListener(selectionDirtyAdapter);
		colorMethods.addSelectionListener(selectionDirtyAdapter);
		comboLowType.addSelectionListener(selectionDirtyAdapter);
		comboHighType.addSelectionListener(selectionDirtyAdapter);

		predefinedNormBtn.addSelectionListener(selectionDirtyAdapter);
		customNormBtn.addSelectionListener(selectionDirtyAdapter);
		editNormScriptBtn.addSelectionListener(selectionDirtyAdapter);

		customNormTxt.addKeyListener(dirtyAdapter);
		comboFormatString.addKeyListener(dirtyAdapter);

		compositeGeneral.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		compositeFormula.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		normalizationCmp.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		compositeClassification.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		compositeColorMethod.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		compositeCurve.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		fillWellTypeCombos();
		fillNormalizationScopeCombo();
	}

	@Override
	public void dispose() {
		// Nothing to dispose.
	}

	@Override
	public void setFocus() {
		textName.setFocus();
		// Set focus
	}

	@Override
	public boolean setFormInput(Object input) {
		return false;
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		if (structuredSelection.isEmpty()) {
			return;
		}
		Feature feature = (Feature) structuredSelection.getFirstElement();

		if (m_bindingContext != null) {
			Object[] list = m_bindingContext.getBindings().toArray();
			for (Object object : list) {
				Binding binding = (Binding) object;
				m_bindingContext.removeBinding(binding);
				binding.dispose();
			}

			m_bindingContext.dispose();
		}

		this.feature = feature;
		m_bindingContext = initDataBindings();

		fillGroupingCombo();
		fillClassificationTable();
		setActiveColorMethod();
		filterCurveCombos();
		fillGroupByCombos();
		
		boolean isCustomNorm = NormalizationService.NORMALIZATION_CUSTOM.equals(feature.getNormalization());
		toggleNormalizationButtons(isCustomNorm);
	}

	@Override
	public void commit(boolean onSave) {
		// Commit
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isStale() {
		return false;
	}

	@Override
	public void refresh() {
		// Nothing to do.
	}

	private void setActiveColorMethod() {
		final String[] ids = ColorMethodRegistry.getInstance().getIds();
		String id = ColorMethodFactory.getColorMethodId(feature);
		int index = CollectionUtils.find(ids, id);
		colorMethods.select(index);
	}

	private void fillGroupingCombo() {
		if (feature != null && feature.getProtocolClass().getFeatureGroups() != null) {
			List<FeatureGroup> featureGroups = ProtocolService.getInstance().getCustomFeatureGroups(feature.getProtocolClass(), GroupType.WELL);
			FeatureGroup currentGroup = null;
			if (feature.getFeatureGroup() != null) {
				currentGroup = feature.getFeatureGroup();
			}
			int index = 0;
			String[] fgNames = new String[featureGroups.size()+1];
			fgNames[index] = "";
			for (FeatureGroup fg : featureGroups) {
				fgNames[++index] = fg.getName();
			}
			comboGrouping.setItems(fgNames);
			if (currentGroup != null) {
				for (int i = 0; i < comboGrouping.getItems().length; i++) {
					if (comboGrouping.getItems()[i].equals(currentGroup.getName())) {
						comboGrouping.select(i);
					}
				}
			}
		} else {
			comboGrouping.setItems(new String[0]);
		}
	}

	private void fillClassificationTable() {
		if (feature.getFeatureClasses() == null) feature.setFeatureClasses(new ArrayList<FeatureClass>());
		classifications = new WritableList(feature.getFeatureClasses(), FeatureClass.class);
		classificationTableViewer.setInput(classifications);
	}

	private void fillGroupByCombos() {
		List<String> annotationList = ProtocolService.streamableList(feature.getProtocolClass().getFeatures()).stream()
				.filter(f -> f.isAnnotation())
				.map(f -> f.getName())
				.sorted()
				.collect(Collectors.toList());
		annotationList.add(0, "");
		String[] annotations = annotationList.toArray(new String[annotationList.size()]);
		
		String[] groupBy = {
				feature.getCurveSettings().get(CurveSettings.GROUP_BY_1),
				feature.getCurveSettings().get(CurveSettings.GROUP_BY_2),
				feature.getCurveSettings().get(CurveSettings.GROUP_BY_3)	
		};
		comboCurveGroupBy1.setItems(annotations);
		comboCurveGroupBy2.setItems(annotations);
		comboCurveGroupBy3.setItems(annotations);
		comboCurveGroupBy1.select(annotationList.indexOf(groupBy[0]));
		comboCurveGroupBy2.select(annotationList.indexOf(groupBy[1]));
		comboCurveGroupBy3.select(annotationList.indexOf(groupBy[2]));
	}
	
	private void filterCurveCombos() {
		int kindIndex = comboCurveKind.getSelectionIndex();
		if (kindIndex == -1) return;

		CurveKind kind = CurveKind.valueOf(comboCurveKind.getItem(kindIndex));
		CurveModel[] models = CurveService.getInstance().getCurveModels(kind);
		comboCurveModel.setItems(StringUtils.getEnumNames(models));
		CurveMethod[] methods = CurveService.getInstance().getCurveMethods(kind);
		comboCurveMethod.setItems(StringUtils.getEnumNames(methods));
		CurveType[] types = CurveService.getInstance().getCurveTypes();
		comboCurveType.setItems(StringUtils.getEnumNames(types));
	}
	
	private void clearCurveSettings() {
		comboCurveKind.select(-1);
		comboCurveModel.select(-1);
		comboCurveMethod.select(-1);
		comboCurveType.select(-1);
		txtCurveThreshold.setText("");
		txtCurveManualLB.setText("");
		txtCurveManualUB.setText("");
		parentPage.markDirty();
	}

	private void fillWellTypeCombos() {
		List<WellType> types = ProtocolService.getInstance().getWellTypes();

		String[] welltypes = new String[types.size()+1];
		welltypes[0] = "";
		int i = 1;
		for (WellType type : types) {
			welltypes[i] = type.getCode();
			i++;
		}

		comboLowType.setItems(welltypes);
		comboHighType.setItems(welltypes);
	}

	private void fillNormalizationScopeCombo() {
		NormalizationScope[] scopes = NormalizationScope.values();
		String[] scopeNames = new String[scopes.length];
		for (int i=0; i<scopes.length; i++){
			scopeNames[i] = scopes[i].getLabel();
		}
		normScopeCmb.setItems(scopeNames);
	}

	private void toggleNormalizationButtons(boolean isCustomNorm) {
		predefinedNormBtn.setSelection(!isCustomNorm);
		comboNormalization.setEnabled(!isCustomNorm);
		comboLowType.setEnabled(!isCustomNorm);
		comboHighType.setEnabled(!isCustomNorm);

		customNormBtn.setSelection(isCustomNorm);
		customNormTxt.setEnabled(isCustomNorm);
		editNormScriptBtn.setEnabled(isCustomNorm);
		customNormLanguage.setEnabled(isCustomNorm);
		normScopeCmb.setEnabled(isCustomNorm);

		if (isCustomNorm) {
			comboNormalization.select(comboNormalization.indexOf(NormalizationService.NORMALIZATION_CUSTOM));
		} else {
			comboNormalization.select(comboNormalization.indexOf(feature.getNormalization()));
		}
	}
	
	private DataBindingContext initDataBindings() {

		DataBindingContext ctx = new DataBindingContext();

		FormEditorUtils.bindSelection(checkAnnotation, feature, "annotation", ctx);
		FormEditorUtils.bindSelection(checkNumeric, feature, "numeric", ctx);
		FormEditorUtils.bindSelection(checkKeyfeature, feature, "key", ctx);
		FormEditorUtils.bindSelection(checkExport, feature, "uploaded", ctx);
		FormEditorUtils.bindSelection(checkClassificationRestricted, feature, "classificationRestricted", ctx);
		
		FormEditorUtils.bindText(textName, feature, "name", ctx);
		FormEditorUtils.bindText(textAlias, feature, "shortName", ctx);
		FormEditorUtils.bindText(textDescription, feature, "description", ctx);
		FormEditorUtils.bindSelection(comboFormatString, feature, "formatString", ctx);

		FormEditorUtils.bindText(textCalcFormula, feature, "calculationFormula", ctx);
		FormEditorUtils.bindSelection(spinnerSeq, feature, "calculationSequence", ctx);

		FormEditorUtils.bindSelection(comboNormalization, feature, "normalization", ctx);
		FormEditorUtils.bindText(customNormTxt, feature, "normalizationFormula", ctx);
		FormEditorUtils.bindSelection(customNormLanguage, new NormalizationLanguageMapper(feature), "language", ctx);
		FormEditorUtils.bindSelection(comboHighType, feature, "highWellTypeCode", ctx);
		FormEditorUtils.bindSelection(comboLowType, feature, "lowWellTypeCode", ctx);

		// Special binding is required for these settings:

		if (feature != null) {

			FeatureGroupMapper featureGroup = new FeatureGroupMapper(feature);
			FormEditorUtils.bindSelection(comboGrouping, featureGroup, "featureGroup", ctx);

			NormalizationScopeMapper normalizationScope = new NormalizationScopeMapper(feature);
			FormEditorUtils.bindSelection(normScopeCmb, normalizationScope, "normalizationScope", ctx);

			FormEditorUtils.bindSelection(comboFormulaLanguage, new CalculationLanguageMapper(feature), "language", ctx);
			FormEditorUtils.bindSelection(comboFormulaTrigger, new CalculationTriggerMapper(feature), "trigger", ctx);

			CurveSettingsMapper curveSettings = new CurveSettingsMapper(feature);
			FormEditorUtils.bindSelection(comboCurveKind, curveSettings, "kind", ctx);
			FormEditorUtils.bindSelection(comboCurveMethod, curveSettings, "method", ctx);
			FormEditorUtils.bindSelection(comboCurveModel, curveSettings, "model", ctx);
			FormEditorUtils.bindSelection(comboCurveType, curveSettings, "type", ctx);
			FormEditorUtils.bindText(txtCurveThreshold, curveSettings, "threshold", ctx);
			FormEditorUtils.bindText(txtCurveManualLB, curveSettings, "lb", ctx);
			FormEditorUtils.bindText(txtCurveManualUB, curveSettings, "ub", ctx);
			FormEditorUtils.bindSelection(comboCurveGroupBy1, curveSettings, "groupBy1", ctx);
			FormEditorUtils.bindSelection(comboCurveGroupBy2, curveSettings, "groupBy2", ctx);
			FormEditorUtils.bindSelection(comboCurveGroupBy3, curveSettings, "groupBy3", ctx);
		}

		return ctx;
	}

	public static class FeatureGroupMapper {

		private Feature feature;

		public FeatureGroupMapper(Feature feature) {
			this.feature = feature;
		}

		public String getFeatureGroup() {
			if (feature.getFeatureGroup() != null) {
				return feature.getFeatureGroup().getName();
			}
			return null;
		}

		public void setFeatureGroup(String featureGroup) {
			if (featureGroup == null || featureGroup.isEmpty()) {
				feature.setFeatureGroup(null);
			} else {
				List<FeatureGroup> featureGroups = feature.getProtocolClass().getFeatureGroups();
				if (featureGroups != null) {
					for (FeatureGroup fg : featureGroups) {
						if (fg.getName().equals(featureGroup)) {
							feature.setFeatureGroup(fg);
							break;
						}
					}
				}
			}
		}

	}

	public static class NormalizationScopeMapper {

		private Feature feature;

		public NormalizationScopeMapper(Feature feature) {
			this.feature = feature;
		}

		public String getNormalizationScope() {
			return NormalizationScope.getFor(feature.getNormalizationScope()).getLabel();
		}

		public void setNormalizationScope(String scopeName) {
			NormalizationScope scope = NormalizationScope.getFor(scopeName);
			feature.setNormalizationScope(scope.getId());
		}
	}

	public static class CurveSettingsMapper {

		private Feature feature;

		public CurveSettingsMapper(Feature feature) {
			this.feature = feature;
		}

		public String getKind() {
			return feature.getCurveSettings().get(CurveSettings.KIND);
		}
		public void setKind(String kind) {
			setValue(CurveSettings.KIND, kind);
		}
		public String getMethod() {
			return feature.getCurveSettings().get(CurveSettings.METHOD);
		}
		public void setMethod(String method) {
			setValue(CurveSettings.METHOD, method);
		}
		public String getModel() {
			return feature.getCurveSettings().get(CurveSettings.MODEL);
		}
		public void setModel(String model) {
			setValue(CurveSettings.MODEL, model);
		}
		public String getType() {
			return feature.getCurveSettings().get(CurveSettings.TYPE);
		}
		public void setType(String type) {
			setValue(CurveSettings.TYPE, type);
		}
		public String getThreshold() {
			return feature.getCurveSettings().get(CurveSettings.THRESHOLD);
		}
		public void setThreshold(String threshold) {
			setValue(CurveSettings.THRESHOLD, threshold);
		}
		public String getUb() {
			return feature.getCurveSettings().get(CurveSettings.UB);
		}
		public void setUb(String ub) {
			setValue(CurveSettings.UB, ub);
		}
		public String getLb() {
			return feature.getCurveSettings().get(CurveSettings.LB);
		}
		public void setLb(String lb) {
			setValue(CurveSettings.LB, lb);
		}
		public String getGroupBy1() {
			return getGroupBy(CurveSettings.GROUP_BY_1);
		}
		public void setGroupBy1(String groupBy) {
			setGroupBy(CurveSettings.GROUP_BY_1, groupBy);
		}
		public String getGroupBy2() {
			return getGroupBy(CurveSettings.GROUP_BY_2);
		}
		public void setGroupBy2(String groupBy) {
			setGroupBy(CurveSettings.GROUP_BY_2, groupBy);
		}
		public String getGroupBy3() {
			return getGroupBy(CurveSettings.GROUP_BY_3);
		}
		public void setGroupBy3(String groupBy) {
			setGroupBy(CurveSettings.GROUP_BY_3, groupBy);
		}
		
		private String getGroupBy(String nr) {
			String value = feature.getCurveSettings().get(nr);
			if (value == null) return "";
			return value;
		}
		private void setGroupBy(String nr, String value) {
			if ("".equals(value)) value = null;
			setValue(nr, value);
		}
		
		private void setValue(String name, String value) {
			if (value == null || value.isEmpty()) feature.getCurveSettings().remove(name);
			else feature.getCurveSettings().put(name, value);
		}
	}
	
	public static class NormalizationLanguageMapper {
		private Feature feature;

		public NormalizationLanguageMapper(Feature feature) {
			this.feature = feature;
		}

		public String getLanguage() {
			CalculationLanguage lang = CalculationLanguage.get(feature.getNormalizationLanguage());
			if (lang == null) return "";
			return lang.getLabel();
		}

		public void setLanguage(String label) {
			CalculationLanguage lang = Arrays.stream(CalculationLanguage.getLanguages()).filter(l -> l.getLabel().equals(label)).findAny().orElse(null);
			if (lang != null) feature.setNormalizationLanguage(lang.getId());
		}
	}
	
	public static class CalculationLanguageMapper {
		private Feature feature;

		public CalculationLanguageMapper(Feature feature) {
			this.feature = feature;
		}

		public String getLanguage() {
			CalculationLanguage lang = CalculationLanguage.get(feature.getCalculationLanguage());
			if (lang == null) return "";
			return lang.getLabel();
		}

		public void setLanguage(String label) {
			CalculationLanguage lang = Arrays.stream(CalculationLanguage.getLanguages()).filter(l -> l.getLabel().equals(label)).findAny().orElse(null);
			if (lang != null) feature.setCalculationLanguage(lang.getId());
		}
	}
	
	public static class CalculationTriggerMapper {
		private Feature feature;

		public CalculationTriggerMapper(Feature feature) {
			this.feature = feature;
		}

		public String getTrigger() {
			String id = feature.getCalculationTrigger();
			if (id == null || id.isEmpty()) return "";
			return CalculationTrigger.valueOf(id).getLabel();
		}

		public void setTrigger(String label) {
			CalculationTrigger trigger = Arrays.stream(CalculationTrigger.values()).filter(t -> t.getLabel().equals(label)).findAny().orElse(null);
			if (trigger != null) feature.setCalculationTrigger(trigger.name());
		}
	}
}
