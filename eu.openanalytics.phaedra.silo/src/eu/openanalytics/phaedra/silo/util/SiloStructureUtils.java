package eu.openanalytics.phaedra.silo.util;

import java.util.Set;

import eu.openanalytics.phaedra.silo.util.SiloStructureVisitors.DataGroupCounter;
import eu.openanalytics.phaedra.silo.util.SiloStructureVisitors.DataGroupFinder;
import eu.openanalytics.phaedra.silo.util.SiloStructureVisitors.DataSetCounter;
import eu.openanalytics.phaedra.silo.util.SiloStructureVisitors.DataSetFinder;

public class SiloStructureUtils {

	public static Set<SiloStructure> getAllDataSets(SiloStructure struct) {
		return struct.receive(new DataSetCounter()).getDatasets();
	}
	
	public static Set<SiloStructure> getAllDataGroups(SiloStructure struct) {
		return struct.receive(new DataGroupCounter()).getDatagroups();
	}
	
	public static SiloStructure findDataGroup(SiloStructure struct, String dataGroup) {
		return struct.receive(new DataGroupFinder(dataGroup)).getMatch();
	}
	
	public static SiloStructure findDataSet(SiloStructure struct, String path, String name) {
		return struct.receive(new DataSetFinder(path, name)).getMatch();
	}
	
	public static SiloStructure getRoot(SiloStructure struct) {
		SiloStructure root = struct;
		while (root.getParent() != null) root = root.getParent();
		return root;
	}
}
