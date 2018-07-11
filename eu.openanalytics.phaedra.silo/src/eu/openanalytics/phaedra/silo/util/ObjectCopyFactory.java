package eu.openanalytics.phaedra.silo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class ObjectCopyFactory {

	public static void copy(Silo from, Silo to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setAccessScope(from.getAccessScope());
		to.setCreationDate(from.getCreationDate());
		to.setOwner(from.getOwner());
		to.setType(from.getType());
		to.setProtocolClass(from.getProtocolClass());
		
		if (from.getDatasets() != null) {
			// Look for new/existing datasets
			if (to.getDatasets() == null) to.setDatasets(new ArrayList<>());
			for (SiloDataset fromDs: from.getDatasets()) {
				SiloDataset toDs = findMatch(to.getDatasets(), d -> d.getName().equals(fromDs.getName()));
				if (toDs == null) {
					toDs = new SiloDataset();
					toDs.setSilo(to);
					to.getDatasets().add(toDs);
				}
				copy(fromDs, toDs, copyIds);
			}
		}
		if (to.getDatasets() != null) {
			// Look for removed datasets
			List<SiloDataset> datasetsToRemove = new ArrayList<>();
			for (SiloDataset toDs: to.getDatasets()) {
				SiloDataset match = findMatch(from.getDatasets(), d -> d.getName().equals(toDs.getName()));
				if (match == null) datasetsToRemove.add(toDs);
			}
			for (SiloDataset ds: datasetsToRemove) {
				int index = findMatchIndex(to.getDatasets(), d -> d.getName().equals(ds.getName()));
				to.getDatasets().remove(index);
			}
		}
	}

	public static void copy(SiloDataset from, SiloDataset to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		
		if (from.getColumns() != null) {
			// Look for new/existing columns
			if (to.getColumns() == null) to.setColumns(new ArrayList<>());
			for (SiloDatasetColumn fromCol: from.getColumns()) {
				SiloDatasetColumn toCol = findMatch(to.getColumns(), c -> c.getName().equals(fromCol.getName()));
				if (toCol == null) {
					toCol = new SiloDatasetColumn();
					toCol.setDataset(to);
					to.getColumns().add(toCol);
				}
				copy(fromCol, toCol, copyIds);
			}
		}
		if (to.getColumns() != null) {
			// Look for removed columns
			List<SiloDatasetColumn> columnsToRemove = new ArrayList<>();
			for (SiloDatasetColumn toCol: to.getColumns()) {
				SiloDatasetColumn match = findMatch(from.getColumns(), c -> c.getName().equals(toCol.getName()));
				if (match == null) columnsToRemove.add(toCol);
			}
			for (SiloDatasetColumn col: columnsToRemove) {
				int index = findMatchIndex(to.getColumns(), c -> c.getName().equals(col.getName()));
				to.getColumns().remove(index);
			}
		}
	}
	
	public static void copy(SiloDatasetColumn from, SiloDatasetColumn to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		to.setType(from.getType());
	}
	
	private static <E> E findMatch(List<E> list, Predicate<E> matcher) {
		if (list == null || list.isEmpty()) return null;
		return SiloService.streamableList(list).stream().filter(matcher).findAny().orElse(null);
	}
	
	private static <E> int findMatchIndex(List<E> list, Predicate<E> matcher) {
		if (list == null || list.isEmpty()) return -1;
		return IntStream.range(0, list.size()).filter(i -> matcher.test(list.get(i))).findAny().orElse(-1);
	}
}
