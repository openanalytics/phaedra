package eu.openanalytics.phaedra.model.subwell.data;

import eu.openanalytics.phaedra.base.environment.IEnvironment;

public class DataSourceFactory {

	public static ISubWellDataSource loadDataSource(IEnvironment env) {
		String db = env.getConfig().getValue(env.getName(), "db", "subwelldata");
		if (Boolean.valueOf(db)) {
			return new DBDataSource();
		} else {
			return new HDF5Datasource();
		}
	}
}
