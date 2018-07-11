package eu.openanalytics.phaedra.ui.silo.dialog;


import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.calculation.jep.JEPFormulaDialog;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class AddJEPColumnDialog extends JEPFormulaDialog {

	private Label errorMessage;
	private Text columnText;
	
	private String columnName;
	
	private SiloDataset dataset;

	public AddJEPColumnDialog(Shell parentShell, SiloDataset dataset) {
		super(parentShell, dataset.getSilo().getProtocolClass());
		setShellStyle(SWT.TITLE | SWT.RESIZE);
		this.dataset = dataset;
	}
	
	@Override
	public int open() {
		if (columnText != null && !columnText.isDisposed()) {
			columnText.setText(columnName);
		}
		return super.open();
	}

	public String getColumnName() {
		return columnName;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	@Override
	protected void createExpression(Composite subContainer) {
		super.createExpression(subContainer);
		Label lbl = new Label(subContainer, SWT.NONE);
		lbl.setText("Column Name:");
		
		columnText = new Text(subContainer, SWT.BORDER | SWT.WRAP | SWT.SINGLE);
		columnText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String newText = columnText.getText();
				if (newText.isEmpty()) {
					errorMessage.setText("Please enter a name for the new column");
					return;
				} else {
					for (SiloDatasetColumn column: dataset.getColumns()) {
						if (column.getName().equals(newText)) {
							errorMessage.setText("A column with this name already exists");
							return;
						}
					}
				}
				errorMessage.setText("");
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(columnText);
		
		errorMessage = new Label(subContainer, SWT.NONE);
		errorMessage.setText("Please enter a name for the new column");
		errorMessage.setForeground(new Color(Display.getDefault(), 255, 0, 0));
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(errorMessage);
	}
	
	@Override
	protected void createTables(Composite container) {
		final Table table = createTable(container, "Silo Columns:", 100, s -> "~" + s + "~");
		for (SiloDatasetColumn column: dataset.getColumns()) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(column.getName());
			item.setData("info", column);
		}
		super.createTables(container);
	}

	@Override
	protected void okPressed() {
		columnName = columnText.getText();
		if (!errorMessage.getText().isEmpty()) {
			columnText.setFocus();
			return;
		}
		super.okPressed();
	}

}