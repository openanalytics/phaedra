package eu.openanalytics.phaedra.ui.link.importer.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem;
import eu.openanalytics.phaedra.datacapture.log.IDataCaptureLogListener;

public class DataCaptureLogView extends ViewPart implements IDataCaptureLogListener {

	private RichTableViewer tableViewer;
	
	private List<DataCaptureLogItem> currentItems;
	
	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		tableViewer = new RichTableViewer(container, SWT.BORDER);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(configureColumns());
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				DataCaptureLogItem item = SelectionUtils.getFirstObject(event.getSelection(), DataCaptureLogItem.class);
				showItemDetails(item);
			}
		});
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableViewer.getControl());
		
		createToolbar();
		
		currentItems = new ArrayList<>();
		tableViewer.setInput(currentItems);
		
		DataCaptureService.getInstance().addLogListener(this);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewDataCaptureLog");
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		DataCaptureService.getInstance().removeLogListener(this);
		super.dispose();
	}
	
	@Override
	public void logEvent(DataCaptureLogItem item) {
		currentItems.add(0, item);
		
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				tableViewer.refresh();
			}
		});
	}
	
	private ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("", ColumnDataType.Image, 30);
		RichLabelProvider labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				return null;
			}
			@Override
			public Image getImage(Object element) {
				DataCaptureLogItem item = (DataCaptureLogItem)element;
				return item.severity.getIcon();
			}
			@Override
			public String getToolTipText(Object element) {
				DataCaptureLogItem item = (DataCaptureLogItem)element;
				return item.severity.toString();
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);

		config = ColumnConfigFactory.create("Date", ColumnDataType.Date, 120);
		labelProvider = new RichLabelProvider(config) {
			private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
			@Override
			public String getText(Object element) {
				DataCaptureLogItem item = (DataCaptureLogItem)element;
				return format.format(item.timestamp);
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Module", ColumnDataType.String, 130);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				DataCaptureLogItem item = (DataCaptureLogItem)element;
				return item.module == null ? "" : item.module.getName();
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Reading", ColumnDataType.String, 110);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				DataCaptureLogItem item = (DataCaptureLogItem)element;
				return item.reading == null ? "" : item.reading.getBarcode();
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Message", ColumnDataType.String, 750);
		labelProvider = new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				DataCaptureLogItem item = (DataCaptureLogItem)element;
				return item.message;
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);	
	}
	
	private void createToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		Action clearAction = new Action("Clear") {
			public void run() {
				currentItems.clear();
				tableViewer.refresh();
			}
		};
		clearAction.setImageDescriptor(IconManager.getIconDescriptor("bin.png"));
		mgr.add(clearAction);
	}
	
	private void showItemDetails(DataCaptureLogItem item) {
		
		final Shell shell = new Shell(SWT.DIALOG_TRIM | SWT.RESIZE);
		shell.setText("Validation Item Details");
		shell.setImage(item.severity.getIcon());
		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_ESCAPE) {
					shell.close();
					event.detail = SWT.TRAVERSE_NONE;
					event.doit = false;
				}
			}
		});
		GridLayoutFactory.fillDefaults().applyTo(shell);
		
		FormToolkit toolkit = new FormToolkit(shell.getDisplay());
		Form form = toolkit.createForm(shell);
		form.setText("Validation Item Details");
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(form.getBody());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		
		Label lbl = toolkit.createLabel(form.getBody(), "Severity:");
		
		Composite c = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(c);
		
		lbl = toolkit.createLabel(c, "");
		lbl.setImage(item.severity.getIcon());
		lbl = toolkit.createLabel(c, item.severity.toString());
		
		lbl = toolkit.createLabel(form.getBody(), "Module:");
		
		Text txt = toolkit.createText(form.getBody(), item.module == null ? "" : item.module.getName());
		txt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(txt);
		
		lbl = toolkit.createLabel(form.getBody(), "Module Id:");
		
		txt = toolkit.createText(form.getBody(), item.module == null ? "" : item.module.getId());
		txt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(txt);
		
		lbl = toolkit.createLabel(form.getBody(), "Reading:");
		
		txt = toolkit.createText(form.getBody(), item.reading == null ? "" : item.reading.getBarcode());
		txt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(txt);
		
		lbl = toolkit.createLabel(form.getBody(), "Timestamp:");
		
		txt = toolkit.createText(form.getBody(), item.timestamp.toString());
		txt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(txt);
		
		lbl = toolkit.createLabel(form.getBody(), "Message:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		txt = toolkit.createText(form.getBody(), item.message, SWT.WRAP);
		txt.setEditable(false);
		GridDataFactory.fillDefaults().hint(500, 100).grab(true, true).applyTo(txt);
		
		String errorMsg = item.errorCause == null ? null : StringUtils.getStackTrace(item.errorCause);
		if (errorMsg != null) {
			lbl = toolkit.createLabel(form.getBody(), "Error Stack:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
			
			txt = toolkit.createText(form.getBody(), errorMsg, SWT.V_SCROLL | SWT.WRAP);
			txt.setEditable(false);
			GridDataFactory.fillDefaults().hint(500, 100).grab(true, true).applyTo(txt);
		}
		
		Button closeBtn = new Button(form.getBody(), SWT.PUSH);
		closeBtn.setText("Close");
		closeBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		GridDataFactory.fillDefaults().span(2,1).align(SWT.END, SWT.BEGINNING).applyTo(closeBtn);
		
		shell.pack();
		shell.open();
	}
}
