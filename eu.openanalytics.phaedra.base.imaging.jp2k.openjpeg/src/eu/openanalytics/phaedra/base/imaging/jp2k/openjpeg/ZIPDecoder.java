package eu.openanalytics.phaedra.base.imaging.jp2k.openjpeg;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.openjpeg.ImagePixels;
import org.openjpeg.JavaByteSource;
import org.openjpeg.OpenJPEGDecoder;

import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TVFS;
import net.java.truevfs.comp.zip.ZipEntry;
import net.java.truevfs.comp.zip.ZipFile;

//TODO Support additionalDiscardLevels
public class ZIPDecoder implements IDecodeAPI {

	private String filePath;
	private RandomAccessFile raf;
	private Lock lock = new ReentrantLock();
	
	private int bgColor = 0xCCCCCC;
	private int nrCodestreamsPerImage;
	private long[] codestreamOffsets;
	private long[] codestreamSizes;

	private OpenJPEGDecoder decoder;
	private JavaByteSource activeCodestream;
	
	public ZIPDecoder(String filePath, int nrCodestreamsPerImage) throws IOException {
		this.filePath = filePath;
		this.nrCodestreamsPerImage = nrCodestreamsPerImage;
		scanZipEntries();
	}

	@Override
	public void open() throws IOException {
		lock.lock();
		if (raf != null) return;
		try {
			raf = new RandomAccessFile(filePath, "r");
			decoder = new OpenJPEGDecoder();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		lock.lock();
		try {
			decoder = null;
			if (raf != null) {
				raf.close();
				raf = null;
			}
		} catch (IOException e) {
			// Ignore
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setBgColor(int bgColor) {
		this.bgColor = bgColor;
	}

	@Override
	public Point getSize(int imageNr) throws IOException {
		lock.lock();
		try {
			openCodestream(imageNr, 0);
			int[] size = decoder.getSize(activeCodestream);
			return new Point(size[0], size[1]);
		} finally {
			closeCodestream();
			lock.unlock();
		}
	}

	@Override
	public int getBitDepth(int imageNr, int component) throws IOException {
		lock.lock();
		try {
			openCodestream(imageNr, component);
			int[] size = decoder.getSize(activeCodestream);
			return size[2];
		} finally {
			closeCodestream();
			lock.unlock();
		}
	}

	@Override
	public ImageData renderImage(int w, int h, int imageNr, int component) throws IOException {
		ImageData data = null;
		
		lock.lock();
		try {
			openCodestream(imageNr, component);
			
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
	public ImageData renderImage(float scale, int imageNr, int component) throws IOException {
		return renderImage(scale, 0, imageNr, component);
	}

	@Override
	public ImageData renderImage(float scale, int additionalDiscardLevels, int imageNr, int component) throws IOException {
		ImageData data = null;
		
		lock.lock();
		try {
			openCodestream(imageNr, component);
			int[] fullSize = decoder.getSize(activeCodestream);
			data = decode(fullSize, scale, null);
		} finally {
			closeCodestream();
			lock.unlock();
		}
		
		return data;
	}

	@Override
	public ImageData renderImageRegion(float scale, Rectangle region, int imageNr, int component) throws IOException {
		return renderImageRegion(scale, 0, region, imageNr, component);
	}

	@Override
	public ImageData renderImageRegion(float scale, int additionalDiscardLevels, Rectangle region, int imageNr, int component) throws IOException {
		ImageData data = null;
		
		lock.lock();
		try {
			openCodestream(imageNr, component);
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
	
	private void scanZipEntries() throws IOException {
		// Scan the ZIP file and calculate codestream offsets.
		try (ZipFile zipFile = new ZipFile(Paths.get(filePath))) {
			Map<Integer, Long> offsetMap = new HashMap<>();
			Map<Integer, Long> sizeMap = new HashMap<>();
			Pattern codestreamPattern = Pattern.compile("codestream_(\\d+)\\.j2c");
			
			int highestCodestreamNr = 0;
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry entry = zipEntries.nextElement();
				if (entry.isDirectory()) continue;
				Matcher matcher = codestreamPattern.matcher(entry.getName());
				if (matcher.matches()) {
					if (entry.getMethod() != ZipEntry.STORED) throw new IOException("Only ZIP files with method STORED (no compression) are supported.");
					int codestreamNr = Integer.parseInt(matcher.group(1));
					if (codestreamNr > highestCodestreamNr) highestCodestreamNr = codestreamNr;
					// getRawOffset: This is the reason we use TrueVFS instead of java.util.zip.
					long offset = 30 + entry.getName().length() + ((long)ReflectionUtils.invoke("getRawOffset", entry));
					offsetMap.put(codestreamNr, offset);
					sizeMap.put(codestreamNr, entry.getSize());
				}
			}
			
			codestreamOffsets = new long[highestCodestreamNr+1];
			codestreamSizes = new long[codestreamOffsets.length];
			for (Integer index: offsetMap.keySet()) {
				codestreamOffsets[index] = offsetMap.get(index);
				codestreamSizes[index] = sizeMap.get(index);
			}
		} finally {
			TVFS.umount(new TFile(filePath));
		}
	}
	
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
	
	private void openCodestream(int imageNr, int componentNr) throws IOException {
		if (activeCodestream != null) closeCodestream();
		int codestreamNr = imageNr*nrCodestreamsPerImage + componentNr;
		if (codestreamNr >= codestreamOffsets.length || codestreamOffsets[codestreamNr] == 0) {
			throw new IOException("Codestream not found: " + codestreamNr);
		}
		long offset = codestreamOffsets[codestreamNr];
		long size = codestreamSizes[codestreamNr];
		activeCodestream = new ZippedCodestreamSource(offset, size);
	}
	
	private void closeCodestream() {
		if (activeCodestream != null) {
			activeCodestream.close();
			activeCodestream = null;
		}
	}
	
	private class ZippedCodestreamSource extends JavaByteSource {

		private long csOffset;
		private long csSize;
		
		public ZippedCodestreamSource(long offset, long size) throws IOException {
			super(SRC_TYPE_J2K);
			this.csOffset = offset;
			this.csSize = size;
			raf.seek(csOffset);
		}
		
		@Override
		public boolean seek(long pos) {
			if (pos >= (csOffset + csSize)) return false;
			try {
				raf.seek(csOffset + pos);
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		
		@Override
		public long skip(long len) {
			long currentPos = getPos();
			long canSkip = Math.min(len, (csOffset + csSize - currentPos));
			long newPos = currentPos + canSkip;
			
			if (newPos == csOffset + csSize) {
				try { raf.seek(csOffset + csSize); } catch (IOException e) {}
				return -1;
			}
			else seek(newPos);
			return canSkip;
		}
		
		@Override
		public long getPos() {
			try {
				return (raf.getFilePointer() - csOffset);
			} catch (IOException e) {
				return -1;
			}
		}
		
		@Override
		public long getSize() {
			return csSize;
		}
		
		@Override
		public int read(int len) {
			byte[] buf = new byte[len];
			try {
				int bytesRead = raf.read(buf);
				addBytesRead(buf, 0, bytesRead);
				return bytesRead;
			} catch (IOException e) {
				return 0;
			}
		}
	}
}
