package eu.openanalytics.phaedra.datacapture.jp2k;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.w3c.dom.Node;

import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.IConcurrentExecutor;
import eu.openanalytics.phaedra.base.util.threading.IConcurrentFactory;
import eu.openanalytics.phaedra.base.util.threading.MTFactory;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.ComponentConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.ComponentFileConfig;
import eu.openanalytics.phaedra.datacapture.jp2k.config.Config;
import eu.openanalytics.phaedra.datacapture.jp2k.config.parser.ConfigParser;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.AbstractModule;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter.PatternMatch;
import eu.openanalytics.phaedra.datacapture.util.VariableResolver;

public class ImageCompressionModule extends AbstractModule {

	public static final String TYPE = "ImageCompressionModule";
	public static final String PARAM_CONTAINER_FORMAT = "image.container.format";
	
	private Config config;
	private IConcurrentFactory<String[]> factory;
	
	public ImageCompressionModule() {
		super();
		factory = new MTFactory<>();
	}
	
	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void configure(ModuleConfig cfg) throws DataCaptureException {
		super.configure(cfg);
		Node configNode = (Node)getConfig().getParameters().getParameter("config");
		this.config = new ConfigParser().parse(configNode);
	}

	@Override
	public void execute(DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		SubMonitor mon = SubMonitor.convert(monitor);
		mon.beginTask("Gathering image data", 100);
		
		if (context.getReadings().length > 0) {
			int progressPerPlate = 100/context.getReadings().length;
			
			for (PlateReading reading: context.getReadings()) {
				if (mon.isCanceled()) break;
				context.setActiveReading(reading);
				compressPlate(reading, context, mon.split(progressPerPlate));
			}
		}
	}
	
	@Override
	public int getWeight() {
		return 100;
	}
	
	private void compressPlate(PlateReading reading, DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		SubMonitor mon = SubMonitor.convert(monitor);
		mon.beginTask("Gathering image data for plate '" + reading.getBarcode() + "' ...", 100);
		context.getLogger().info(reading, "Compressing images for " + config.components.length + " channel(s)");
		
		int rows = reading.getRows();
		int cols = reading.getColumns();
		if (rows == 0 || cols == 0) {
			throw new DataCaptureException("Plate layout unknown. Please make sure the welldata is captured as well.");
		}
		
		String workingDir = FileUtils.generateTempFolder(true);
	
		try {
			// Locate input files
			mon.subTask("Locating input files");
			String[][][] inputFiles = locateInputFiles(reading, context, mon.split(5));
			
			// Run the validator
			validateInput(inputFiles, reading, context);
			
			// Set up Executor
			int nrOfThreads = PrefUtils.getNumberOfThreads();
			IConcurrentExecutor<String[]> compressPool = factory.createExecutor();
			compressPool.init(nrOfThreads);
			
			// Submit compression job for each well.
			for (int row = 0; row<rows; row++) {
				for (int col = 0; col<cols; col++) {
					CompressWellCallable job = new CompressWellCallable(
							row, col, config, inputFiles, workingDir);
					
					compressPool.queue(factory.createCallable(job, compressPool));
				}
			}
			
			// Wait until done.
			List<String[]> output = compressPool.run(mon.split(90));
			if (mon.isCanceled()) return;
			
			String outputFormat = (String)VariableResolver.get(PARAM_CONTAINER_FORMAT, context);
			if (outputFormat == null) outputFormat = CodecFactory.FORMAT_ZIP;
			
			// Create a compressed image file.
			String destination = workingDir + "/plate." + outputFormat;
			List<String> fileNames = new ArrayList<String>();
			for (String[] files: output) {
				for (String file: files) {
					fileNames.add(file);
				}
			}
			
			try (IEncodeAPI composer = CodecFactory.getEncoder()) {
				String[] files = fileNames.toArray(new String[fileNames.size()]);
				composer.composeCodestreamFile(files, destination, mon.split(5));
			} catch (Exception e) {
				throw new DataCaptureException("Image creation failed", e);
			}
			
			// Move the image file into the capture store.
			IDataCaptureStore store = context.getStore(reading);
			store.saveImage(destination);
		
			monitor.done();
		} finally {
			// Clean up temporary objects.
			FileUtils.deleteRecursive(new File(workingDir));	
		}
	}
	
	private String[][][] locateInputFiles(PlateReading reading, DataCaptureContext context, IProgressMonitor monitor) {
		
		int rows = reading.getRows();
		int cols = reading.getColumns();
		String sourcePath = reading.getSourcePath();
		
		String[][][] inputFiles = new String[rows][cols][config.components.length];
		
		for (ComponentConfig component: config.components) {
			Map<String, String> inputFileMap = new HashMap<String, String>();
			
			for (ComponentFileConfig fileCfg: component.files) {
				String filePath = CaptureUtils.resolvePath(fileCfg.path, sourcePath, context);
				File dir = new File(filePath);
				if (!dir.exists()) {
					context.getLogger().warn(reading, "Image path not found: " + filePath);
					continue;
				}
				
				FilePatternInterpreter interpreter = new FilePatternInterpreter(fileCfg.pattern, fileCfg.patternIdGroups, null);
				File[] files = dir.listFiles();
				for (File file: files) {
					String name = file.getName();
					PatternMatch match = interpreter.match(name);
					if (match.isMatch) inputFileMap.put(match.id, file.getAbsolutePath());
				}
			}
			
			for (String id: inputFileMap.keySet()) {
				int wellNr = 0;
				try {
					// Is it numeric? E.g. "25"
					wellNr = Integer.parseInt(id);
				} catch (NumberFormatException e) {
					// Is it a position? E.g. "B2"
					wellNr = NumberUtils.getWellNr(id, cols);
				}
				if (wellNr > 0) {
					int[] pos = NumberUtils.getWellPosition(wellNr, cols);
					if (inputFiles.length < pos[0] || inputFiles[pos[0]-1].length < pos[1]) {
						context.getLogger().warn(reading, "Skipping image: outside plate dimensions (" + rows + "x" + cols + "): " + inputFileMap.get(id));
					} else {
						inputFiles[pos[0]-1][pos[1]-1][component.id] = inputFileMap.get(id);
					}
				}
			}
		}
		
		return inputFiles;
	}

	private void validateInput(String[][][] inputFiles, PlateReading reading, DataCaptureContext context) {
		int rows = inputFiles.length;
		int columns = rows > 0 ? inputFiles[0].length : 0;
		int components = columns > 0 ? inputFiles[0][0].length : 0;
		
		for (int c=0; c<components; c++) {
			// Test for missing files
			int missingCount = 0;
			for (int x=0; x<inputFiles.length; x++) {
				for (int y=0; y<inputFiles[x].length; y++) {
					if (inputFiles[x][y][c] == null) missingCount++;
				}
			}
			
			if (missingCount > 0) {
				int expectedCount = rows * columns;
				ComponentFileConfig cfg = null;
				if (config.components[c].files.length > 0) cfg = config.components[c].files[0];
				String msg = "Image component " + c + " : expected " + expectedCount + " images, found "
						+ (expectedCount - missingCount) + " images.\n Path: " + cfg.path + ", pattern: " + cfg.pattern;
				context.getLogger().warn(reading, msg);
			}
		}
		
		//TODO The old validator also adjusted threadpool size according to image size and RAM
	}
}