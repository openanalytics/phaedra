package eu.openanalytics.phaedra.base.imaging.jp2k.openjpeg;

import eu.openanalytics.phaedra.base.imaging.jp2k.ICodec;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;

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
	public IDecodeAPI getDecoder() {
		return new ZIPDecoder();
	}

}
