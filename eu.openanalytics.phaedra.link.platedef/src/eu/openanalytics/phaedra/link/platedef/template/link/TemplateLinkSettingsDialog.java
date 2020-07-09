package eu.openanalytics.phaedra.link.platedef.template.link;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridContentProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridLabelProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettingsDialog;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;


public class TemplateLinkSettingsDialog extends PlateLinkSettingsDialog {

	private PlateLinkSettings settings;
	private PlateTemplate currentTemplate;
	private List<Plate> plates;
	private long[] protocolClassIds;
	private long selectedProtocolClassId;
	
	private Combo pClassCombo;
	private Combo templatesCombo;
	private GridViewer gridViewer;
	
	public TemplateLinkSettingsDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.settings = new PlateLinkSettings();
		this.plates = plates;
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(600,500);
		newShell.setText("Plate Layout Link");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Plate Layout Link");
		setMessage("Select one of the available templates below.");
		
		Composite container = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,5).applyTo(container);
		
		Label pClassLbl = new Label(container, SWT.NONE);
		pClassLbl.setText("Protocol Class:");
		
		pClassCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		pClassCombo.setVisibleItemCount(20);
		pClassCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int index = pClassCombo.getSelectionIndex();
				selectProtocolClass(index);
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).span(2,1).applyTo(pClassCombo);
		
		Label templatesLbl = new Label(container, SWT.NONE);
		templatesLbl.setText("Template:");
		
		templatesCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		templatesCombo.setVisibleItemCount(20);
		templatesCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int index = templatesCombo.getSelectionIndex();
				selectTemplate(index);
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).span(2,1).applyTo(templatesCombo);
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Preview:");
		GridDataFactory.fillDefaults().grab(true,false).span(2,1).applyTo(lbl);
		
		gridViewer = new GridViewer(container, 8, 12);
		GridDataFactory.fillDefaults().span(3,1).grab(true,true).applyTo(gridViewer.getControl());

		gridViewer.setContentProvider(new AbstractGridContentProvider(){
			@Override
			public int getColumns(Object inputElement) {
				return (currentTemplate == null) ? 12 : currentTemplate.getColumns();
			}
			@Override
			public int getRows(Object inputElement) {
				return (currentTemplate == null) ? 8 : currentTemplate.getRows();
			}
			@Override
			public Object getElement(int row, int column) {
				if (currentTemplate == null) return null;
				int wellNr = NumberUtils.getWellNr(row+1, column+1, currentTemplate.getColumns());
				return currentTemplate.getWells().get(wellNr);
			}
		});
		gridViewer.setLabelProvider(new AbstractGridLabelProvider(){
			private BaseGridCellRenderer renderer = new TemplateLinkGridCellRenderer();
			
			@Override
			public IGridCellRenderer createCellRenderer() {
				return renderer;
			}
		});
		gridViewer.getGrid().setSelectionEnabled(false);
		
		return container;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);		
		initFields();
	}
	
	@Override
	protected void okPressed() {
		settings.getSettings().put("template", currentTemplate);
		super.okPressed();
	}
	
	@Override
	public PlateLinkSettings getSettings() {
		return settings;
	}
	
	private void initFields() {
		List<ProtocolClass> pClasses = ProtocolService.getInstance().getProtocolClasses();
		Collections.sort(pClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
		
		ProtocolClass preselectPClass = null;
		int preselectPClassIndex = 0;
		if (plates != null && !plates.isEmpty()) {
			preselectPClass = PlateUtils.getProtocolClass(plates.get(0));
		}
		
		protocolClassIds = new long[pClasses.size()];
		for (int i = 0; i < protocolClassIds.length; i++) {
			ProtocolClass pClass = pClasses.get(i);
			pClassCombo.add(pClass.getName());
			protocolClassIds[i] = pClass.getId();
			if (pClass.equals(preselectPClass)) preselectPClassIndex = i;
		}
		
		if (!pClasses.isEmpty()) {
			pClassCombo.select(preselectPClassIndex);
			selectProtocolClass(preselectPClassIndex);
		}
	}
	
	private void selectProtocolClass(int index) {
		if (index < 0 || index >= protocolClassIds.length) return;
		selectedProtocolClassId = protocolClassIds[index];

		try {
			String[] ids = PlateDefinitionService.getInstance().getTemplateManager().getTemplateIds(selectedProtocolClassId)
					.stream().toArray(i -> new String[i]);
			templatesCombo.setItems(ids);
			if (ids.length > 0) {
				templatesCombo.select(0);
				selectTemplate(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void selectTemplate(int index) {
		if (index < 0) return;
		try {
			String id = templatesCombo.getItem(index);
			currentTemplate = PlateDefinitionService.getInstance().getTemplateManager().getTemplate(id);
			gridViewer.getGrid().resetGrid(currentTemplate.getRows(), currentTemplate.getColumns());
			gridViewer.setInput("root");
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
