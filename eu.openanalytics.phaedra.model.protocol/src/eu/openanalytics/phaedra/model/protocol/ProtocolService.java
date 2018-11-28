package eu.openanalytics.phaedra.model.protocol;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.protocol.util.FeatureGroupManager;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolClassSummary;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

/**
 * API for interaction with protocols and protocol classes.
 * This includes creation, retrieval and modification of protocols, protocol classes
 * and features.
 */
public class ProtocolService extends BaseJPAService {

	private static ProtocolService instance = new ProtocolService();
	
	private FeatureGroupManager featureGroupManager;
	
	private ProtocolService() {
		// Hidden constructor
		featureGroupManager = new FeatureGroupManager();
		ModelEventService.getInstance().addEventListener(e -> {
			if (e.type == ModelEventType.ObjectChanged && e.source instanceof ProtocolClass) {
				featureGroupManager.reloadGroups((ProtocolClass) e.source);
			}
		});
	}

	public static ProtocolService getInstance() {
		return instance;
	}

	/**
	 * Get a list of all configured well types.
	 * 
	 * @return An alphabetical list of all known well types.
	 */
	public List<WellType> getWellTypes() {
		return streamableList(getList(WellType.class)).stream()
				.sorted((wt1, wt2) -> {
					if (wt1 == null && wt2 == null) return 0;
					if (wt1 == null) return -1;
					if (wt2 == null) return 1;
					return wt1.getCode().compareTo(wt2.getCode());
				})
				.collect(Collectors.toList());
	}

