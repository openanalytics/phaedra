package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.misc.HyperlinkLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.columbus.ColumbusService;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.ui.columbus.importwizard.Meas.MeasAnalysis;
import eu.openanalytics.phaedra.ui.columbus.util.ScreenSelector;
import eu.openanalytics.phaedra.ui.link.importer.util.CustomParameterUI;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class SelectSourceColumbus extends BaseStatefulWizardPage {

	private ScreenSelector screenSelector;
	private RichTableViewer plateTableViewer;
	private Button selectAllBtn;
	private Button selectNoneBtn;
	
	private Shell analysisSelector;
	private Meas[] selectedMeasurements;
	private CustomParameterUI customParameterUI;
	
	public SelectSourceColumbus() {
		super("Select Source");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		Group group = new Group(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		
		screenSelector = new ScreenSelector(group, s -> screenSelected(s));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(screenSelector.getControl());

		new Label(group, SWT.NONE).setText("Plates found:");
		
		plateTableViewer = new RichTableViewer(group, SWT.BORDER | SWT.FULL_SELECTION);
		plateTableViewer.setContentProvider(new ArrayContentProvider());
		plateTableViewer.applyColumnConfig(createColumns());
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 150).applyTo(plateTableViewer.getControl());
		
		Composite btnComp = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnComp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(btnComp);
		
		selectAllBtn = new Button(btnComp, SWT.PUSH);
		selectAllBtn.setImage(getImportIcon(true));
		selectAllBtn.setText("Select All");
		selectAllBtn.addListener(SWT.Selection, e -> {
			for (Meas meas: selectedMeasurements) meas.isIncluded = true;
			plateTableViewer.refresh();
			checkPageComplete();
		});
		
		selectNoneBtn = new Button(btnComp, SWT.PUSH);
		selectNoneBtn.setImage(getImportIcon(false));
		selectNoneBtn.setText("Select None");
		selectNoneBtn.addListener(SWT.Selection, e -> {
			for (Meas meas: selectedMeasurements) meas.isIncluded = false;
			plateTableViewer.refresh();
			checkPageComplete();
		});
		
		group = new Group(container, SWT.NONE);
		group.setText("Advanced: custom parameters");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		
		customParameterUI = new CustomParameterUI();
		RichTableViewer customParameterViewer = customParameterUI.create(group);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 150).applyTo(customParameterViewer.getControl());
		
		setTitle("Select Source");
    	setDescription("Select the Screen containing the data you want to import."
    			+ "\nIf there are multiple analysis results, select one by clicking on the analysis name.");
    	setControl(container);
    	setPageComplete(false);
	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		if (firstTime) screenSelector.init();
		String captureConfigId = ((ImportWizardState)state).task.getCaptureConfigId();
		customParameterUI.load(captureConfigId);
	}
	
	@Override
	public void collectState(IWizardState state) {
		ImportWizardState s = (ImportWizardState)state;
		s.task.sourcePath = "Columbus/" + screenSelector.getSelectedUser().loginname + "/" + screenSelector.getSelectedScreen().screenName;
		s.task.getParameters().put(OperaImportHelper.PARAM_MEAS_SOURCES, Lists.newArrayList(selectedMeasurements));
		ColumbusService.getInstance().setInstanceConfig(s.task.getParameters(), screenSelector.getSelectedInstanceId());
		for (String key: customParameterUI.getCustomParameterKeys()) s.task.getParameters().put(key, customParameterUI.getParameterValue(key));
	}
	
	private ColumnConfiguration[] createColumns() {
		List<ColumnConfiguration> configs = new ArrayList<>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Import?", ColumnDataType.String, 70);
		config.setLabelProvider(new HyperlinkLabelProvider(plateTableViewer.getTable(), 0) {
			@Override
			protected Image getImage(Object o) {
				if (o instanceof Meas) {
					Meas source = (Meas)o;
					return getImportIcon(source.isIncluded);
				} else {
					return null;
				}
			}
			@Override
			protected void handleLinkClick(Object o) {
				if (o instanceof Meas) {
					Meas source = (Meas)o;
					source.isIncluded = !source.isIncluded;
					plateTableViewer.refresh();
					checkPageComplete();
				}
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Plate", ColumnDataType.String, 170);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof String) {
					cell.setText(cell.getElement().toString());
				} else {
					Meas source = (Meas)cell.getElement();
					cell.setText(source.barcode);
				}
			}
		});
		config.setSorter(new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (o1 == null && o2 != null) return -1;
				if (o2 == null) return 1;
				if (o1 instanceof Meas && o2 instanceof Meas) {
					return ((Meas)o1).barcode.compareTo(((Meas)o2).barcode);
				}
				return o1.toString().compareTo(o2.toString());
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Measurement", ColumnDataType.Date, 100);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof String) return;
				Meas source = (Meas)cell.getElement();
				cell.setText(source.name);
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Analysis", ColumnDataType.Date, 250);
		config.setLabelProvider(new HyperlinkLabelProvider(plateTableViewer.getTable(), 3) {
			@Override
			protected String getText(Object o) {
				if (o instanceof String) return null;
				Meas source = (Meas)o;
				if (source.selectedAnalysis != null) return source.selectedAnalysis.name + " (" + source.selectedAnalysis.source + ")";
				else return null;
			}
			@Override
			protected boolean isHyperlinkEnabled(Object o) {
				if (o instanceof String) return false;
				Meas source = (Meas)o;
				return (source.availableAnalyses != null && source.availableAnalyses.length > 1);
			}
			@Override
			protected void handleLinkClick(Object o) {
				if (o instanceof String) return;
				showAnalysisPopup((Meas)o);
			}
		});
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
	
	private void screenSelected(Screen screen) {
		try {
			final List<Meas> sourceList = new ArrayList<>();
			
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask("Loading analysis results", IProgressMonitor.UNKNOWN);
					selectedMeasurements = ColumbusImportHelper.findMeasSources(screen, screenSelector.getSelectedInstanceId());
					for (Meas source: selectedMeasurements) {
						sourceList.add(source);
					}
					monitor.done();
				}
			});
			plateTableViewer.setInput(sourceList);
			
			if (sourceList.isEmpty()) {
				plateTableViewer.setInput(new String[]{"<No measurements found>"});
			}

			checkPageComplete();
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Failed to load analysis files", "Failed to load analysis files: " + e.getMessage());
		} catch (InterruptedException e) {
			// Cancel is disabled.
		}
	}
	
	private void checkPageComplete() {
		setPageComplete(selectedMeasurements != null && Arrays.stream(selectedMeasurements).anyMatch(m -> m.isIncluded && m.selectedAnalysis != null));
	}
	
	private void showAnalysisPopup(final Meas source) {
		if (analysisSelector != null) {
			closeAnalysisSelector();
		}
		
		analysisSelector = new Shell(getContainer().getShell(), SWT.NONE);
		analysisSelector.setSize(450, 60);
		analysisSelector.setLocation(Display.getDefault().getCursorLocation());
		GridLayoutFactory.fillDefaults().applyTo(analysisSelector);
		
		TableViewer tableViewer = new TableViewer(analysisSelector, SWT.FULL_SELECTION);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				MeasAnalysis analysis = SelectionUtils.getFirstObject(event.getSelection(), MeasAnalysis.class);
				source.selectedAnalysis = analysis;
				plateTableViewer.refresh();
				closeAnalysisSelector();
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl());
		
		TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
		col.getColumn().setWidth(440);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				MeasAnalysis a = (MeasAnalysis)cell.getElement();
				cell.setText(a.name + " (" + a.source + ")");
			}
		});
		
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(source.availableAnalyses);
		
		plateTableViewer.getControl().getShell().setCapture(false);
		analysisSelector.open();
	}
	
	private void closeAnalysisSelector() {
		if (analysisSelector != null && !analysisSelector.isDisposed()) {
			analysisSelector.close();
		}
		analysisSelector = null;
		plateTableViewer.getControl().getShell().setCapture(true);
	}
	
	private Image getImportIcon(boolean ok) {
		String icon = ok ? "accept.png" : "cancel.png";
		return IconManager.getIconImage(icon);
	}
}
