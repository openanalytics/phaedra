package eu.openanalytics.phaedra.silo.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;

/**
 * Represents the structure of a Silo (hierarchy of groups and datasets), without loading its data.
 */
public class SiloStructure extends PlatformObject {

	private String name;
	private String path;
	private SiloStructure parent;
	private Silo silo;

	private boolean isDataset;
	private long datasetSize;
	private List<SiloStructure> children;

	// For memory conservation, these fields are only available on the root.
	private List<String> dataGroups;
	private Map<String, String[]> dataSets;
	private Map<String, Long> dataSetSizes;

	public SiloStructure(SiloStructure parent) {
		this(null, parent);
	}

	public SiloStructure(Silo silo, SiloStructure parent) {
		this.silo = silo;
		if (silo == null && parent != null) {
			this.silo = parent.getSilo();
		}
		this.parent = parent;
		if (parent == null) {
			dataGroups = new ArrayList<>();
			dataSets = new HashMap<>();
			dataSetSizes = new HashMap<>();
		}
		this.children = new ArrayList<>();
	}

	public boolean isDataset() {
		return isDataset;
	}

	public void setDataset(boolean isDataset) {
		this.isDataset = isDataset;
	}

	public long getDatasetSize() {
		return datasetSize;
	}

	public void setDatasetSize(long datasetSize) {
		this.datasetSize = datasetSize;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFullName() {
		return path + (path.endsWith("/") ? "" : "/") + name;
	}

	public SiloStructure getParent() {
		return parent;
	}

	public Silo getSilo() {
		return silo;
	}

	public List<SiloStructure> getChildren() {
		return children;
	}

	public List<String> getDataGroups() {
		return dataGroups;
	}

	public String[] getDataSets(String dataGroup) {
		return dataSets.get(dataGroup);
	}

	public long getDatasetSize(String dataGroup) {
		long size = 0;
		if (dataSetSizes.containsKey(dataGroup)) size = dataSetSizes.get(dataGroup);
		return size;
	}

	public void setDataSetSize(String dataGroup, long size) {
		dataSetSizes.put(dataGroup, size);
	}

	public void addDataSet(String dataGroup, String dataSet) {
		String[] currentSets = dataSets.get(dataGroup);
		if (currentSets == null) {
			currentSets = new String[] { dataSet };
			dataSets.put(dataGroup, currentSets);
		} else {
			if (CollectionUtils.contains(currentSets, dataSet)) return;
			String[] newSets = Arrays.copyOf(currentSets, currentSets.length + 1);
			newSets[newSets.length - 1] = dataSet;
			dataSets.put(dataGroup, newSets);
		}
	}

	public void removeDataSet(String dataGroup, String dataSet) {
		String[] currentSets = dataSets.get(dataGroup);
		if (currentSets == null || currentSets.length == 0) return;
		if (!CollectionUtils.contains(currentSets, dataSet)) return;
		if (currentSets.length == 1) {
			dataSets.remove(dataGroup);
			dataSetSizes.remove(dataGroup);
			return;
		}
		String[] newSets = new String[currentSets.length - 1];
		int index = 0;
		for (int i=0; i<currentSets.length; i++) {
			if (currentSets[i].equals(dataSet)) continue;
			newSets[index++] = currentSets[i];
		}
		dataSets.put(dataGroup, newSets);
	}

	public <T extends ISiloStructureVisitor> T receive(T visitor) {
		visitor.visit(this);
		// Since the visitor may modify the children, iterate on a copy of the children.
		SiloStructure[] childArray = children.toArray(new SiloStructure[children.size()]);
		for (SiloStructure child: childArray) child.receive(visitor);
		return visitor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SiloStructure other = (SiloStructure) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getFullName() + " @ " + silo;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == Silo.class) return adapter.cast(getSilo());
		return super.getAdapter(adapter);
	}
}
