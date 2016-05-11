package eu.openanalytics.phaedra.datacapture.montage;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;

import au.com.bytecode.opencsv.CSVWriter;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.layout.FieldLayout;
import eu.openanalytics.phaedra.datacapture.montage.layout.FieldLayoutSourceRegistry;
import eu.openanalytics.phaedra.datacapture.parser.ParseException;
import eu.openanalytics.phaedra.datacapture.parser.ParserService;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedSubWellDataset;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedWell;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter;
import eu.openanalytics.phaedra.datacapture.util.FilePatternInterpreter.PatternMatch;

/**
 * <p>
 * Using the configuration supplied via init(), this class
 * looks for subwell data files and modifies position-related
 * features (such as CenterX, CenterY, Row, Col, ...) so that
 * their value is relative to the montage image, rather than
 * the field image.
 * </p><p>
 * For each reading, a number of modified files is written to outputPath
 * (one file per well).
 * These files are tab-separated text files, and contain the modified feature
 * values, in addition to all other unmodified features.
 * </p>
 */
public class SubwellDataProcessor {

	private MontageConfig montageConfig;
	
	private FieldLayout fieldLayout;
	private Point imageDimensions;
	private boolean fullImageDimensions;
	
	private int modifyFeatureCount;
	
	public void init(MontageConfig montageConfig) {
		this.montageConfig = montageConfig;
		
		modifyFeatureCount = 0;
		if (montageConfig.subwellDataXFeatures != null) {
			modifyFeatureCount += montageConfig.subwellDataXFeatures.length;
		}
		if (montageConfig.subwellDataYFeatures != null) {
			modifyFeatureCount += montageConfig.subwellDataYFeatures.length;
		}
	}
	
	public void setImageDimensions(Point imageDimensions, boolean fullDimensions) {
		this.imageDimensions = imageDimensions;
		this.fullImageDimensions = fullDimensions;
	}
	
	public void process(PlateReading reading, String outputPath, DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		// Abort if there are no features to modify.
		if (modifyFeatureCount == 0) return;
		if (monitor.isCanceled()) return;
		
		if (imageDimensions == null || (imageDimensions.x == 0 && imageDimensions.y == 0)) {
			throw new DataCaptureException("Cannot montage subwell data: no image montage was performed, or image capture was skipped");
		}
		
		// Locate the file(s) containing subwell data.
		File[] files = new File[0];
		String resolvedPath = CaptureUtils.resolvePath(montageConfig.subwellDataPath, reading.getSourcePath());
		if (resolvedPath != null && new File(resolvedPath).isDirectory()) {
			files = new File(resolvedPath).listFiles();
		} else {
			context.getLogger().warn(reading, "Subwell data path not found: " + resolvedPath);
		}
		FilePatternInterpreter interpreter = new FilePatternInterpreter(montageConfig.subwellDataPattern, montageConfig.subwellDataPatternIdGroups, montageConfig.subwellDataPatternFieldGroup);
		
		Set<Integer> uniqueFields = new HashSet<>();
		Set<String> idSet = new HashSet<>();
		Map<String, String> inputFileMap = new HashMap<String, String>();
		for (File file: files) {
			PatternMatch match = interpreter.match(file.getName());
			if (match.isMatch) {
				idSet.add(match.id);
				inputFileMap.put(match.id + "#" + match.field, file.getAbsolutePath());
				uniqueFields.add(match.field);
			}
		}

		fieldLayout = FieldLayoutSourceRegistry.getInstance().getLayout(reading, uniqueFields.size(), montageConfig, context);
		if (fullImageDimensions) {
			imageDimensions.x = imageDimensions.x / fieldLayout.getColumns();
			imageDimensions.y = imageDimensions.y / fieldLayout.getRows();
		}
		
		monitor.beginTask("Processing subwell data", idSet.size());

		for (String wellId: idSet) {
			if (monitor.isCanceled()) return;
			monitor.subTask("Processing well " + wellId);
			
			context.getActiveModule().getConfig().getParameters().setParameter("wellNr", wellId);
			String outputFile = outputPath + "/" + CaptureUtils.resolveVars(montageConfig.subwellDataOutput, false);
			
			modifySubwellData(wellId, inputFileMap, outputFile);
			
			monitor.worked(1);
		}
		
		monitor.done();
	}
	
