package eu.openanalytics.phaedra.model.protocol.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

/**
 * <p>
 * Features can be grouped together in feature groups.
 * There are two types of groups: groups containing well features and groups containing
 * subwell features.
 * </p><p>
 * Furthermore, each protocol class has a set of default groups, which are always available
 * and cannot be deleted. In addition to the default groups, the user can define custom groups.
 * </p>
 * <p>
 * A feature can belong to multiple default groups, but it can belong to only one custom group.
 * </p>
 */
public class FeatureGroupManager {

	private Map<String, List<FeatureGroup>> groups;

	public final static String FEATURE_GROUP_KEY = "Key Features";
	public final static String FEATURE_GROUP_ALL = "All";
	public final static String FEATURE_GROUP_UPLOAD = "Upload Features";
	public final static String FEATURE_GROUP_NONE = "No Group";

	private final static String[] DEFAULT_GROUP_NAMES = { FEATURE_GROUP_ALL, FEATURE_GROUP_KEY, FEATURE_GROUP_UPLOAD, FEATURE_GROUP_NONE };
	
	private final static Predicate<IFeature> FEATURE_COLLECTOR_KEY = f -> f.isKey();
	private final static Predicate<IFeature> FEATURE_COLLECTOR_ALL = f -> true;
	private final static Predicate<IFeature> FEATURE_COLLECTOR_UPLOAD = f -> (f instanceof Feature && ((Feature)f).isUploaded());
	private final static Predicate<IFeature> FEATURE_COLLECTOR_NONE = f -> f.getFeatureGroup() == null;

	public FeatureGroupManager() {
		groups = new HashMap<>();
	}
	
	public List<FeatureGroup> getGroups(ProtocolClass pClass, GroupType type, boolean includeDefault) {
		List<FeatureGroup> allGroups = null;
		
		ProtocolClass originalPClass = ProtocolService.getInstance().getProtocolClass(pClass.getId());
		boolean isWorkingCopy = (originalPClass.equals(pClass) && originalPClass != pClass);
		if (isWorkingCopy) {
			// This is a clone. Bypass the cache entirely (which contains entries for the originals).
			allGroups = loadFeatureGroups(pClass, type);
		} else {
			String key = getKey(pClass, type);
			if (groups.containsKey(key)) {
				allGroups = groups.get(key);
			} else {
				allGroups = loadFeatureGroups(pClass, type);
				groups.put(key, allGroups);
			}
		}
		
		if (includeDefault) return allGroups;
		else return allGroups.stream().filter(fg -> !CollectionUtils.contains(DEFAULT_GROUP_NAMES, fg.getName())).collect(Collectors.toList());
	}

	public void reloadGroups(ProtocolClass pClass) {
		for (GroupType type : GroupType.values()) {
			String key = getKey(pClass, type);
			groups.remove(key);
		}
	}

	public FeatureGroup getGroup(ProtocolClass pClass, GroupType type, String name) {
		List<FeatureGroup> groups = getGroups(pClass, type, true);
		for (FeatureGroup group: groups) {
			if (group.getName().equals(name)) return group;
		}
		return null;
	}

	public boolean isMember(IFeature feature, FeatureGroup group) {
		if (feature == null || group == null) return false;
		if (!feature.getProtocolClass().equals(group.getProtocolClass())) return false;
		List<Feature> features = getMembers(group);
		return features.contains(feature);
	}

	@SuppressWarnings("unchecked")
	public <F extends IFeature> List<F> getMembers(FeatureGroup group) {
		List<F> members = new ArrayList<>();
		if (group == null) return members;

		List<F> candidates = (group.getType() == GroupType.WELL.getType()) ?
				(List<F>)group.getProtocolClass().getFeatures() : (List<F>)group.getProtocolClass().getSubWellFeatures();
		candidates = ProtocolService.streamableList(candidates);

		if (group.getName().equals(FEATURE_GROUP_ALL)) {
			members = candidates.stream().filter(FEATURE_COLLECTOR_ALL).collect(Collectors.toList());
		} else if (group.getName().equals(FEATURE_GROUP_KEY)) {
			members = candidates.stream().filter(FEATURE_COLLECTOR_KEY).collect(Collectors.toList());
		} else if (group.getName().equals(FEATURE_GROUP_UPLOAD)) {
			members = candidates.stream().filter(FEATURE_COLLECTOR_UPLOAD).collect(Collectors.toList());
		} else if (group.getName().equals(FEATURE_GROUP_NONE)) {
			members = candidates.stream().filter(FEATURE_COLLECTOR_NONE).collect(Collectors.toList());
		} else {
			members = candidates.stream().filter(f -> group.equals(f.getFeatureGroup())).collect(Collectors.toList());
		}

		return members;
	}

	/*
	 * Non-public
	 * **********
	 */

	private List<FeatureGroup> loadFeatureGroups(ProtocolClass pClass, GroupType type) {
		List<FeatureGroup> pClassGroups = new ArrayList<>();
		// Default groups
		pClassGroups.add(ProtocolService.getInstance().createFeatureGroup(pClass, type, FEATURE_GROUP_ALL));
		pClassGroups.add(ProtocolService.getInstance().createFeatureGroup(pClass, type, FEATURE_GROUP_KEY));
		pClassGroups.add(ProtocolService.getInstance().createFeatureGroup(pClass, type, FEATURE_GROUP_UPLOAD));
		pClassGroups.add(ProtocolService.getInstance().createFeatureGroup(pClass, type, FEATURE_GROUP_NONE));
		// Custom groups
		List<FeatureGroup> customGroups = ProtocolService.streamableList(pClass.getFeatureGroups()).stream()
				.filter(fg -> fg.getType() == type.getType())
				.sorted(ProtocolUtils.FEATURE_GROUP_NAME_SORTER)
				.collect(Collectors.toList());
		pClassGroups.addAll(customGroups);
		return pClassGroups;
	}

	private static String getKey(ProtocolClass pClass, GroupType type) {
		return pClass.getId() + "#" + type.getType();
	}
}
