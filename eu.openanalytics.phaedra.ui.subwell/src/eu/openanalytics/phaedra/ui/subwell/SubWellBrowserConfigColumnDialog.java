package eu.openanalytics.phaedra.ui.subwell;

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
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class SubWellBrowserConfigColumnDialog extends CustomizeColumnsDialog {

	private List<SubWellFeature> features;
	private Map<TableItem, SubWellFeature> tableItemFeatureMap;

	public SubWellBrowserConfigColumnDialog(Shell parentShell, RichTableViewer tableviewer, List<SubWellFeature> features) {
		super(parentShell, tableviewer);
		this.features = features;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		tableItemFeatureMap = new HashMap<>();
		for (TableItem item : configTableViewer.getTable().getItems()) {
			for (SubWellFeature f : features) {
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
			@Override
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
			@Override
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

		return control;
	}

	private void setChecked(TableItem item, boolean checked) {
		item.setChecked(checked);
		if (item.getData() instanceof TableColumn) {
			toggleColumnVisible((TableColumn) item.getData(), checked);
		}
	}

}