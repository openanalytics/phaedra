package eu.openanalytics.phaedra.silo.util;

import java.util.HashSet;
import java.util.Set;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

public class SiloStructureVisitors {

	/**
	 * Get the names of all valid datasets in this silo.
	 */
	public static class DataSetCounter implements ISiloStructureVisitor {
		
		private Set<SiloStructure> datasets = new HashSet<>();
		
		@Override
		public void visit(SiloStructure structure) {
			if (structure.isDataset()) datasets.add(structure);
		}
		
		public Set<SiloStructure> getDatasets() {
			return datasets;
		}
	}

	/**
	 * Get the names of all valid datagroups in this silo.
	 * A datagroup is a group containing at least one dataset.
	 */
	public static class DataGroupCounter implements ISiloStructureVisitor {
		
		private Set<SiloStructure> datagroups = new HashSet<>();
		
		@Override
		public void visit(SiloStructure structure) {
			for (SiloStructure child: structure.getChildren()) {
				if (child.isDataset()) {
					datagroups.add(structure);		
					break;
				}
			}
		}
		
		public Set<SiloStructure> getDatagroups() {
			return datagroups;
		}
	}
	
	public static class DataSetFinder implements ISiloStructureVisitor {
		
		private String path;
		private String name;
		private SiloStructure match;
		
		public DataSetFinder(String path, String name) {
			this.path = path;
			this.name = name;
			this.match = null;
		}
		
		@Override
		public void visit(SiloStructure structure) {
			if (match == null && structure.isDataset()
					&& structure.getPath().equals(path)
					&& structure.getName().equals(name)) match = structure;
		}
		
		public SiloStructure getMatch() {
			return match;
		}
	}
	
	public static class DataGroupFinder implements ISiloStructureVisitor {
		
		private String datagroup;
		private SiloStructure match;
		
		public DataGroupFinder(String datagroup) {
			this.datagroup = datagroup;
			this.match = null;
		}
		
		@Override
		public void visit(SiloStructure structure) {
			if (match == null && !structure.isDataset() && structure.getFullName().equals(datagroup)) match = structure;
		}
		
		public SiloStructure getMatch() {
			return match;
		}
	}
	
	/*
	 * Update a SiloStructure to reflect a new data group.
	 */
	public static class DataGroupChangedVisitor implements ISiloStructureVisitor {
		private String dataGroup;
		private String dataGroupParent;
		private boolean done;
		
		public DataGroupChangedVisitor(String dataGroup) {
			this.dataGroup = dataGroup;
			this.dataGroupParent = FileUtils.getPath(dataGroup);
			if (dataGroupParent.isEmpty()) dataGroupParent = "/";
			this.done = false;
		}
		
		@Override
		public void visit(SiloStructure structure) {
			if (done) return;
			if (structure.getPath().equals(dataGroupParent)) {
				addGroup(structure);
				done = true;
			}
		}
		
		private void addGroup(SiloStructure parent) {
			SiloStructure newChild = new SiloStructure(parent);
			newChild.setDataset(false);
			newChild.setPath(dataGroupParent);
			newChild.setName(FileUtils.getName(dataGroup));
			parent.getChildren().add(newChild);
			
			SiloStructure root = SiloStructureUtils.getRoot(parent);
			CollectionUtils.addUnique(root.getDataGroups(), dataGroup);
		}
	}
	
	/*
	 * Update a SiloStructure to reflect a new/changed/removed dataset.
	 * A size argument of zero means the dataset is removed.
	 */
	public static class DatasetChangedVisitor implements ISiloStructureVisitor {
		private String path;
		private String name;
		private String parentName;
		private long size;
		private boolean done;
		
		public DatasetChangedVisitor(String path, String dataset, long size) {
			this.path = path;
			this.name = dataset;
			this.parentName = FileUtils.getName(path);
			this.size = size;
			this.done = false;
		}
		
		@Override
		public void visit(SiloStructure structure) {
			if (done) return;
			
			if (size == 0 && structure.isDataset() && structure.getPath().equals(path) && structure.getName().equals(name)) {
				// We found the removed item.
				removeChild(structure);
				done = true;
			} else if (size > 0 && !structure.isDataset() && structure.getName().equals(parentName)) {
				// We found the parent of the added/modified item.
				addChild(structure);
				done = true;
			}
		}
		
		private void addChild(SiloStructure parent) {
			SiloStructure newChild = new SiloStructure(parent);
			newChild.setDataset(true);
			newChild.setDatasetSize(size);
			newChild.setPath(path);
			newChild.setName(name);
			
			int existingIndex = parent.getChildren().indexOf(newChild);
			if (existingIndex == -1) {
				parent.getChildren().add(newChild);
			} else {
				parent.getChildren().get(existingIndex).setDatasetSize(size);
			}
			
			SiloStructure root = SiloStructureUtils.getRoot(parent);
			root.addDataSet(path, name);
			root.setDataSetSize(path, size);
		}
		
		private void removeChild(SiloStructure itemToRemove) {
			SiloStructure parent = itemToRemove.getParent();
			parent.getChildren().remove(itemToRemove);
			
			SiloStructure root = SiloStructureUtils.getRoot(itemToRemove);
			root.removeDataSet(path, name);
			
			if (parent.getChildren().isEmpty()) {
				// There are no datasets left in this group: remove the group entirely.
				SiloStructure group = parent;
				group.getParent().getChildren().remove(group);
				root.getDataGroups().remove(group.getFullName());
			}
		}
	}
}