	/**
	 * Create a new well type, to be used in any protocol class.
	 * 
	 * @param code The welltype code (must be unique).
	 * @return The new welltype.
	 */
	public WellType createWellType(String code) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOLCLASS_CREATE, null);
		WellType wt = new WellType();
		wt.setCode(code);
		wt.setDescription(code);
		save(wt);
		return wt;
	}
	
	/**
	 * Get a list of all feature groups in the given protocol class of the given type.
	 * 
	 * @param pClass The parent protocol class.
	 * @param groupType The type of group: well or subwell.
	 * @return An alphabetical list of matching groups.
	 */
	public List<FeatureGroup> getAllFeatureGroups(ProtocolClass pClass, GroupType groupType) {
		return featureGroupManager.getGroups(pClass, groupType, true);
	}
	
	/**
	 * Get a list of custom feature groups in the given protocol class of the given type.
	 * Custom feature groups are groups that were created by users, excluding the default
	 * groups (All, Key, etc).
	 * 
	 * @param pClass The parent protocol class.
	 * @param groupType The type of group: well or subwell.
	 * @return An alphabetical list of matching groups.
	 */
	public List<FeatureGroup> getCustomFeatureGroups(ProtocolClass pClass, GroupType groupType) {
		return featureGroupManager.getGroups(pClass, groupType, false);
	}
	
	/**
	 * Get a feature group by its name.
	 * 
	 * @param pClass The parent protocol class.
	 * @param groupType The type of group: well or subwell.
	 * @param name The name of the group.
	 * @return The matching group, or null if no match was found.
	 */
	public FeatureGroup getFeatureGroup(ProtocolClass pClass, GroupType groupType, String name) {
		return featureGroupManager.getGroup(pClass, groupType, name);
	}
	
	/**
	 * Check if the given feature is member of the given feature group.
	 *  
	 * @param feature The feature to test.
	 * @param group The group to look in.
	 * @return True if the feature is a member of the given group.
	 */
	public boolean isMember(IFeature feature, FeatureGroup group) {
		return featureGroupManager.isMember(feature, group);
	}

	/**
	 * List all the features of the given feature group.
	 * 
	 * @param group The group to list features for.
	 * @return A list of features in the given group.
	 */
	public <F extends IFeature> List<F> getMembers(FeatureGroup group) {
		return featureGroupManager.getMembers(group);
	}

	/**
	 * Create a new feature group. Make sure to  call {@link ProtocolService#updateProtocolClass(ProtocolClass)} afterwards.
	 * 
	 * @param pClass The parent protocol class.
	 * @param type The type of group to create: well or subwell.
	 * @param name The name for the new group.
	 * @return A newly created feature group, not yet saved.
	 */
	public FeatureGroup createFeatureGroup(ProtocolClass pClass, GroupType type, String name) {
		FeatureGroup fGroup = new FeatureGroup();
		fGroup.setName(name);
		fGroup.setDescription("");
		fGroup.setProtocolClass(pClass);
		fGroup.setType(type.getType());
		return fGroup;
	}

	/**
	 * Get a list of all protocol classes the current user has access to.
	 * 
	 * @return A list of all protocol classes.
	 */
	public List<ProtocolClass> getProtocolClasses() {
		return streamableList(getList(ProtocolClass.class)).stream()
				.filter(pc -> SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, pc))
				.collect(Collectors.toList());
	}

	/**
	 * Retrieve a protocol class by its primary ID.
	 * 
	 * @param protocolClassId The primary ID of the protocol class to retrieve.
	 * @return The matching protocol class, or null if no match was found.
	 */
	public ProtocolClass getProtocolClass(long protocolClassId) {
		ProtocolClass pc = getEntityManager().find(ProtocolClass.class, protocolClassId);
		if (!SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, pc)) return null;
		return pc;
	}

	/**
	 * Create a new protocol class. Make sure to call {@link ProtocolService#updateProtocolClass(ProtocolClass)} afterwards.
	 * 
	 * @return A new protocol class, not yet saved.
	 */
	public ProtocolClass createProtocolClass() {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOLCLASS_CREATE, null);
		// Note: the security check is performed when the user attempts to save.
		ProtocolClass pClass = new ProtocolClass();
		pClass.setFeatures(new ArrayList<Feature>());
		pClass.setSubWellFeatures(new ArrayList<SubWellFeature>());
		pClass.setEditable(true);
		pClass.setInDevelopment(true);
		pClass.setImageSettings(new ImageSettings());
		pClass.getImageSettings().setZoomRatio(2);
		pClass.getImageSettings().setGamma(16);
		pClass.getImageSettings().setPixelSizeX(1.0f);
		pClass.getImageSettings().setPixelSizeY(1.0f);
		pClass.getImageSettings().setPixelSizeZ(1.0f);
		pClass.getImageSettings().setImageChannels(new ArrayList<ImageChannel>());
		pClass.setName("New Protocol Class");
		pClass.setLowWellTypeCode("LC");
		pClass.setHighWellTypeCode("HC");
		return pClass;
	}

	/**
	 * Create a full copy of an existing protocol class. This includes:
	 * <ul>
	 * <li>All the well and subwell feature configurations</li>
	 * <li>All image channels</li>
	 * <li>Data capture configuration</li>
	 * </ul>
	 * Note that this will <b>not</b> copy the experiments and plates of the underlying protocols.
	 * 
	 * @param protocolClass The protocol class to clone.
	 * @return A new copy of the protocol class.
	 */
	public ProtocolClass cloneProtocolClass(ProtocolClass protocolClass) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOLCLASS_CREATE, null);
		ProtocolClass clone = createProtocolClass();
		ObjectCopyFactory.copySettings(protocolClass, clone, false);
		updateProtocolClass(clone);
		
		try {
			String dcConfig = Screening.getEnvironment().getFileServer().getContentsAsString(getDCConfigFile(protocolClass.getDefaultCaptureConfig()));
			String newId = String.format("protocolclass-%d.capture", clone.getId());
			Screening.getEnvironment().getFileServer().putContents(getDCConfigFile(newId), dcConfig.getBytes());
			clone.setDefaultCaptureConfig(newId);
			updateProtocolClass(clone);
		} catch (IOException e) {
			EclipseLog.error("Failed to clone capture configuration", e, Activator.getDefault());
		}
		
		return clone;
	}
	
	/**
	 * Get the path to the data capture configuration file with the given ID.
	 * 
	 * @param configId The ID of the config file to look up.
	 * @return A path to the config file, relative to the file server root.
	 */
	public String getDCConfigFile(String configId) {
		//TODO This path is an implementation detail of the DC bundles, see eu.openanalytics.phaedra.datacapture.module.ModuleFactory
		String path = "/data.capture.configurations/%s.xml";
		return String.format(path, configId);
	}
	
	/**
	 * Save any changes made to a protocol class. This includes the feature and image channel configurations.
	 * 
	 * @param protocolClass The protocol class to update.
	 */
	public void updateProtocolClass(ProtocolClass protocolClass) {
		checkCanEditProtocolClass(protocolClass);
		validateProtocolClass(protocolClass);
		save(protocolClass);
	}

	/**
	 * Delete a protocol class.
	 * <p>
	 * <b>Important:</b> this will permanently delete all configuration,
	 * as well as all experiments and plates for the given protocol class.
	 * </p>
	 * @param protocolClass The protocol class to delete.
	 */
	public void deleteProtocolClass(ProtocolClass protocolClass) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOLCLASS_DELETE, protocolClass);
		delete(protocolClass);
	}

	/**
	 * Check if the current user has permission to edit the given protocol class.
	 * If not, an exception will be thrown.
	 * 
	 * @param protocolClass The protocol class to check.
	 */
	public void checkCanEditProtocolClass(ProtocolClass protocolClass) {
		if (!canEditProtocolClass(protocolClass)) throw new PermissionDeniedException("No permission to update Protocol Class");
	}
	
	/**
	 * Check if the current user has permission to edit the given protocol class.
	 * 
	 * @param protocolClass The protocol class to check.
	 * @return True if the current user has permission.
	 */
	public boolean canEditProtocolClass(ProtocolClass protocolClass) {
		return canEditProtocolClass(SecurityService.getInstance().getCurrentUserName(), protocolClass);
	}

	/**
	 * Check if the given user has permission to edit the given protocol class.
	 * 
	 * @param userName The username to check.
	 * @param protocolClass The protocol class to check.
	 * @return True if the given user has permission.
	 */
	public boolean canEditProtocolClass(String userName, ProtocolClass protocolClass) {
		SecurityService security = SecurityService.getInstance();
		boolean isAdmin = security.isGlobalAdmin(userName);
		boolean isPClassEditor = security.check(userName, Permissions.PROTOCOLCLASS_EDIT, protocolClass);
		boolean isPClassUser = security.check(userName, Roles.USER, protocolClass);

		if (isAdmin) return true;
		else if (isPClassEditor && protocolClass.isEditable()) return true;
		else if (isPClassUser && protocolClass.isInDevelopment()) return true;
		return false;
	}

	/**
	 * Validate the current settings of the given protocol class.
	 * <p>
	 * This will check for duplicate feature names, and other constraints
	 * such as max feature name length.
	 * </p>
	 * This method is automatically called before attempting to save a protocol class.
	 * 
	 * @param protocolClass The protocol class to check.
	 */
	public void validateProtocolClass(ProtocolClass protocolClass) {
		// Ensure the protocol class has no duplicate feature names.
		Set<String> featureNames = new HashSet<String>();
		for (Feature f: protocolClass.getFeatures()) {
			String name = f.getName();
			if (featureNames.contains(name)) throw new RuntimeException("Duplicate feature name: " + name);
			else featureNames.add(name);
		}
		featureNames.clear();
		for (SubWellFeature f: protocolClass.getSubWellFeatures()) {
			String name = f.getName();
			if (featureNames.contains(name)) throw new RuntimeException("Duplicate sub-well feature name: " + name);
			else featureNames.add(name);
		}
		
		int maxNameLength = 100;
		IFeature feature = streamableList(protocolClass.getFeatures()).stream().filter(f -> f.getName().length() > maxNameLength).findAny().orElse(null);
		if (feature != null) throw new RuntimeException("Feature name too long: " + feature.getName());
		feature = streamableList(protocolClass.getSubWellFeatures()).stream().filter(f -> f.getName().length() > maxNameLength).findAny().orElse(null);
		if (feature != null) throw new RuntimeException("Feature name too long: " + feature.getName());
		
		int maxShortNameLength = 36;
		feature = streamableList(protocolClass.getFeatures()).stream().filter(f -> f.getShortName() != null && f.getShortName().length() > maxShortNameLength).findAny().orElse(null);
		if (feature != null) throw new RuntimeException("Feature alias too long: " + feature.getName());
		feature = streamableList(protocolClass.getSubWellFeatures()).stream().filter(f -> f.getShortName() != null && f.getShortName().length() > maxShortNameLength).findAny().orElse(null);
		if (feature != null) throw new RuntimeException("Feature alias too long: " + feature.getName());
	}

	/**
	 * Get a summary of a protocol class.
	 * <p>
	 * The summary contains an overview of the number of protocols, features, and image channels.
	 * </p>
	 * @param pClass The protocol class to get a summary for.
	 * @return A protocol class summary object.
	 */
	public ProtocolClassSummary getProtocolClassSummary(ProtocolClass pClass) {
		ProtocolClassSummary summary = new ProtocolClassSummary();

		String sql = "select"
				+ " (select count(protocol_id) from phaedra.hca_protocol where protocolclass_id = ?) protocols,"
				+ " (select count(feature_id) from phaedra.hca_feature where protocolclass_id = ?) features,"
				+ " (select count(subwellfeature_id) from phaedra.hca_subwellfeature where protocolclass_id = ?) swFeatures,"
				+ " (select count(image_channel_id) from phaedra.hca_image_channel where image_setting_id = "
				+ " 	(select image_setting_id from phaedra.hca_protocolclass where protocolclass_id = ?)) imageChannels "
				+ JDBCUtils.getFromDual();
		sql = sql.replace("?", "" + pClass.getId());

		PreparedStatement ps = null;
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			ps = conn.prepareStatement(sql);
			ResultSet resultSet = ps.executeQuery();

			if (resultSet.next()) {
				summary.protocols = resultSet.getInt(1);
				summary.features = resultSet.getInt(2);
				summary.swFeatures = resultSet.getInt(3);
				summary.imageChannels = resultSet.getInt(4);
			}
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			if (ps != null) try { ps.close(); } catch (SQLException e) {};
		}

		return summary;
	}

	/**
	 * Get a list of all protocols the current user has access to.
	 * 
	 * @return A list of accessible protocols.
	 */
	public List<Protocol> getProtocols() {
		return streamableList(getList(Protocol.class)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Get a list of all accessible protocols for the given protocol class.
	 * 
	 * @param pClass The protocol class to get protocols for.
	 * @return A list of accessible protocols.
	 */
	public List<Protocol> getProtocols(ProtocolClass pClass) {
		String query = "select p from Protocol p where p.protocolClass = ?1";
		return streamableList(getList(query, Protocol.class, pClass)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Retrieve the protocol with the given primary ID.
	 * 
	 * @param protocolId The ID of the protocol to retrieve.
	 * @return The matching protocol, or null if no match was found.
	 */
	public Protocol getProtocol(long protocolId) {
		Protocol p = getEntityManager().find(Protocol.class, protocolId);
		if (!SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p)) return null;
		return p;
	}

	/**
	 * Get all accessible protocol(s) that match the given name.
	 * 
	 * @param protocolName The name of the protocol(s) to retrieve.
	 * @return The matching protocols.
	 */
	public List<Protocol> getProtocolsByName(String protocolName) {
		String query = "select p from Protocol p where p.name = ?1";
		return streamableList(getList(query, Protocol.class, protocolName)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Create a new protocol under the given protocol class.
	 * Make sure to call {@link ProtocolService#updateProtocol(Protocol)} afterwards.
	 * 
	 * @param protocolClass The parent protocol class.
	 * @return A new protocol, not yet saved.
	 */
	public Protocol createProtocol(ProtocolClass protocolClass) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOL_CREATE, protocolClass);
		Protocol p = new Protocol();
		p.setProtocolClass(protocolClass);
		p.setName("New Protocol");
		return p;
	}

	/**
	 * Save any changes made to a protocol.
	 * 
	 * @param protocol The protocol to update.
	 */
	public void updateProtocol(Protocol protocol) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOL_EDIT, protocol);
		save(protocol);
	}

	/**
	 * Delete a protocol.
	 * <p>
	 * <b>Important:</b> this will permanently delete all experiments and plates for the given protocol.
	 * </p>
	 * @param protocol The protocol to delete.
	 */
	public void deleteProtocol(Protocol protocol) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOL_DELETE, protocol);
		delete(protocol);
	}

	/**
	 * Create a new well feature. Make sure to call {@link ProtocolService#updateProtocolClass(ProtocolClass)} afterwards.
	 * 
	 * @param pClass The protocol class to create the feature in.
	 * @return The newly created feature.
	 */
	public Feature createFeature(ProtocolClass pClass) {
		checkCanEditProtocolClass(pClass);
		Feature feature = new Feature();
		feature.setName("New feature");
		feature.setNormalization("NONE");
		feature.setProtocolClass(pClass);
		feature.setNumeric(true);
		feature.setFormatString("#.##");
		feature.setCurveSettings(new HashMap<>());
		feature.setColorMethodSettings(new HashMap<>());
		feature.setFeatureClasses(new ArrayList<>());
		feature.setCalculationLanguage("jep");
		feature.setCalculationTrigger("PlateRecalc");
		feature.setNormalizationLanguage("jep");
		return feature;
	}

	/**
	 * Retrieve a well feature by its primary ID.
	 * 
	 * @param featureId The ID of the feature.
	 * @return The matching feature, or null if no match was found.
	 */
	public Feature getFeature(long featureId) {
		Feature f = getEntity(Feature.class, featureId);
		if (!SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, f)) return null;
		return f;
	}

	/**
	 * Create a new subwell feature. Make sure to call {@link ProtocolService#updateProtocolClass(ProtocolClass)} afterwards.
	 * 
	 * @param pClass The protocol class to create the feature in.
	 * @return The newly created feature.
	 */
	public SubWellFeature createSubWellFeature(ProtocolClass pClass) {
		checkCanEditProtocolClass(pClass);
		SubWellFeature feature = new SubWellFeature();
		feature.setName("New feature");
		feature.setNumeric(true);
		feature.setFormatString("#.##");
		feature.setFeatureClasses(new ArrayList<FeatureClass>());
		feature.setProtocolClass(pClass);
		return feature;
	}

	/**
	 * Retrieve a subwell feature by its primary ID.
	 * 
	 * @param featureId The ID of the feature.
	 * @return The matching feature, or null if no match was found.
	 */
	public SubWellFeature getSubWellFeature(long featureId) {
		SubWellFeature f = getEntity(SubWellFeature.class, featureId);
		//TODO Causes a big performance issue, because this method may be called many times in succession.
		//if (!SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, f)) return null;
		return f;
	}

	/**
	 * Create a new feature class for classification features.
	 * Make sure to call {@link ProtocolService#updateProtocolClass(ProtocolClass)} afterwards.
	 * 
	 * @return The new class, not yet saved.
	 */
	public FeatureClass createFeatureClass() {
		FeatureClass fClass = new FeatureClass();
		fClass.setPattern("1");
		fClass.setPatternType("bit");
		fClass.setLabel("NewClass");
		fClass.setDescription("");
		fClass.setSymbol("Rectangle");
		return fClass;
	}

	/**
	 * Create a new image channel for a protocol class.
	 * Make sure to call {@link ProtocolService#updateProtocolClass(ProtocolClass)} afterwards.
	 * <p>
	 * <b>Important:</b> do not create or remove channels in a protocol class that already contains plate data.
	 * This will corrupt the plate's image data.
	 * </p>
	 * @param settings The settings for the new channel.
	 * @return The new image channel, not yet saved.
	 */
	public ImageChannel createChannel(ImageSettings settings) {
		ImageChannel channel = new ImageChannel();
		channel.setName("New Channel");
		channel.setSource(ImageChannel.CHANNEL_SOURCE_JP2K);
		channel.setBitDepth(ImageChannel.CHANNEL_BIT_DEPTH_16);
		channel.setColorMask(0xffffff);
		channel.setAlpha(0xff);
		channel.setLevelMax(0xffff);
		channel.setShowInPlateView(true);
		channel.setShowInWellView(true);
		channel.setChannelConfig(new HashMap<String,String>());
		return channel;
	}

	protected void fire(ModelEventType type, Object object, int status) {
		ModelEvent event = new ModelEvent(object, type, status);
		ModelEventService.getInstance().fireEvent(event);
	}

	@Override
	protected void afterSave(Object o) {
		//TODO If object is new, fire ObjectCreated instead.
		fire(ModelEventType.ObjectChanged, o, 0);
	}

	@Override
	protected void beforeDelete(Object o) {
		fire(ModelEventType.ObjectAboutToBeRemoved, o, 0);
	}

	@Override
	protected void afterDelete(Object o) {
		fire(ModelEventType.ObjectRemoved, o, 0);
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}
}