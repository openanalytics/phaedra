package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;

import eu.openanalytics.phaedra.base.ui.util.misc.CustomComboBoxCellEditor;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.dialog.CreateAnnotationDialog;

public class AnnotationHelper {

	public static final String ANN_MULTIPLE_VALUES = "<Multiple Values>";
	
	public static TableViewer createTableViewer(Composite parent, Function<Feature, String> annotationGetter,
			BiConsumer<Feature, String> annotationSetter) {
		
		TableViewer annotationTableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		annotationTableViewer.setContentProvider(new ArrayContentProvider());
		annotationTableViewer.setLabelProvider(new LabelProvider());
		
		Table table = annotationTableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		TableViewerColumn tvc = new TableViewerColumn(annotationTableViewer, SWT.NONE);
		tvc.getColumn().setText("Name");
		tvc.getColumn().setWidth(150);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Feature) element).getName();
			};
		});
		
		tvc = new TableViewerColumn(annotationTableViewer, SWT.NONE);
		tvc.getColumn().setText("Values");
		tvc.getColumn().setWidth(250);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return annotationGetter.apply((Feature) element);
			};
		});
		tvc.setEditingSupport(new EditingSupport(annotationTableViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
	
			@Override
			protected CellEditor getCellEditor(Object element) {
				Composite parent = ((TableViewer)getViewer()).getTable();
				if (((Feature) element).isClassificationRestricted()) {
					return new CustomComboBoxCellEditor(parent, getComboItems(element));
				}
				return new TextCellEditor(parent);
			}
	
			@Override
			protected Object getValue(Object element) {
				String value = annotationGetter.apply((Feature) element);
				if (((Feature) element).isClassificationRestricted()) {
					return Arrays.binarySearch(getComboItems(element), value);
				}
				return value;
			}
	
			@Override
			protected void setValue(Object element, Object value) {
				if (ANN_MULTIPLE_VALUES.equals(value)) return;
				if (((Feature) element).isClassificationRestricted() && value instanceof Integer) {
					int index = (Integer) value;
					if (index == -1) return;
					value = getComboItems(element)[index];
				}
				annotationSetter.accept((Feature) element, String.valueOf(value));
				annotationTableViewer.update(element, null);
			}
			
			private String[] getComboItems(Object element) {
				String[] items = null;
				if (((Feature) element).isClassificationRestricted()) {
					items = ProtocolService.streamableList(((Feature) element).getFeatureClasses()).stream()
							.map(fc -> fc.getPattern()).toArray(i -> new String[i]);
				}
				return items;
			}
		});
		
		return annotationTableViewer;
	}

	public static Link createAnnotationLink(Composite parent, Supplier<ProtocolClass> protocolClassGetter, Runnable okCallback) {
		Link link = new Link(parent, SWT.NONE);
		link.setText("<a>Create New Annotation</a>");
		link.addListener(SWT.Selection, e -> {
			CreateAnnotationDialog dialog = new CreateAnnotationDialog(Display.getCurrent().getActiveShell(), "New Annotation", protocolClassGetter.get(), null);
			int retCode = dialog.open();
			if (retCode == Window.OK) okCallback.run();
		});
		return link;
	}
}
