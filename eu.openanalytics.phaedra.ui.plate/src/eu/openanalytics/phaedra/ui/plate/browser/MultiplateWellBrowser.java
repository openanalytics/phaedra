package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.WorkbenchSiteJobScheduler;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncData1toNViewerInput;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.table.MultiplateWellTableColumns;

public class MultiplateWellBrowser extends EditorPart {
	
	
	private AsyncData1toNViewerInput<Plate, Well> viewerInput;
	private AsyncDataLoader<Plate> dataLoader;
	
	private DataFormatSupport dataFormatSupport;

	private CTabFolder tabFolder;
	private CTabItem tableTab;

	private RichTableViewer tableViewer;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		
		this.dataLoader = new AsyncDataLoader<>("data for well browser",
				new WorkbenchSiteJobScheduler(this) );
		this.viewerInput = new AsyncData1toNViewerInput<Plate, Well>(Plate.class, Well.class, this.dataLoader) {
			@Override
			public Plate getBaseElement(final Well well) {
				return well.getPlate();
			}
			@Override
			public List<Well> getViewerElements(final Plate plate) {
				List<Well> list = new ArrayList<>(plate.getWells());
				list.sort(PlateUtils.WELL_NR_SORTER);
				return list;
			}
			@Override
			public int getViewerElementsSize(Plate plate) {
				return plate.getWells().size();
			}
			@Override
			public int getViewerElementIndexOf(final Plate plate, Well well) {
				return PlateUtils.getWellNrIdx(well);
			}
			
			@Override
			protected List<Plate> loadElements() {
				VOEditorInput input = (VOEditorInput)getEditorInput();
				List<IValueObject> valueObjects = input.getValueObjects();
				List<Plate> plates = new ArrayList<>();
				for (IValueObject vo: valueObjects) if (vo instanceof Plate) plates.add((Plate)vo);
				return plates;
			}
			
		};
	}
	
	private List<Feature> getFeatures() {
		final List<Plate> elements = this.viewerInput.getBaseElements();
		if (elements.isEmpty()) {
			return Collections.emptyList();
		}
		return elements.get(0).getExperiment().getProtocol().getProtocolClass().getFeatures();
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		this.dataFormatSupport = new DataFormatSupport(this.viewerInput::refreshViewer);
		
		tabFolder = new CTabFolder(parent, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> tabChanged(e.item));

		/* Table tab */

		tableTab = new CTabItem(tabFolder, SWT.NONE);
		tableTab.setText("Multi-Plate Table View");

		Composite container = new Composite(tabFolder, SWT.NONE);
		container = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		tableTab.setControl(container);

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName()) {
//			@Override
//			protected CustomizeColumnsDialog createConfigureColumnDialog() {
//				return new WellBrowserConfigColumnDialog(Display.getDefault().getActiveShell(), this,
//						getFeatures() );
//			}
		};
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(MultiplateWellTableColumns.configureColumns(this.dataLoader,
				getFeatures(), dataFormatSupport, this.viewerInput::refreshViewer ));
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableViewer.getControl());

		this.viewerInput.connect(tableViewer);
		/* Other */

		createContextMenu();

		selectionProvider = new SelectionProviderIntermediate();
		selectionProvider.addSelectionChangedListener(event -> MultiplateWellBrowser.this.selectionChanged(event));
		selectionProvider.setSelectionProviderDelegate(tableViewer);
		getSite().setSelectionProvider(selectionProvider);

		selectionListener = (part, selection) -> {
			if (part == MultiplateWellBrowser.this) return;
			List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
			if (wells.isEmpty()) {
				List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
				if (!compounds.isEmpty()) {
					wells = new ArrayList<Well>();
					for (Compound c: compounds) wells.addAll(c.getWells());
				}
			}
			if (!wells.isEmpty()) {
				tableViewer.setSelection(new StructuredSelection(wells));
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		tabFolder.setSelection(tableTab);
	}

	@Override
	public void dispose() {
		if (this.viewerInput != null) this.viewerInput.dispose();
		if (this.dataFormatSupport != null) this.dataFormatSupport.dispose();
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Do nothing.
	}

	@Override
	public void doSaveAs() {
		// Do nothing.
	}
	
	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		// The context menu for the table viewer.
		menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Contributions are added here.
		manager.add(new Separator());
		tableViewer.contributeConfigButton(manager);
	}

	private void tabChanged(Widget tab) {
		// Update the selection provider intermediate.
		if (tab == tableTab) {
			selectionProvider.setSelectionProviderDelegate(tableViewer);
//			if (!tableTabInitialized) initializeTableTab();
		}
	}

	private void selectionChanged(SelectionChangedEvent event) {
		//we have only one tab, no action required here
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

}
