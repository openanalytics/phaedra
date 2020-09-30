package eu.openanalytics.phaedra.datacapture.montage;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

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
		SubMonitor mon = SubMonitor.convert(monitor);
		int componentCount = montageConfig.imageComponents.length;
		mon.beginTask("Creating montage", componentCount*100);
		fieldLayout = null;
		
		for (int componentNr=0; componentNr<componentCount; componentNr++) {
			if (mon.isCanceled()) return;
			
			ImageComponent component = montageConfig.imageComponents[componentNr];
			
			File[] files = new File[0];
			String resolvedPath = CaptureUtils.resolvePath(component.path, reading.getSourcePath(), context);
			if (resolvedPath != null && new File(resolvedPath).isDirectory()) {
				files = new File(resolvedPath).listFiles();
			} else {
				context.getLogger().warn(reading, "Image path not found: " + resolvedPath);
			}
			String resolvedPattern = CaptureUtils.resolveVars(component.pattern, true, context);
			FilePatternInterpreter interpreter = new FilePatternInterpreter(resolvedPattern, component.patternIdGroups, component.patternFieldGroup);
			
			Set<Integer> uniqueFields = new HashSet<>();
			Set<String> idSet = new HashSet<>();
			Map<String, String> inputFileMap = new HashMap<String, String>();
			
			mon.subTask("Locating image files");
			for (File file: files) {
				if (mon.isCanceled()) return;
				
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
			mon.worked(5);
			
			int fieldCount = uniqueFields.stream().mapToInt(i -> i).max().orElse(0);
			if (uniqueFields.contains(Integer.valueOf(0))) fieldCount++;
			
			if (fieldLayout == null) fieldLayout = FieldLayoutSourceRegistry.getInstance().getLayout(reading, fieldCount, montageConfig, context);
			
			MTExecutor<String> threadPool = createThreadPool();
			for (String wellId: idSet) {
				String outputFile = outputPath + "/" + CaptureUtils.resolveVars(component.output, false, context,
						Stream.of(new SimpleEntry<>("wellNr", wellId)).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));
				MTCallable<String> task = new MTCallable<>();
				task.setDelegate(new MontageWellCallable(wellId, fieldCount, inputFileMap, outputFile, component.overlay));
				threadPool.queue(task);
			}
			threadPool.run(mon.split(95));
		}
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
		private boolean isOverlay;
		
		public MontageWellCallable(String wellId, int fieldCount, Map<String,String> inputFileMap, String outputFile, boolean isOverlay) {
			this.wellId = wellId;
			this.fieldCount = fieldCount;
			this.inputFileMap = inputFileMap;
			this.outputFile = outputFile;
			this.isOverlay = isOverlay;
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
						path = PlaceHolderFactory.getInstance().getPlaceholder(
								imageDimensions[0], imageDimensions[1], imageDimensions[3], 
								isOverlay ? PlaceHolderFactory.MODE_OVERLAY : PlaceHolderFactory.MODE_OPAQUE);
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
