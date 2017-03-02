package eu.openanalytics.phaedra.base.util.misc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import eu.openanalytics.phaedra.base.util.Activator;

public class VersionUtils {

	public static final String UNKNOWN = "Unknown";

	public static void init(BundleContext context) {
		// Do nothing.
	}

	public static String getPhaedraVersion() {
		return getVersion("eu.openanalytics.phaedra.feature.group");
	}

	public static String getVersion(String iuName) {
		BundleContext ctx = Activator.getDefault().getBundle().getBundleContext();
		ServiceReference<?> reference = ctx.getServiceReference(IProvisioningAgent.class.getName());
		IProvisioningAgent agent = (IProvisioningAgent)ctx.getService(reference);

		try {

			IProfileRegistry profileReg = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
			IProfile profile = profileReg.getProfiles()[0];

			IQuery<IInstallableUnit> query = QueryUtil.createLatestIUQuery();
			IQueryResult<IInstallableUnit> result = profile.available(query, null);

			if (result != null) {
				Iterator<IInstallableUnit> it = result.iterator();
				while (it.hasNext()) {
					IInstallableUnit iu = it.next();
					if (iu.getId().equals(iuName)) {
						return iu.getVersion().toString();
					}
				}
			}
		} catch (Exception e) {
			// If run from IDE, queryable will be null because there
			// is no metadata repository.
		}
		return UNKNOWN;
	}

	public static String getVersionData(String version) {
		String regex = "\\d\\.\\d\\.\\d\\.\\d*";
		String lcVersion = version.toLowerCase();
		if (lcVersion.equals("unknown")) {
			// Unknown version, use todays date.
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
			return dateFormat.format(new Date());
		} else if (lcVersion.matches(regex)) {
			// A 2.5 or later release.
			return version.substring(6);
		} else if (lcVersion.contains("dev")) {
			// A Dev version.
			return version.substring(9);
		} else {
			// A pre 2.5 release.
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
			return dateFormat.format(new Date(0));
		}
	}

	public static boolean isVersionUnknown(String version) {
		return UNKNOWN.equalsIgnoreCase(version);
	}
}
