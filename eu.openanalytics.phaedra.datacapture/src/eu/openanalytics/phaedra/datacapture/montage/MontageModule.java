package eu.openanalytics.phaedra.datacapture.montage;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.imaging.util.ImageIdentifier;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.AbstractModule;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter.PatternMatch;

public class MontageModule extends AbstractModule {

	private MontageConfig montageConfig;
	
	private ImageProcessor imageProcessor;
	private SubwellDataProcessor swDataProcessor;
	
	private Set<String> tempFolders;
	
	@Override
	public String getType() {
		return getClass().getSimpleName();
	}

	@Override
	public void configure(ModuleConfig cfg) throws DataCaptureException {
		super.configure(cfg);
		Node configNode = (Node)getConfig().getParameters().getParameter("config");
		this.montageConfig = new MontageConfigParser().parse(configNode);
		this.tempFolders = new HashSet<>();
		
		imageProcessor = new ImageProcessor();
		imageProcessor.init(montageConfig);
		
		swDataProcessor = new SubwellDataProcessor();
		swDataProcessor.init(montageConfig);
	}
	
	@Override
	public void execute(DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		monitor.beginTask("Creating image montages", 100);
		
		if (context.getReadings().length > 0) {
			int progressPerPlate = 100/context.getReadings().length;
			
			for (PlateReading reading: context.getReadings()) {
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, progressPerPlate);
				context.setActiveReading(reading);
				processReading(reading, context, subMonitor);
				subMonitor.done();
			}
		}
		
		monitor.done();
	}

	@Override
	public void postCapture(DataCaptureContext context, IProgressMonitor monitor) {
		// All modules have been executed: clean up the temp folders.
		if (tempFolders == null) return;
		for (String file: tempFolders) {
			FileUtils.deleteRecursive(new File(file));
		}
	}
	
	@Override
	public int getWeight() {
		return 50;
	}
	
	private void processReading(PlateReading reading, DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		
		String msg = "Create montage for plate '" + reading.getBarcode() + "' ...";
		monitor.beginTask(msg, 100);
		
		if (monitor.isCanceled()) return;
		
		String outputPath = FileUtils.generateTempFolder(true);
		tempFolders.add(outputPath);
		
		Boolean importImageData = (Boolean)getConfig().getParameters().getParameter("importImageData");
		Boolean importSubWellData = (Boolean)getConfig().getParameters().getParameter("importSubWellData");
		
		context.getParameters(reading).setParameter("imagePath.montaged", outputPath);
		context.getParameters(reading).setParameter("subwellDataPath.montaged", outputPath);
		
		if ((importImageData == null || importImageData.booleanValue()) && montageConfig.imageComponents != null) {
			context.getLogger().info(reading, "Creating image montage for " + montageConfig.imageComponents.length + " channel(s)");
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
			imageProcessor.process(reading, outputPath, context, subMonitor);
		} else {
			monitor.worked(50);
		}
		
		if (importSubWellData == null || importSubWellData.booleanValue()) {
			context.getLogger().info(reading, "Recalculating sub-well data for all fields");
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
			Point dims = new Point(0,0);
			AtomicBoolean fullDims = new AtomicBoolean();
			retrieveImageDimensions(reading, dims, fullDims, context);
			swDataProcessor.setImageDimensions(dims, fullDims.get());
			swDataProcessor.process(reading, outputPath, context, subMonitor);
		} else {
			monitor.worked(50);
		}
		
		monitor.done();
	}
	
	private void retrieveImageDimensions(PlateReading reading, Point dimensions, AtomicBoolean fullDimensions, DataCaptureContext context)
			throws DataCaptureException {
		
		// If the image montage was executed, field dimensions are readily available.
		if (imageProcessor.getImageDimensions() != null) {
			dimensions.setLocation(imageProcessor.getImageDimensions());
			fullDimensions.set(false); // These are field dimensions, not full dimensions.
		}
		// Else, pre-montaged images are used (or no images are available at all).
		try {
			CaptureConfig cfg = DataCaptureService.getInstance().getCaptureConfig(context.getTask().getConfigId());
			for (ModuleConfig modCfg: cfg.getModuleConfigs()) {
				//TODO Relies on configuration that is specific to ImageCompressionModule
				if (modCfg.getType().equals("ImageCompressionModule")) {
					Node configNode = (Node)modCfg.getParameters().getParameter("config");
					NodeList fileTags = XmlUtils.findTags("components/component/files", configNode);
					Element fileTag = (Element)fileTags.item(0);
					String path = fileTag.getAttribute("path");
					String pattern = fileTag.getAttribute("pattern");
					String patternIdGroups = fileTag.getAttribute("pattern-id-groups");
					
					String filePath = CaptureUtils.resolvePath(path, reading.getSourcePath());
					File dir = new File(filePath);
					FilePatternInterpreter interpreter = new FilePatternInterpreter(pattern, patternIdGroups, null);
					File[] files = dir.listFiles();
					if (files == null) throw new DataCaptureException("Cannot list contents of folder " + filePath);
					for (File file: files) {
						String name = file.getName();
						PatternMatch match = interpreter.match(name);
						if (match.isMatch) {
							int[] fullDims = ImageIdentifier.identify(file.getAbsolutePath());
							dimensions.setLocation(fullDims[0], fullDims[1]);
							fullDimensions.set(true); // These are full dimensions of a pre-montaged image.
						}
					}
				}
			}
		} catch (IOException e) {
			throw new DataCaptureException("Failed to obtain field image dimensions", e);
		}
	}
}
