package eu.openanalytics.phaedra.ui.protocol.breadcrumb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;
import org.openscada.ui.breadcrumbs.IBreadcrumbDropDownSite;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.protocol.cmd.CopyQualifiedNames;
import eu.openanalytics.phaedra.ui.protocol.cmd.CopyURLs;

/**
 * <p>This factory creates breadcrumbs (horizontal navigation bars)
 * that show the hierarchy of objects. E.g. for a Plate, it can show
 * the parent Experiment, Protocol and Protocol Class.</p>
 *
 * <p>The breadcrumb is interactive: each element has a context menu
 * that allows opening the parent element in an appropriate view or editor,
 * e.g. an Experiment Inspector or a Protocol Browser.</p>
 */
public class BreadcrumbFactory {

	private static final ValueObservable observable = new ValueObservable();

	public static BreadcrumbViewer createBreadcrumb(Composite parent) {

		final BreadcrumbViewer breadcrumb = new BreadcrumbViewer(parent, SWT.NONE) {
			@Override
			protected Control createDropDown(Composite parent, final IBreadcrumbDropDownSite site, final TreePath path) {
				return createMenuFor(parent, site, path);
			}

		};

		// Observer that toggles the breadcrumb
		final Observer observer = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if ((boolean) arg) {
					GridDataFactory.fillDefaults().hint(0, 0).applyTo(breadcrumb.getControl());
				} else {
					GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());
				}
				breadcrumb.getControl().getParent().layout();
			}
		};

		// Add the observer
		observable.addObserver(observer);
		breadcrumb.getControl().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				observable.deleteObserver(observer);
			}
		});

		// Open dropdown on left-click
		breadcrumb.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IValueObject o = SelectionUtils.getFirstObject(event.getSelection(), IValueObject.class);
				if (o != null) breadcrumb.openDropDown(o);
			}
		});

		breadcrumb.setContentProvider(new ITreePathContentProvider() {
			private IValueObject input;

			@Override
			public Object[] getElements(Object inputElement) {
				IValueObject root = input;
				IValueObject p = root.getParent();
				while (p != null) {
					root = p;
					p = p.getParent();
				}
				return new Object[]{root};
			}

			@Override
			public Object[] getChildren(TreePath parentPath) {
				Object o = parentPath.getLastSegment();
				IValueObject root = input;
				IValueObject p = root;
				while (p != null) {
					root = p;
					p = p.getParent();
					if (p == o) {
						return new Object[]{root};
					}
				}
				return null;
			}

			@Override
			public boolean hasChildren(TreePath path) {
				return true;
			}

			@Override
			public TreePath[] getParents(Object element) {
				return null;
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput == null) {
					input = null;
				} else if (newInput instanceof IValueObject && newInput instanceof PlatformObject) {
					input = (IValueObject) newInput;
				} else {
					String clazz = (newInput == null) ? "<null>" : newInput.getClass().toString();
					throw new IllegalArgumentException("Invalid class for breadcrumb input: " + clazz);
				}
			}

			@Override
			public void dispose() {
				// Do nothing.
			}
		});

		breadcrumb.setLabelProvider(new ITreePathLabelProvider() {

			private Map<Class<?>, Image> imageIcons = new HashMap<>();

			@Override
			public void updateLabel(ViewerLabel label, TreePath elementPath) {
				Object o = elementPath.getLastSegment();

				Image img = null;
				if (imageIcons.containsKey(o.getClass())) {
					 img = imageIcons.get(o.getClass());
				} else {
					ImageDescriptor iconDescriptor = IconManager.getDefaultIconDescriptor(o.getClass());
					if (iconDescriptor != null) {
						img = iconDescriptor.createImage();
						imageIcons.put(o.getClass(), img);
					}
				}
				if (img != null) label.setImage(img);

				// See issue PHAEDRA-2569, consider improving updateSize() method in BreadcrumbViewer.
				//label.setText(StringUtils.trim(o.toString(), 30));
				label.setText(o.toString());
				label.setTooltipText(o.toString());
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
				// Do nothing.
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
				// Do nothing.
			}

			@Override
			public void dispose() {
				for (Image i: imageIcons.values()) i.dispose();
			}
		});

		return breadcrumb;
	}

	public static void toggleBreadcrumbs(boolean isHidden) {
		observable.valueChanged(isHidden);
	}

	private static Control createMenuFor(Composite parent, final IBreadcrumbDropDownSite site, final TreePath path) {

		Composite dropDown = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dropDown);
		GridLayoutFactory.fillDefaults().margins(5,0).applyTo(dropDown);

		Table table = new Table(dropDown, SWT.FULL_SELECTION);
		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Table t = (Table)e.getSource();
				TableItem selectedItem = t.getItem(t.getSelectionIndex());
				SelectionListener l = (SelectionListener)selectedItem.getData();
				if (l != null) l.widgetSelected(e);
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		// Two columns: image and text
		new TableColumn(table, SWT.NONE);
		new TableColumn(table, SWT.NONE);

		// Menu items are based on the Object type
		final Object o = path.getLastSegment();

		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IBreadcrumbProvider.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object exEl = el.createExecutableExtension(IBreadcrumbProvider.ATTR_CLASS);
				if (exEl instanceof IBreadcrumbProvider) {
					IBreadcrumbProvider breadcrumbProvider = (IBreadcrumbProvider) exEl;
					breadcrumbProvider.addMenuContribution(o, table, path);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}

		List<IBreadcrumbProvider> providers = new ArrayList<>();
		for (IBreadcrumbProvider provider : providers ) {
			provider.addMenuContribution(o, table, path);
		}

		if (o instanceof IValueObject) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setImage(0, IconManager.getIconImage("page_copy.png"));
			item.setText(1, "Copy Qualified Name");
			item.setData(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					copyToClipboard(path.getLastSegment());
					site.close();
				}
			});
			item = new TableItem(table, SWT.NONE);
			item.setImage(0, IconManager.getIconImage("page_copy.png"));
			item.setText(1, "Copy URL to Clipboard");
			item.setData(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					copyURLToClipboard(path.getLastSegment());
					site.close();
				}
			});
		}

		for (TableColumn c: table.getColumns()) c.pack();
		return dropDown;
	}

	private static void copyToClipboard(Object o) {
		if (o instanceof IValueObject) CopyQualifiedNames.execute((IValueObject)o);
	}

	private static void copyURLToClipboard(Object o) {
		if (o instanceof IValueObject) CopyURLs.execute((IValueObject)o);
	}

}