	private void modifySubwellData(String wellId, Map<String, String> inputFileMap, String outputPath) throws DataCaptureException {
		
		// First, build a map of input files (one per field) for this well, using the full input map.
		Map<String, String> fieldFiles = new HashMap<>();
		for (String key: inputFileMap.keySet()) {
			if (key.startsWith(wellId + "#")) {
				String field = key.substring(key.indexOf("#")+1);
				fieldFiles.put(field, inputFileMap.get(key));
			}
		}
		
		// Parse each field data file in turn (if field = -1, there is just one data file, already merged)
		String[] parsedFeatures = null;
		List<ParsedWell> parsedFields = new ArrayList<>();
		for (String field: fieldFiles.keySet()) {
			String filePath = fieldFiles.get(field);
			
			ParsedWell well = null;
			ParsedModel parsedModel = null;
			try {
				parsedModel = ParserService.getInstance().parse(filePath, montageConfig.subwellDataParserId);
			} catch (ParseException e) {
				throw new DataCaptureException("Failed to parse subwell data file", e);
			}
			
			if (parsedModel == null || parsedModel.getPlates().length == 0 || parsedModel.getPlate(0).getWells().length == 0) return;
			well = parsedModel.getPlate(0).getWell(1,1);
			
			// Get a set of all available features.
			parsedFeatures = well.getSubWellData().keySet().toArray(new String[0]);
			parsedFields.add(well);
			well.addKeyword("Field", field);
		}
		
		// Find out which features need to be updated, and whether or not a "Field" feature should be added.
		int[] xFeatureIndices = getXFeatureIndices(parsedFeatures);
		int[] yFeatureIndices = getYFeatureIndices(parsedFeatures);
		int fieldFeatureIndex = getFieldFeatureIndex(parsedFeatures);
		
		boolean isSingleField = (fieldFiles.size() == 1 && fieldFiles.keySet().contains("-1"));
		if (!isSingleField && fieldFeatureIndex == -1) {
			// We are in file-per-field mode and there is no Field feature yet: add one now.
			parsedFeatures = Arrays.copyOf(parsedFeatures, parsedFeatures.length + 1);
			parsedFeatures[parsedFeatures.length-1] = "Field";
		}
		int featureCount = parsedFeatures.length;
		
		// Now write a new subwell data file containing the merged data.
		try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath), '\t', CSVWriter.NO_QUOTE_CHARACTER)) {
			
			// First, write the feature names as headers
			writer.writeNext(parsedFeatures);
			
			for (ParsedWell well: parsedFields) {
				int fieldNr = Integer.parseInt(well.getKeyword("Field"));
				int cellCount = well.getSubWellData().get(parsedFeatures[0]).getCellCount();
				for (int row=0; row<cellCount; row++) {
					
					String[] rowValues = new String[featureCount];
					
					// If we are processing a single-file well, obtain the field nr from the "Field" feature.
					if (isSingleField) {
						if (fieldFeatureIndex == -1) throw new DataCaptureException("Cannot montage: no Field feature found");
						fieldNr = (int)well.getSubWellDataset(parsedFeatures[fieldFeatureIndex]).getAllNumericValues()[0][row];
					}
					
					// Modify subwelldata where needed
					for (int i=0; i<featureCount; i++) {
						String feature = parsedFeatures[i];
						ParsedSubWellDataset ds = well.getSubWellDataset(feature);
						
						double value = Double.NaN;
						if (ds != null && ds.isNumeric()) {
							float[][] data = ds.getAllNumericValues();
							if (CollectionUtils.contains(xFeatureIndices, i)) {
								boolean horizontal = true;
								value = calculateValue(data[0][row], fieldNr, horizontal);
							} else if (CollectionUtils.contains(yFeatureIndices, i)) {
								boolean horizontal = false;
								value = calculateValue(data[0][row], fieldNr, horizontal);
							} else {
								value = data[0][row];
							}
						}
						rowValues[i] = "" + value;
					}
					if (!isSingleField && fieldFeatureIndex == -1) {
						rowValues[featureCount - 1] = "" + fieldNr;
					}
					
					writer.writeNext(rowValues);
				}				
			}
		} catch (IOException e) {
			throw new DataCaptureException("Failed to write modified subwell data file", e);
		}
	}
	
	private double calculateValue(double original, int fieldNr, boolean horizontal) {
		Point position = fieldLayout.getFieldPosition(fieldNr);
		if (position == null) throw new IllegalArgumentException("Unable to determine position of field " + fieldNr + " (check montage configuration)");
		int offsetFields = horizontal ? position.x : position.y;
		int fieldSize = horizontal ? imageDimensions.x : imageDimensions.y;
		return original + offsetFields*(fieldSize + 2*montageConfig.padding);
	}
	
	private int getFieldFeatureIndex(String[] features) {
		Pattern pattern = Pattern.compile(".*[Ff]ield.*");
		for (int i=0; i<features.length; i++) {
			if (pattern.matcher(features[i]).matches()) return i;
		}
		return -1;
	}
	
	private int[] getXFeatureIndices(String[] features) {
		List<Integer> indices = new ArrayList<>();
		
		for (int i=0; i<features.length; i++) {
			int index = CollectionUtils.find(montageConfig.subwellDataXFeatures, features[i]);
			if (index != -1) indices.add(i);
		}
		
		return CollectionUtils.toIntArray(indices);
	}
	
	private int[] getYFeatureIndices(String[] features) {
		List<Integer> indices = new ArrayList<>();
		
		for (int i=0; i<features.length; i++) {
			int index = CollectionUtils.find(montageConfig.subwellDataYFeatures, features[i]);
			if (index != -1) indices.add(i);
		}
		
		return CollectionUtils.toIntArray(indices);
	}
}
