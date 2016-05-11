package eu.openanalytics.phaedra.ui.plate.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class FindPlateDialog extends TitleAreaDialog {

	private Text barcodeTxt;
	private Label matchLbl;
	private RichTableViewer matchTableViewer;
	
	private ProtocolClass pClass;
	private java.util.List<Plate> matchingPlates;
	private Plate selectedPlate;
	
	public FindPlateDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setProtocolClass(ProtocolClass pClass) {
		this.pClass = pClass;
	}
	
	public Plate getSelectedPlate() {
		return selectedPlate;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Find Plate");
		newShell.setSize(400,450);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// Container of the main part of the dialog (Input)
		Composite container = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(container);

		barcodeTxt = new Text(container, SWT.BORDER);
		barcodeTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				doSearchPlates(barcodeTxt.getText());
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(barcodeTxt);
		
		matchLbl = new Label(container, SWT.NONE);
		matchLbl.setText("Matches:");
		GridDataFactory.fillDefaults().grab(true,false).applyTo(matchLbl);
		
		matchTableViewer = new RichTableViewer(container, SWT.SINGLE | SWT.BORDER);
		matchTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		matchTableViewer.getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedPlate = SelectionUtils.getFirstObject(matchTableViewer.getSelection(), Plate.class);
			}
		});
		matchTableViewer.getTable().setLinesVisible(false);
		matchTableViewer.setContentProvider(new ArrayContentProvider());
		matchTableViewer.applyColumnConfig(configureColumns());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(matchTableViewer.getTable());
		
		setTitle("Find Plate");
		setMessage("Find a plate by entering (part of) its barcode.");

		return container;
	}

	@Override
	protected void okPressed() {
		if (selectedPlate == null && !matchTableViewer.getSelection().isEmpty()) {
			selectedPlate = SelectionUtils.getFirstObject(matchTableViewer.getSelection(), Plate.class);
		}
		super.okPressed();
	}
	
	private void doSearchPlates(String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			matchTableViewer.setInput(new Plate[0]);
			matchLbl.setText("Matches:");
			return;
		}
		
		matchingPlates = PlateService.getInstance().getPlates(pattern, pClass);
		matchLbl.setText("Matches: " + matchingPlates.size());
		matchTableViewer.setInput(matchingPlates);
	}
	
	public static ColumnConfiguration[] configureColumns() {

		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Id", "getId", ColumnDataType.Numeric, 60);
		configs.add(config);

		config = ColumnConfigFactory.create("Sequence", "getSequence", ColumnDataType.Numeric, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Barcode", "getBarcode", ColumnDataType.String, 100);
		configs.add(config);

		config = ColumnConfigFactory.create("Experiment", "getExperiment", ColumnDataType.String, 250);
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}