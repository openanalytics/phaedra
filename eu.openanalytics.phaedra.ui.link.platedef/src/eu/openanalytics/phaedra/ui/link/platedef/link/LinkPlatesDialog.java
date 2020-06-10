package eu.openanalytics.phaedra.ui.link.platedef.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.misc.ThreadsafeDialogHelper;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettingsDialog;
import eu.openanalytics.phaedra.link.platedef.source.AbstractDefinitionSource;
import eu.openanalytics.phaedra.link.platedef.source.IPlateDefinitionSource;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.link.platedef.Activator;

public class LinkPlatesDialog extends TitleAreaDialog {

	private Combo sourceCombo;
	
	private RichTableViewer tableViewer;
	
	private Button settingsBtn;
	private Button keepBarcodeBtn;
	private Button recalcBtn;
	
	private List<Plate> plates;
	private Map<Plate, String> statusMap;
	private Map<Plate, String> barcodeMap;
	
	private IPlateDefinitionSource selectedSource;
	private PlateLinkSettings settings;

	public LinkPlatesDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);

		// The plate(s) that are being linked.
		statusMap = new HashMap<Plate, String>();
		barcodeMap = new HashMap<Plate, String>();
		for (Plate plate : plates) {
			statusMap.put(plate, "");
			barcodeMap.put(plate, plate.getBarcode());
		}

		this.plates = plates;
		this.settings = new PlateLinkSettings();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Link Plate Definitions");
		newShell.setSize(600,400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		
		// Container of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(0,0).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		
		// Container of the main part of the dialog (Input)
		Composite main = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Group sourceGrp = new Group(main, SWT.SHADOW_ETCHED_IN);
		sourceGrp.setText("Link With");
		GridDataFactory.fillDefaults().grab(true,false).applyTo(sourceGrp);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,5).applyTo(sourceGrp);
		
		Label lbl = new Label(sourceGrp, SWT.NONE);
		lbl.setText("Source:");

		String[] sourceIds = PlateDefinitionService.getInstance().getSourceIds();
		Arrays.sort(sourceIds);
		sourceCombo = new Combo(sourceGrp, SWT.READ_ONLY);
		sourceCombo.setItems(sourceIds);
		sourceCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int index = sourceCombo.getSelectionIndex();
				String selectedId = sourceCombo.getItem(index);
				selectedSource = PlateDefinitionService.getInstance().getSource(selectedId);
				settingsBtn.setEnabled(selectedSource.requiresSettings());
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(sourceCombo);
		
		settingsBtn = new Button(sourceGrp, SWT.PUSH);
		settingsBtn.setText("Select...");
		settingsBtn.setEnabled(false);
		settingsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedSource != null) {
					PlateLinkSettingsDialog dialog = selectedSource.createSettingsDialog(getShell(), plates);
					if (dialog.open() == Window.OK) {
						settings = dialog.getSettings();
					}
				}
			}
		});
		GridDataFactory.fillDefaults().hint(80,SWT.DEFAULT).applyTo(settingsBtn);
		
		Group plateGrp = new Group(main, SWT.SHADOW_ETCHED_IN);
		plateGrp.setText("Plates");
		GridDataFactory.fillDefaults().grab(true,true).applyTo(plateGrp);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(plateGrp);

		tableViewer = new RichTableViewer(plateGrp, SWT.BORDER);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(configureColumns());
		GridDataFactory.fillDefaults().grab(true, true).span(3,1).applyTo(tableViewer.getTable());
		
		tableViewer.setInput(plates);

		final Table table = tableViewer.getTable();
		final TableEditor editor = new TableEditor(table);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent event) {
				// Dispose any existing editor
				Control old = editor.getEditor();
				if (old != null && !old.isDisposed()) old.dispose();

				Point pt = new Point(event.x, event.y);
				final TableItem item = table.getItem(pt);
				if (item != null) {
					int column = -1;
					for (int i = 0, n = table.getColumnCount(); i < n; i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							column = i;
							break;
						}
					}

					final int barcodeColumn = 1;
					
					final Plate plate = (Plate) item.getData();
					if (column == barcodeColumn) {
						Text newEditor = new Text(table, SWT.NONE);
						newEditor.setText(item.getText(barcodeColumn));
						newEditor.addModifyListener(new ModifyListener() {
							@Override
							public void modifyText(ModifyEvent me) {
								Text text = (Text) editor.getEditor();
								editor.getItem().setText(barcodeColumn, text.getText());
								barcodeMap.put(plate, text.getText().trim());
							}
						});
						newEditor.selectAll();
						newEditor.setFocus();
						editor.setEditor(newEditor, item, barcodeColumn);
					}
				}
			}
		});

		recalcBtn = new Button(plateGrp, SWT.CHECK);
		recalcBtn.setText("Recalculate plates after linking");
		recalcBtn.setSelection(true);
		
		keepBarcodeBtn = new Button(plateGrp, SWT.CHECK);
		keepBarcodeBtn.setText("Keep original barcodes (do not replace Phaedra barcodes with the barcodes entered above)");
		
		setMessage("By linking plates with the system they were defined in, compound and control information can be retrieved and applied to the plate(s).");
		setTitle("Link Plate Definitions");

		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setCapture(true);
		createButton(parent, IDialogConstants.OK_ID, "Link", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Close", false);

		sourceCombo.select(0);
		
		// If the first plate's protocol class has a default source, select it.
		if (plates != null && !plates.isEmpty()) {
			ProtocolClass pClass = plates.get(0).getExperiment().getProtocol().getProtocolClass();
			String defaultSource = pClass.getDefaultLinkSource();
			if (defaultSource != null && !defaultSource.isEmpty() && sourceCombo.indexOf(defaultSource) != -1) {
				sourceCombo.select(sourceCombo.indexOf(defaultSource));
			}
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			close();
			return;
		} else if (buttonId == IDialogConstants.OK_ID) {
			doLinkPlates();
			super.okPressed();
		}
	}
	
	private void doLinkPlates() {
		if (plates == null || plates.isEmpty()) return;
		final boolean keepBarcodes = keepBarcodeBtn.getSelection();
		final boolean recalcPlates = recalcBtn.getSelection();
		
		Job linkJob = new Job("Linking plates") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Linking " + plates.size() + " plate(s)...", plates.size());
				try {
					PlateLinkSettings[] batchedSettings = new PlateLinkSettings[plates.size()];
					for (int i=0; i<plates.size(); i++) {
						Plate plate = plates.get(i);
						PlateLinkSettings plateSettings = new PlateLinkSettings();
						plateSettings.setSettings(new HashMap<>());
						plateSettings.getSettings().putAll(settings.getSettings());
						plateSettings.getSettings().put("keepBarcodes", keepBarcodes);
						plateSettings.getSettings().put("recalcPlates", recalcPlates);
						plateSettings.setBarcode(barcodeMap.get(plate));
						plateSettings.setPlate(plate);
						batchedSettings[i] = plateSettings;
					}
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					
					int failedCount = 0;
					StringBuilder failedMsg = new StringBuilder();
					failedMsg.append("Plates that failed to link:\n\n");
					
					String[] status = PlateDefinitionService.getInstance().linkSource(selectedSource, batchedSettings, monitor);
					for (int i=0; i<plates.size(); i++) {
						statusMap.put(batchedSettings[i].getPlate(), status[i]);
						if (!status[i].equalsIgnoreCase(AbstractDefinitionSource.STATUS_OK)) {
							failedCount++;
							failedMsg.append(batchedSettings[i].getPlate().getBarcode() + ": " + status[i] + "\n");
						}
					}
					
					if (failedCount == 0) {
						ThreadsafeDialogHelper.openInfo("Link Complete", "All plates linked successfully.");	
					} else {
						ThreadsafeDialogHelper.openWarning("Link Complete", "Not all plates were linked successfully.\n" + failedMsg.toString());						
					}
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to link barcodes", e);
				}
				return Status.OK_STATUS;
			}
		};
		linkJob.setUser(true);
		linkJob.schedule();
	}

	private ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;
		
		config = ColumnConfigFactory.create("Sequence", "getSequence", DataType.Integer, 80); 
		configs.add(config);

		config = ColumnConfigFactory.create("Barcode (click to edit)", DataType.String, 160);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Plate plate = (Plate)element;
				String barcode = barcodeMap.get(plate);
				if (barcode == null) barcode = plate.getBarcode();
				return barcode;
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Barcode");
		configs.add(config);
		
		config = ColumnConfigFactory.create("Status", DataType.String, 350);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Plate plate = (Plate)element;
				String status = statusMap.get(plate);
				return status.replace("\r\n", " ");
			}
			@Override
			public Color getBackground(Object element) {
				Plate plate = (Plate)element;
				String status = statusMap.get(plate);
				if (status.startsWith(AbstractDefinitionSource.STATUS_ERROR)) return Display.getDefault().getSystemColor(SWT.COLOR_RED);
				if (status.startsWith(AbstractDefinitionSource.STATUS_BARCODE_NOT_FOUND)) return Display.getDefault().getSystemColor(SWT.COLOR_RED);
				if (status.equalsIgnoreCase(AbstractDefinitionSource.STATUS_NO_BARCODE_ENTERED)) return Display.getDefault().getSystemColor(SWT.COLOR_RED);
				if (status.equalsIgnoreCase(AbstractDefinitionSource.STATUS_OK)) return Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
				if (!status.isEmpty()) return Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
				return super.getBackground(element);
			}
		};
		config.setLabelProvider(labelProvider);
		config.setTooltip("Status");
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
