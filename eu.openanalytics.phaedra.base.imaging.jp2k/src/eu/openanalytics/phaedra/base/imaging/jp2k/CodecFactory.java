package eu.openanalytics.phaedra.base.imaging.jp2k;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;


public class CodecFactory {

	public final static String FORMAT_ZIP = "zip";
	public final static String FORMAT_JPX = "jpx";
	
	private final static String[] SUPPORTED_FORMATS = { FORMAT_ZIP, FORMAT_JPX };
	
	private static List<ICodec> codecs;
	private static ICodec preferredCodec;
	static {
		loadCodecs();
	}
	
	public static String[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	public static IDecodeAPI getDecoder(SeekableByteChannel channel, int imageCount, int componentCount) throws IOException {
		if (channel == null) return null;
		if (preferredCodec != null) {
			IDecodeAPI decoder = preferredCodec.getDecoder(channel, imageCount, componentCount);
			if (decoder != null) return decoder;
		}
		for (ICodec codec: codecs) {
			IDecodeAPI decoder = codec.getDecoder(channel, imageCount, componentCount);
			if (decoder != null) return decoder;
		}
		return null;
	}

	public static IEncodeAPI getEncoder() {
		if (preferredCodec != null) {
			IEncodeAPI encoder = preferredCodec.getEncoder();
			if (encoder != null) return encoder;
		}
		for (ICodec codec: codecs) {
			IEncodeAPI encoder = codec.getEncoder();
			if (encoder != null) return encoder;
		}
		return null;
	}
			
	private static void loadCodecs() {
		codecs = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ICodec.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				ICodec codec = (ICodec)el.createExecutableExtension(ICodec.ATTR_CLASS);
				boolean preferred = Boolean.valueOf(el.getAttribute(ICodec.ATTR_PREFERRED));
				if (preferred && preferredCodec == null) preferredCodec = codec;
				codecs.add(codec);
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
		EclipseLog.info(String.format("%d codecs registered. Preferred codec: %s", codecs.size(), preferredCodec), Activator.getDefault());
	}
}