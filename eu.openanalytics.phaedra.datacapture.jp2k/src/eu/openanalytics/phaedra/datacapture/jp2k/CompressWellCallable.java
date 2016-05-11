package eu.openanalytics.phaedra.datacapture.jp2k;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import eu.openanalytics.phaedra.base.console.ConsoleManager;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;
import eu.openanalytics.phaedra.base.imaging.util.IMConverter;
import eu.openanalytics.phaedra.base.imaging.util.ImageIdentifier;
import eu.openanalytics.phaedra.base.imaging.util.placeholder.PlaceHolderFactory;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.datacapture.jp2k.config.ComponentConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.CompressionConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.Config;

public class CompressWellCallable implements Callable<String[]>, Serializable {

	private static final long serialVersionUID = 1698296773402562223L;
	private int row;
	private int column;
	private Config config;
	private String[][][] inputFiles;
	private String workingDir;
	
	private List<String> filesToDelete;
	
	public CompressWellCallable(int row, int column, Config config,
			String[][][] inputFiles, String workingDir) {
	
		this.row = row;
		this.column = column;
		this.config = config;
		this.inputFiles = inputFiles;
		this.workingDir = workingDir;
	}
	
	@Override
	public String[] call() throws Exception {

		filesToDelete = new ArrayList<String>();
		
		List<String> filesToCompress = new ArrayList<>();
		List<Integer> componentIndicesPerFile = new ArrayList<>();
		
		int[] expectedDimensions = null;
		
		for (ComponentConfig component: config.components) {
			if (Thread.interrupted()) return null;
			
			String inputFile = inputFiles[row][column][component.id];
			if (inputFile == null) {
				// Generate placeholder(s) based on config.
				String[] phs = generatePlaceholders(component);
				for (String ph: phs) {
					filesToCompress.add(ph);
					componentIndicesPerFile.add(component.id);
				}
			} else {
				// Before proceeding, verify the image dimensions match.
				int[] dimensions = ImageIdentifier.identify(inputFile);
				if (expectedDimensions == null) {
					expectedDimensions = dimensions;
				} else {
					if (expectedDimensions[0] != dimensions[0] || expectedDimensions[1] != dimensions[1]) {
						// The image dimensions differ from the expected dimensions.
						String well = NumberUtils.getWellCoordinate(row+1, column+1);
						ConsoleManager.getInstance().print(
								"WARNING: Well " + well + " has unexpected image size: " + dimensions[0] + "x" + dimensions[1] 
										+ " at channel " + component.id + ". Skipping image.");
						// Use a placeholder instead.
						String[] phs = generatePlaceholders(component);
						for (String ph: phs) {
							filesToCompress.add(ph);
							componentIndicesPerFile.add(component.id);
						}
						continue;
					}
				}
				
				// Build a list of files to compress.
				// Usually (without splitting), 1 component gives 1 file.
				String[] processedFiles = new String[]{inputFile};
				processedFiles = applyConvert(component, processedFiles);
				processedFiles = checkName(component, processedFiles);
				
				for (String file: processedFiles) {
					filesToCompress.add(file);
					componentIndicesPerFile.add(component.id);
				}
			}
		}
		
		List<String> codestreamFiles = new ArrayList<>();
		
		// Compress all input files into codestream files.
		try (IEncodeAPI compressor = CodecFactory.getEncoder()) {
			for (int i=0; i<filesToCompress.size(); i++) {
				String inputFile = filesToCompress.get(i);
				String outputFile = workingDir + "/" + UUID.randomUUID().toString() + ".j2c";
				int component = componentIndicesPerFile.get(i);
				compressor.compressCodeStream(createCompressionConfig(config, component), inputFile, outputFile);
				codestreamFiles.add(outputFile);
			}
		}
		
		// Clean up generated temporary files.
		for (String fileToDelete: filesToDelete) {
			FileUtils.deleteRecursive(new File(fileToDelete));
		}
		
		return codestreamFiles.toArray(new String[codestreamFiles.size()]);
	}

