package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomizeColumnsDialog;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellBrowserConfigColumnDialog extends CustomizeColumnsDialog {

	private List<Feature> features;
	private Map<TableItem, Feature> tableItemFeatureMap;

	public WellBrowserConfigColumnDialog(Shell parentShell, RichTableViewer tableviewer, List<Feature> features) {
		super(parentShell, tableviewer);
		this.features = features;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		
		tableItemFeatureMap = new HashMap<>();
		for (TableItem item : configTableViewer.getTable().getItems()) {
			for (Feature f : features) {
				if (f.getDisplayName().equals(item.getText())) {
					tableItemFeatureMap.put(item, f);
					break;
				}
			}
		}

		// Add Feature specific buttons
		Label txt = new Label(compositeRight, SWT.NONE);
		txt.setText("Select:");
		
		final Button button_2 = new Button(compositeRight, SWT.NONE);
		button_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button_2.setText("Key Features");
		button_2.setToolTipText("Select Key Feature columns");
		button_2.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				tableviewer.getTable().setRedraw(false);
				for (TableItem item : configTable.getItems()) {
					if (tableItemFeatureMap.containsKey(item)) {
						boolean checked = tableItemFeatureMap.get(item).isKey();
						setChecked(item, checked);
					}
				}
				tableviewer.getTable().setRedraw(true);
			}
		});
		
		final Button button_3 = new Button(compositeRight, SWT.NONE);
		button_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button_3.setText("Num. Features");
		button_3.setToolTipText("Select Numeric Feature columns");
		button_3.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				tableviewer.getTable().setRedraw(false);
				for (TableItem item : configTable.getItems()) {
					if (tableItemFeatureMap.containsKey(item)) {
						boolean checked = tableItemFeatureMap.get(item).isNumeric();
						setChecked(item, checked);
					}
				}
				tableviewer.getTable().setRedraw(true);
			}
		});
		
		final Button button_4 = new Button(compositeRight, SWT.NONE);
		button_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button_4.setText("Req. Features");
		button_4.setToolTipText("Select Required Feature columns");
		button_4.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				tableviewer.getTable().setRedraw(false);
				for (TableItem item : configTable.getItems()) {
					if (tableItemFeatureMap.containsKey(item)) {
						boolean checked = tableItemFeatureMap.get(item).isRequired();
						setChecked(item, checked);
					}
				}
				tableviewer.getTable().setRedraw(true);
			}
		});
		
		final Button button_5 = new Button(compositeRight, SWT.NONE);
		button_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button_5.setText("Upl. Features");
		button_5.setToolTipText("Select Uploaded Feature columns");
		button_5.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				tableviewer.getTable().setRedraw(false);
				for (TableItem item : configTable.getItems()) {
					if (tableItemFeatureMap.containsKey(item)) {
						boolean checked = tableItemFeatureMap.get(item).isUploaded();
						setChecked(item, checked);
					}
				}
				tableviewer.getTable().setRedraw(true);
			}
		});
		
		return control;
	}
	
	private void setChecked(TableItem item, boolean checked) {
		item.setChecked(checked);
		if (item.getData() instanceof TableColumn) {
			toggleColumnVisible((TableColumn) item.getData(), checked);
		}
	}

}