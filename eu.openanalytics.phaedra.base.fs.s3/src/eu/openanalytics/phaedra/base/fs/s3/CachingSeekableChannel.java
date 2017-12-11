package eu.openanalytics.phaedra.base.fs.s3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class CachingSeekableChannel implements SeekableByteChannel {

	private SeekableByteChannel delegate;

	private byte[] cache;
	private long cachePos;
	private long pos;
	private int defaultCacheSize;
	
	public CachingSeekableChannel(SeekableByteChannel delegate) {
		this.delegate = delegate;
		this.defaultCacheSize = 2 * 1024 * 1024;
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (cache == null) updateCache(defaultCacheSize);
		
		int reqRead = Math.min(dst.remaining(), (int) (size() - pos));
		int cacheLeft = Math.max(0, (int) ((cachePos + cache.length) - pos));
		if (pos < cachePos) cacheLeft = 0;
		
		if (pos == size()) {
			return -1;
		} else if (reqRead <= cacheLeft) {
			// Cache is sufficient, return cached bytes
			int offsetInCache = (int) (pos - cachePos);
			dst.put(cache, offsetInCache, reqRead);
			pos += reqRead;
			return reqRead;
		} else {
			// Cache is insufficient, perform more reading
			int missing = reqRead - cacheLeft;
			updateCache(missing);
			return read(dst);
		}
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
		if (pos < cachePos) updateCache(defaultCacheSize);
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
	
	private void updateCache(int minSize) throws IOException {
		// Check current pos, make sure 'enough' bytes are cached
		int cacheSize = Math.max(minSize, defaultCacheSize);
		int bytesLeft = (int) (size() - pos);
		cacheSize = Math.min(bytesLeft, cacheSize);
		
		// Optimization: initial cache shouldn't be too big.
		// E.g. for ZIP files, the reader will immediately go to the TOC (at the end of the file) anyway.
		if (pos == 0) cacheSize = Math.min(cacheSize, 1024);
		
		cache = new byte[cacheSize];
		cachePos = pos;
		System.out.println(String.format("Filling cache with %d bytes at offset %d", cacheSize, cachePos));
		delegate.position(cachePos);
		
		ByteBuffer bb = ByteBuffer.wrap(cache);
		int totalBytesRead = 0;
		while (totalBytesRead < cacheSize) {
			totalBytesRead += delegate.read(bb);
		}
	}
}
