package eu.openanalytics.phaedra.base.util.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CachingByteRange {

	private List<CacheRange> ranges;
	private DataFetcher dataFetcher;
	private int blockSize;
	
	public CachingByteRange(long size, DataFetcher dataFetcher) {
		this.ranges = new ArrayList<>();
		this.dataFetcher = dataFetcher;
		this.blockSize = 200*1024;
		clear();
	}
	
	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}
	
	public byte[] getBytes(long offset, int size) throws IOException {
		
		byte[] data = new byte[size];
		int pos = 0;
		
		while (pos < size) {
			long globalPos = offset + pos;
			int processedSize = 0;
			
			CacheRange cr = findCR(globalPos);
			if (cr.filled) {
				// This cr contains (some) of the requested data.
				processedSize = Math.min(diffAsInt(cr.end, globalPos) + 1, size - pos);
				System.arraycopy(cr.data, diffAsInt(globalPos, cr.start), data, pos, processedSize);
			} else {
				CacheRange next = findNextCR(cr);
				CacheRange fetched = null;
				if (next == null) {
					// This was the last cr. Fetch all remaining data now.
					processedSize = size - pos;
					fetched = fetch(globalPos, processedSize);
				} else {
					// Fetch (part of) the missing piece between here and the next cr.
					processedSize = Math.min(diffAsInt(next.start, globalPos), Math.max(blockSize, size));
					fetched = fetch(globalPos, processedSize);
					processedSize = Math.min(diffAsInt(next.start, globalPos), size - pos);
				}
				System.arraycopy(fetched.data, diffAsInt(globalPos, fetched.start), data, pos, processedSize);
			}
			
			pos += processedSize;
		}
		
		return data;
	}
	
	public void clear() {
		this.ranges.clear();
		this.ranges.add(new CacheRange(0, null));
	}
	
	private int diffAsInt(long a, long b) {
		return (int) Math.min(a - b, Integer.MAX_VALUE);
	}
	
	private CacheRange findCR(long position) {
		for (CacheRange cr: ranges) {
			if (cr.start <= position && position <= cr.end) return cr;
		}
		return null;
	}
	
	private CacheRange findNextCR(CacheRange cr) {
		for (int i = 0; i < ranges.size(); i++) {
			if (cr == ranges.get(i) && (i + 1) < ranges.size()) return ranges.get(i + 1);
		}
		return null;
	}
	
	private CacheRange fetch(long offset, int size) throws IOException {
		byte[] newData = dataFetcher.fetch(offset, size);
		CacheRange cr = new CacheRange(offset, newData);
		
		int index = 0;
		for (int i = 0; i < ranges.size(); i++) {
			CacheRange next = ranges.get(i);
			if (cr.start < next.start) {
				index = i;
				break;
			} else if (cr.start == next.start) {
				index = i;
				if (cr.end == next.end) {
					ranges.remove(next);
				} else {
					next.start = cr.end + 1;
					if (next.filled) {
						int offsetInData = cr.data.length;
						int remaining = next.data.length - offsetInData;
						System.arraycopy(next.data, offsetInData, next.data, 0, remaining);
					}
				}
				break;
			} else if (cr.start < next.end && !next.filled) {
				// In the middle.
				ranges.add(i, new CacheRange(next.start, cr.start - 1, null));
				index = i + 1;
				next.start = cr.end + 1;
				break;
			}
		}
		
		ranges.add(index, cr);
		return cr;
	}
	
	public static interface DataFetcher {
		public byte[] fetch(long offset, int size) throws IOException;
	}
	
	private static class CacheRange {
		
		public boolean filled;
		public byte[] data;
		public long start;
		public long end;
		
		public CacheRange(long start, byte[] data) {
			this(start, (data == null) ? Long.MAX_VALUE : start + data.length - 1, data);
		}
		
		public CacheRange(long start, long end, byte[] data) {
			this.data = data;
			this.filled = data != null;
			this.start = start;
			this.end = end;
		}
		
		@Override
		public String toString() {
			return String.format("[%d - %d] [data: %b]", start, end, filled);
		}
	}
}
