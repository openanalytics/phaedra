package eu.openanalytics.phaedra.base.ui.editor;

public class EditorMapping {
	
	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".editorMapping";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_EDITOR_ID = "editorId";
	public final static String ATTR_MULTI_ITEM_EDITOR_ID = "multiItemEditorId";
	public final static String ATTR_CUSTOM_FACTORY = "customFactory";
	
	private String className;
	private String editorId;
	private String multiItemEditorId;
	private IEditorInputFactory editorInputFactory;
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getEditorId() {
		return editorId;
	}
	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}
	public String getMultiItemEditorId() {
		return multiItemEditorId;
	}
	public void setMultiItemEditorId(String multiItemEditorId) {
		this.multiItemEditorId = multiItemEditorId;
	}
	public IEditorInputFactory getEditorInputFactory() {
		return editorInputFactory;
	}
	public void setEditorInputFactory(IEditorInputFactory editorInputFactory) {
		this.editorInputFactory = editorInputFactory;
	}
}