	private String[] applyConvert(ComponentConfig component, String[] files) throws IOException {
		if (component.convert) {
			for (int i=0; i<files.length; i++) {
				String convertArgs = component.convertArgs;
				String inputPath = files[i] + (component.convertFrame == null ? "" : "[" + component.convertFrame + "]");
				String outputPath = workingDir + "/r" + row + "-c" + column + "-comp" + component.id + "-ch" + i + "-converted.tif";
				try {
					IMConverter.convert(inputPath, convertArgs, outputPath);
				} catch (Exception e) {
					if (component.convertArgsOnFail != null) {
						convertArgs = component.convertArgsOnFail;
						IMConverter.convert(inputPath, convertArgs, outputPath);
					} else {
						throw e;
					}
				}
				files[i] = outputPath;
				filesToDelete.add(outputPath);
			}
		}
		return files;
	}
	
	private String[] checkName(ComponentConfig component, String[] files) throws IOException {
		for (int i=0; i<files.length; i++) {
			if (files[i] != null && files[i].contains(",")) {
				// Workaround: Kakadu compress.exe cannot handle commas in file names.
				String outputPath = workingDir + "/r" + row + "-c" + column + "-comp" + component.id + "-ch" + i + "-copy.tif";
				FileUtils.copy(files[i], outputPath);
				files[i] = outputPath;
				filesToDelete.add(outputPath);
			}
		}
		return files;
	}
	
	private String[] generatePlaceholders(ComponentConfig component) throws IOException {
		String[] files = new String[]{ generatePlaceholder(component) };
		return files;
	}
	
	private String generatePlaceholder(ComponentConfig component) throws IOException {
		
		// Get size from another component
		int x = 500;
		int y = 500;
		
		int compIndex = 0;
		String sample = null;
		while (sample == null && compIndex < inputFiles[row][column].length) {
			sample = inputFiles[row][column][compIndex];
			compIndex++;
		}
		if (sample == null) {
			// All components are null. Size doesn't matter.
		} else {
			int[] sampleSize = ImageIdentifier.identify(sample);
			x = sampleSize[0];
			y = sampleSize[1];
		}
		
		// Get bpp from another well
		int bpp = 8;
		int r = 0; int c = 0;
		sample = null;
		while (sample == null && r < inputFiles.length && c < inputFiles[r].length) {
			sample = inputFiles[r++][c][component.id];
			if (r == inputFiles.length) {
				r = 0;
				c++;
			}
		}
		if (sample == null) {
			// This component is missing from all wells. Bpp doesn't matter.
		} else {
			int[] sampleSize = ImageIdentifier.identify(sample);
			bpp = sampleSize[3];
			if (bpp == 0) throw new RuntimeException("Failed to identify image depth of " + sample);
		}
		
		String type = "";
		if (config.defaultCompression != null && config.defaultCompression.type != null) type = config.defaultCompression.type;
		if (component.compression != null && component.compression.type != null) type = component.compression.type;
		boolean isOverlay = type.equalsIgnoreCase("r53");
		int mode = (isOverlay) ? PlaceHolderFactory.MODE_OVERLAY : PlaceHolderFactory.MODE_OPAQUE;
		
		return PlaceHolderFactory.getInstance().getPlaceholder(x, y, bpp, mode);
	}
	
	private eu.openanalytics.phaedra.base.imaging.jp2k.CompressionConfig createCompressionConfig(Config config, int component) {
		
		eu.openanalytics.phaedra.base.imaging.jp2k.CompressionConfig output = new eu.openanalytics.phaedra.base.imaging.jp2k.CompressionConfig();
		
		CompressionConfig defaults = config.defaultCompression;
		CompressionConfig settings = config.components[component].compression;

		// Type (reversible or not)
		if (settings != null && settings.type != null) output.reversible = settings.type.equalsIgnoreCase("r53");
		else if (defaults != null && defaults.type != null) output.reversible = defaults.type.equalsIgnoreCase("r53");
		
		// Resolution Levels
		if (settings != null && settings.levels != 0) output.resolutionLevels = settings.levels;
		else if (defaults != null && defaults.levels != 0) output.resolutionLevels = defaults.levels;

		// Order
		if (settings != null && settings.order != null) output.order = settings.order;
		else if (defaults != null && defaults.order != null) output.order = defaults.order;

		// Slope
		if (settings != null && settings.slope != 0) output.slope = settings.slope;
		else if (defaults != null && defaults.slope != 0) output.slope = defaults.slope;

		// Precincts
		if (settings != null && settings.precincts != null) output.precincts = settings.precincts;
		else if (defaults != null && defaults.precincts != null) output.precincts = defaults.precincts;

		return output;
	}
}