/*=============================================================================#
 # Copyright (c) 2010-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package eu.openanalytics.phaedra.base.r.rservi.setup;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.osgi.framework.Bundle;

import eu.openanalytics.phaedra.base.r.rservi.Activator;


public class RSetupUtil {
	
	
	public static final String EXTENSION_POINT_ID = "org.eclipse.statet.rj.RSetups"; //$NON-NLS-1$
	
	private static final ILog LOGGER = Platform.getLog(Platform.getBundle(Activator.PLUGIN_ID));
	
	
	private static final String SETUP_ELEMENT_NAME = "setup"; //$NON-NLS-1$
	private static final String BASE_ELEMENT_NAME = "base"; //$NON-NLS-1$
	private static final String LIBRARY_ELEMENT_NAME = "library"; //$NON-NLS-1$
	
	private static final String ID_ATTRIBUTE_NAME = "id"; //$NON-NLS-1$
	private static final String SETUP_ID_ATTRIBUTE_NAME = "setupId"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String INHERIT_BASE_ATTRIBUTE_NAME = "inheritBase"; //$NON-NLS-1$
	private static final String LOCATION_ATTRIBUTE_NAME = "location"; //$NON-NLS-1$
	private static final String GROUP_ATTRIBUTE_NAME = "group"; //$NON-NLS-1$
	
	
	/**
	 * Loads all available R setups for the current or the specified platform.
	 * 
	 * A filter map specifying a platform contains the arguments like <code>$os$</code> and 
	 * <code>$arch$</code> as keys; its values must be set to the known constant of the desired.
	 * platform.
	 * 
	 * @param filter a platform filter or <code>null</code> for the current platform
	 * @return a list with available R setups
	 */
	public static List<RSetup> loadAvailableSetups(final Map<String, String> filter) {
		final Set<String> ids = new HashSet<String>();
		final List<RSetup> setups = new ArrayList<RSetup>();
		final IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
		for (int i = 0; i < configurationElements.length; i++) {
			if (configurationElements[i].getName().equals(SETUP_ELEMENT_NAME)) {
				final String id = configurationElements[i].getAttribute(ID_ATTRIBUTE_NAME);
				if (id != null && id.length() > 0 && !ids.contains(id)) {
					ids.add(id);
					final RSetup setup = loadSetup(id, filter, configurationElements);
					if (setup != null) {
						setups.add(setup);
					}
				}
			}
		}
		return setups;
	}
	
	/**
	 * Loads the R setup with the specified id for the current or the specified platform.
	 * 
	 * A filter map specifying a platform contains the arguments like <code>$os$</code> and 
	 * <code>$arch$</code> as keys; its values must be set to the known constant of the desired.
	 * platform.
	 * 
	 * @param id the id of the R setup
	 * @param filter a platform filter or <code>null</code> for the current platform
	 * @return the R setup or <code>null</code> if loading failed
	 */
	public static RSetup loadSetup(final String id, final Map<String, String> filter) {
		final IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
		return loadSetup(id, filter, configurationElements);
	}
	
	private static RSetup loadSetup(final String id, final Map<String, String> filter,
			final IConfigurationElement[] configurationElements) {
		try {
			for (int i = 0; i < configurationElements.length; i++) {
				final IConfigurationElement element = configurationElements[i];
				if (element.getName().equals(SETUP_ELEMENT_NAME)
						&& id.equals(element.getAttribute(ID_ATTRIBUTE_NAME)) ) {
					final RSetup setup = new RSetup(id);
					if (filter != null && filter.containsKey("$os$")) { //$NON-NLS-1$
						setup.setOS(filter.get("$os$"), filter.get("$arch$")); //$NON-NLS-1$ //$NON-NLS-2$
					}
					else {
						setup.setOS(Platform.getOS(), Platform.getOSArch());
					}
					setup.setName(element.getAttribute(NAME_ATTRIBUTE_NAME));
					loadSetupBase(id, filter, configurationElements, setup);
					if (setup.getRHome() == null) {
						log(IStatus.WARNING, "Incomplete R setup: Missing R home for setup '" + id + "', the setup is ignored.", element);
						return null;
					}
					loadSetupLibraries(id, filter, configurationElements, setup);
					return setup;
				}
			}
		}
		catch (final Exception e) {
			LOGGER.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error in R setup: Failed to load setup.", e));
		}
		return null;
	}
	
	private static void loadSetupBase(final String id, final Map<String, String> filter,
			final IConfigurationElement[] configurationElements, final RSetup setup)
			throws Exception {
		for (int i = 0; i < configurationElements.length; i++) {
			final IConfigurationElement element = configurationElements[i];
			if (element.getName().equals(BASE_ELEMENT_NAME)
					&& id.equals(element.getAttribute(SETUP_ID_ATTRIBUTE_NAME)) ) {
				final String path = getLocation(element, filter);
				setup.setRHome(path);
				return;
			}
		}
		for (int i = 0; i < configurationElements.length; i++) {
			final IConfigurationElement element = configurationElements[i];
			if (element.equals(SETUP_ELEMENT_NAME)
					&& id.equals(element.getAttribute(SETUP_ID_ATTRIBUTE_NAME)) ) {
				final String inheritId = element.getAttribute(INHERIT_BASE_ATTRIBUTE_NAME);
				if (inheritId != null) {
					loadSetupBase(inheritId, filter, configurationElements, setup);
				}
				return;
			}
		}
	}
	
	private static void loadSetupLibraries(final String id, final Map<String, String> filter,
			final IConfigurationElement[] configurationElements, final RSetup setup)
			throws Exception {
		for (int i = 0; i < configurationElements.length; i++) {
			final IConfigurationElement element = configurationElements[i];
			if (element.getName().equals(LIBRARY_ELEMENT_NAME)
					&& id.equals(element.getAttribute(SETUP_ID_ATTRIBUTE_NAME)) ) {
				final String path = getLocation(element, filter);
				if (path == null) {
					continue;
				}
				final String groupId = element.getAttribute(GROUP_ATTRIBUTE_NAME);
				if (groupId == null || groupId.length() == 0 || groupId.equals("R_LIBS")) { //$NON-NLS-1$
					setup.getRLibs().add(path);
				}
				else if (groupId.equals("R_LIBS_SITE")) { //$NON-NLS-1$
					setup.getRLibsSite().add(path);
				}
				else if (groupId.equals("R_LIBS_USER")) { //$NON-NLS-1$
					setup.getRLibsUser().add(path);
				}
				else {
					log(IStatus.WARNING, "Invalid R setup element: " +
							"Unknown library group '" + groupId + "', the library is ignored", element);
				}
			}
		}
	}
	
	private static String getLocation(final IConfigurationElement element, final Map<String, String> filter) throws Exception {
		final Bundle bundle = Platform.getBundle(element.getContributor().getName());
		final String path = element.getAttribute(LOCATION_ATTRIBUTE_NAME);
		if (bundle != null && path != null && path.length() > 0) {
			final URL[] bundleURLs = FileLocator.findEntries(bundle, new Path(path), filter);
			for (int i = 0; i < bundleURLs.length; i++) {
				final URI fileURI = URIUtil.toURI(FileLocator.toFileURL(bundleURLs[i]));
				if (fileURI.getScheme().equals("file")) { //$NON-NLS-1$
					final File file = new File(fileURI);
					if (file.exists() && file.isDirectory() && file.list().length > 0) {
						return file.getAbsolutePath();
					}
				}
			}
		}
		
		return null;
	}
	
	private static void log(final int severity, final String message, final IConfigurationElement element) {
		final StringBuilder info = new StringBuilder(message);
		if (element != null) {
			info.append(" (contributed by '");
			info.append(element.getContributor().getName());
			info.append("').");
		}
		LOGGER.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, info.toString()));
	}
	
}
