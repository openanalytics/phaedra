package eu.openanalytics.phaedra.base.imaging.jp2k.codestream;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ICodeStreamByteSource {

	public void get(long offset, int size, ByteBuffer destination) throws IOException;

}
