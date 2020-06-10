package eu.openanalytics.phaedra.ui.plate.calculation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class OutlierDetectionDialog extends TitleAreaDialog {

	private RichTableViewer tableViewer;
	
	private List<Plate> plates;
	private List<Well> outliers;
	
	public OutlierDetectionDialog(Shell parentShell, List<Plate> plates, List<Well> outliers) {
		super(parentShell);
		this.plates = plates;
		this.outliers = outliers;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Outlier Detection");
		newShell.setSize(600, 400);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(main);
		
		tableViewer = new RichTableViewer(main, SWT.BORDER | SWT.SINGLE);
		tableViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl());
		configureColumns();
		tableViewer.setInput(outliers);
		
		setTitle("Outlier Detection");
		setMessage(String.format("Outlier detection has been performed on %d plate(s) and found the following %d outliers."
				+ "\nSelect Ok to auto-reject these wells.", plates.size(), outliers.size()));
		
		return main;
	}
	
	private void configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<>();
		
		configs.add(ColumnConfigFactory.create("Well", w -> PlateUtils.getWellCoordinate((Well) w), DataType.String, 50));
		configs.add(ColumnConfigFactory.create("Plate", w -> ((Well) w).getPlate().getBarcode(), DataType.String, 200));
		configs.add(ColumnConfigFactory.create("Well Type", w -> ((Well) w).getWellType(), DataType.String, 150));
		
		tableViewer.applyColumnConfig(configs);
	}
}