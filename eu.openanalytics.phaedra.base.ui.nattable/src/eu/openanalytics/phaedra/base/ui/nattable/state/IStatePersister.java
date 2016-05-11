package eu.openanalytics.phaedra.base.ui.nattable.state;

import java.io.IOException;
import java.util.Properties;

public interface IStatePersister {

	public void save(String key, Properties props) throws IOException;
	
	public boolean load(String key, Properties props) throws IOException;
}
