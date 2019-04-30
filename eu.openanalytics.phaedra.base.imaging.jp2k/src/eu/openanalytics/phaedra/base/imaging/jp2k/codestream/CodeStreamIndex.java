package eu.openanalytics.phaedra.base.imaging.jp2k.codestream;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;
import net.java.truevfs.comp.zip.ZipEntry;
import net.java.truevfs.comp.zip.ZipFile;

public class CodeStreamIndex {

	private int codeStreamCount;
	private long[] offsets;
	private long[] sizes;
	
	public void parse(SeekableByteChannel channel) throws IOException {
		@SuppressWarnings("resource") // No the channel is not closed, it's going to be reused later on!
		ZipFile zipFile = new ZipFile(channel);
		
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
				// get(Raw)Offset: This is the reason we use TrueVFS instead of java.util.zip.
				long offset = ((long)ReflectionUtils.invoke("getOffset", entry));
				offset += 30L + (long)(entry.getName().length());
				offsetMap.put(codestreamNr, offset);
				sizeMap.put(codestreamNr, entry.getSize());
			}
		}
		
		codeStreamCount = highestCodestreamNr + 1;
		offsets = new long[codeStreamCount];
		sizes = new long[codeStreamCount];
		for (Integer index: offsetMap.keySet()) {
			offsets[index] = offsetMap.get(index);
			sizes[index] = sizeMap.get(index);
		}
		
		channel.position(0);
	}
	
	public int getCodeStreamCount() {
		return codeStreamCount;
	}
	
	public long getOffset(int codestream) {
		if (codestream >= codeStreamCount) return -1;
		return offsets[codestream];
	}
	
	public long getSize(int codestream) {
		if (codestream >= codeStreamCount) return -1;
		return sizes[codestream];
	}
}
