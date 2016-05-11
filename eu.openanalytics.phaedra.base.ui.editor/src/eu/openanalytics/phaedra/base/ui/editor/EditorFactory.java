package eu.openanalytics.phaedra.base.ui.editor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class EditorFactory {
	
	private static EditorFactory instance;
	
	private Map<String, EditorMapping> editorMappings;
	
	private EditorFactory() {
		// Hidden constructor
		loadMappings();
	}
	
	public static synchronized EditorFactory getInstance() {
		if (instance == null) instance = new EditorFactory();
		return instance;
	}
	
	public IEditorPart openEditor(IValueObject vo) {
		if (vo == null) return null;
		return openEditor(Lists.newArrayList(vo));
	}
	
	public IEditorPart openEditor(List<? extends IValueObject> vo) {
		return openEditor(vo, null);
	}
	
	public IEditorPart openEditor(List<? extends IValueObject> vo, String editorId) {
		if (vo == null || vo.isEmpty()) return null;
		
		EditorMapping mapping = editorMappings.get(vo.get(0).getClass().getName());
		if (mapping == null) return null;
		
		IEditorInput input = mapping.getEditorInputFactory().createEditorInput(vo);
		if (editorId == null) editorId = mapping.getEditorInputFactory().getEditorIdFor(input);
		
		if (editorId == null) {
			editorId = mapping.getEditorId();
			if (vo.size() > 1 && mapping.getMultiItemEditorId() != null) editorId = mapping.getMultiItemEditorId();
		}
		
		if (editorId == null || input == null) return null;
		
		return openEditor(input, editorId);
	}
	
	public IEditorPart openEditor(IEditorInput input, String editorId) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			return page.openEditor(input, editorId);
		} catch (PartInitException e) {
			EclipseLog.error("Failed to open editor associated with input " + input.getName(), e, Activator.getDefault());
		}
		return null;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void loadMappings() {
		if (editorMappings == null) editorMappings = new HashMap<>();
		else editorMappings.clear();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EditorMapping.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				String className = el.getAttribute(EditorMapping.ATTR_CLASS);
				
				EditorMapping mapping = new EditorMapping();
				mapping.setClassName(className);
				mapping.setEditorId(el.getAttribute(EditorMapping.ATTR_EDITOR_ID));
				mapping.setMultiItemEditorId(el.getAttribute(EditorMapping.ATTR_MULTI_ITEM_EDITOR_ID));
				
				IEditorInputFactory factory = null;
				if (el.getAttribute(EditorMapping.ATTR_CUSTOM_FACTORY) == null) {
					factory = new VOEditorInputFactory();
				} else {
					factory = (IEditorInputFactory) el.createExecutableExtension(EditorMapping.ATTR_CUSTOM_FACTORY);
				}
				mapping.setEditorInputFactory(factory);
				
				editorMappings.put(className, mapping);
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
}
