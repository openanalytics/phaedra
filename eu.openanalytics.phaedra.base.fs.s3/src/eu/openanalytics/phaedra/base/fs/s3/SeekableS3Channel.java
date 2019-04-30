package eu.openanalytics.phaedra.base.fs.s3;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * Implements a SeekableByteChannel on top of a S3 object.
 * Every read() will trigger an HTTP call, so performance-wise it is important
 * to perform sufficiently large chunk reads and implement caching on a higher level.
 */
public class SeekableS3Channel implements SeekableByteChannel, Closeable {

	private AmazonS3 s3;
	private String bucketName;
	private String key;

	private long length;
	
	private boolean open;
	private long pos;
	
	public SeekableS3Channel(AmazonS3 s3, String bucketName, String key) throws IOException {
		this.s3 = s3;
		this.bucketName = bucketName;
		this.key = key;
		
		this.length = s3.getObjectMetadata(bucketName, key).getContentLength();
		this.open = true;
		this.pos = -1;
	}
	
	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() throws IOException {
		open = false;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int requestedRead = dst.remaining();
		if (requestedRead < 1) return requestedRead;
		
		byte[] bytes = new byte[requestedRead];
		int totalRead = 0;
		
		GetObjectRequest req = new GetObjectRequest(bucketName, key);
		req.setRange(pos, pos + requestedRead - 1);
		try (
			S3Object o = s3.getObject(req);
			S3ObjectInputStream input = o.getObjectContent();
		) {
			while (totalRead < requestedRead) {
				int read = input.read(bytes, totalRead, bytes.length - totalRead);
				if (read == -1) break;
				totalRead += read;
			}
		}
		
		dst.put(bytes, 0, totalRead);
		pos += totalRead;
		return totalRead;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new IOException("S3 writes are not supported");
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
		return length;
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new IOException("S3 writes are not supported");
	}
}
