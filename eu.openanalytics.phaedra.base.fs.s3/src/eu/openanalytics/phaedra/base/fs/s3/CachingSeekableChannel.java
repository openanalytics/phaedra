package eu.openanalytics.phaedra.base.fs.s3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class CachingSeekableChannel implements SeekableByteChannel {

	private SeekableByteChannel delegate;
	private CachingByteRange cachedRange;
	private long pos;
	
	public CachingSeekableChannel(SeekableByteChannel delegate) {
		this.delegate = delegate;
		try {
			this.cachedRange = new CachingByteRange(size(), (o,l) -> {
//				System.out.println(String.format("Fetching: %d bytes at position %d", l, o));
				byte[] data = new byte[l];
				if (delegate instanceof SeekableS3Channel) ((SeekableS3Channel) delegate).position(o, l);
				else delegate.position(o);
				delegate.read(ByteBuffer.wrap(data));
				return data;
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public void close() throws IOException {
		cachedRange.clear();
		delegate.close();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (pos >= size()) return -1;
		
		long bytesLeftInChannel = Math.min(size() - pos, Integer.MAX_VALUE);
		int requestedRead = Math.min(dst.remaining(), (int) bytesLeftInChannel);
		
		byte[] data = cachedRange.getBytes(pos, requestedRead);
		pos += data.length;
		
		dst.put(data);
		return data.length;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new IOException("This channel does not support writes");
	}

	@Override
	public long position() throws IOException {
		return pos;
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		pos = newPosition;
		return this;
	}

	@Override
	public long size() throws IOException {
		return delegate.size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new IOException("This channel does not support writes");
	}
}
