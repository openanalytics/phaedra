package eu.openanalytics.phaedra.base.imaging.jp2k.openjpeg;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.openjpeg.ImagePixels;
import org.openjpeg.JavaByteSource;
import org.openjpeg.OpenJPEGDecoder;

import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.imaging.jp2k.codestream.CodeStreamAccessor;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

//TODO Support additionalDiscardLevels
public class ZIPDecoder implements IDecodeAPI {

	private Lock lock = new ReentrantLock();
	
	private int bgColor = 0xCCCCCC;

	private OpenJPEGDecoder decoder;
	private JavaByteSource activeCodestream;
	private CodeStreamAccessor activeCodestreamAccessor;

	@Override
	public void open() throws IOException {
		lock.lock();
		if (decoder != null) return;
		try {
			decoder = new OpenJPEGDecoder();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		lock.lock();
		decoder = null;
		lock.unlock();
	}

	@Override
	public void setBgColor(int bgColor) {
		this.bgColor = bgColor;
	}

	@Override
	public Point getSize(CodeStreamAccessor accessor) throws IOException {
		lock.lock();
		try {
			openCodestream(accessor);
			int[] size = decoder.getSize(activeCodestream);
			return new Point(size[0], size[1]);
		} finally {
			closeCodestream();
			lock.unlock();
		}
	}

	@Override
	public int getBitDepth(CodeStreamAccessor accessor) throws IOException {
		lock.lock();
		try {
			openCodestream(accessor);
			int[] size = decoder.getSize(activeCodestream);
			return size[2];
		} finally {
			closeCodestream();
			lock.unlock();
		}
	}

	@Override
	public ImageData renderImage(int w, int h, CodeStreamAccessor accessor) throws IOException {
		ImageData data = null;
		
		lock.lock();
		try {
			openCodestream(accessor);
			
			// Decode while maintaining aspect ratio.
			int[] fullSize = decoder.getSize(activeCodestream);
			float scale = Math.min(((float)w)/fullSize[0], ((float)h)/fullSize[1]);
			ImageData temp = decode(fullSize, scale, null);
			
			// Paste onto the final image, centering if needed.
			data = createImageData(w, h, temp.depth);
			int[] offset = { (w-temp.width)/2, (h-temp.height)/2 };
			for (int y=0; y<temp.height; y++) {
				int[] row = new int[temp.width];
				temp.getPixels(0, y, row.length, row, 0);
				data.setPixels(offset[0], offset[1] + y, row.length, row, 0);
			}
		} finally {
			closeCodestream();
			lock.unlock();
		}
		
		return data;
	}

	@Override
	public ImageData renderImage(float scale, CodeStreamAccessor accessor) throws IOException {
		return renderImage(scale, 0, accessor);
	}

	@Override
	public ImageData renderImage(float scale, int additionalDiscardLevels, CodeStreamAccessor accessor) throws IOException {
		ImageData data = null;
		
		lock.lock();
		try {
			openCodestream(accessor);
			int[] fullSize = decoder.getSize(activeCodestream);
			data = decode(fullSize, scale, null);
		} finally {
			closeCodestream();
			lock.unlock();
		}
		
		return data;
	}

	@Override
	public ImageData renderImageRegion(float scale, Rectangle region, CodeStreamAccessor accessor) throws IOException {
		return renderImageRegion(scale, 0, region, accessor);
	}

	@Override
	public ImageData renderImageRegion(float scale, int additionalDiscardLevels, Rectangle region, CodeStreamAccessor accessor) throws IOException {
		ImageData data = null;
		
		lock.lock();
		try {
			openCodestream(accessor);
			int[] fullSize = decoder.getSize(activeCodestream);
			// If the region falls outside the image, render only the part within the image.
			Rectangle clipped = region.intersection(new Rectangle(0, 0, fullSize[0], fullSize[1]));
			if (clipped.width > 0 && clipped.height > 0) data = decode(fullSize, scale, clipped);
		} finally {
			closeCodestream();
			lock.unlock();
		}
		
		return data;
	}

	/*
	 * Non-public
	 * **********
	 */
	
	private int calculateDiscardLevels(int[] fullSize, float scale) {
		int discard = 0;
		if (scale >= 1.0f) return discard;
		
		// Find the discard levels needed to obtain a scaled image.
		int[] size = { fullSize[0], fullSize[1] };
		while (size[0] > fullSize[0] * scale) {
			size[0] /= 2;
			size[1] /= 2;
			discard++;
		}
		return discard;
	}
	
	private ImageData createImageData(int w, int h, int depth) {
		PaletteData palette = null;
		switch (depth) {
		case 8:
		case 16:
			palette = new PaletteData(0xFF, 0xFF, 0xFF);
			break;
		default:
			palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
		}
		ImageData data = new ImageData(w, h, depth, palette);
		// Fill background
		int[] bgColorLine = new int[w];
		for (int x=0; x<w; x++) bgColorLine[x] = bgColor;
		for (int y=0; y<h; y++) {
			data.setPixels(0, y, w, bgColorLine, 0);
		}
		return data;
	}
	
	private ImageData decode(int[] fullSize, float scale, Rectangle region) {
		int discardLevels = calculateDiscardLevels(fullSize, scale);
		
		int[] regionPoints = (region == null) ? null : SWTUtils.getPoints(region);
		ImagePixels img = decoder.decode(activeCodestream, discardLevels, regionPoints);
		ImageData data = createImageData(img.width, img.height, img.depth);
		data.setPixels(0, 0, img.pixels.length, img.pixels, 0);
		
		// If the requested scale was not a pow2 scale, perform additional scaling.
		int[] expectedSize = { (int)Math.ceil(fullSize[0] * scale), (int)Math.ceil(fullSize[1] * scale) };
		if (region != null) {
			expectedSize[0] = (int)Math.ceil(region.width * scale);
			expectedSize[1] = (int)Math.ceil(region.height * scale);
		}
		if (expectedSize[0] != img.width || expectedSize[1] != img.height) {
			data = data.scaledTo(expectedSize[0], expectedSize[1]);	
		}
		
		return data;
	}
	
	private void openCodestream(CodeStreamAccessor accessor) throws IOException {
		if (activeCodestream != null) closeCodestream();
		activeCodestream = new ZippedCodestreamSource();
		activeCodestreamAccessor = accessor;
	}
	
	private void closeCodestream() {
		if (activeCodestream != null) {
			activeCodestream.close();
			activeCodestream = null;
			activeCodestreamAccessor = null;
		}
	}
	
	private class ZippedCodestreamSource extends JavaByteSource {

		private long pos;
		
		public ZippedCodestreamSource() throws IOException {
			super(SRC_TYPE_J2K);
			this.pos = 0;
		}
		
		@Override
		public boolean seek(long pos) {
			this.pos = pos;
			return true;
		}
		
		@Override
		public long skip(long len) {
			long currentPos = getPos();
			long size = getSize();
			long canSkip = Math.min(len, size - currentPos);
			long newPos = currentPos + canSkip;
			
			seek(newPos);
			if (newPos == size) return -1;
			else return canSkip;
		}
		
		@Override
		public long getPos() {
			return pos;
		}
		
		@Override
		public long getSize() {
			return activeCodestreamAccessor.getSize();
		}
		
		@Override
		public int read(int len) {
			try {
				byte[] data = activeCodestreamAccessor.getBytes(pos, len);
				pos += data.length;
				addBytesRead(data, 0, data.length);
				return data.length;
			} catch (IOException e) {
				return 0;
			}
		}
	}
}
