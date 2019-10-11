package eu.openanalytics.phaedra.ui.protocol.editor.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.filter.FilterMatcher;
import eu.openanalytics.phaedra.base.ui.util.table.FilteredTable;
import eu.openanalytics.phaedra.base.ui.util.table.TableViewerSorter;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.protocol.dialog.ManageGroupsDialog;


public class SubWellFeaturesMasterBlock extends MasterDetailsBlock {

	private TableViewer tableViewer;
	private WritableList<SubWellFeature> inputList;
	private SubWellFeaturesPage parentPage;
	private ProtocolClass protocolClass;

	public SubWellFeaturesMasterBlock(ProtocolClass protocolClass, SubWellFeaturesPage page) {
		List<SubWellFeature> features = protocolClass.getSubWellFeatures();
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

		this.inputList = new WritableList<>(features, SubWellFeature.class);
		this.parentPage = page;
		this.protocolClass = protocolClass;
	}

	@Override
	protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();

		final Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		toolkit.paintBordersFor(container);

		final Section section = toolkit.createSection(container,
				Section.TITLE_BAR | Section.FOCUS_TITLE | Section.EXPANDED);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setText("Sub-well Features");

		final Composite subContainer = toolkit.createComposite(section, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(subContainer);
		toolkit.paintBordersFor(subContainer);
		section.setClient(subContainer);

		final SectionPart part = new SectionPart(section);
		managedForm.addPart(part);

		FilterMatcher matcher = new FilterMatcher();
		FilteredTable filteredTable = new FilteredTable(subContainer, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER, matcher, true);
		tableViewer = filteredTable.getViewer();

		Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL,SWT.FILL)
			.grab(true, true)
			.hint(SWT.DEFAULT,50) // Won't actually be 50, but prevents table from grabbing too much.
			.span(2,1)
			.applyTo(table);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (event != null) {
					managedForm.fireSelectionChanged(part, event.getSelection());
				}
			}
		});

		createColumns();

		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(inputList);

		final Composite toolbar = toolkit.createComposite(section, SWT.TRANSPARENT);
		toolbar.setLayout(new GridLayout(3, false));
		section.setTextClient(toolbar);
		toolkit.paintBordersFor(toolbar);

		fillToolbar(toolbar, toolkit);
		createMenu();
	}

	private void createColumns() {
		TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setWidth(0);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return "";
			}
		});

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Key");
		tvc.getColumn().setToolTipText("This is a Key Feature");
		tvc.getColumn().setWidth(40);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
			@Override
			public Image getImage(Object element) {
				SubWellFeature f = (SubWellFeature) element;
				if (f.isKey()) return IconManager.getIconImage("key.png");
				return null;
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null) return -1;
				if (e2 == null) return 1;
				boolean b1 = ((SubWellFeature) e1).isKey();
				boolean b2 = ((SubWellFeature) e2).isKey();
				return (b1 ^ b2) ? b1 ? 1 : -1 : 0;
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Sub-well Feature");
		tvc.getColumn().setWidth(200);
		tvc.getColumn().setToolTipText("Sub-well Feature");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SubWellFeature f = (SubWellFeature) element;
				return f.getName();
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null) return -1;
				if (e2 == null) return 1;
				return ((SubWellFeature) e1).getName().compareTo(((SubWellFeature) e2).getName());
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Alias");
		tvc.getColumn().setWidth(85);
		tvc.getColumn().setToolTipText("Alias / Short name");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SubWellFeature f = (SubWellFeature) element;
				return f.getShortName();
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null) return -1;
				if (e2 == null) return 1;
				if (((SubWellFeature) e1).getShortName() == null) return -1;
				if (((SubWellFeature) e2).getShortName() == null) return 1;
				return ((SubWellFeature) e1).getShortName().compareTo(((SubWellFeature) e2).getShortName());
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Group");
		tvc.getColumn().setWidth(150);
		tvc.getColumn().setToolTipText("Group Name");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SubWellFeature f = (SubWellFeature) element;
				if (f.getFeatureGroup() != null) {
					return f.getFeatureGroup().getName();
				}
				return "";
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				if (e1 == null) return -1;
				if (e2 == null) return 1;
				if (((SubWellFeature) e1).getFeatureGroup() == null) return -1;
				if (((SubWellFeature) e2).getFeatureGroup() == null) return 1;
				return ((SubWellFeature) e1).getFeatureGroup().getName().compareTo(((SubWellFeature) e2).getFeatureGroup().getName());
			}
		};
	}

	private void fillToolbar(Composite toolbar, FormToolkit toolkit) {
		ImageHyperlink addFeatureLink = toolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		addFeatureLink.setText("Add feature");
		addFeatureLink.setImage(IconManager.getIconImage("tag_red_add.png"));
		addFeatureLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				addSubWellFeature();
			}
		});
		addFeatureLink.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		ImageHyperlink deleteFeatureLink = toolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		deleteFeatureLink.setText("Delete feature(s)");
		deleteFeatureLink.setImage(IconManager.getIconImage("tag_red_delete.png"));
		deleteFeatureLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				deleteSubWellFeatues();
			}
		});
		deleteFeatureLink.setEnabled(parentPage.getEditor().isSaveAsAllowed());

		ImageHyperlink refreshAction = toolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		refreshAction.setToolTipText("Refresh");
		refreshAction.setImage(IconManager.getIconImage("refresh.png"));
		refreshAction.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				tableViewer.refresh(true);
			}
		});
	}

	private void createMenu() {
		if (!parentPage.getEditor().isSaveAsAllowed()) return;

		Menu menu = new Menu(tableViewer.getTable());
		tableViewer.getTable().setMenu(menu);

		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText("Add to group");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
				if (selection != null && !selection.isEmpty()) {
					List<IFeature> features = new ArrayList<>();
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						SubWellFeature f = (SubWellFeature) iterator.next();
						features.add(f);
					}
					int buttonPressed = new ManageGroupsDialog(Display.getDefault().getActiveShell(), protocolClass, GroupType.SUBWELL, features).open();
					if (buttonPressed == Window.OK) {
						tableViewer.refresh();
						parentPage.markDirty();
					}
				}
			}
		});
	}

	protected void deleteSubWellFeatues() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		boolean yes = MessageDialog.openQuestion(shell, "Delete?",
						"Are you sure you want to delete the selected feature(s)?");
		if (!yes) return;

		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				SubWellFeature f = (SubWellFeature) iterator.next();
				inputList.remove(f);
			}
			tableViewer.refresh();
			parentPage.markDirty();
		}

	}

	protected void addSubWellFeature() {
		SubWellFeature f = ProtocolService.getInstance().createSubWellFeature(protocolClass);
		inputList.add(f);
		SWTUtils.smartRefresh(tableViewer, false);
		tableViewer.reveal(f);
		tableViewer.setSelection(new StructuredSelection(f));
		parentPage.markDirty();
	}

	@Override
	protected void registerPages(DetailsPart part) {
		part.registerPage(SubWellFeature.class, new SubWellFeaturesDetailBlock(parentPage, this));
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();

		Action haction = new Action("hor", Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText("Horizontal orientation");
		haction.setImageDescriptor(IconManager
				.getIconDescriptor("application_tile_horizontal.png"));

		Action vaction = new Action("ver", Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText("Vertical orientation");
		vaction.setImageDescriptor(IconManager
				.getIconDescriptor("application_split.png"));

		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}

	public void refreshViewer() {
		SWTUtils.smartRefresh(tableViewer, true);
	}
}
