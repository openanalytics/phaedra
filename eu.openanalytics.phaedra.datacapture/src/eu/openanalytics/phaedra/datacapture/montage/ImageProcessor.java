package eu.openanalytics.phaedra.datacapture.montage;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.imaging.util.ImageIdentifier;
import eu.openanalytics.phaedra.base.imaging.util.Montage;
import eu.openanalytics.phaedra.base.imaging.util.placeholder.PlaceHolderFactory;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.MTExecutor;
import eu.openanalytics.phaedra.base.util.threading.MTFactory.MTCallable;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig.ImageComponent;
import eu.openanalytics.phaedra.datacapture.montage.layout.FieldLayout;
import eu.openanalytics.phaedra.datacapture.montage.layout.FieldLayoutSourceRegistry;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter.PatternMatch;

/**
 * <p>
 * Using the configuration supplied via init(), this class
 * creates montage images of multiple input images.
 * </p><p>
 * For each well, per component, the input images are collected
 * and montaged. The output image is written to the outputPath directory.
 * </p>
 */
public class ImageProcessor {

	private MontageConfig montageConfig;
	
	private FieldLayout fieldLayout;
	private int[] imageDimensions;
	
	public void init(MontageConfig montageConfig) {
		this.montageConfig = montageConfig;
	}
	
	public void process(PlateReading reading, String outputPath, DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		int componentCount = montageConfig.imageComponents.length;
		fieldLayout = null;
		monitor.beginTask("Creating montage", componentCount);
		
		for (int componentNr=0; componentNr<componentCount; componentNr++) {
			if (monitor.isCanceled()) return;
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
			subMonitor.beginTask("Combining fields for component " + componentNr, 100);
			
			ImageComponent component = montageConfig.imageComponents[componentNr];
			
			File[] files = new File[0];
			String resolvedPath = CaptureUtils.resolvePath(component.path, reading.getSourcePath());
			if (resolvedPath != null && new File(resolvedPath).isDirectory()) {
				files = new File(resolvedPath).listFiles();
			} else {
				context.getLogger().warn(reading, "Image path not found: " + resolvedPath);
			}
			FilePatternInterpreter interpreter = new FilePatternInterpreter(component.pattern, component.patternIdGroups, component.patternFieldGroup);
			
			Set<Integer> uniqueFields = new HashSet<>();
			Set<String> idSet = new HashSet<>();
			Map<String, String> inputFileMap = new HashMap<String, String>();
			
			subMonitor.subTask("Locating image files");
			for (File file: files) {
				if (monitor.isCanceled()) return;
				
				PatternMatch match = interpreter.match(file.getName());
				if (match.isMatch) {
					if (imageDimensions == null) loadImageDimensions(file.getAbsolutePath());
					String value = file.getAbsolutePath();
					if (component.frame != null && NumberUtils.isDigit(component.frame)) value = Montage.appendFrameNr(value, Integer.valueOf(component.frame));
					
					inputFileMap.put(match.id + "#" + match.field, value);
					idSet.add(match.id);
					uniqueFields.add(match.field);
				}
			}
			subMonitor.worked(15);
			
			int fieldCount = uniqueFields.size();
			if (fieldLayout == null) fieldLayout = FieldLayoutSourceRegistry.getInstance().getLayout(reading, fieldCount, montageConfig, context);
			
			SubProgressMonitor wellMonitor = new SubProgressMonitor(subMonitor, 85);
			
			MTExecutor<String> threadPool = createThreadPool();
			for (String wellId: idSet) {
				context.getActiveModule().getConfig().getParameters().setParameter("wellNr", wellId);
				String outputFile = outputPath + "/" + CaptureUtils.resolveVars(component.output, false);
				MTCallable<String> task = new MTCallable<>();
				task.setDelegate(new MontageWellCallable(wellId, fieldCount, inputFileMap, outputFile));
				threadPool.queue(task);
			}
			threadPool.run(wellMonitor);
			
			wellMonitor.done();
			subMonitor.done();
		}
		
		monitor.done();
	}
	
	public Point getImageDimensions() {
		if (imageDimensions == null) return null;
		return new Point(imageDimensions[0], imageDimensions[1]);
	}
	
	private void loadImageDimensions(String imagePath) throws DataCaptureException {
		try {
			imageDimensions = ImageIdentifier.identify(imagePath);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to inspect sample image dimensions", e);
		}
	}
	
	private MTExecutor<String> createThreadPool() {
		MTExecutor<String> executor = new MTExecutor<>();
		executor.init(PrefUtils.getNumberOfThreads());
		return executor;
	}
	
	private class MontageWellCallable implements Callable<String> {

		private String wellId;
		private int fieldCount;
		private Map<String,String> inputFileMap;
		private String outputFile;
		
		public MontageWellCallable(String wellId, int fieldCount, Map<String,String> inputFileMap, String outputFile) {
			this.wellId = wellId;
			this.fieldCount = fieldCount;
			this.inputFileMap = inputFileMap;
			this.outputFile = outputFile;
		}
		
		@Override
		public String call() throws Exception {
			montageWellImages();
			return null;
		}
		
		private void montageWellImages() throws DataCaptureException {
			String[] inputFiles = new String[fieldCount];
			for (int i=0; i<fieldCount; i++) {
				int fieldNr = fieldLayout.getFieldNr(i);
				String key = wellId + "#" + fieldNr;
				String path = inputFileMap.get(key);
				if (path == null) {
					try {
						path = PlaceHolderFactory.getInstance().getPlaceholder(imageDimensions[0], imageDimensions[1], imageDimensions[3], PlaceHolderFactory.MODE_OPAQUE);
					} catch (IOException e) {
						throw new DataCaptureException("Failed to generate placeholder image", e);
					}
				}
				inputFiles[i] = path;
			}
			
			try {
				Montage.montage(inputFiles, (fieldLayout == null) ? null : fieldLayout.getLayoutString(), montageConfig.padding, outputFile);
			} catch (IOException e) {
				throw new DataCaptureException("Failed to montage images", e);
			}
		}
	}
}
