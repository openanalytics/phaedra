package eu.openanalytics.phaedra.ui.plate.inspector.plate;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlatePropertyProvider;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateUploadStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationUtils;

public class PlateInspector extends DecoratedView {

	private BreadcrumbViewer breadcrumb;

	private FormToolkit formToolkit;
	private RichTableViewer tableViewer;

	private Label[] calculationLbls;
	private Label[] validationLbls;
	private Label[] approvalLbls;
	private Label[] uploadLbls;

	private Label plateLayoutLbl;
	private RichTableViewer compoundTableViewer;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	private IModelEventListener modelEventListener;

	private Plate currentPlate;

	@Override
	public void createPartControl(Composite parent) {
		formToolkit = FormEditorUtils.createToolkit();

		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		Label separator = formToolkit.createSeparator(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

		final ScrolledForm form = FormEditorUtils.createScrolledForm("Plate: <no plate selected>", 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);

		// Section 1: Plate Status ------------------------------

		Section section = FormEditorUtils.createSection("Status", form.getBody(), formToolkit);
		Composite statusContainer = FormEditorUtils.createComposite(4, section, formToolkit);

		FormEditorUtils.createLabel("Calculation:", statusContainer, formToolkit);
		calculationLbls = new Label[3];
		calculationLbls[0] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		calculationLbls[1] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		calculationLbls[2] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(calculationLbls[1]);

		FormEditorUtils.createLabel("Validation:", statusContainer, formToolkit);
		validationLbls = new Label[3];
		validationLbls[0] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		validationLbls[1] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		validationLbls[2] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(validationLbls[1]);

		FormEditorUtils.createLabel("Approval:", statusContainer, formToolkit);
		approvalLbls = new Label[3];
		approvalLbls[0] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		approvalLbls[1] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		approvalLbls[2] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(approvalLbls[1]);

		FormEditorUtils.createLabel("Upload:", statusContainer, formToolkit);
		uploadLbls = new Label[3];
		uploadLbls[0] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		uploadLbls[1] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		uploadLbls[2] = FormEditorUtils.createLabel("", statusContainer, formToolkit);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(uploadLbls[1]);

		// Section 2: Plate Properties ------------------------------

		section = FormEditorUtils.createSection("Properties", form.getBody(), formToolkit, true);
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, 150).applyTo(section);
		Composite propertyContainer = FormEditorUtils.createComposite(2, section, formToolkit);

		Table t = formToolkit.createTable(propertyContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		t.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.stateMask == SWT.CTRL && e.keyCode == 'c') {
					String property = SelectionUtils.getFirstObject(tableViewer.getSelection(), String.class);
					if (property != null) CopyItems.execute(PlatePropertyProvider.getValue(property, currentPlate));
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(t);

		tableViewer = new RichTableViewer(t);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(createPropertyTableColumns());
		tableViewer.setInput(PlatePropertyProvider.getKeys(null));

		// Section 3: Plate Compounds ------------------------------

		section = FormEditorUtils.createSection("Contents", form.getBody(), formToolkit, true);
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, 150).applyTo(section);
		Composite contentsContainer = FormEditorUtils.createComposite(2, section, formToolkit);

		plateLayoutLbl = FormEditorUtils.createLabelPair("Layout:", contentsContainer, formToolkit);

		t = formToolkit.createTable(contentsContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(t);

		compoundTableViewer = new RichTableViewer(t);
		compoundTableViewer.setContentProvider(new ArrayContentProvider());
		compoundTableViewer.applyColumnConfig(createContentTableColumns());

		// Selection handling
		selectionListener = (part, selection) -> {
			if (part == PlateInspector.this) return;

			Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
			if (plate != null && !plate.equals(currentPlate)) {
				currentPlate = plate;
				form.setText("Plate: " + currentPlate.getBarcode());
				loadPlate();
				form.reflow(true);
			}

			Compound compound = SelectionUtils.getFirstObject(selection, Compound.class);
			if (compound != null) {
				List<Compound> compouds = SelectionUtils.getObjects(selection, Compound.class);
				compoundTableViewer.setSelection(new StructuredSelection(compouds));
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		modelEventListener = event -> {
			boolean refreshPlate = false;
			if (event.type == ModelEventType.ValidationChanged || event.type == ModelEventType.Calculated) {
				if (event.source instanceof Plate) {
					Plate plate = (Plate)event.source;
					if (plate.equals(currentPlate)) refreshPlate = true;
				} else if (event.source instanceof Object[]) {
					Object[] items = (Object[])event.source;
					for (Object o: items) {
						if (o instanceof Plate && ((Plate)o).equals(currentPlate)) refreshPlate = true;
					}
				}
			}
			if (refreshPlate) {
				Display.getDefault().asyncExec(() -> loadPlate());
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ModelEventService.getInstance().removeEventListener(modelEventListener);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					ModelEventService.getInstance().addEventListener(modelEventListener);
				}
			}
		});
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		selectionProvider = new SelectionProviderIntermediate();
		selectionProvider.setSelectionProviderDelegate(compoundTableViewer);
		getSite().setSelectionProvider(selectionProvider);

		createContextMenu();

		SelectionUtils.triggerActiveSelection(selectionListener);
		if (currentPlate == null) SelectionUtils.triggerActiveEditorSelection(selectionListener);
		form.reflow(true);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewPlateInspector");
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
		super.dispose();
	}

	private void loadPlate() {
		breadcrumb.setInput(currentPlate);
		breadcrumb.getControl().getParent().layout();

		PlateCalcStatus calc = PlateCalcStatus.getByCode(currentPlate.getCalculationStatus());
		PlateValidationStatus val = PlateValidationStatus.getByCode(currentPlate.getValidationStatus());
		PlateApprovalStatus appr = PlateApprovalStatus.getByCode(currentPlate.getApprovalStatus());
		PlateUploadStatus upload = PlateUploadStatus.getByCode(currentPlate.getUploadStatus());

		calculationLbls[0].setImage(IconManager.getIconImage(ValidationUtils.getIcon(calc)));
		calculationLbls[1].setText(calc.toString());
		String calcErr = currentPlate.getCalculationError();
		calculationLbls[2].setText(calcErr == null ? "" : "(" + calcErr + ")");

		validationLbls[0].setImage(IconManager.getIconImage(ValidationUtils.getIcon(val)));
		validationLbls[1].setText(val.toString());
		String user = currentPlate.getValidationUser();
		validationLbls[2].setText(user == null ? "" : "(" + user + ")");

		approvalLbls[0].setImage(IconManager.getIconImage(ValidationUtils.getIcon(appr)));
		approvalLbls[1].setText(appr.toString());
		user = currentPlate.getApprovalUser();
		approvalLbls[2].setText(user == null ? "" : "(" + user + ")");

		uploadLbls[0].setImage(IconManager.getIconImage(ValidationUtils.getIcon(upload)));
		uploadLbls[1].setText(upload.toString());
		user = currentPlate.getUploadUser();
		uploadLbls[2].setText(user == null ? "" : "(" + user + ")");

		Object[] loadingInput = new Object[] { "Loading..." };
		tableViewer.setInput(loadingInput);
		compoundTableViewer.setInput(loadingInput);

		JobUtils.runUserJob(monitor -> {
			String[] keys = PlatePropertyProvider.getKeys(currentPlate);
			List<Compound> compounds = currentPlate.getCompounds();

			Display.getDefault().asyncExec(() -> {
				if (tableViewer.getTable().isDisposed()) return;
				plateLayoutLbl.setText(
						compounds.size() + " compounds"
								+ " in " + currentPlate.getWells().size() + " wells"
								+ " (" + currentPlate.getRows() + " x " + currentPlate.getColumns() + ")");
				tableViewer.setInput(keys);
				compoundTableViewer.setInput(compounds);

				tableViewer.getTable().getParent().getParent().getParent().layout(true, true);
			});
		}, "Loading Plate Properties for Plate " + currentPlate.getBarcode(), 100, toString(), null);
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		// The same context menu for the table viewer.
		menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(compoundTableViewer.getControl());
		compoundTableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, compoundTableViewer);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Contributions are added here.
		manager.add(new Separator());
		compoundTableViewer.contributeConfigButton(manager);
	}

	private ColumnConfiguration[] createPropertyTableColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Property", ColumnDataType.String, 100);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(cell.getElement().toString());
			}
		});
		config.setSorter((String o1, String o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			return o1.compareToIgnoreCase(o2);
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Value", ColumnDataType.String, 250);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String name = cell.getElement().toString();
				if (currentPlate != null) {
					String value = PlatePropertyProvider.getValue(name, currentPlate);
					cell.setText(value);
				}
			}
		});
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private ColumnConfiguration[] createContentTableColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Compound", ColumnDataType.String, 100);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(cell.getElement().toString());
			}
		});
		config.setSorter((Compound o1, Compound o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			return o1.toString().compareTo(o2.toString());
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Samples", ColumnDataType.String, 250);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object o = cell.getElement();
				if (o instanceof Compound) {
					Compound c = (Compound) o;
					int acceptedCount = (int) PlateService.streamableList(c.getWells()).stream().filter(PlateUtils.ACCEPTED_WELLS_ONLY).count();
					int rejectedCount = c.getWells().size() - acceptedCount;
					String lbl = c.getWells().size() + " samples";
					lbl += " (" + rejectedCount + " rejected)";
					cell.setText(lbl);
				}
			}
		});
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
