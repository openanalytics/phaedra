package eu.openanalytics.phaedra.silo.util;

import java.util.ArrayList;

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
			if (to.getDatasets() == null) to.setDatasets(new ArrayList<>());
			for (SiloDataset ds: from.getDatasets()) {
				SiloDataset dsTarget = null;
				for (SiloDataset ds2: to.getDatasets()) {
					if (ds2.getName().equals(ds.getName())) {
						dsTarget = ds2;
						break;
					}
				}
				if (dsTarget == null) {
					dsTarget = new SiloDataset();
					dsTarget.setSilo(to);
					to.getDatasets().add(dsTarget);
				}
				copy(ds, dsTarget, copyIds);
			}
		}
	}

	public static void copy(SiloDataset from, SiloDataset to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		
		if (from.getColumns() != null) {
			if (to.getColumns() == null) to.setColumns(new ArrayList<>());
			for (SiloDatasetColumn col: from.getColumns()) {
				SiloDatasetColumn colTarget = null;
				for (SiloDatasetColumn col2: to.getColumns()) {
					if (col2.getName().equals(col.getName())) {
						colTarget = col2;
						break;
					}
				}
				if (colTarget == null) {
					colTarget = new SiloDatasetColumn();
					colTarget.setDataset(to);
					to.getColumns().add(colTarget);
				}
				copy(col, colTarget, copyIds);
			}
		}
	}
	
	public static void copy(SiloDatasetColumn from, SiloDatasetColumn to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		to.setType(from.getType());
	}
}
