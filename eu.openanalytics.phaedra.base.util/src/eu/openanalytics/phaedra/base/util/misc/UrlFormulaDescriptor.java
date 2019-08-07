package eu.openanalytics.phaedra.base.util.misc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import eu.openanalytics.phaedra.base.util.Activator;


/**
 * Graphic representation of a formula read from SVG file.
 */
public class UrlFormulaDescriptor extends FormulaDescriptor {
	
	
	private URL url;
	
	private boolean loaded;
	private byte[] svg;
	
	
	/**
	 * Creates new descriptor
	 * 
	 * @param url the url of the SVG file
	 */
	public UrlFormulaDescriptor(URL url) {
		this.url = url;
	}
	
	
	public URL getUrl() {
		return url;
	}
	
	@Override
	public byte[] getSvg() {
		if (!loaded) {
			loadSvg();
		}
		return svg;
	}
	
	private void loadSvg() {
		try (InputStream in = url.openConnection().getInputStream()) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] b = new byte[4096];
			int n;
			while ((n = in.read(b)) > 0) {
				out.write(b, 0, n);
			}
			svg = out.toByteArray();
		} catch (Exception e) {
			EclipseLog.error(String.format("Failed to load SVG of formula from '%1$s'.", url),
					e, Activator.getDefault() );
		} finally {
			loaded = false;
		}
	}
	
}
