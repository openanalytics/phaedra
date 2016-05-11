package eu.openanalytics.phaedra.base.imaging.util.placeholder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.imaging.util.IMConverter;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class PlaceHolderFactory {
	
	private static PlaceHolderFactory instance;

	public final static int MODE_OPAQUE = 1;
	public final static int MODE_OVERLAY = 2;
	
	private String tempDir;
	private String defaultPlaceholderPath;
	private Map<String, String> placeHolders;
	
	private PlaceHolderFactory() {
		// Hidden constructor.
		
		tempDir = FileUtils.generateTempFolder(true);
		placeHolders = new HashMap<>();
		
		defaultPlaceholderPath = tempDir + "/placeholder.tif";
		InputStream in = PlaceHolderFactory.class.getResourceAsStream("placeholder.tif");
		OutputStream out = null;
		try {
			out = new FileOutputStream(defaultPlaceholderPath);
			StreamUtils.copyAndClose(in, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static PlaceHolderFactory getInstance() {
		synchronized (PlaceHolderFactory.class) {
			if (instance == null) instance = new PlaceHolderFactory();
		}
		return instance;
	}
	
	public synchronized String getPlaceholder(int x, int y, int bpp, int mode) throws IOException {
		String key = x + "-" + y + "-" + bpp + "-" + mode;
		String ph = placeHolders.get(key);
		if (ph == null) {
			ph = tempDir + "/" + key + ".tif";
			generatePlaceholder(x, y, bpp, mode, ph);
			placeHolders.put(key, ph);
		}
		return ph;
	}
	
	public void generatePlaceholder(int x, int y, int bpp, int mode, String destination) throws IOException {
		List<String> args = new ArrayList<String>();

		if (mode == MODE_OPAQUE) args.add(defaultPlaceholderPath);
		
		args.add("-compress");
		args.add("None");
		
		// Apply size
		args.add("-resize");
		args.add(""+x+"x"+y+"!");
		
		boolean trueColor = bpp >= 24;
		
		// Apply bitrate (per channel)
		int depth = trueColor ? 8 : bpp;
		args.add("-depth");
		args.add(""+depth);
		
		// Apply color type
		// Assume 24 bpp is TrueColor, all other depths are GrayScale
		String type = trueColor ? "TrueColor" : "GrayScale";
		args.add("-type");
		args.add(type);
		
		if (mode == MODE_OVERLAY) args.add("xc:black");
		
		args.add(destination);
		
		String[] argArray = args.toArray(new String[args.size()]);
		IMConverter.convert(argArray);
	}
}
