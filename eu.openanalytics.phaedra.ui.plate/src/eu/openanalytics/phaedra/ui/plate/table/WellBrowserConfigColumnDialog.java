package eu.openanalytics.phaedra.ui.plate.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomizeColumnsDialog;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;


public class WellBrowserConfigColumnDialog extends CustomizeColumnsDialog {
	
	
	private List<Feature> features;
	
	private Map<TableColumn, Feature> columnFeatureMap;
	
	
	public WellBrowserConfigColumnDialog(Shell parentShell, RichTableViewer tableViewer, List<Feature> features) {
		super(parentShell, tableViewer);
		
		this.features = features;
		this.columnFeatureMap = new HashMap<>();
		for (TableColumn column : tableViewer.getTable().getColumns()) {
			for (Feature feature : features) {
				if (feature.getDisplayName().equals(column.getText())) {
					columnFeatureMap.put(column, feature);
					break;
				}
			}
		}
	}
	
	
	@Override
	protected void addShowHideActionButtons(Composite composite) {
		super.addShowHideActionButtons(composite);
		
		addColumnActionButton(composite, "Show Key Feat.", "Show Key Features Columns",
				createChangeColumnVisibleAction((final TableColumn column) -> {
					Feature feature = columnFeatureMap.get(column);
					return (feature != null) ? feature.isKey() : null;
				}));
		addColumnActionButton(composite, "Show Num. Feat.", "Show Numeric Feature Columns",
				createChangeColumnVisibleAction((final TableColumn column) -> {
					Feature feature = columnFeatureMap.get(column);
					return (feature != null) ? feature.isNumeric() : null;
				}));
		addColumnActionButton(composite, "Show Req. Feat.", "Show Required Feature Columns",
				createChangeColumnVisibleAction((final TableColumn column) -> {
					Feature feature = columnFeatureMap.get(column);
					return (feature != null) ? feature.isRequired() : null;
				}));
	}
	
}
