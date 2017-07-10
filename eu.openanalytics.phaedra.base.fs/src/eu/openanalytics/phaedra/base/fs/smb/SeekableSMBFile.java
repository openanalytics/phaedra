package eu.openanalytics.phaedra.base.fs.smb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

public class SeekableSMBFile implements SeekableByteChannel, AutoCloseable {

	private SmbRandomAccessFile raf;
	private boolean open = false;
	
	public SeekableSMBFile(SmbFile file, String mode) throws IOException {
		this.raf = new SmbRandomAccessFile(file, mode);
		open = true;
	}
	
	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() throws IOException {
		raf.close();
		open = false;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int len = dst.remaining();
		if (len < 1) return len;
		
		byte[] bytes = new byte[len];
		
		len = raf.read(bytes);
		if (len < 1) return len;
		
		dst.put(bytes, 0, len);
		return len;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		int len = src.remaining();
		if (len < 1) return len;
		byte[] bytes = new byte[len];
		src.get(bytes);
		raf.write(bytes);
		return len;
	}

	@Override
	public long position() throws IOException {
		return raf.getFilePointer();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		raf.seek(newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		return raf.length();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		raf.setLength(size);
		return this;
	}

}
