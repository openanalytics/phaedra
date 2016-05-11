package eu.openanalytics.phaedra.ui.link.importer.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.misc.HyperlinkLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ParameterGroup;
import eu.openanalytics.phaedra.ui.link.importer.Activator;

public class CustomParameterUI {

	private RichTableViewer customParameterViewer;
	
	private Map<String, String> customParameters;
	private ParameterGroup defaultParameters;
	private String currentCaptureConfigId;
	
	public CustomParameterUI() {
		customParameters = new HashMap<>();
		defaultParameters = new ParameterGroup();
	}
	
	public RichTableViewer create(Composite parent) {
		customParameterViewer = new RichTableViewer(parent, SWT.BORDER);
		customParameterViewer.setContentProvider(new ArrayContentProvider());
		
		TableViewerColumn col = new TableViewerColumn(customParameterViewer, SWT.NONE);
		col.getColumn().setText("Parameter");
		col.getColumn().setWidth(150);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(cell.getElement().toString());
			}
		});
		
		col = new TableViewerColumn(customParameterViewer, SWT.NONE);
		col.getColumn().setText("Value");
		col.getColumn().setWidth(400);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(getParameterValue(cell.getElement().toString()));
			}
		});
		col.setEditingSupport(new ParameterEditingSupport(customParameterViewer));
		
		col = new TableViewerColumn(customParameterViewer, SWT.NONE);
		col.getColumn().setWidth(50);
		col.setLabelProvider(new HyperlinkLabelProvider(customParameterViewer.getTable(), 2) {
			protected String getText(Object o) { return "Reset"; };
			protected void handleLinkClick(Object o) {
				customParameters.remove(o.toString());
				customParameterViewer.refresh();
			};
		});
		
		return customParameterViewer;
	}
	
	public void load(String captureConfigId) {
		if (captureConfigId != null && captureConfigId.equals(currentCaptureConfigId)) return;
		
		currentCaptureConfigId = captureConfigId;
		customParameters.clear();
		
		try {
			CaptureConfig captureConfig = DataCaptureService.getInstance().getCaptureConfig(captureConfigId);
			if (captureConfig != null) defaultParameters = captureConfig.getParameters();
			else defaultParameters = new ParameterGroup();
		} catch (Exception e) {
			EclipseLog.error("Failed to retrieve capture configuration", e, Activator.getDefault());
		}
		
		String[] keys = defaultParameters.getParameterKeys();
		Arrays.sort(keys);
		customParameterViewer.setInput(keys);
	}
	
	public String[] getCustomParameterKeys() {
		return customParameters.keySet().toArray(new String[customParameters.size()]);
	}
	
	public String getParameterValue(String name) {
		if (customParameters.containsKey(name)) return customParameters.get(name);
		Object value = defaultParameters.getParameter(name);
		return (value == null) ? null : value.toString();
	}
	
	private class ParameterEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private final CellEditor editor;
		
		public ParameterEditingSupport(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected Object getValue(Object element) {
			return getParameterValue(element.toString());
		}

		@Override
		protected void setValue(Object element, Object value) {
			customParameters.put(element.toString(), value.toString());
			viewer.update(element, null);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
}
