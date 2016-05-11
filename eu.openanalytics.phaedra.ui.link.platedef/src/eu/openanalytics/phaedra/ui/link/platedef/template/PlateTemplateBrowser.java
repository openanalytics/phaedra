package eu.openanalytics.phaedra.ui.link.platedef.template;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;


public class PlateTemplateBrowser extends ViewPart {

	private TreeViewer treeViewer;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(parent);

		treeViewer = new TreeViewer(parent);
		treeViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof ProtocolClass) {
					return ((ProtocolClass)element).getName();
				}
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof ProtocolClass) {
					return IconManager.getIconImage("struct.png");
				}
				return IconManager.getIconImage("plate.png");
			}
		});
		treeViewer.setContentProvider(createTreeContentProvider());
		treeViewer.setInput("root");

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection)event.getSelection();
				Object selectedItem = sel.getFirstElement();
				if (selectedItem instanceof PlateTemplate) {
					PlateTemplateEditorInput input = new PlateTemplateEditorInput();
					PlateTemplate template = (PlateTemplate)selectedItem;
					try {
						// In the editor, use a clone instead of the original
						// If the user aborts the editing, the clone will be
						// thrown away.
						input.setPlateTemplate(template.clone());
						getSite().getPage().openEditor(input, PlateTemplateEditor.class.getName());
					} catch (PartInitException e) {
						// Do nothing.
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				} else {
					treeViewer.expandToLevel(selectedItem, 1);
				}
			}
		});

		GridDataFactory.fillDefaults().grab(true, true).applyTo(treeViewer.getControl());

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewPlateTemplateBrowser");
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	public void reloadTemplates() {
		treeViewer.setInput("root");
	}

	private ITreeContentProvider createTreeContentProvider() {
		ITreeContentProvider provider = new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				List<ProtocolClass> pClasses = ProtocolService.getInstance().getProtocolClasses();
				Collections.sort(pClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
				ProtocolClass[] array = CollectionUtils.listToArray(pClasses);
				return array == null ? new ProtocolClass[0] : array;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof ProtocolClass) {
					ProtocolClass protocolClass = (ProtocolClass)parentElement;
					try {
						List<PlateTemplate> templates =
								PlateDefinitionService.getInstance().getTemplateManager().getTemplates(protocolClass.getId());
						PlateTemplate[] array = CollectionUtils.listToArray(templates);
						return array == null ? new PlateTemplate[0] : array;
					} catch (IOException e) {}
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return (element instanceof String
						|| element instanceof ProtocolClass);
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// Do nothing.
			}

			@Override
			public void dispose() {
				// Do nothing.
			}
		};
		return provider;
	}
}
