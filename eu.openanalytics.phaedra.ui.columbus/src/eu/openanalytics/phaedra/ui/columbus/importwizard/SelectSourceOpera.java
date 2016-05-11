package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import eu.openanalytics.phaedra.base.ui.util.misc.FolderSelector;
import eu.openanalytics.phaedra.base.ui.util.misc.HyperlinkLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.DelayedTrigger;
import eu.openanalytics.phaedra.ui.columbus.importwizard.Meas.MeasAnalysis;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class SelectSourceOpera extends BaseStatefulWizardPage {

	private FolderSelector sourceFolderSelector;
	private RichTableViewer plateTableViewer;
	
	private Button importImagesBtn;
	private FolderSelector imageFolderSelector;
	private Label imagePathIconLbl;
	private Label imagePathInfoLbl;
	
	private Button importSubwellDataBtn;
	private FolderSelector subwellFolderSelector;
	private Label subwellDataPathIconLbl;
	private Label subwellDataPathInfoLbl;
	
	private DelayedTrigger sourceSelectionTrigger;
	private Shell analysisSelector;
	
	private String sourcePath;
	private Meas[] selectedMeasurements;
	private boolean importImages;
	private boolean importSubwellData;
	private String imagePath;
	private String subwellDataPath;
	
	public SelectSourceOpera() {
		super("Select Source");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		Group group = new Group(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		
		Label lbl = new Label(group, SWT.NONE);
		lbl.setText("Select an experiment folder:");
		GridDataFactory.fillDefaults().applyTo(lbl);
		
		sourceSelectionTrigger = new DelayedTrigger();
		sourceFolderSelector = new FolderSelector(group, SWT.NONE);
		sourceFolderSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = (String)e.data;
				if (text.isEmpty()) return;
				final File file = new File(text);
				sourceSelectionTrigger.schedule(500, true, new Runnable() {
					@Override
					public void run() {
						sourceSelected(file);
					}
				});
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sourceFolderSelector);
		
		new Label(group, SWT.NONE).setText("Plates found:");
		
		plateTableViewer = new RichTableViewer(group, SWT.BORDER | SWT.FULL_SELECTION);
		plateTableViewer.setContentProvider(new ArrayContentProvider());
		plateTableViewer.applyColumnConfig(createColumns());
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 150).applyTo(plateTableViewer.getControl());
		
		group = new Group(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		
		importImagesBtn = new Button(group, SWT.CHECK);
		importImagesBtn.setText("Import images from:");
		importImagesBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importImages = importImagesBtn.getSelection();
				imageFolderSelector.setEnabled(importImages);
				imagePathInfoLbl.setEnabled(importImages);
			}
		});
		
		imageFolderSelector = new FolderSelector(group, SWT.NONE);
		imageFolderSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = (String)e.data;
				imagePathSelected(text);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(imageFolderSelector);
		
		Composite infoComposite = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(20, 0).applyTo(infoComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(infoComposite);
		
		imagePathIconLbl = new Label(infoComposite, SWT.NONE);
		GridDataFactory.fillDefaults().hint(16, 16).applyTo(imagePathIconLbl);
		imagePathInfoLbl = new Label(infoComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(imagePathInfoLbl);
		
		group = new Group(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		
		importSubwellDataBtn = new Button(group, SWT.CHECK);
		importSubwellDataBtn.setText("Import subwell data from:");
		importSubwellDataBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importSubwellData = importSubwellDataBtn.getSelection();
				subwellFolderSelector.setEnabled(importSubwellData);
				subwellDataPathInfoLbl.setEnabled(importSubwellData);
			}
		});
		
		subwellFolderSelector = new FolderSelector(group, SWT.NONE);
		subwellFolderSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = (String)e.data;
				subwellDataPathSelected(text);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subwellFolderSelector);
		
		infoComposite = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(20, 0).applyTo(infoComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(infoComposite);
		
		subwellDataPathIconLbl = new Label(infoComposite, SWT.NONE);
		GridDataFactory.fillDefaults().hint(16, 16).applyTo(subwellDataPathIconLbl);
		subwellDataPathInfoLbl = new Label(infoComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subwellDataPathInfoLbl);
		
		setTitle("Select Source");
    	setDescription("Select a folder containing the data you want to import."
    			+ "\nIf there are multiple analysis files, select one by clicking on the analysis name.");
    	setControl(container);
    	setPageComplete(false);
	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		if (firstTime) {
			sourceFolderSelector.setSelectedFolder(OperaImportHelper.getSourceBaseLocation());
			imageFolderSelector.setSelectedFolder(OperaImportHelper.getImageBaseLocation());
			subwellFolderSelector.setSelectedFolder(OperaImportHelper.getSubwellBaseLocation());
			importImages = true;
			importSubwellData = true;
			importImagesBtn.setSelection(importImages);
			importSubwellDataBtn.setSelection(importSubwellData);			
		}
	}
	
	@Override
	public void collectState(IWizardState state) {
		ImportWizardState s = (ImportWizardState)state;
		s.task.sourcePath = sourcePath;
		s.task.importImageData = importImages;
		s.task.importSubWellData = importSubwellData;
		s.task.getParameters().put(OperaImportHelper.PARAM_MEAS_SOURCES, Lists.newArrayList(selectedMeasurements));
		s.task.getParameters().put(OperaImportHelper.PARAM_IMG_BASE_PATH, imagePath);
		s.task.getParameters().put(OperaImportHelper.PARAM_SW_BASE_PATH, subwellDataPath);
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
					imagePathSelected(imagePath);
					subwellDataPathSelected(subwellDataPath);
					plateTableViewer.refresh();
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
				if (source.selectedAnalysis != null) return source.selectedAnalysis.name;
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
	
	private void sourceSelected(final File source) {
		this.sourcePath = source.getAbsolutePath();
		
		try {
			final List<Meas> sourceList = new ArrayList<>();
			
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask("Locating analysis files", IProgressMonitor.UNKNOWN);
					monitor.subTask("Scanning folder " + source);
					selectedMeasurements = OperaImportHelper.findMeasSources(source.getAbsolutePath());
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
			
			imagePathSelected(imagePath);
			subwellDataPathSelected(subwellDataPath);
			setPageComplete(!sourceList.isEmpty());
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Failed to load analysis files", "Failed to load analysis files: " + e.getMessage());
		} catch (InterruptedException e) {
			// Cancel is disabled.
		}
	}
	
	private void imagePathSelected(final String path) {
		this.imagePath = path;
		
		if (selectedMeasurements == null || selectedMeasurements.length == 0) {
			imagePathIconLbl.setImage(getStatusIcon(false));
			imagePathInfoLbl.setText("Please select a valid experiment folder first");
			return;
		}
		imagePathIconLbl.setImage(null);
		imagePathInfoLbl.setText("");
	}
	
	private void subwellDataPathSelected(final String path) {
		this.subwellDataPath = path;
		
		if (selectedMeasurements == null || selectedMeasurements.length == 0) {
			subwellDataPathIconLbl.setImage(getStatusIcon(false));
			subwellDataPathInfoLbl.setText("Please select a valid experiment folder first");
			return;
		}
		subwellDataPathIconLbl.setImage(null);
		subwellDataPathInfoLbl.setText("");
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
				cell.setText(((MeasAnalysis)cell.getElement()).name);
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
	
	private Image getStatusIcon(boolean ok) {
		String icon = ok ? "accept.png" : "error.png";
		return IconManager.getIconImage(icon);
	}
}
