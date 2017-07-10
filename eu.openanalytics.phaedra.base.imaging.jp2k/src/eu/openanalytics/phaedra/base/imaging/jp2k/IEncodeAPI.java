package eu.openanalytics.phaedra.base.imaging.jp2k;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.ImageData;

public interface IEncodeAPI extends AutoCloseable {
	
	/**
	 * Close any resources that were used by this encoder.
	 */
	public void close();
	
	/**
	 * Compress an image file into a JPEG2000 codestream.
	 * 
	 * @param config The compression parameters.
	 * @param inputFile The image file to compress.
	 * @param outputFile The destination for the JPEG2000 codestream file.
	 * @throws IOException If the compression fails.
	 */
	public void compressCodeStream(CompressionConfig config, String inputFile, String outputFile) throws IOException;
	
	/**
	 * Compress an image file into a JPEG2000 codestream.
	 * 
	 * @param config The compression parameters.
	 * @param inputFile The image data to compress.
	 * @param outputFile The destination for the JPEG2000 codestream file.
	 * @throws IOException If the compression fails.
	 */
	public void compressCodeStream(CompressionConfig config, ImageData data, String outputFile) throws IOException;
	
	/**
	 * Compose multiple codestream files into a single output file.
	 * 
	 * @param inputFiles The codestream files to compose.
	 * @param outputFile The destination file.
	 * @param monitor An optional progress monitor.
	 * @throws IOException If the composition fails.
	 */
	public void composeCodestreamFile(String[] inputFiles, String outputFile, IProgressMonitor monitor) throws IOException;
	
	/**
	 * Generate an updated composed codestream file by replacing one or more codestreams.
	 * 
	 * @param inputFile The existing composed codestream file.
	 * @param codestreamFiles The codestreams to replace.
	 * @param outputFile The destination for the new composed codestream file.
	 * @param monitor An optional progress monitor.
	 * @throws IOException If the composition fails.
	 */
	public void updateCodestreamFile(String inputFile, Map<Integer,String> codestreamFiles, String outputFile, IProgressMonitor monitor) throws IOException;
	public void updateCodestreamFile(InputStream input, Map<Integer,String> codestreamFiles, String outputFile, IProgressMonitor monitor) throws IOException;
}
