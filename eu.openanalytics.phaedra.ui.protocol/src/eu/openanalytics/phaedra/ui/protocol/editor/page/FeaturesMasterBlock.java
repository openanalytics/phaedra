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
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.dialog.ManageGroupsDialog;

public class FeaturesMasterBlock extends MasterDetailsBlock {

	private TableViewer tableViewer;

	private WritableList inputList;
	private FeaturesPage parentPage;
	private ProtocolClass protocolClass;

	public FeaturesMasterBlock(ProtocolClass protocolClass, FeaturesPage page) {
		List<Feature> features = protocolClass.getFeatures();
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

		this.inputList = new WritableList(features, Feature.class);
		this.parentPage = page;
		this.protocolClass = protocolClass;
	}

	@Override
	protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();

		// Create layout

		final Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		toolkit.paintBordersFor(container);

		final Section section = toolkit.createSection(container, Section.TITLE_BAR | Section.FOCUS_TITLE | Section.EXPANDED);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setText("Features");

		final Composite subContainer = toolkit.createComposite(section, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(subContainer);
		toolkit.paintBordersFor(subContainer);
		section.setClient(subContainer);

		final SectionPart part = new SectionPart(section);
		managedForm.addPart(part);

		// Create components

		FilterMatcher matcher = new FilterMatcher();
		FilteredTable filteredTable = new FilteredTable(subContainer, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER, matcher, true);
		tableViewer = filteredTable.getViewer();

		Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL,SWT.FILL)
			.span(2,1)
			.hint(SWT.DEFAULT,50) // Won't actually be 50, but prevents table from grabbing too much.
			.grab(true,true)
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

		// Create toolbar

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
		tvc.getColumn().setToolTipText("This is a Key feature");
		tvc.getColumn().setWidth(35);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
			@Override
			public Image getImage(Object element) {
				Feature f = (Feature) element;
				if (f.isKey()) return IconManager.getIconImage("key.png");
				return null;
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				boolean b1 = ((Feature) e1).isKey();
				boolean b2 = ((Feature) e2).isKey();
				return (b1 ^ b2) ? b1 ? 1 : -1 : 0;
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Calc");
		tvc.getColumn().setToolTipText("This is a Calculated feature");
		tvc.getColumn().setWidth(40);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
			@Override
			public Image getImage(Object element) {
				Feature f = (Feature) element;
				if (f.isCalculated()) return IconManager.getIconImage("aggregation.gif");
				return null;
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				boolean b1 = ((Feature) e1).isCalculated();
				boolean b2 = ((Feature) e2).isCalculated();
				return (b1 ^ b2) ? b1 ? 1 : -1 : 0;
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Fit");
		tvc.getColumn().setWidth(40);
		tvc.getColumn().setToolTipText("This feature has a dose-response curve");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
			@Override
			public Image getImage(Object element) {
				Feature f = (Feature) element;
				if (f.getCurveSettings().containsKey(CurveSettings.KIND)) {
					return IconManager.getIconImage("curve.png");
				}
				return null;
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				boolean b1 = ((Feature) e1).getCurveSettings().containsKey(CurveSettings.KIND);
				boolean b2 = ((Feature) e2).getCurveSettings().containsKey(CurveSettings.KIND);
				return (b1 ^ b2) ? b1 ? 1 : -1 : 0;
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Feature");
		tvc.getColumn().setWidth(200);
		tvc.getColumn().setToolTipText("Feature");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return f.getName();
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				String n1 = ((Feature) e1).getName();
				String n2 = ((Feature) e2).getName();
				if (n1 == n2) return 0;
				if (n1 == null) return -1;
				if (n2 == null) return 1;
				return (n1.compareTo(n2));
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Alias");
		tvc.getColumn().setWidth(80);
		tvc.getColumn().setToolTipText("Alias / Short name");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				return f.getShortName();
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				String n1 = ((Feature) e1).getShortName();
				String n2 = ((Feature) e2).getShortName();
				if (n1 == n2) return 0;
				if (n1 == null) return -1;
				if (n2 == null) return 1;
				return (n1.compareTo(n2));
			}
		};

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Group");
		tvc.getColumn().setWidth(100);
		tvc.getColumn().setToolTipText("Group Name");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Feature f = (Feature) element;
				if (f.getFeatureGroup() != null) {
					return f.getFeatureGroup().getName();
				}
				return "";
			}
		});
		new TableViewerSorter(tableViewer, tvc){
			@Override
			protected int doCompare(Viewer viewer, Object e1, Object e2) {
				FeatureGroup fg1 = ((Feature) e1).getFeatureGroup();
				FeatureGroup fg2 = ((Feature) e2).getFeatureGroup();
				return ProtocolUtils.FEATURE_GROUP_NAME_SORTER.compare(fg1, fg2);
			}
		};
	}

	private void fillToolbar(Composite toolbar, FormToolkit toolkit) {
		ImageHyperlink addFeatureLink = toolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		addFeatureLink.setText("Add feature");
		addFeatureLink.setImage(IconManager.getIconImage("tag_blue_add.png"));
		addFeatureLink.setEnabled(parentPage.getEditor().isSaveAsAllowed());
		addFeatureLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				addFeature();
			}
		});

		ImageHyperlink deleteFeatureLink = toolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		deleteFeatureLink.setText("Delete feature(s)");
		deleteFeatureLink.setImage(IconManager.getIconImage("tag_blue_delete.png"));
		deleteFeatureLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				deleteFeatues();
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
						Feature f = (Feature) iterator.next();
						features.add(f);
					}
					int buttonPressed = new ManageGroupsDialog(Display.getDefault().getActiveShell(), protocolClass, GroupType.WELL, features).open();
					if (buttonPressed == Window.OK) {
						tableViewer.refresh();
						parentPage.markDirty();
					}
				}
			}
		});
	}

	protected void deleteFeatues() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		boolean confirmed = MessageDialog.openQuestion(shell, "Delete?",
				"Are you sure you want to delete the selected feature(s)?");
		if (!confirmed) return;

		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				Feature f = (Feature) iterator.next();
				inputList.remove(f);
			}
			tableViewer.refresh();
			parentPage.markDirty();
		}
	}

	protected void addFeature() {
		Feature f = ProtocolService.getInstance().createFeature(protocolClass);
		inputList.add(f);
		tableViewer.refresh();
		tableViewer.reveal(f);
		tableViewer.setSelection(new StructuredSelection(f));
		parentPage.markDirty();
	}

	@Override
	protected void registerPages(DetailsPart part) {
		part.registerPage(Feature.class, new FeaturesDetailBlock(parentPage, this));
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();

		Action action = new Action("hor", Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		action.setChecked(true);
		action.setToolTipText("Horizontal orientation");
		action.setImageDescriptor(IconManager.getIconDescriptor("application_tile_horizontal.png"));
		form.getToolBarManager().add(action);

		action = new Action("ver", Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		action.setChecked(false);
		action.setToolTipText("Vertical orientation");
		action.setImageDescriptor(IconManager.getIconDescriptor("application_split.png"));
		form.getToolBarManager().add(action);
	}

	public void refreshViewer() {
		SWTUtils.smartRefresh(tableViewer, true);
	}
}
