package eu.openanalytics.phaedra.model.subwell.data;

import eu.openanalytics.phaedra.base.environment.IEnvironment;

public class DataSourceFactory {

	public static ISubWellDataSource loadDataSource(IEnvironment env) {
		String swDataURL = env.getConfig().getValue(env.getName(), "sw-db", "url");
		if (swDataURL == null || swDataURL.isEmpty()) {
			return new HDF5Datasource();
		} else {
			String username = env.getConfig().getValue(env.getName(), "sw-db", "user");
			String password = env.getConfig().getValue(env.getName(), "sw-db", "password");
			return new DBDataSource(swDataURL, username, password);
		}
	}
}
