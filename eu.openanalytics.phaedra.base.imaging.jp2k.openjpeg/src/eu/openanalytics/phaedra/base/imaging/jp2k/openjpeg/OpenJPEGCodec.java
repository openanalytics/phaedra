package eu.openanalytics.phaedra.base.imaging.jp2k.openjpeg;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

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
	public IDecodeAPI getDecoder(SeekableByteChannel channel, int imageCount, int componentCount) throws IOException {
		return new ZIPDecoder(channel, componentCount);
	}

}
