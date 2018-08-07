package eu.openanalytics.phaedra.ui.plate.metadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellMetaDataView extends DecoratedView {

	private FormToolkit formToolkit;
	private Form form;

	private RichTableViewer tableViewer;

	private ISelectionListener selectionListener;

	private List<Well> currentWells;

	private WellMetaDataCalculator calc;

	@Override
	public void createPartControl(Composite parent) {
		formToolkit = FormEditorUtils.createToolkit();
		calc = new WellMetaDataCalculator();

		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(parent);

		Label separator = formToolkit.createSeparator(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

		form = FormEditorUtils.createForm("Wells selected: 0", 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);

		Section section = FormEditorUtils.createSection("Well Metadata", form.getBody(), formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);

		Composite sectionContainer = FormEditorUtils.createComposite(2, section, formToolkit);

		Table t = formToolkit.createTable(sectionContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(t);

		tableViewer = new RichTableViewer(t);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(createTableColumns());
		createContextMenu();

		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				List<Well> wells = new ArrayList<>();
				wells = SelectionUtils.getObjects(selection, Well.class);
				if (wells == null || wells.isEmpty()) {
					Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
					if (plate != null) wells = plate.getWells();
					else return;
				}
				if (currentWells == null || !currentWells.equals(wells)) {
					currentWells = wells;
					tableViewer.getTable().setRedraw(false);
					calc.setCurrentWells(currentWells);
					tableViewer.applyColumnConfig(createTableColumns());
					tableViewer.setInput(currentWells);
					tableViewer.getTable().setRedraw(true);
					form.setText("Wells selected: " + currentWells.size());
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewWellMetadata");
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				tableViewer.contributeConfigButton(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private ColumnConfiguration[] createTableColumns() {
		calc.calculate();

		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Position", ColumnDataType.String, 75);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Well well = (Well)cell.getElement();
				cell.setText(NumberUtils.getWellCoordinate(well.getRow(), well.getColumn()));
			}
		});
		config.setSorter(new Comparator<Well>(){
			@Override
			public int compare(Well o1, Well o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				String well1 = NumberUtils.getWellCoordinate(o1.getRow(), o1.getColumn());
				String well2 = NumberUtils.getWellCoordinate(o2.getRow(), o2.getColumn());
				return well1.toLowerCase().compareTo(well2.toLowerCase());
			}
		});
		configs.add(config);

		for (final String columnName : calc.getKeywords()) {
			config = ColumnConfigFactory.create(columnName, ColumnDataType.String, 100);
			config.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					Well well = (Well)cell.getElement();
					String wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns()) + "";
					cell.setText((null == calc.getMappedKeywords().get(wellNr) || null == calc.getMappedKeywords().get(wellNr).get(columnName)) ? "" : calc.getMappedKeywords().get(wellNr).get(columnName));
				}
			});
			config.setSorter(new Comparator<Well>(){
				@Override
				public int compare(Well o1, Well o2) {
					if (o1 == null && o2 == null) return 0;
					if (o1 == null) return -1;
					String wellNr1 = NumberUtils.getWellNr(o1.getRow(), o1.getColumn(), o1.getPlate().getColumns()) + "";
					String wellNr2 = NumberUtils.getWellNr(o2.getRow(), o2.getColumn(), o2.getPlate().getColumns()) + "";
					String well1 = (null == calc.getMappedKeywords().get(wellNr1) || null == calc.getMappedKeywords().get(wellNr1).get(columnName)) ?
							"" : calc.getMappedKeywords().get(wellNr1).get(columnName);
					String well2 = (null == calc.getMappedKeywords().get(wellNr2) || null == calc.getMappedKeywords().get(wellNr2).get(columnName)) ?
							"" : calc.getMappedKeywords().get(wellNr2).get(columnName);
					return well1.toLowerCase().compareTo(well2.toLowerCase());
				}
			});
			configs.add(config);
		}

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

}