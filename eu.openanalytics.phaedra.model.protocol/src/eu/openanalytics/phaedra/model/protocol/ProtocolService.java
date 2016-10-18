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
 * This service controls the creation, retrieval and modification of Protocols and their related objects:
 * Protocol Classes, Features, Feature Groups.
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

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	/**
	 * Get an alphabetical list of all available well types (a.k.a. well roles).
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

	/* Feature groups
	 * **************
	 */

	/**
	 * Get an alphabetical list of all feature groups (default and custom groups) for
	 * a given protocol class and group type (well or subwell).
	 */
	public List<FeatureGroup> getAllFeatureGroups(ProtocolClass pClass, GroupType groupType) {
		return featureGroupManager.getGroups(pClass, groupType, true);
	}
	
	/**
	 * Get an alphabetical list of the custom feature groups for
	 * a given protocol class and group type (well or subwell).
	 */
	public List<FeatureGroup> getCustomFeatureGroups(ProtocolClass pClass, GroupType groupType) {
		return featureGroupManager.getGroups(pClass, groupType, false);
	}
	
	public FeatureGroup getFeatureGroup(ProtocolClass pClass, GroupType groupType, String name) {
		return featureGroupManager.getGroup(pClass, groupType, name);
	}
	
	public boolean isMember(IFeature feature, FeatureGroup group) {
		return featureGroupManager.isMember(feature, group);
	}

	public <F extends IFeature> List<F> getMembers(FeatureGroup group) {
		return featureGroupManager.getMembers(group);
	}

	public FeatureGroup createFeatureGroup(ProtocolClass pClass, GroupType type, String name) {
		FeatureGroup fGroup = new FeatureGroup();
		fGroup.setName(name);
		fGroup.setDescription("");
		fGroup.setProtocolClass(pClass);
		fGroup.setType(type.getType());
		return fGroup;
	}

	/* Protocol classes
	 * ****************
	 */

	/**
	 * Get a list of all protocol classes that are visible to the current user.
	 */
	public List<ProtocolClass> getProtocolClasses() {
		return streamableList(getList(ProtocolClass.class)).stream()
				.filter(pc -> SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, pc))
				.collect(Collectors.toList());
	}

	/**
	 * Get the protocol class with the given id, if the current user has access to it.
	 */
	public ProtocolClass getProtocolClass(long protocolClassId) {
		ProtocolClass pc = getEntityManager().find(ProtocolClass.class, protocolClassId);
		if (!SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, pc)) return null;
		return pc;
	}

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
	
	public String getDCConfigFile(String configId) {
		//TODO This path is an implementation detail of the DC bundles, see eu.openanalytics.phaedra.datacapture.module.ModuleFactory
		String path = "/data.capture.configurations/%s.xml";
		return String.format(path, configId);
	}
	
	public void updateProtocolClass(ProtocolClass protocolClass) {
		checkCanEditProtocolClass(protocolClass);
		validateProtocolClass(protocolClass);
		save(protocolClass);
	}

	public void deleteProtocolClass(ProtocolClass protocolClass) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOLCLASS_DELETE, protocolClass);
		delete(protocolClass);
	}

	public void checkCanEditProtocolClass(ProtocolClass protocolClass) {
		if (!canEditProtocolClass(protocolClass)) throw new PermissionDeniedException("No permission to update Protocol Class");
	}
	
	public boolean canEditProtocolClass(ProtocolClass protocolClass) {
		return canEditProtocolClass(SecurityService.getInstance().getCurrentUserName(), protocolClass);
	}

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

	public ProtocolClassSummary getProtocolClassSummary(ProtocolClass pClass) {
		ProtocolClassSummary summary = new ProtocolClassSummary();

		String sql = "select"
				+ " (select count(protocol_id) from phaedra.hca_protocol where protocolclass_id = ?) protocols,"
				+ " (select count(feature_id) from phaedra.hca_feature where protocolclass_id = ?) features,"
				+ " (select count(subwellfeature_id) from phaedra.hca_subwellfeature where protocolclass_id = ?) swFeatures,"
				+ " (select count(image_channel_id) from phaedra.hca_image_channel where image_setting_id = "
				+ " 	(select image_setting_id from phaedra.hca_protocolclass where protocolclass_id = ?)) imageChannels "
				+ " from dual";
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

	/* Protocols
	 * *********
	 */

	/**
	 * Get a list of all protocols that are visible to the current user.
	 */
	public List<Protocol> getProtocols() {
		return streamableList(getList(Protocol.class)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Get a list of all protocols in the specified protocol class that are visible to the current user.
	 */
	public List<Protocol> getProtocols(ProtocolClass pClass) {
		String query = "select p from Protocol p where p.protocolClass = ?1";
		return streamableList(getList(query, Protocol.class, pClass)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p))
				.collect(Collectors.toList());
	}

	/**
	 * Get the protocol with the given id, if the current user has access to it.
	 */
	public Protocol getProtocol(long protocolId) {
		Protocol p = getEntityManager().find(Protocol.class, protocolId);
		if (!SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p)) return null;
		return p;
	}

	public List<Protocol> getProtocolsByName(String protocolName) {
		String query = "select p from Protocol p where p.name = ?1";
		return streamableList(getList(query, Protocol.class, protocolName)).stream()
				.filter(p -> SecurityService.getInstance().check(Permissions.PROTOCOL_OPEN, p))
				.collect(Collectors.toList());
	}

	public Protocol createProtocol(ProtocolClass protocolClass) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOL_CREATE, protocolClass);
		Protocol p = new Protocol();
		p.setProtocolClass(protocolClass);
		p.setImageSettings(new ImageSettings());
		p.getImageSettings().setImageChannels(new ArrayList<ImageChannel>());
		p.setName("New Protocol");
		return p;
	}

	public void updateProtocol(Protocol protocol) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOL_EDIT, protocol);
		save(protocol);
	}

	public void deleteProtocol(Protocol protocol) {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOL_DELETE, protocol);
		delete(protocol);
	}

	/* Features
	 * ********
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

	public Feature getFeature(long featureId) {
		Feature f = getEntity(Feature.class, featureId);
		if (!SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, f)) return null;
		return f;
	}

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

	public SubWellFeature getSubWellFeature(long featureId) {
		SubWellFeature f = getEntity(SubWellFeature.class, featureId);
		//TODO Causes a big performance issue, because this method may be called many times in succession.
		//if (!SecurityService.getInstance().check(Permissions.PROTOCOLCLASS_OPEN, f)) return null;
		return f;
	}

	public FeatureClass createFeatureClass() {
		FeatureClass fClass = new FeatureClass();
		fClass.setPattern("1");
		fClass.setPatternType("bit");
		fClass.setLabel("NewClass");
		fClass.setDescription("");
		fClass.setSymbol("Rectangle");
		return fClass;
	}

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

	/*
	 * **************
	 * Event handling
	 * **************
	 */

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

}