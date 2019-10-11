package eu.openanalytics.phaedra.ui.protocol.editor.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.list.WritableList;
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
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.geometry.CalculatorFactory;
import eu.openanalytics.phaedra.ui.protocol.dialog.ManageGroupsDialog;
import eu.openanalytics.phaedra.ui.protocol.util.ClassificationTableFactory;
import eu.openanalytics.phaedra.ui.protocol.util.ListenerHelper;


public class SubWellFeaturesDetailBlock implements IDetailsPage {

	private SubWellFeature feature;
	private boolean dirty;

	private IManagedForm managedform;
	private SubWellFeaturesMasterBlock master;
	private SubWellFeaturesPage parentPage;

	private DataBindingContext m_bindingContext;

	private Text textName;
	private Text textDescription;	
	private Text textAlias;

	private Button checkNumeric;
	private Button checkKeyfeature;

	private CCombo comboFormatString;

	private CCombo comboPositionRole;
	
	private CCombo comboGrouping;

	private Image symbolImage;

	private TableViewer classificationTableViewer;
	private WritableList<FeatureClass> classifications;

	public SubWellFeaturesDetailBlock(SubWellFeaturesPage page, SubWellFeaturesMasterBlock master) {
		this.master = master;
		this.parentPage = page;
	}

	public void initialize(IManagedForm form) {
		managedform = form;
	}

