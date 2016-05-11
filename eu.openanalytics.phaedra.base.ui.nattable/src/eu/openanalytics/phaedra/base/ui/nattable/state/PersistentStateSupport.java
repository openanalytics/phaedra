package eu.openanalytics.phaedra.base.ui.nattable.state;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.swt.SWT;

import eu.openanalytics.phaedra.base.ui.nattable.Activator;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IAsyncColumnAccessor;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class PersistentStateSupport {

	private NatTable table;
	private String key;
	private IStatePersister persister;

	public PersistentStateSupport(String key, IStatePersister persister) {
		this.key = key;
		this.persister = persister;
	}

	public void apply(NatTable table, IColumnPropertyAccessor<?> columnAccessor) {
		this.table = table;

		table.getParent().addListener(SWT.Dispose, e -> saveColumnState());

		if (columnAccessor instanceof IAsyncColumnAccessor) ((IAsyncColumnAccessor<?>) columnAccessor).setAsync(false);
		loadColumnState();
		if (columnAccessor instanceof IAsyncColumnAccessor) ((IAsyncColumnAccessor<?>) columnAccessor).setAsync(true);
	}

	/*
	 * Non-public
	 * **********
	 */

	private void saveColumnState() {
		Properties properties = new Properties();
		table.saveState("", properties);
		try {
			persister.save(key, properties);
		} catch (IOException e) {
			EclipseLog.warn("Failed to save state for NatTable " + key, e, Activator.getDefault());
		}
	}

	private void loadColumnState() {
		try {
			Properties properties = new Properties();
			if (persister.load(key, properties)) table.loadState("", properties);
		} catch (IOException e) {
			EclipseLog.warn("Failed to load state for NatTable " + key, e, Activator.getDefault());
		}
	}
}
