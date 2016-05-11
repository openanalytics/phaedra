package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.ui.columbus.Activator;
import eu.openanalytics.phaedra.ui.columbus.importwizard.Meas.MeasAnalysis;
import eu.openanalytics.phaedra.ui.columbus.preferences.Prefs;

public class OperaImportHelper {

	// Parameters used only by the wizard:
	public final static String PARAM_PROTOCOL = "protocol";
	public final static String PARAM_EXPERIMENT_NAME = "experimentName";
	// Parameters passed on to the datacapture task (see module "capture.acapella.data"):
	public final static String PARAM_MEAS_SOURCES = "measSources";
	public final static String PARAM_IMPORT_IMG_DATA = "importImageData";
	public final static String PARAM_IMPORT_SW_DATA = "importSubWellData";
	public final static String PARAM_IMG_BASE_PATH = "imageBasePath";
	public final static String PARAM_SW_BASE_PATH = "subwellBasePath";
	
	private final static Pattern MEAS_DIR_PATTERN = Pattern.compile("Meas_(\\d+)");
	private final static Pattern ANALYSIS_FILE_PATTERN = Pattern.compile("An_(\\d+)_Me(\\d+)_.*");

	public static Meas[] findMeasSources(String path) {
		List<String> measFolders = new ArrayList<>();
		findMeasFolders(path, measFolders, 1, 2);
		List<Meas> sources = measFolders.stream()
				.map(f -> getMeas(f))
				.collect(Collectors.toList());
		return sources.toArray(new Meas[sources.size()]);
	}

	public static String getSourceBaseLocation() {
		return Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_SOURCE_PATH);
	}

	public static String getImageBaseLocation() {
		return Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_IMAGE_PATH);
	}

	public static String getSubwellBaseLocation() {
		return Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_SUBWELL_DATA_PATH);
	}

	/*
	 * Non-public
	 * **********
	 */
	
	private static void findMeasFolders(String path, List<String> foundMeasFolders, int depth, int maxDepth) {
		File dir = new File(path);
		if (!dir.isDirectory()) return;

		if (MEAS_DIR_PATTERN.matcher(dir.getName()).matches()) {
			foundMeasFolders.add(path);
		} else {
			File[] children = dir.listFiles();
			for (File child: children) {
				if (!child.isDirectory()) continue;
				if (MEAS_DIR_PATTERN.matcher(child.getName()).matches()) {
					foundMeasFolders.add(child.getAbsolutePath());
				} else if (depth < maxDepth) {
					findMeasFolders(child.getAbsolutePath(), foundMeasFolders, depth+1, maxDepth);
				}
			}
		}
	}

	private static Meas getMeas(String measFolder) {
		File dir = new File(measFolder);
		Meas m = new Meas();
		m.source = dir.getAbsolutePath();
		m.barcode = dir.getParentFile().getName();
		m.name = dir.getName();
		m.isIncluded = true;
		
		List<MeasAnalysis> analyses = Arrays.stream(dir.listFiles())
				.filter(f -> ANALYSIS_FILE_PATTERN.matcher(f.getName()).matches())
				.filter(f -> f.isFile())
				.map(f -> {
					MeasAnalysis a = new MeasAnalysis();
					a.source = f.getAbsolutePath();
					a.name = f.getName();
					return a;
				})
				.collect(Collectors.toList());
		m.availableAnalyses = analyses.toArray(new MeasAnalysis[analyses.size()]);
		
		m.selectedAnalysis = analyses.stream()
				.sorted((a1, a2) -> (int)(new File(a2.source).lastModified() - new File(a1.source).lastModified()))
				.findFirst().orElse(null);
		if (m.selectedAnalysis == null) m.isIncluded = false;
		return m;
	}
}
