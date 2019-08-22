package eu.openanalytics.phaedra.base.util.misc;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import eu.openanalytics.phaedra.base.util.Activator;


/**
 * Graphic representation of a formula read from SVG or PNG file.
 */
public class UrlFormulaDescriptor extends FormulaDescriptor {
	
	private URL svgUrl;
	private URL pngUrl;
	
	private boolean loaded;
	
	private byte[] svg;
	private byte[] png;
	
	
	/**
	 * Creates new descriptor
	 * 
	 * @param url the url of the SVG file
	 */
	public UrlFormulaDescriptor(URL svgUrl, URL pngUrl) {
		this.svgUrl = svgUrl;
		this.pngUrl = pngUrl;
	}
	
	
	public URL getUrl() {
		return svgUrl;
	}
	
	@Override
	public byte[] getSvg() {
		if (!loaded) {
			load();
		}
		return svg;
	}
	
	@Override
	public byte[] getPng() {
		if (!loaded) {
			load();
		}
		return png;
	}
	
	private void load() {
		svg = loadBytes(svgUrl);
		png = loadBytes(pngUrl);
	}
	
	private byte[] loadBytes(URL url) {
		if (url == null) return null;
		try (InputStream in = url.openConnection().getInputStream()) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] b = new byte[4096];
			int n;
			while ((n = in.read(b)) > 0) {
				out.write(b, 0, n);
			}
			return out.toByteArray();
		} catch (FileNotFoundException e) {
			// Do not log this exception.
		} catch (Exception e) {
			EclipseLog.error(String.format("Failed to load image of formula from '%1$s'.", url),
					e, Activator.getDefault() );
		} finally {
			loaded = true;
		}
		return null;
	}
	
}
