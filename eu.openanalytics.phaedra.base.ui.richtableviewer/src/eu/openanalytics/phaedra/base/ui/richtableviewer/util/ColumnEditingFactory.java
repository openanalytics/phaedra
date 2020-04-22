package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnEditingConfiguration;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class ColumnEditingFactory {
	
	private static boolean columnEditingEnabled;

	public static ColumnEditingConfiguration create(String getterName, String setterName, Consumer<Object> saver, Function<Object, Boolean> editableChecker) {
		ColumnEditingConfiguration cfg = new ColumnEditingConfiguration();
		cfg.valueGetter = (o) -> {
			Object value = ReflectionUtils.invoke(getterName, o);
			if (value == null) return "";
			return value.toString();
		};
		cfg.valueSetter = (o, v) -> {
			ReflectionUtils.invoke(setterName, o, v);
			saver.accept(o);
		};
		cfg.editableChecker = editableChecker;
		return cfg;
	}
	
	public static ColumnEditingConfiguration create(
			Function<Object, Object> valueGetter,
			BiConsumer<Object, Object> valueSetter,
			Function<Object, Boolean> editableChecker) {
		
		ColumnEditingConfiguration cfg = new ColumnEditingConfiguration();
		cfg.valueGetter = valueGetter;
		cfg.valueSetter = valueSetter;
		cfg.editableChecker = editableChecker;
		return cfg;
	}
	
	public static void apply(TableViewerColumn tvc, ColumnEditingConfiguration cfg) {
		if (cfg == null) {
			tvc.setEditingSupport(null);
			return;
		}
		EditingSupport support = new TextEditingSupport(tvc.getViewer(), cfg);
		tvc.setEditingSupport(support);
	}
	
	public static void toggleColumnEditing(boolean enabled) {
		columnEditingEnabled = enabled;
	}
	
	public static class TextEditingSupport extends EditingSupport {

		private final ColumnViewer viewer;
		private ColumnEditingConfiguration editingConfig;
		
		public TextEditingSupport(ColumnViewer viewer, ColumnEditingConfiguration editingConfig) {
			super(viewer);
			this.viewer = viewer;
			this.editingConfig = editingConfig;
		}

		@Override
		protected boolean canEdit(Object element) {
			boolean canEdit = true;
			if (editingConfig.editableChecker != null) canEdit = editingConfig.editableChecker.apply(element);
			return canEdit && columnEditingEnabled;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer)getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			Object value = editingConfig.valueGetter.apply(element);
			if (value == null) return "";
			return value;
		}

		@Override
		protected void setValue(Object element, Object value) {
			String currentValue = editingConfig.valueGetter.apply(element).toString();
			String newValue = (String) value;
			if ((currentValue == null || currentValue.isEmpty()) && (newValue == null || newValue.isEmpty())) return;
			if (currentValue != null && currentValue.equals(newValue)) return;
			editingConfig.valueSetter.accept(element, value);
			viewer.update(element, null);
		}
	}
}
