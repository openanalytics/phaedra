package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.icons.IconRegistry;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.link.importer.ImportUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class SelectProtocol extends BaseStatefulWizardPage {

	private RichTableViewer protocolTableViewer;
	private Combo captureConfigCmb;
	
	private Protocol selectedProtocol;
	private String captureConfigId;
	
	public SelectProtocol() {
		super("Select Protocol");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		new Label(container, SWT.NONE).setText("Select a destination protocol:");
		
		protocolTableViewer = new RichTableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
		protocolTableViewer.setContentProvider(new ArrayContentProvider());
		protocolTableViewer.applyColumnConfig(createProtocolColumns());
		protocolTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				protocolSelected(SelectionUtils.getFirstObject(event.getSelection(), Protocol.class));
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 350).applyTo(protocolTableViewer.getControl());
		
		new Label(container, SWT.NONE).setText("Data capture configuration:");
		captureConfigCmb = new Combo(container, SWT.READ_ONLY);
		captureConfigCmb.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String id = captureConfigCmb.getText();
				if (!id.isEmpty()) captureConfigSelected(id);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(captureConfigCmb);
		
		Composite infoCmp = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().indent(10, 0).grab(true,false).applyTo(infoCmp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(infoCmp);
		
		Label lbl = new Label(infoCmp, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("information.png"));
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		new Label(infoCmp, SWT.NONE).setText("The data capture configuration determines how the data gets imported."
				+ "\nIf you are not sure what this value should be, use the default value.");
		
		setTitle("Select Protocol");
    	setDescription("Select the protocol you want to import data for.");
    	setControl(container);
    	
    	setPageComplete(false);
	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		List<Protocol> protocols = new ArrayList<>(ProtocolService.getInstance().getProtocols());
		Collections.sort(protocols, ProtocolUtils.PROTOCOL_NAME_SORTER);
    	protocolTableViewer.setInput(protocols);
    	
    	try {
    		captureConfigId = ((ImportWizardState)state).task.getCaptureConfigId();
    	} catch (RuntimeException e) {
    		// getCaptureConfigId fails if no config or target experiment are set yet.
    	}
		try {
			String[] captureConfigs = DataCaptureService.getInstance().getAllCaptureConfigIds();
			captureConfigCmb.setItems(captureConfigs);
			if (captureConfigId != null) {
				int index = CollectionUtils.find(captureConfigs, captureConfigId);
				captureConfigCmb.select(index);
			}
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Error", "Could not retrieve available data capture configurations from the Phaedra server.\n\nError message:\n"+e.getMessage());
		}
		
		// Support pre-selection from an experiment browser.
		ImportTask task = ((ImportWizardState)state).task;
		if (task.targetExperiment != null) {
			protocolTableViewer.setSelection(new StructuredSelection(task.targetExperiment.getProtocol()), true);
		}
	}
	
	@Override
	public void collectState(IWizardState state) {
		ImportWizardState s = (ImportWizardState)state;
		s.task.setCaptureConfigId(captureConfigId);
		s.task.getParameters().put(OperaImportHelper.PARAM_PROTOCOL, selectedProtocol);
	}
	
	private ColumnConfiguration[] createProtocolColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;
		
		config = ColumnConfigFactory.create("", ColumnDataType.String, 30);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public Image getImage(Object element) {
				return (Image)JFaceResources.getResources().get(IconRegistry.getInstance().getDefaultImageDescriptorFor(Protocol.class));
			}
			@Override
			public String getText(Object element) {
				return null;
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Protocol", "getName", ColumnDataType.String, 270); 
		configs.add(config);

		config = ColumnConfigFactory.create("Id", "getId", ColumnDataType.Numeric, 50);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Protocol Class", ColumnDataType.String, 150);
		labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Protocol p = (Protocol) element;
				return (p == null || p.getProtocolClass() == null) ? "" : p.getProtocolClass().getName();
			}
		};
		config.setLabelProvider(labelProvider);
		config.setSorter(new Comparator<Protocol>(){
			@Override
			public int compare(Protocol p1, Protocol p2) {
				if (p1 == null) return -1;
				if (p2 == null) return 1;
				return p1.getProtocolClass().getName().compareTo(p2.getProtocolClass().getName());
			}
		});
		config.setTooltip("Protocol Class");
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
	
	private void protocolSelected(Protocol p) {
		selectedProtocol = p;
		String defaultCaptureConfigId = ImportUtils.getCaptureConfigId(p);
		if (defaultCaptureConfigId != null) {
			captureConfigCmb.select(captureConfigCmb.indexOf(defaultCaptureConfigId));
		} else if (captureConfigCmb.getItemCount() > 0) {
			captureConfigCmb.select(0);
		}
		checkPageComplete();
	}
	
	private void captureConfigSelected(String id) {
		captureConfigId = id;
		checkPageComplete();
	}
	
	private void checkPageComplete() {
		setPageComplete(captureConfigId != null && selectedProtocol != null);
	}
}
