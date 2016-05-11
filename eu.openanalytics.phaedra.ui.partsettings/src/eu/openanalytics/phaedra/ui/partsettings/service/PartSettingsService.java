package eu.openanalytics.phaedra.ui.partsettings.service;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.Activator;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;

public class PartSettingsService extends BaseJPAService {

	private static PartSettingsService instance = new PartSettingsService();

	private PartSettingsService() {
		// Hidden constructor.
	}

	public static PartSettingsService getInstance() {
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

	public PartSettings createPartSettings(Protocol protocol, String className, Properties properties) {
		SecurityService.getInstance().checkWithException(Roles.USER, protocol);
		PartSettings settings = new PartSettings();
		settings.setUserName(getCurrentUserName());
		settings.setName("New Settings");
		settings.setProtocol(protocol);
		settings.setClassName(className);
		settings.setProperties(properties);
		return settings;
	}

	public PartSettings createPartSettings(IWorkbenchPart part, String name) {
		SettingsDecorator decorator = getDecorator(part);
		if (decorator == null) return null;
		return decorator.savePartSettings(name);
	}
	
	public void updatePartSettings(PartSettings partSettings) {
		checkCanModify(partSettings);
		save(partSettings);
	}

	public void deletePartSettings(PartSettings partSettings) {
		checkCanModify(partSettings);
		delete(partSettings);
	}

	public void applySettings(IWorkbenchPart part, PartSettings partSettings) {
		SettingsDecorator decorator = getDecorator(part);
		if (decorator != null) decorator.loadPartSettings(Optional.of(partSettings));
	}
	
	/**
	 * <p>Returns a list of part settings for the specified class name and protocol for the current user.
	 * Global part settings for the specified class name are also included.</p>
	 *
	 * <p>This does not include part settings marked as template.</p>
	 */
	public List<PartSettings> getPartSettings(Protocol protocol, String className) {
		SecurityService.getInstance().checkWithException(Roles.USER, protocol);
		String userName = getCurrentUserName();
		String jpql = "select s from PartSettings s where s.className = ?1 and s.protocol = ?2 and s.userName = ?3 and s.isTemplate = ?4";
		return getPartSettings(jpql, className, protocol, userName, false);
	}

	/**
	 * <p>Returns a list of part settings for the specified class name and protocol that are marked as template.
	 * Global part settings for the specified class name are also included.</p>
	 */
	public List<PartSettings> getPartSettingsTemplates(Protocol protocol, String className) {
		SecurityService.getInstance().checkWithException(Roles.USER, protocol);
		String jpql = "select s from PartSettings s where s.className = ?1 and s.protocol = ?2 and s.isTemplate = ?3";
		return getPartSettings(jpql, className, protocol, true);
	}
	
	/*
	 * **********
	 * Non-public
	 * **********
	 */
	
	private List<PartSettings> getPartSettings(String jpql, Object... args) {
		List<PartSettings> results = getList(jpql, PartSettings.class, args);
		// Fix: EclipseLink doesn't generate a deep clone on the property map.
		// This means it won't detect changes to objects in the property map.
		for (PartSettings result: results) {
			try {
				result.setProperties(result.getProperties().deepClone());
			} catch (RuntimeException e) {
				String msg = "Failed to clone properties of " + result.getName() + ". Not all properties may be saved correctly.";
				EclipseLog.warn(msg, Activator.getDefault());
			}
		}
		return results;
	}

	private void checkCanModify(PartSettings partSettings) {
		if (partSettings.getUserName().equalsIgnoreCase(getCurrentUserName())) return;
		SecurityService.getInstance().checkWithException(Roles.ADMINISTRATOR, partSettings);
	}

	private String getCurrentUserName() {
		return SecurityService.getInstance().getCurrentUserName();
	}

	private SettingsDecorator getDecorator(IWorkbenchPart part) {
		return Optional.of(part)
			.filter(p -> p instanceof IDecoratedPart)
			.map(p -> (IDecoratedPart)p)
			.map(dp -> dp.hasDecorator(SettingsDecorator.class))
			.orElse(null);
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