	public void createContents(final Composite parent) {

		FormToolkit toolkit = managedform.getToolkit();
		parent.setLayout(new GridLayout());

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

		/*
		 * Section: general
		 * ****************
		 */

		final Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText("General");

		ImageHyperlink generalIcon = toolkit.createImageHyperlink(section, SWT.TRANSPARENT);
		generalIcon.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				section.setExpanded(true);
			}
		});
		generalIcon.setImage(IconManager.getIconImage("tag_red.png"));
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
		textName.addModifyListener(ListenerHelper.createTextNotEmptyListener(
				"Name", managedform.getMessageManager()));
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
		label.setLayoutData(new GridData(110, SWT.DEFAULT));

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
			public void handleEvent(Event event) {
				int buttonPressed = new ManageGroupsDialog(Display.getDefault().getActiveShell(), feature.getProtocolClass(), GroupType.SUBWELL).open();
				if (buttonPressed == Window.OK) {
					fillGroupingCombo();
					parentPage.markDirty();
				}
			}
		});

		label = toolkit.createLabel(compositeGeneral, "Position role:", SWT.NONE);
		label.setLayoutData(new GridData(110, SWT.DEFAULT));

		comboPositionRole = new CCombo(compositeGeneral, SWT.BORDER | SWT.READ_ONLY);
		comboPositionRole.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboPositionRole.setItems(CalculatorFactory.getInstance().getAvailablePositionRoles());
		comboPositionRole.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String role = comboPositionRole.getText();
				if (role.isEmpty()) return;
				for (SubWellFeature f: feature.getProtocolClass().getSubWellFeatures()) {
					if (f == feature || f.getPositionRole() == null) continue;
					if (f.getPositionRole().equals(role)) f.setPositionRole(null);
				}
			}
		});
		toolkit.adapt(comboPositionRole, true, true);
		
		final Composite composite_1 = toolkit.createCompositeSeparator(compositeGeneral);
		final GridData gd_composite_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_composite_1.verticalIndent = 5;
		gd_composite_1.heightHint = 1;
		composite_1.setLayoutData(gd_composite_1);

		toolkit.createLabel(compositeGeneral, "This is a:", SWT.NONE);

		checkKeyfeature = toolkit.createButton(compositeGeneral, "Key feature (displayed in all tables and charts)", SWT.CHECK);
		checkKeyfeature.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		new Label(compositeGeneral, SWT.NONE);

		checkNumeric = toolkit.createButton(compositeGeneral, "Numeric feature (contains a numeric value)", SWT.CHECK);
		checkNumeric.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		new Label(compositeGeneral, SWT.NONE);

		textName.addKeyListener(dirtyAdapter);
		textAlias.addKeyListener(dirtyAdapter);
		textDescription.addKeyListener(dirtyAdapter);


		comboGrouping.addSelectionListener(selectionDirtyAdapter);
		comboPositionRole.addSelectionListener(selectionDirtyAdapter);
		comboFormatString.addSelectionListener(selectionDirtyAdapter);
		comboFormatString.addKeyListener(dirtyAdapter);

		checkKeyfeature.addSelectionListener(selectionDirtyAdapter);
		checkNumeric.addSelectionListener(selectionDirtyAdapter);

		compositeGeneral.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		/*
		 * Section: classification
		 * ***********************
		 */

		final Section sctnFeatureClass = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION | Section.TWISTIE);
		sctnFeatureClass.setDescription("Define feature classes to perform classification on this feature.");
		sctnFeatureClass.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sctnFeatureClass.setText("Classification");

		ImageHyperlink sectionIcon = toolkit.createImageHyperlink(sctnFeatureClass, SWT.TRANSPARENT);
		sectionIcon.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				sctnFeatureClass.setExpanded(true);
			}
		});
		sectionIcon.setImage(IconManager.getIconImage("tag_red_edit.png"));
		sectionIcon.setToolTipText(sctnFeatureClass.getDescription());
		sctnFeatureClass.setTextClient(sectionIcon);
		toolkit.paintBordersFor(sectionIcon);


		final Composite compositeFeatureClass = toolkit.createComposite(sctnFeatureClass, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).spacing(10, SWT.DEFAULT).applyTo(compositeFeatureClass);
		sctnFeatureClass.setClient(compositeFeatureClass);
		toolkit.paintBordersFor(compositeFeatureClass);

		final SectionPart part = new SectionPart(sctnFeatureClass);
		this.managedform.addPart(part);

		// FEATURE CLASS TABLE

		Link link = ClassificationTableFactory.createAddClassLink(compositeFeatureClass, new Listener() {
			public void handleEvent(Event event) {
				FeatureClass newClass = ProtocolService.getInstance().createFeatureClass();
				classifications.add(newClass);
				classificationTableViewer.refresh();
				selectionDirtyAdapter.widgetSelected(null);
			}
		});
		link.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		link = ClassificationTableFactory.createRemoveClassLink(compositeFeatureClass, new Listener() {
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

		classificationTableViewer = ClassificationTableFactory.createTableViewer(compositeFeatureClass, true, selectionDirtyAdapter);
		classificationTableViewer.setComparer(ClassificationTableFactory.createClassComparer());
		GridDataFactory.fillDefaults().grab(true, true).indent(0, 5).span(4,1).applyTo(classificationTableViewer.getTable());

		compositeFeatureClass.setEnabled(parentPage.getEditor().isSaveAsAllowed());
	}

	public void dispose() {
		// Dispose
		if (symbolImage != null && !symbolImage.isDisposed()) symbolImage.dispose();
	}

	public void setFocus() {
		// Set focus
	}

	private void fillGroupingCombo() {
		if (feature != null && feature.getProtocolClass().getFeatureGroups() != null) {
			List<FeatureGroup> featureGroups = ProtocolService.getInstance().getCustomFeatureGroups(feature.getProtocolClass(), GroupType.SUBWELL);
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
		classifications = new WritableList<>(feature.getFeatureClasses(), FeatureClass.class);
		classificationTableViewer.setInput(classifications);
	}

	public boolean setFormInput(Object input) {
		return false;
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		if (structuredSelection.isEmpty()) {
			return;
		}
		SubWellFeature feature = (SubWellFeature)structuredSelection.getFirstElement();

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
	}

	public void commit(boolean onSave) {
		// Commit
	}

	public boolean isDirty() {
		return dirty;
	}

	public boolean isStale() {
		return false;
	}

	public void refresh() {
		// Nothing to do.
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext ctx = new DataBindingContext();
		
		FormEditorUtils.bindText(textName, feature, "name", ctx);
		FormEditorUtils.bindText(textAlias, feature, "shortName", ctx);
		FormEditorUtils.bindSelection(checkKeyfeature, feature, "key", ctx);
		FormEditorUtils.bindSelection(checkNumeric, feature, "numeric", ctx);
		
		FormEditorUtils.bindText(textDescription, feature, "description", ctx);
		FormEditorUtils.bindSelection(comboFormatString, feature, "formatString", ctx);
		FormEditorUtils.bindSelection(comboPositionRole, feature, "positionRole", ctx);
		
		if (feature != null) {
			FeatureGroupMapper featureGroup = new FeatureGroupMapper(feature);
			FormEditorUtils.bindSelection(comboGrouping, featureGroup, "featureGroup", ctx);
		}

		return ctx;
	}
	
	public static class FeatureGroupMapper {

		private SubWellFeature feature;

		public FeatureGroupMapper(SubWellFeature feature) {
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
	
}