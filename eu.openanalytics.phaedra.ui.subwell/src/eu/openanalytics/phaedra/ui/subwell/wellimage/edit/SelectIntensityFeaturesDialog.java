package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.protocol.util.TableComboCellEditor;
import eu.openanalytics.phaedra.ui.subwell.wellimage.edit.DrawCellsPaletteTool.FeatureMapping;
import eu.openanalytics.phaedra.ui.subwell.wellimage.edit.DrawCellsPaletteTool.FeatureMappings;

public class SelectIntensityFeaturesDialog extends TitleAreaDialog {

	private RichTableViewer tableViewer;
	
	private FeatureMappings mappings;
	private FeatureMappings mappingsWorkingCopy;
	
	public SelectIntensityFeaturesDialog(Shell parentShell, FeatureMappings mappings) {
		super(parentShell);
		this.mappings = mappings;
		
		this.mappingsWorkingCopy = new FeatureMappings(mappings.pClass);
		copyMappings(mappings, mappingsWorkingCopy);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Features");
		newShell.setSize(450, 500);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		
		tableViewer = new RichTableViewer(container, SWT.BORDER);
		tableViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl());
		
		configureTable();
		
		setTitle("Select Features");
		setMessage("Select the features that will store the intensity values per channel."
				+ "\nIntensity values without a mapped feature will not be saved.");
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.ABORT_ID, "Reset", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.ABORT_ID) {
			copyMappings(mappings, mappingsWorkingCopy);
			tableViewer.refresh();
		} else if (buttonId == IDialogConstants.OK_ID) {
			copyMappings(mappingsWorkingCopy, mappings);
			super.okPressed();
		}
		else super.buttonPressed(buttonId);
	}
	
	private void copyMappings(FeatureMappings from, FeatureMappings to) {
		for (FeatureMapping m: from.mappings.values()) {
			FeatureMapping copy = new FeatureMapping(m.channel, m.stat, m.feature);
			to.mappings.put(m.channel + m.stat, copy);
		}
	}
	
	private void configureTable() {
		ColumnConfiguration config = ColumnConfigFactory.create("Channel", "getChannel", ColumnDataType.String, 80);
		TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText(config.getName());
		tvc.getColumn().setWidth(config.getWidth());
		tvc.setLabelProvider(config.getLabelProvider());
		new ColumnViewerSorter<>(tableViewer, tvc, config.getSorter());
		
		config = ColumnConfigFactory.create("Statistic", "getStat", ColumnDataType.String, 100);
		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText(config.getName());
		tvc.getColumn().setWidth(config.getWidth());
		tvc.setLabelProvider(config.getLabelProvider());
		new ColumnViewerSorter<>(tableViewer, tvc, config.getSorter());
		
		config = ColumnConfigFactory.create("Saved as Feature", "getFeature", ColumnDataType.String, 200);
		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText(config.getName());
		tvc.getColumn().setWidth(config.getWidth());
		tvc.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				IFeature f = ((FeatureMapping)element).getFeature();
				return (f == null) ? "" : f.getName();
			}
		});
		tvc.setEditingSupport(new FeatureEditingSupport(tableViewer, mappings.pClass));
		
		List<FeatureMapping> mappingList = new ArrayList<>(mappingsWorkingCopy.mappings.values());
		Collections.sort(mappingList, (f1, f2) -> {
			int c = f1.getStat().compareTo(f2.getStat());
			if (c == 0) return f1.getChannel() - f2.getChannel();
			else return c;
		});
		tableViewer.setInput(mappingList);
	}
	
	private static class FeatureEditingSupport extends EditingSupport {

		private TableViewer viewer;
		private ProtocolClass pClass;
		
		public FeatureEditingSupport(TableViewer viewer, ProtocolClass pClass) {
			super(viewer);
			this.viewer = viewer;
			this.pClass = pClass;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
		
		@Override
		protected CellEditor getCellEditor(Object element) {
			TableComboCellEditor editor = new TableComboCellEditor(((TableViewer)getViewer()).getTable());

			List<SubWellFeature> features = new ArrayList<>(pClass.getSubWellFeatures());
			Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);
			
			LabelProvider labelProvider = new LabelProvider() {
				public String getText (Object element) {
					return ((SubWellFeature)element).getName();
				}
			};

			editor.setContentProvider(new ArrayContentProvider());
			editor.setLabelProvider(labelProvider);
			editor.setInput(features);

			return editor;
		}

		@Override
		protected Object getValue(Object element) {
			return ((FeatureMapping)element).getFeature();
		}

		@Override
		protected void setValue(Object element, Object value) {
			((FeatureMapping)element).feature = (IFeature)value;
			viewer.update(element, null);
		}	
	}
}
