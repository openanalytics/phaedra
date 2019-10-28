package eu.openanalytics.phaedra.datacapture.module;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.base.imaging.util.TIFFCodec;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter.PatternMatch;

/**
 * For an input image set consisting of multiple planes, group the input images
 * and merge them so that the result is single-plane.
 * 
 * Currently, the only supported method is merging by Maximum Intensity Projection (MIP).
 */
public class MergePlanesModule extends AbstractModule {

	private Set<String> tempFolders;
	
	@Override
	public String getType() {
		return getClass().getSimpleName();
	}

	@Override
	public void configure(ModuleConfig cfg) throws DataCaptureException {
		super.configure(cfg);
		tempFolders = new HashSet<>();
		
		String method = (String) getConfig().getParameters().getParameter("method");
		if (!"mip".equalsIgnoreCase(method)) throw new DataCaptureException("Invalid merge method: " + method);
	}
	
	@Override
	public void execute(DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		SubMonitor mon = SubMonitor.convert(monitor, "Merge Image Planes", 100);
		if (context.getReadings().length > 0) {
			int progressPerPlate = 100/context.getReadings().length;
			for (PlateReading reading: context.getReadings()) {
				context.setActiveReading(reading);
				processReading(reading, context, mon.split(progressPerPlate));
			}
		}
	}

	@Override
	public void postCapture(DataCaptureContext context, IProgressMonitor monitor) {
		if (tempFolders == null) return;
		for (String file: tempFolders) FileUtils.deleteRecursive(new File(file));
	}
	
	private void processReading(PlateReading reading, DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		SubMonitor mon = SubMonitor.convert(monitor);
		mon.beginTask("Merging planes for plate '" + reading.getBarcode() + "' ...", 100);
		if (mon.isCanceled()) return;
		
		try {
			String outputPath = FileUtils.generateTempFolder(true);
			tempFolders.add(outputPath);
			context.getParameters(reading).setParameter("image.path.merged", outputPath);
			Map<String, List<PatternMatch>> groups = locateInputFiles(reading);
			context.getLogger().info(reading, String.format("Merging planes for %d groups", groups.size()));
			groups.entrySet().parallelStream().forEach(entry -> {
				if (mon.isCanceled()) return;
				mergePlanes(reading.getSourcePath(), entry.getKey(), entry.getValue(), outputPath);	
			});
		} catch (IOException e) {
			throw new DataCaptureException("Failed to merge planes for plate " + reading.getBarcode(), e);
		}
	}
	
	private Map<String, List<PatternMatch>> locateInputFiles(PlateReading reading) throws IOException {
		String filePattern = (String) getConfig().getParameters().getParameter("file.pattern");
		String idGroups = (String) getConfig().getParameters().getParameter("file.pattern.id.groups");
		String fieldGroup = (String) getConfig().getParameters().getParameter("file.pattern.field.group");
		String channelGroup = (String) getConfig().getParameters().getParameter("file.pattern.channel.group");
		FilePatternInterpreter interpreter = new FilePatternInterpreter(filePattern, idGroups, fieldGroup);

		Map<String, List<PatternMatch>> groups = Files.list(Paths.get(reading.getSourcePath()))
			.map(p -> interpreter.match(p.getFileName().toString()))
			.filter(pm -> pm.isMatch)
			.collect(Collectors.groupingBy(pm -> pm.id + "_" + pm.field + "_" + pm.matcher.group(Integer.parseInt(channelGroup))));
		return groups;
	}
	
	private void mergePlanes(String inputPath, String groupName, List<PatternMatch> group, String outputPath) {
		try {
			ImageData output = null;
			for (PatternMatch match: group) {
				String inputFull = inputPath + "/" + match.matcher.group();
				ImageData[] pages = TIFFCodec.read(inputFull);
				if (pages == null || pages.length == 0) continue;
				if (output == null) output = pages[0];
				else applyMIP(pages[0], output);
			}
			if (output == null) {
				EclipseLog.debug("Skipping empty group: " + groupName, MergePlanesModule.class);
			} else {
				String destination = outputPath + "/" + groupName + ".tif";
				TIFFCodec.write(output, destination);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to merge planes for group " + groupName, e);
		}
	}
	
	private void applyMIP(ImageData input, ImageData output) {
		int[] inLine = new int[input.width];
		int[] outLine = new int[output.width];
		for (int y = 0; y < output.height; y++) {
			input.getPixels(0, y, inLine.length, inLine, 0);
			output.getPixels(0, y, outLine.length, outLine, 0);
			for (int x = 0; x < outLine.length; x++) {
				outLine[x] = Math.max(inLine[x], outLine[x]);
			}
			output.setPixels(0, y, outLine.length, outLine, 0);
		}
	}
}
