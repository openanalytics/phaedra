package eu.openanalytics.phaedra.base.imaging.jp2k.codestream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * Offers access to bytes of a JP2K codestream, assuming that the codestream
 * is embedded in a byte source containing multiple codestreams (e.g. a ZIP file).
 * This accessor makes efforts to cache and buffer bytes efficiently for renderings
 * of various resolution.
 */
public class CodeStreamAccessor {

	@IgnoreSizeOf
	private ICodeStreamByteSource byteSource;
	
	private long csOffset;
	private int csSize;
	
	private int blockSize;
	private int blockCount;
	private boolean[] blockCached;
	private byte[] data;
	
	public void init(ICodeStreamByteSource byteSource, long csOffset, int csSize, float blockSizeFraction) {
		this.byteSource = byteSource;
		this.csOffset = csOffset;
		this.csSize = csSize;
		
		this.blockSize = (int) (csSize * blockSizeFraction);
		if (csSize < 100000) blockSize = csSize;
		
		this.blockCount = (int) (csSize / blockSize);
		if (csSize % blockSize > 0) this.blockCount++;
		
		this.blockCached = new boolean[blockCount];
		this.data = new byte[(int) csSize];
	}
	
	public long getSize() {
		return csSize;
	}
	
	public byte[] getBytes(long offset, int range) throws IOException {
		range = Math.min(range, (int) (data.length - offset));
		ensureCached(offset, range);
		byte[] retVal = new byte[range];
		System.arraycopy(data, (int) offset, retVal, 0, retVal.length);
		return retVal;
	}

	private synchronized void ensureCached(long offset, int range) throws IOException {
		int startBlock = (int) (offset / blockSize);
		int endBlock = (int) ((offset + range) / blockSize);
		if ((offset + range) % blockSize == 0) endBlock--;
		
		List<int[]> rangesToFetch = new ArrayList<>();
		int[] currentRange = null;
		
		for (int i = startBlock; i <= endBlock; i++) {
			if (blockCached[i]) {
				if (currentRange == null) {
					continue;
				} else {
					rangesToFetch.add(currentRange);
					currentRange = null;
				}
			} else {
				int thisBlockSize = blockSize;
				if (i == blockCount - 1) thisBlockSize = csSize - (blockCount * blockSize);
				if (thisBlockSize == 0) thisBlockSize = blockSize;
				if (thisBlockSize < 0) thisBlockSize += blockSize;
				
				if (currentRange == null) {
					currentRange = new int[] { i * blockSize , thisBlockSize };
				} else {
					currentRange[1] += thisBlockSize;
				}
			}
		}
		if (currentRange != null) rangesToFetch.add(currentRange);
		
		if (!rangesToFetch.isEmpty()) {
			for (int i = startBlock; i <= endBlock; i++) blockCached[i] = true;
		}
		
		for (int[] r: rangesToFetch) {
			EclipseLog.debug(String.format("Fetching byte range: %d - %d (blocks: %d - %d, block size: %d, cs size: %d) (%s)",
					r[0], r[0] + r[1], startBlock, endBlock, blockSize, csSize, Thread.currentThread().getName()),
					CodeStreamAccessor.class);
			ByteBuffer dst = ByteBuffer.wrap(data, r[0], r[1]);
			byteSource.get(csOffset + r[0], r[1], dst);
		}
	}
}
