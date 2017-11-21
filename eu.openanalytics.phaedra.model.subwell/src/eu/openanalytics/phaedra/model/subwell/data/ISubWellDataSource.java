package eu.openanalytics.phaedra.model.subwell.data;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;

public interface ISubWellDataSource {

	public int getNrCells(Well well);
	
	public float[] getNumericData(Well well, SubWellFeature feature);
	public String[] getStringData(Well well, SubWellFeature feature);
	
	public void updateData(Map<SubWellFeature, Map<Well, Object>> data);
	
	public void preloadData(List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache, IProgressMonitor monitor);
	
	public void close();
}
