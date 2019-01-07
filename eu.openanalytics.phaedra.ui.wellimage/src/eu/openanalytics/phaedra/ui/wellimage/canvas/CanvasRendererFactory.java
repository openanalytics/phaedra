package eu.openanalytics.phaedra.ui.wellimage.canvas;

public class CanvasRendererFactory {

	private static final String PROP_RENDER_TYPE = "phaedra.canvas.renderer";
	
	private static final String RENDER_TYPE_INCR = "incremental";
	private static final String RENDER_TYPE_SLICED = "sliced";
	
	public static ICanvasRenderer createRenderer() {
		String renderType = System.getProperty(PROP_RENDER_TYPE);
		if (renderType == null || renderType.isEmpty() || renderType.toLowerCase().equals(RENDER_TYPE_INCR)) {
			return new IncrementalCanvasRenderer();
		} else if (renderType.toLowerCase().equals(RENDER_TYPE_SLICED)) {
			return new SlicedCanvasRenderer();
		}
		throw new IllegalArgumentException("Invalid setting: " + PROP_RENDER_TYPE + "=" + renderType);
	}
}
