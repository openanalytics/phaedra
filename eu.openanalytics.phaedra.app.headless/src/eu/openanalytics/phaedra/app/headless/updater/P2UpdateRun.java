package eu.openanalytics.phaedra.app.headless.updater;

import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import eu.openanalytics.phaedra.app.headless.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * @see http://wiki.eclipse.org/Equinox/p2/Adding_Self-Update_to_an_RCP_Application
 */
public class P2UpdateRun implements Runnable {
	
	private IJobChangeListener provisionListener;
	
	public P2UpdateRun(IJobChangeListener provisionListener) {
		this.provisionListener = provisionListener;
	}

	@Override
	public void run() {
		//Enable stacktraces in threadpool
		try{
		BundleContext context = FrameworkUtil.getBundle(P2UpdateRun.class).getBundleContext();
		ServiceReference<?> reference = context.getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (reference == null) return;
		IProvisioningAgent agent = (IProvisioningAgent) context.getService(reference);
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		
		if (!reloadUpdateRepos(repoManager)) return;
		
		UpdateOperation updateOperation = createUpdateOperation(agent);
		
		if (hasUpdates(updateOperation)) {
			createProvisioningJob(updateOperation).schedule();
			EclipseLog.info("ProvisioningJob has been scheduled", Activator.getDefault());
		} else {
			EclipseLog.info("No updates have been found", Activator.getDefault());
		}
		
		context.ungetService(reference);	
		}catch(Exception e){e.printStackTrace();};
	}

	private ProvisioningJob createProvisioningJob(UpdateOperation updateOperation) {
		ProvisioningJob provisioningJob = updateOperation.getProvisioningJob(new NullProgressMonitor());
		provisioningJob.addJobChangeListener(provisionListener);
		return provisioningJob;
	}

	private boolean hasUpdates(UpdateOperation updateOperation) {
		IStatus result = updateOperation.resolveModal(new NullProgressMonitor());
		return result.getCode() != UpdateOperation.STATUS_NOTHING_TO_UPDATE;
	}

	private UpdateOperation createUpdateOperation(IProvisioningAgent agent) {
		ProvisioningSession session = new ProvisioningSession(agent);
		UpdateOperation update = new UpdateOperation(session);
		return update;
	}

	private boolean reloadUpdateRepos(IMetadataRepositoryManager repoManager) {
		try {
			URI[] uris = repoManager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
			for (URI uri : uris) {
				repoManager.removeRepository(uri);
				repoManager.addRepository(uri);
				repoManager.loadRepository(uri, new NullProgressMonitor());
			}
			return true;
		} catch (ProvisionException | OperationCanceledException e) {
			return false;
		}
	}
}
