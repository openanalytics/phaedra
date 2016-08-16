package eu.openanalytics.phaedra.datacapture.jp2k;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.w3c.dom.Node;

import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IEncodeAPI;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.IConcurrentExecutor;
import eu.openanalytics.phaedra.base.util.threading.IConcurrentFactory;
import eu.openanalytics.phaedra.base.util.threading.MTFactory;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
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
		monitor.beginTask("Gathering image data", 100);
		if (context.getReadings().length > 0) {
			int progressPerPlate = 100/context.getReadings().length;
			
			for (PlateReading reading: context.getReadings()) {
				if (monitor.isCanceled()) break;
				context.setActiveReading(reading);
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, progressPerPlate);
				compressPlate(reading, context, subMonitor);
				subMonitor.done();
			}
		}
		monitor.done();
	}
	
	@Override
	public int getWeight() {
		return 100;
	}
	
	private void compressPlate(PlateReading reading, DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		
		String msg = "Gathering image data for plate '" + reading.getBarcode() + "' ...";
		monitor.beginTask(msg, 100);
		context.getLogger().info(reading, "Compressing images for " + config.components.length + " channel(s)");
		
		int rows = reading.getRows();
		int cols = reading.getColumns();
		if (rows == 0 || cols == 0) {
			throw new DataCaptureException("Plate layout unknown. Please make sure the welldata is captured as well.");
		}
		
		String workingDir = FileUtils.generateTempFolder(true);
	
		try {
			// Locate input files
			monitor.subTask("Locating input files");
			String[][][] inputFiles = locateInputFiles(reading, context, monitor);
			
			// Run the validator
			runValidator(inputFiles, reading, context);
			
			// Set up Executor
			int nrOfThreads = ((Number)context.getTask().getParameters().get(DataCaptureTask.PARAM_NR_THREADS)).intValue();
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
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 95);
			List<String[]> output = compressPool.run(subMonitor);
			subMonitor.done();
			if (monitor.isCanceled()) {
				return;
			}
			
			String outputFormat = (String)VariableResolver.get(PARAM_CONTAINER_FORMAT);
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
				composer.composeCodestreamFile(files, destination, new SubProgressMonitor(monitor, 5));
			} catch (Exception e) {
				throw new DataCaptureException("Image creation failed", e);
			}
			
			// Move the image file into the capture store.
			IDataCaptureStore store = context.getStore(reading);
			store.addImage(destination, true);
		
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
				String filePath = CaptureUtils.resolvePath(fileCfg.path, sourcePath);
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

	private void runValidator(String[][][] inputFiles, PlateReading reading, DataCaptureContext context) {
		// Execute the validator script.
		DataCaptureTask task = context.getTask();
		try {
			// Get number of Threads.
			task.getParameters().put(DataCaptureTask.PARAM_NR_THREADS, PrefUtils.getNumberOfThreads());
			
			String scriptName = this.getClass().getSimpleName().toLowerCase() + ".validator";
			
			Map<String,Object> params = new HashMap<>();
			params.put("args", params); // Add the map itself, so it is passed to the ScriptCatalog.run method.
			params.put("inputFiles", inputFiles);
			params.put("reading", reading);
			params.put("ctx", context);
			params.put("config", config);
			params.put("task", task);
			
			// Running a script inside a script is a bit silly, but it's the only way to access the ScriptCatalog.
			ScriptService.getInstance().executeScript("scripts.run('dc/" + scriptName + "', args);", params);
		} catch (Exception e) {
			// Ignore exceptions. Validation exceptions are passed via the DataCaptureContext to the validator.
			EclipseLog.error("Image validation failed", e, Activator.getDefault());
		}
	}

}