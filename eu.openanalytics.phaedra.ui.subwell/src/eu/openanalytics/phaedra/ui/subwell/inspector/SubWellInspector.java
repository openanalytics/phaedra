package eu.openanalytics.phaedra.ui.subwell.inspector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.misc.ImageCanvas;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.plate.util.FeaturePatternFilter;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.subwell.Activator;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class SubWellInspector extends DecoratedView {

	private BreadcrumbViewer breadcrumb;

	private FormToolkit formToolkit;

	private ISelectionListener selectionListener;
	private IUIEventListener imageSettingListener;

	private Well currentWell;
	private int currentIndex;

	private ImageCanvas imgCanvas;
	private ImageControlPanel imgControlPanel;

	private TreeViewer treeViewer;

	@Override
	public void createPartControl(Composite parent) {
		formToolkit = FormEditorUtils.createToolkit();

		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		Label separator = formToolkit.createSeparator(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

		final ScrolledForm form = FormEditorUtils.createScrolledForm("Cell: <no cell selected>", 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);

		// Section 1: Subwell Image
		Section section = FormEditorUtils.createSection("Subwell Image", form.getBody(), formToolkit, false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(section);
		Composite sectionContainer = FormEditorUtils.createComposite(1, section, formToolkit);

		imgControlPanel = new ImageControlPanel(sectionContainer, SWT.NONE, true, false);
		imgControlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void componentToggled(int component, boolean state) {
				updateSubWellImage();
			}
			@Override
			public void scaleChanged(float ratio) {
				updateSubWellImage();
				form.reflow(true);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(imgControlPanel);

		imgCanvas = new ImageCanvas(sectionContainer, SWT.NONE, SWT.CENTER);
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, 50).applyTo(imgCanvas);

		// Section 2: Subwell Features
		section = FormEditorUtils.createSection("Subwell Features", form.getBody(), formToolkit, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
		sectionContainer = FormEditorUtils.createComposite(1, section, formToolkit);

		FilteredTree filteredTree = new FilteredTree(sectionContainer
				, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
				, new FeaturePatternFilter(), true);
		treeViewer = filteredTree.getViewer();
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		TreeColumn col = column.getColumn();
		col.setAlignment(SWT.LEFT);
		col.setText("Feature");
		col.setToolTipText("Feature (Group)");
		col.setWidth(180);
		new ColumnViewerSorter<>(treeViewer, column, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1 instanceof FeatureGroup && o2 instanceof FeatureGroup) {
					FeatureGroup f1 = (FeatureGroup) o1;
					FeatureGroup f2 = (FeatureGroup) o2;
					return f1.getName().compareTo(f2.getName());
				}
				if (o1 instanceof SubWellFeature && o2 instanceof SubWellFeature) {
					SubWellFeature f1 = (SubWellFeature) o1;
					SubWellFeature f2 = (SubWellFeature) o2;
					return f1.getName().compareTo(f2.getName());
				}
				return 0;
			}
		});
		column = new TreeViewerColumn(treeViewer, SWT.RIGHT);
		col = column.getColumn();
		col.setAlignment(SWT.RIGHT);
		col.setText("Feature Value");
		col.setToolTipText("Feature Value");
		col.setWidth(100);
		new ColumnViewerSorter<>(treeViewer, column, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1 instanceof SubWellFeature && o2 instanceof SubWellFeature) {
					SubWellFeature f1 = (SubWellFeature) o1;
					SubWellFeature f2 = (SubWellFeature) o2;
					Object d1 = SubWellService.getInstance().getData(currentWell, f1);
					Object d2 = SubWellService.getInstance().getData(currentWell, f2);
					String t1 = "";
					String t2 = "";
					if (d1 != null) t1 = Formatters.getInstance().format(Array.get(d1, currentIndex), f1);
					if (d2 != null) t2 = Formatters.getInstance().format(Array.get(d2, currentIndex), f2);
					return StringUtils.compareToNumericStrings(t1, t2);
				}
				return 0;
			}
		});

		treeViewer.setContentProvider(new CellFeatureContentProvider());
		treeViewer.setLabelProvider(new CellFeatureLabelProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(filteredTree);

		// Selection handling
		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				SubWellSelection subwellSelection = SelectionUtils.getFirstObject(selection, SubWellSelection.class);
				if (subwellSelection != null) {
					int indexSelection = subwellSelection.getIndices().nextSetBit(0);
					Well wellSelection = subwellSelection.getWell();
					if (indexSelection >= 0 && (indexSelection != currentIndex || !wellSelection.equals(currentWell))) {
						currentWell = wellSelection;
						currentIndex = indexSelection;
						String pos = NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn());
						form.setText("Cell: " + currentIndex + " @ Well " + pos);
						loadSubWell();
						form.reflow(true);
					}
				} else {
					SubWellItem swItem = SelectionUtils.getFirstObject(selection, SubWellItem.class);
					if (swItem != null) {
						int indexSelection = swItem.getIndex();
						Well wellSelection = swItem.getWell();
						if (indexSelection >= 0 && (indexSelection != currentIndex || !wellSelection.equals(currentWell))) {
							currentWell = wellSelection;
							currentIndex = indexSelection;
							String pos = NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn());
							form.setText("Cell: " + currentIndex + " @ Well " + pos);
							loadSubWell();
							form.reflow(true);
						}
					}
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		imageSettingListener = event -> {
			if (event.type == EventType.ImageSettingsChanged) {
				updateSubWellImage();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingListener);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		SelectionUtils.triggerActiveSelection(selectionListener);
		form.reflow(true);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewCellInspector");
	}

	@Override
	public void setFocus() {
		// Do nothing.
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(imageSettingListener);
		super.dispose();
	}

	@Override
	protected void fillToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		treeViewer.setData("isExpanded", false);
		Action expandAllAction = new Action() {
			@Override
			public void run() {
				if ((boolean) treeViewer.getData("isExpanded")) {
					treeViewer.collapseAll();
					treeViewer.setData("isExpanded", false);
				} else {
					treeViewer.expandAll();
					treeViewer.setData("isExpanded", true);
				}
			}
		};
		expandAllAction.setToolTipText("Collapse All");
		expandAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
		mgr.add(expandAllAction);

		super.fillToolbar();
	}

	private void loadSubWell() {
		ProtocolClass pClass = PlateUtils.getProtocolClass(currentWell);
		imgControlPanel.setImage(pClass);

		breadcrumb.setInput(currentWell);
		breadcrumb.getControl().getParent().layout();

		updateSubWellImage();

		TreePath[] expandedTreePaths = treeViewer.getExpandedTreePaths();
		treeViewer.setInput(new SubWellItem(currentWell, currentIndex));
		treeViewer.setExpandedTreePaths(expandedTreePaths);
	}

	private void updateSubWellImage() {
		if (currentWell == null) return;

		boolean[] channels = imgControlPanel.getButtonStates();
		float scale = imgControlPanel.getCurrentScale();

		Rectangle rect = ImageRenderService.getInstance().getSubWellImageBounds(currentWell, currentIndex, scale);
		int prefHeight = rect.height;
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, prefHeight).applyTo(imgCanvas);

		try {
			ImageData imageData = ImageRenderService.getInstance().getSubWellImageData(currentWell, currentIndex, scale, channels);
			imgCanvas.setImageData(imageData);
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private Protocol getProtocol() {
		if (currentWell == null) return null;
		return (Protocol) currentWell.getAdapter(Protocol.class);
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty("channels", imgControlPanel.getButtonStates());
		properties.addProperty("scale", imgControlPanel.getCurrentScale());
		return properties;
	}

	private void setProperties(Properties properties) {
		imgControlPanel.setButtonStates(properties.getProperty("channels", imgControlPanel.getButtonStates()));
		imgControlPanel.setCurrentScale(properties.getProperty("scale", imgControlPanel.getCurrentScale()));
	}
	
	private class CellFeatureLabelProvider extends CellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			String text = "";
			if (element instanceof SubWellFeature) {
				SubWellFeature feature = (SubWellFeature) element;
				if (cell.getColumnIndex() == 0) {
					text = feature.getName();
				} else {
					Object data = SubWellService.getInstance().getData(currentWell, feature);
					if (data != null) {
						text = Formatters.getInstance().format(Array.get(data, currentIndex), feature);
					}
				}
			}
			if (element instanceof FeatureGroup) {
				if (cell.getColumnIndex() == 0) {
					text = ((FeatureGroup) element).getName();
				}
			}
			cell.setText(text);
		}
	}

	private class CellFeatureContentProvider implements ITreeContentProvider {
		private ProtocolClass pClass;
		@Override
		public void dispose() {
			// Do nothing.
		}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof SubWellItem) {
				pClass = PlateUtils.getProtocolClass(((SubWellItem) inputElement).getWell());
				List<FeatureGroup> fgs = ProtocolService.getInstance().getAllFeatureGroups(pClass, GroupType.SUBWELL);
				return fgs.toArray();
			}
			return null;
		}
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof FeatureGroup) {
				List<SubWellFeature> featuresByGroup = ProtocolService.getInstance().getMembers((FeatureGroup) parentElement);
				return featuresByGroup.toArray();
			}
			return null;
		}
		@Override
		public Object getParent(Object element) {
			return null;
		}
		@Override
		public boolean hasChildren(Object element) {
			return element instanceof FeatureGroup;
		}
	}

}