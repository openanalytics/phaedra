package eu.openanalytics.phaedra.base.util.io;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A collection of utilities for manipulating input- and outputstreams.
 */
public class StreamUtils {

	private final static int DEFAULT_BUFFER_SIZE = 1024*1024;
	
	/**
	 * Copy the contents of an InputStream to an OutputStream without closing the streams.
	 * 
	 * @param in The source InputStream
	 * @param out The destination OutputStream
	 */
	public static void copy(InputStream in, OutputStream out) throws IOException {
		copy(in,out,DEFAULT_BUFFER_SIZE);
	}
	
	/**
	 * Copy the contents of an InputStream to an OutputStream without closing the streams.
	 * 
	 * @param in The source InputStream
	 * @param out The destination OutputStream
	 * @param bufferSize The size of the buffer used during the copy.
	 */
	public static void copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
		copy(in, out, bufferSize, 0, null);
	}
	
	/**
	 * Copy the contents of an InputStream to an OutputStream without closing either
	 * stream. If an IProgressMonitor and an inputSize are provided, progress info
	 * will be sent to the monitor.
	 * 
	 * @param in The source InputStream
	 * @param out The destination OutputStream
	 * @param bufferSize The size of the buffer used during the copy
	 * @param inputSize The size of the InputStream (the number of bytes)
	 * @param monitor The monitor to report progress to (may be null)
	 * @throws IOException If the copy fails due to an I/O error.
	 */
	public static void copy(InputStream in, OutputStream out,
			int bufferSize, long inputSize, IProgressMonitor monitor) throws IOException {
		
		if (monitor != null) {
			int ticks = IProgressMonitor.UNKNOWN;
			if (inputSize > 0) ticks = (int)(inputSize / bufferSize);
			monitor.beginTask("Copying...", ticks);
		}
		
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = in.read(buffer)) > 0) {
			if (monitor != null && monitor.isCanceled()) return;
			out.write(buffer, 0, len);
			if (monitor != null) monitor.worked(1);
		}
		
		if (monitor != null) {
			monitor.done();
		}
	}
	
	/**
	 * Copy the contents of an InputStream to an OutputStream and attempt
	 * to close both streams (whether the copy succeeded or not).
	 * 
	 * @param in The source InputStream
	 * @param out The destination OutputStream
	 */
	public static void copyAndClose(InputStream in, OutputStream out) throws IOException {
		copyAndClose(in, out, 0, null);
	}
	
	public static void copyAndClose(InputStream in, OutputStream out,
			long inputSize, IProgressMonitor monitor) throws IOException {
		
		try {
			copy(in, out, DEFAULT_BUFFER_SIZE, inputSize, monitor);
		} finally {
			try {
				in.close();
			} catch (IOException e) {}
			try {
				out.close();
			} catch (IOException e) {}
		}
	}
	
	/**
	 * Read all available bytes from an InputStream into a byte array.
	 * 
	 * @param in The InputStream to read.
	 * @return A byte array containing all available bytes.
	 * @throws IOException
	 */
	public static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		copyAndClose(in, buffer);
		byte[] bufferBytes = buffer.toByteArray();
		return bufferBytes;
	}
	
	public static byte[] readBlock(InputStream in, int byteCount) throws IOException {
		byte[] buffer = new byte[byteCount];
		int len = in.read(buffer);
		if (len == -1) return null;
		if (len < byteCount) {
			byte[] out = new byte[len];
			System.arraycopy(buffer, 0, out, 0, len);
			buffer = out;
		}
		return buffer;
	}
	
	/**
	 * Read an entire file into a byte array.
	 * 
	 * @param filePath The path to the file.
	 * @return A byte array containing the entire file.
	 * @throws IOException
	 */
	public static byte[] readAll(String filePath) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(filePath);
			return readAll(in);
		} finally {
			if (in != null)	in.close();
		}
	}

	/**
	 * Calculate the CRC32 value for a stream of data.
	 * 
	 * @param input The data to calculate.
	 * @return The calculated CRC32 value.
	 * @throws IOException If the stream cannot be processed.
	 */
	public static long calculateCRC(InputStream input) throws IOException {
		byte[] readBuffer = new byte[4096];
		CRC32 checksumCalculator = new CRC32();
        try {
            int n;
            while ((n = input.read(readBuffer)) > 0) {
                checksumCalculator.update(readBuffer, 0, n);
            }
        } finally {
            if (input != null) input.close();
        }
        return checksumCalculator.getValue();
	}
}
