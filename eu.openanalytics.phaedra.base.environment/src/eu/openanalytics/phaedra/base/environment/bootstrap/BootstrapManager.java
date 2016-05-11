package eu.openanalytics.phaedra.base.environment.bootstrap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class BootstrapManager {

	public static void bootstrap(IEnvironment env) throws BootstrapException {
		// Check existence of the PHAEDRA schema.
		try (Connection conn = env.getJDBCConnection()) {
			try (ResultSet res = conn.getMetaData().getSchemas()) {
				while (res.next()) {
					if (res.getString(1).equalsIgnoreCase("PHAEDRA")) return;
				}
			}
		} catch (SQLException e) {
			throw new BootstrapException("Failed to connect to database", e);
		}
		
		if (!JDBCUtils.isEmbedded()) throw new BootstrapException(
				"This environment has not been set up correctly.\n"
				+ "Only embedded environments can be bootstrapped. For distributed environments, contact your Phaedra administrator.");
		
		// Run all registered bootstrappers in order.
		for (IBootstrapper bootstrapper: loadBootstrappers()) {
			EclipseLog.info("Running bootstrapper: " + bootstrapper.getClass().getName(), Activator.getDefault());
			bootstrapper.bootstrap(env);
		}
	}
	
	private static List<IBootstrapper> loadBootstrappers() {
		List<OrderedBootstrapper> bootstrappers = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IBootstrapper.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				IBootstrapper bootstrapper = (IBootstrapper) el.createExecutableExtension(IBootstrapper.ATTR_CLASS);
				int seq = 10;
				try {
					seq = Integer.parseInt(el.getAttribute(IBootstrapper.ATTR_SEQUENCE));
				} catch (Exception e) {}
				bootstrappers.add(new OrderedBootstrapper(seq, bootstrapper));
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
		return bootstrappers.stream().sorted((o1,o2) -> o1.sequence - o2.sequence).map(b -> b.bootstrapper).collect(Collectors.toList());
	}
	
	private static class OrderedBootstrapper {
		
		public int sequence;
		public IBootstrapper bootstrapper;
		
		public OrderedBootstrapper(int sequence, IBootstrapper bootstrapper) {
			this.sequence = sequence;
			this.bootstrapper = bootstrapper;
		}
	}
}
