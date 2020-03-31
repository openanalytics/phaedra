package eu.openanalytics.phaedra.ui.plate.inspector.experiment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class ExperimentInspector extends DecoratedView {

	private static final String LOADING = "Loading...";

	private BreadcrumbViewer breadcrumb;

	private FormToolkit formToolkit;

	private Text nameTxt, idTxt, creationDateTxt, creatorTxt, descriptionTxt,
		commentsTxt, protocolTxt, protocolClassTxt, sizeSubwellDataTxt, sizeImageDataTxt;

	private Label[] validationLbls;
	private Label[] approvalLbls;
	private Label[] exportLbls;

	private Label currentFeatureLbl;
	private RichTableViewer statsTableViewer;

	private ISelectionListener selectionListener;
	private IUIEventListener featureListener;
	private volatile Experiment currentExperiment;
	private volatile Feature currentFeature;

	private final static Color LBL_BG_RED = new Color(null, 255, 170, 170);
	private final static Color LBL_BG_GREEN = new Color(null, 170, 255, 170);

	private ExperimentStatistics expStats;
	private FeatureStatistics featureStats;

	@Override
	public void createPartControl(Composite parent) {
		formToolkit = FormEditorUtils.createToolkit();

		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		Label separator = formToolkit.createSeparator(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

		final ScrolledForm form = FormEditorUtils.createScrolledForm("Experiment:\n<none selected>", 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);

		// Section 1: Experiment Properties ------------------------------

		Section section = FormEditorUtils.createSection("Properties", form.getBody(), formToolkit);
		Composite sectionContainer = FormEditorUtils.createComposite(2, section, formToolkit);

		FormEditorUtils.createLabel("Name", sectionContainer, formToolkit);
		nameTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Id", sectionContainer, formToolkit);
		idTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Creation date", sectionContainer, formToolkit);
		creationDateTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Creator", sectionContainer, formToolkit);
		creatorTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Description", sectionContainer, formToolkit);
		descriptionTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Comments", sectionContainer, formToolkit);
		commentsTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Protocol", sectionContainer, formToolkit);
		protocolTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Protocol Class", sectionContainer, formToolkit);
		protocolClassTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Size of Subwell Data", sectionContainer, formToolkit);
		sizeSubwellDataTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Size of Image Data", sectionContainer, formToolkit);
		sizeImageDataTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		// Section 2: Experiment Status ------------------------------

		section = FormEditorUtils.createSection("Status", form.getBody(), formToolkit);
		sectionContainer = FormEditorUtils.createComposite(4, section, formToolkit);

		Color[] lblcolors = { LBL_BG_RED, LBL_BG_GREEN, null };

		FormEditorUtils.createLabel("Validation", sectionContainer, formToolkit);
		validationLbls = new Label[3];
		String[] tooltips = new String[]{ "Invalidated", "Validated", "Validation Not Set" };
		for (int i=0; i<validationLbls.length; i++) {
			validationLbls[i] = FormEditorUtils.createLabel(" 0", sectionContainer, formToolkit);
			validationLbls[i].setBackground(lblcolors[i]);
			validationLbls[i].setToolTipText(tooltips[i]);
			GridDataFactory.fillDefaults().hint(40, SWT.DEFAULT).applyTo(validationLbls[i]);
		}

		FormEditorUtils.createLabel("Approval", sectionContainer, formToolkit);
		approvalLbls = new Label[3];
		tooltips = new String[]{ "Disapproved", "Approved", "Approval Not Set" };
		for (int i=0; i<approvalLbls.length; i++) {
			approvalLbls[i] = FormEditorUtils.createLabel(" 0", sectionContainer, formToolkit);
			approvalLbls[i].setBackground(lblcolors[i]);
			approvalLbls[i].setToolTipText(tooltips[i]);
			GridDataFactory.fillDefaults().hint(40, SWT.DEFAULT).applyTo(approvalLbls[i]);
		}

		FormEditorUtils.createLabel("Export", sectionContainer, formToolkit);
		exportLbls = new Label[3];
		tooltips = new String[]{ "", "Exported", "Not Yet Exported" };
		for (int i=0; i<exportLbls.length; i++) {
			exportLbls[i] = FormEditorUtils.createLabel(" 0", sectionContainer, formToolkit);
			exportLbls[i].setBackground(lblcolors[i]);
			exportLbls[i].setToolTipText(tooltips[i]);
			GridDataFactory.fillDefaults().hint(40, SWT.DEFAULT).applyTo(exportLbls[i]);
		}

		// Section 3: Experiment Quality Stats ------------------------------

		section = FormEditorUtils.createSection("Quality", form.getBody(), formToolkit);
		sectionContainer = FormEditorUtils.createComposite(2, section, formToolkit);

		currentFeatureLbl = FormEditorUtils.createLabelPair("Selected Feature", sectionContainer, formToolkit);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(currentFeatureLbl);

		statsTableViewer = new RichTableViewer(sectionContainer, SWT.BORDER);
		statsTableViewer.setContentProvider(new ArrayContentProvider());
		statsTableViewer.setInput(new String[] { "mean", "stdev", "median", "mad", "min", "max" });
		statsTableViewer.applyColumnConfig(configureColumns());
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(statsTableViewer.getControl());

		// Selection handling
		selectionListener = (part, selection) -> {
			Experiment experiment = SelectionUtils.getFirstObject(selection, Experiment.class);
			if (experiment != null && !experiment.equals(currentExperiment)) {
				currentExperiment = experiment;
				form.setText("Experiment:\n" + currentExperiment.getName());
				loadExperiment();
				form.reflow(true);
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);
		featureListener = event -> {
			if (event.type == EventType.FeatureSelectionChanged) {
				Experiment experiment = currentExperiment;
				Feature feature = ProtocolUIService.getInstance().getCurrentFeature();
				currentFeature = feature;
				JobUtils.runUserJob(monitor -> {
					loadFeatureStats(experiment, feature);
				}, "Updating Experiment Inspector", 100, toString() + "FS", null);
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ProtocolUIService.getInstance().removeUIEventListener(featureListener);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					ProtocolUIService.getInstance().addUIEventListener(featureListener);
				}
			}
		});
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
		SelectionUtils.triggerActiveSelection(selectionListener);
		if (currentExperiment == null) SelectionUtils.triggerActiveEditorSelection(selectionListener);

		form.reflow(true);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewExperimentInspector");
	}

	@Override
	public void setFocus() {
		nameTxt.getParent().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(featureListener);
		super.dispose();
	}

	private void loadExperiment() {
		Experiment experiment = currentExperiment;
		Feature feature = currentFeature;
		
		expStats = new ExperimentStatistics(experiment);
		expStats.load();
		featureStats = null;

		breadcrumb.setInput(experiment);
		breadcrumb.getControl().getParent().layout();

		nameTxt.setText(experiment.getName());
		idTxt.setText("" + experiment.getId());
		creationDateTxt.setText(experiment.getCreateDate().toString());
		creatorTxt.setText(experiment.getCreator());

		String description = experiment.getDescription();
		if (description == null || description.isEmpty()) description = "<None>";
		descriptionTxt.setText(description);
		String comments = experiment.getComments();
		if (comments == null || comments.isEmpty()) comments = "<None>";
		commentsTxt.setText(comments);

		protocolTxt.setText(experiment.getProtocol().toString());
		protocolClassTxt.setText(experiment.getProtocol().getProtocolClass().toString());

		sizeImageDataTxt.setText(LOADING);
		sizeSubwellDataTxt.setText(LOADING);

		validationLbls[0].setText(String.valueOf(expStats.getNrOfInvalidPlates()));
		validationLbls[1].setText(String.valueOf(expStats.getNrOfValidPlates()));
		validationLbls[2].setText(String.valueOf(expStats.getNrOfUnvalidatedPlates()));

		approvalLbls[0].setText(String.valueOf(expStats.getNrOfDisapprovedPlates()));
		approvalLbls[1].setText(String.valueOf(expStats.getNrOfApprovedPlates()));
		approvalLbls[2].setText(String.valueOf(expStats.getNrOfUnapprovedPlates()));

		exportLbls[0].setText(String.valueOf(expStats.getNrOfNotUploadedPlates()));
		exportLbls[1].setText(String.valueOf(expStats.getNrOfUploadedPlates()));
		exportLbls[2].setText(String.valueOf(expStats.getNrOfUploadNotSetPlates()));

		statsTableViewer.refresh();

		JobUtils.runUserJob(monitor -> {
			if (experiment != currentExperiment) return;
			
			List<Plate> plates = PlateService.getInstance().getPlates(experiment);
			long imageSize = 0l;
			long hdf5Size = 0l;
			for (Plate p : plates) {
				String imgPath = PlateService.getInstance().getImageFSPath(p);
				imageSize += getFileSize(imgPath);

				String hdf5Path = PlateService.getInstance().getPlateFSPath(p);
				hdf5Size += getFileSize(hdf5Path + "/" + p.getId() + ".h5");
			}
			long imageSizeFinal = imageSize;
			long hdf5SizeFinal = hdf5Size;
			Display.getDefault().asyncExec(() -> {
				if (sizeImageDataTxt.isDisposed()
						|| experiment != currentExperiment) return;
				
				sizeImageDataTxt.setText(FileUtils.getHumanReadableByteCount(imageSizeFinal, false));
				sizeSubwellDataTxt.setText(FileUtils.getHumanReadableByteCount(hdf5SizeFinal, false));
			});

			// calc.calculate() is called in this method.
			loadFeatureStats(experiment, feature);
		}, "Loading Experiment data (" + currentExperiment.getName() + ")", 100, toString(), null);
	}

	private long getFileSize(String filePath) {
		if (filePath != null) {
			try {
				return Screening.getEnvironment().getFileServer().getLength(filePath);
			} catch (IOException e) {}
		}
		return 0;
	}

	private void loadFeatureStats(Experiment experiment, Feature feature) {
		if (experiment != currentExperiment || feature != currentFeature) return;
		
		FeatureStatistics stats = new FeatureStatistics(experiment, feature);
		stats.load();
		
		Display.getDefault().asyncExec(() -> {
			if (statsTableViewer.getTable().isDisposed()
					|| experiment != currentExperiment || feature != currentFeature) return;
			
			featureStats = stats;
			
			currentFeatureLbl.setText(feature == null ? "" : feature.getDisplayName());
			statsTableViewer.refresh();
		});
	}

	private ColumnConfiguration[] configureColumns() {

		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("", DataType.String, 60);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(cell.getElement().toString());
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Z-Prime", DataType.Real, 60);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				FeatureStatistics stats = featureStats;
				if (stats == null) {
					cell.setText("...");
				} else {
					String stat = cell.getElement().toString();
					String value = NumberUtils.round(StatService.getInstance().calculate(stat, stats.getZPrimes()), 2);
					cell.setText(value);
				}
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("S/N", DataType.Real, 60);
		config.setTooltip("Signal to Noise ratio");
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				FeatureStatistics stats = featureStats;
				if (stats == null) {
					cell.setText("...");
				} else {
					String stat = cell.getElement().toString();
					String value = NumberUtils.round(StatService.getInstance().calculate(stat, stats.getSNS()), 2);
					cell.setText(value);
				}
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("S/B", DataType.Real, 60);
		config.setTooltip("Signal to Background ratio");
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				FeatureStatistics stats = featureStats;
				if (stats == null) {
					cell.setText("...");
				} else {
					String stat = cell.getElement().toString();
					String value = NumberUtils.round(StatService.getInstance().calculate(stat, stats.getSBS()), 2);
					cell.setText(value);
				}
			}
		});
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
