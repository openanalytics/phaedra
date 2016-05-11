package eu.openanalytics.phaedra.base.environment.statuscheck;

import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.ui.trafficlight.AbstractStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;

public class UpdateSiteChecker extends AbstractStatusChecker {

	@Override
	public String getName() {
		return "Update Site";
	}

	@Override
	public String getDescription() {
		return "The Update Site contains the latest upgrades and addons"
				+ " for the application.";
	}

	@Override
	public char getIconLetter() {
		return 'U';
	}

	@Override
	public TrafficStatus poll() {
		long start = System.currentTimeMillis();
		try{
			BundleContext context = Activator.getDefault().getBundle().getBundleContext();
			ServiceReference<?> reference = context.getServiceReference(IProvisioningAgent.SERVICE_NAME);
			if (reference != null) {
				IProvisioningAgent agent = (IProvisioningAgent) context.getService(reference);
				IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
				URI[] uris = repoManager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
				if (uris.length == 0) {
					return new TrafficStatus(TrafficStatus.UNKNOWN, "No Update Site configured.");
				}
				for (URI uri : uris) {
					uri.toURL().openConnection().connect();
				}
				context.ungetService(reference);
			}
		} catch (Exception e) {
			return new TrafficStatus(TrafficStatus.DOWN, "Update Site error: " + e.getMessage());
		}
		long duration = System.currentTimeMillis() - start;
		return new TrafficStatus(TrafficStatus.UP, "Ok\nLatency: " + duration + "ms");
	}

}
