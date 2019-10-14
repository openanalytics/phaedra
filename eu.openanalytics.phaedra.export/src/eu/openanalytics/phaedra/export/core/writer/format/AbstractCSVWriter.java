package eu.openanalytics.phaedra.export.core.writer.format;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.export.core.ExportInfo;


public class AbstractCSVWriter extends AbstractExportWriter {
	
	
	
	protected String getDestinationPath(String destinationPath, String sub) {
		String extension = FileUtils.getExtension(destinationPath);
		destinationPath = destinationPath.substring(0, destinationPath.lastIndexOf('.'));
		return destinationPath + '_' + sub + '.' + extension;
	}
	
	protected void writeExportInfo(au.com.bytecode.opencsv.CSVWriter writer) throws IOException {
		List<ExportInfo> infos = getExportInfos();
		int maxValueSize = infos.stream().mapToInt(i -> i.getValues().size()).max().orElse(1);
		String[] rowData = new String[1 + maxValueSize];
		for (final ExportInfo info : infos) {
			Arrays.fill(rowData, null);
			rowData[0] = info.getName();
			for (int valueIdx = 0; valueIdx < info.getValues().size(); valueIdx++) {
				rowData[1 + valueIdx] = String.valueOf(info.getValues().get(valueIdx));
			}
			writer.writeNext(rowData);
		}
	}
	
}
