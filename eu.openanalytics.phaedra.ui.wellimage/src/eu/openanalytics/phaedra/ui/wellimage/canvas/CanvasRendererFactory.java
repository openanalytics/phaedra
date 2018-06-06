package eu.openanalytics.phaedra.ui.wellimage.canvas;

public class CanvasRendererFactory {

	public static ICanvasRenderer createRenderer() {
		if ("incremental".equalsIgnoreCase(System.getProperty("phaedra.canvas.renderer"))) return new IncrementalCanvasRenderer();
		else return new SlicedCanvasRenderer();
	}
}
