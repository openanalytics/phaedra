package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import eu.openanalytics.phaedra.base.ui.richtableviewer.Activator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;


public abstract class CustomColumnSupport {
	
	// key = custom:<scope>#<nr>$<type>
	// e.g.  custom:user#000000A1$formula
	
	static final String CUSTOM_KEY_PREFIX = "custom:";
	private static final String CUSTOM_USER_KEY_PREFIX= CUSTOM_KEY_PREFIX + "user#";
	
	protected static final ColumnConfiguration getConfig(final TableColumn column) {
		return (ColumnConfiguration)column.getData();
	}
	
	
	public CustomColumnSupport() {
	}
	
	
	public boolean isSupported(final ColumnConfiguration config) {
		final String key;
		return (config != null && (key = config.getKey()) != null
				&& key.length() >= 16 && key.startsWith(CUSTOM_KEY_PREFIX) );
	}
	
	protected String getType(final String key) {
		final int index = key.indexOf('$');
		if (index < 0) {
			throw new IllegalArgumentException();
		}
		return key.substring(index + 1);
	}
	
	public abstract String getDefaultType();
	
	public void applyCustomData(final ColumnConfiguration config) {
		try {
			applyCustomData(config, checkCustomData(config));
		}
		catch (final Exception e) {
			EclipseLog.error(String.format("An error occurred when applying custom configuration for column '%1$s'.", config.getName()),
					e, Activator.getDefault() );
			config.setDataDescription(null);
			config.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(final Object element) {
					return "<ERROR>";
				}
			});
		}
	}
	
	protected Map<String, Object> checkCustomData(final ColumnConfiguration config) {
		Map<String, Object> customData = config.getCustomData();
		if (customData == null) {
			customData = new HashMap<>();
			config.setCustomData(customData);
		}
		return customData;
	}
	
	protected abstract void applyCustomData(final ColumnConfiguration config, final Map<String, Object> customData);
	
	protected abstract EditCustomColumnDialog createEditDialog(final ColumnConfiguration config, final String type,
			final Shell shell);
	
	
	protected ColumnConfiguration createConfig(final RichTableViewer viewer, final String type,
			final Shell shell) {
		final ColumnConfiguration config = new ColumnConfiguration(null, "");
		checkCustomData(config);
		final EditCustomColumnDialog dialog = createEditDialog(config, type, shell);
		if (dialog.open() != Dialog.OK) {
			return null;
		}
		config.setKey(createNewKey(viewer, config, CUSTOM_USER_KEY_PREFIX, type));
		applyCustomData(config);
		return config;
	}
	
	private String createNewKey(final RichTableViewer viewer, final ColumnConfiguration config,
			final String prefix, final String type) {
		final int idStart = prefix.length();
		final int idEnd = idStart + 8;
		int maxNr = -1;
		for (final TableColumn column : viewer.getTable().getColumns()) {
			final String key = getConfig(column).getKey();
			if (key.length() > idEnd && key.startsWith(prefix)) {
				try {
					final int nr = Integer.parseUnsignedInt(key.substring(idStart, idEnd), 16);
					if (nr > maxNr) {
						maxNr = nr;
					}
				} catch (final NumberFormatException e) {}
			}
		}
		return String.format("%1$s%2$08X$%3$s", prefix, maxNr + 1, type);
	}
	
	
	public boolean canAddColumn() {
		return true;
	}
	
	public TableColumn addColumn(final RichTableViewer viewer, final String type,
			final Shell shell) {
		final ColumnConfiguration config = createConfig(viewer, type, shell);
		if (config == null) {
			return null;
		}
		final TableColumn column = viewer.addColumn(config);
		return column;
	}
	
	public boolean canEditColumn(final TableColumn column) {
		return (column != null && isSupported(getConfig(column)));
	}
	
	public boolean editColumn(final RichTableViewer viewer, final TableColumn column,
			final Shell shell) {
		if (!canEditColumn(column)) {
			return false;
		}
		final ColumnConfiguration config = getConfig(column);
		final ColumnConfiguration workingCopy = new ColumnConfiguration(config);
		final EditCustomColumnDialog dialog = createEditDialog(workingCopy, getType(config.getKey()), shell);
		if (dialog.open() != Dialog.OK) {
			return false;
		}
		config.load(workingCopy);
		applyCustomData(config);
		viewer.updateColumn(column);
		return true;
	}
	
	public boolean canDelete(final TableColumn column) {
		return (column != null && isSupported(getConfig(column)));
	}
	
	public boolean deleteColumn(final RichTableViewer viewer, final TableColumn column,
			final Shell shell) {
		if (!canDelete(column)) {
			return false;
		}
		if (!MessageDialog.open(MessageDialog.CONFIRM, shell, "Delete Column",
				String.format("Delete the table column '%s' ?", column.getText()), SWT.NONE /*,
				"Delete", IDialogConstants.CANCEL_LABEL */ )) {
			return false;
		}
		
		viewer.deleteColumn(column);
		return true;
	}
	
}
