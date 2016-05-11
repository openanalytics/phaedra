package eu.openanalytics.phaedra.base.imaging.jp2k.openjpeg;

import java.io.IOException;

import eu.openanalytics.phaedra.base.imaging.jp2k.ICodec;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

public class OpenJPEGCodec implements ICodec {

	@Override
	public String getName() {
		return "OpenJPEG";
	}

	@Override
	public IEncodeAPI getEncoder() {
		return new Encoder();
	}

	@Override
	public IDecodeAPI getDecoder(String filePath, int imageCount, int componentCount) throws IOException {
		String ext = FileUtils.getExtension(filePath).toLowerCase();
		if (ext.equals("zip")) return new ZIPDecoder(filePath, componentCount);
		return null;
	}

}
