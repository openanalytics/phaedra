package eu.openanalytics.phaedra.ui.plate.inspector.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodLegend;
import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.autocomplete.ComboAutoCompleteField;
import eu.openanalytics.phaedra.base.ui.util.misc.ImageBlinker;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.ui.util.tooltip.AdvancedToolTip;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;
import eu.openanalytics.phaedra.ui.protocol.util.FeaturePropertyProvider;
import eu.openanalytics.phaedra.ui.protocol.util.PersonalColorMethodDialog;


public class FeatureInspector extends ViewPart {

	private ComboViewer groupCmb;
	private ComboViewer featureCmb;
	private ComboViewer normalizationCmb;

	private Label groupWarningLbl;
	private DefaultToolTip groupWarningTooltip;

	private ComboAutoCompleteField groupAutoComplete;
	private ComboAutoCompleteField featureAutoComplete;

	private RichTableViewer propertyTableViewer;

	private ColorMethodLegend colorMethodLegend;
	private Button expLimitBtn;
	private Button editBtn;

	private IUIEventListener featureSelectionListener;
	private ISelectionListener selectionListener;
	private IModelEventListener modelEventListener;

	private IColorMethod currentColorMethod;
	private ProtocolClass currentProtocolClass;
	private Plate currentPlate;
	private Well currentWell;

	private Job loadDataJob;

	@Override
	public void createPartControl(Composite parent) {

		GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(parent);

		SplitComposite splitComposite = SplitCompositeFactory.getInstance().prepare(null, SplitComposite.MODE_V_1_2).create(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(splitComposite);

		Composite top = new Composite(splitComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(top);
		GridLayoutFactory.fillDefaults().margins(3, 3).numColumns(1).applyTo(top);

		Composite bottom = new Composite(splitComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(bottom);
		GridLayoutFactory.fillDefaults().margins(3, 3).numColumns(1).applyTo(bottom);

		splitComposite.setWeights(new int[] { 80, 20 });

		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();
		mgr.add(splitComposite.createModeButton());

		// Active group

		Composite c = new Composite(top, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(c);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(c);

		Label lbl = new Label(c, SWT.NONE);
		lbl.setText("Active Group:");
		lbl.setToolTipText("Active Group");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);

		groupWarningLbl = new Label(c, SWT.NONE);
		groupWarningLbl.setImage(IconManager.getIconImage("error.png"));
		groupWarningLbl.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
		groupWarningTooltip = new DefaultToolTip(groupWarningLbl, AdvancedToolTip.NO_RECREATE, false);
		groupWarningTooltip.setShift(new Point(15,15));

		groupCmb = new ComboViewer(top, SWT.NONE);
		groupCmb.setContentProvider(new ArrayContentProvider());
		groupCmb.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				FeatureGroup group = (FeatureGroup) element;
				return group.getName() + " (" + ProtocolService.getInstance().getMembers(group).size() + "/" + group.getProtocolClass().getFeatures().size() + ")";
			}
		});
		groupCmb.addSelectionChangedListener(e -> selectNewGroup(SelectionUtils.getFirstObject(groupCmb.getSelection(), FeatureGroup.class)));
		Combo combo = groupCmb.getCombo();
		combo.setToolTipText("Active Group - Type to filter");
		groupAutoComplete = new ComboAutoCompleteField(groupCmb, combo.getItems());
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(combo);

		// Active feature

		lbl = new Label(top, SWT.NONE);
		lbl.setText("Active Feature:");
		lbl.setToolTipText("Active Feature");

		featureCmb = new ComboViewer(top, SWT.NONE);
		featureCmb.setContentProvider(new ArrayContentProvider());
		featureCmb.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Feature)
					return ((Feature) element).getDisplayName();
				return element.toString();
			}
		});
		featureCmb.addSelectionChangedListener(e -> selectNewFeature(SelectionUtils.getFirstObject(featureCmb.getSelection(), Feature.class)));
		combo = featureCmb.getCombo();
		combo.setToolTipText("Active Feature - Type to filter");
		featureAutoComplete = new ComboAutoCompleteField(featureCmb, combo.getItems());
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(combo);

		// Active normalization

		lbl = new Label(top, SWT.NONE);
		lbl.setText("Active Normalization:");
		lbl.setToolTipText("Active Normalization");

		normalizationCmb = new ComboViewer(top, SWT.READ_ONLY);
		normalizationCmb.setContentProvider(new ArrayContentProvider());
		normalizationCmb.setLabelProvider(new LabelProvider());
		normalizationCmb.addSelectionChangedListener(e -> selectNewNormalization(SelectionUtils.getFirstObject(e.getSelection(), String.class)));
		combo = normalizationCmb.getCombo();
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(combo);

		// Color method

		c = new Composite(top, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(c);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(c);

		colorMethodLegend = new ColorMethodLegend(c, SWT.VERTICAL, true);
		colorMethodLegend.addListener(SWT.Resize, e -> {
			if (colorMethodLegend.getSize().x > 130)
				colorMethodLegend.setSize(130, colorMethodLegend.getSize().y);
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(colorMethodLegend);

		Composite cRight = new Composite(c, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).applyTo(cRight);
		GridLayoutFactory.fillDefaults().applyTo(cRight);

		expLimitBtn = new Button(cRight, SWT.TOGGLE);
		expLimitBtn.setToolTipText("Toggle between Plate Limit and Experiment Limit");
		setExpLimitIcon();
		expLimitBtn.addListener(SWT.Selection, e -> {
			ProtocolUIService.getInstance().setExperimentLimit(expLimitBtn.getSelection());
			setExpLimitIcon();
			refreshColorMethod();
		});
		GridDataFactory.fillDefaults().applyTo(expLimitBtn);

		editBtn = new Button(cRight, SWT.PUSH);
		editBtn.setToolTipText("Change the color method");
		editBtn.setImage(IconManager.getIconImage("palette.png"));
		editBtn.addListener(SWT.Selection, e -> {
			if (currentColorMethod == null) return;
			Feature f = ProtocolUIService.getInstance().getCurrentFeature();
			if (f == null) return;
			new PersonalColorMethodDialog(Display.getCurrent().getActiveShell(), f).open();
		});
		GridDataFactory.fillDefaults().applyTo(editBtn);

		propertyTableViewer = new RichTableViewer(bottom, SWT.BORDER, getClass().getSimpleName());
		propertyTableViewer.setContentProvider(new ArrayContentProvider());
		propertyTableViewer.applyColumnConfig(createPropertyTableColumns());
		propertyTableViewer.setInput(FeaturePropertyProvider.getKeys());
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(propertyTableViewer.getTable());

		featureSelectionListener = event -> handleUIEvent(event);
		ProtocolUIService.getInstance().addUIEventListener(featureSelectionListener);

		selectionListener = (part, selection) -> {
			if (part == FeatureInspector.this) return;

			handleSelectionEvent(selection);
		};
		getSite().getPage().addSelectionListener(selectionListener);

		modelEventListener = event -> handleModelEvent(event);
		ModelEventService.getInstance().addEventListener(modelEventListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewFeatureInspector");
		initializeFields();
	}

	@Override
	public void dispose() {
		ProtocolUIService.getInstance().removeUIEventListener(featureSelectionListener);
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		featureCmb.getControl().setFocus();
	}

	/*
	 * Non-public
	 * **********
	 */

	private void selectNewGroup(FeatureGroup fg) {
		if (fg == null) return;
		// Clear featureCmb to prevent changing the FeatureGroup back to the previous one.
		featureCmb.setInput(null);
		ProtocolUIService.getInstance().setCurrentFeatureGroup(fg);
		Object[] features = createFeatureList(ProtocolUIService.getInstance().getCurrentProtocolClass());
		featureCmb.setInput(features);
		featureAutoComplete.setProposals(featureCmb.getCombo().getItems());
		Feature f = ProtocolUIService.getInstance().getCurrentFeature();
		if (f != null && CollectionUtils.contains(features, f)) {
			featureCmb.setSelection(new StructuredSelection(f));
		} else if (features.length > 0) {
			featureCmb.getCombo().select(0);
		}

		int groupSize = ProtocolService.getInstance().getMembers(fg).size();
		if (groupSize > 50) {
			String warningText = "This group contains " + groupSize + " features.\n"
					+ "This may have a negative impact on the performance of grids and tables.\n"
					+ "For better performance, select a smaller group of features.";
			groupWarningTooltip.setText(warningText);
			groupWarningLbl.setVisible(true);
			ImageBlinker.blinkImage(groupWarningLbl, "error.png", "error_glow.png", 8);
		} else {
			groupWarningLbl.setVisible(false);
		}
	}

	private void selectNewFeature(Feature f) {
		ProtocolUIService.getInstance().setCurrentFeature(f);
	}

	private void selectNewNormalization(String n) {
		ProtocolUIService.getInstance().setCurrentNormalization(n);
	}

	private void initializeFields() {
		// Initial values
		currentColorMethod = ColorMethodRegistry.getInstance().getDefaultColorMethod();
		currentColorMethod.configure(null);
		currentColorMethod.initialize(null);
		colorMethodLegend.setColorMethod(currentColorMethod);

		ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
		Feature f = ProtocolUIService.getInstance().getCurrentFeature();
		String[] normalizations = NormalizationService.getInstance().getNormalizations();
		String n = ProtocolUIService.getInstance().getCurrentNormalization();
		if (pClass != null) {
			groupCmb.setInput(ProtocolService.getInstance().getAllFeatureGroups(pClass, GroupType.WELL));
			groupCmb.setSelection(new StructuredSelection(ProtocolUIService.getInstance().getCurrentFeatureGroup()));
			groupAutoComplete.setProposals(groupCmb.getCombo().getItems());
			featureCmb.setInput(createFeatureList(pClass));
			featureAutoComplete.setProposals(featureCmb.getCombo().getItems());
		}
		if (f != null) {
			featureCmb.setSelection(new StructuredSelection(f));
			currentColorMethod = ColorMethodFactory.createColorMethod(f);
			currentColorMethod.configure(f.getColorMethodSettings());
			currentColorMethod.initialize(null);
			colorMethodLegend.setColorMethod(currentColorMethod);
		}
		normalizationCmb.setInput(normalizations);
		if (n != null)
			normalizationCmb.setSelection(new StructuredSelection(n));
	}

	private void handleUIEvent(UIEvent event) {
		if (event.type == EventType.FeatureSelectionChanged) {
			Feature f = ProtocolUIService.getInstance().getCurrentFeature();
			if (!f.getProtocolClass().equals(currentProtocolClass)) {
				// Feature belongs to a different protocol class
				currentProtocolClass = f.getProtocolClass();
				groupCmb.setInput(ProtocolService.getInstance().getAllFeatureGroups(currentProtocolClass, GroupType.WELL));
				groupCmb.setSelection(new StructuredSelection(ProtocolUIService.getInstance().getCurrentFeatureGroup()));
				groupAutoComplete.setProposals(groupCmb.getCombo().getItems());
				featureCmb.setInput(createFeatureList(currentProtocolClass));
				featureAutoComplete.setProposals(featureCmb.getCombo().getItems());
			}
			featureCmb.setSelection(new StructuredSelection(f));
			normalizationCmb.setInput(NormalizationService.getInstance().getNormalizations());
			refreshPropertiesTable();
			currentColorMethod = ColorMethodFactory.createColorMethod(f);
			refreshColorMethod();
		} else if (event.type == EventType.NormalizationSelectionChanged) {
			String n = ProtocolUIService.getInstance().getCurrentNormalization();
			normalizationCmb.setSelection(new StructuredSelection(n));
			refreshColorMethodHighlight();
			refreshColorMethod();
		}
	}

	private void handleModelEvent(ModelEvent event) {
		if (event.type == ModelEventType.Calculated) {
			Plate plate = (Plate) event.source;
			if (plate != null && plate.equals(currentPlate)) {
				Display.getDefault().asyncExec(() -> refreshColorMethod());
			}
		} else if (event.type == ModelEventType.ObjectChanged && event.source instanceof ProtocolClass) {
			final ProtocolClass pc = (ProtocolClass) event.source;
			ProtocolClass currentPc = ProtocolUIService.getInstance().getCurrentProtocolClass();

			if (currentPc != null && pc.equals(currentPc)) {
				// The current protocol class was changed. Refresh color legend.
				Feature currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
				if (currentFeature == null)
					return;
				currentColorMethod = ColorMethodFactory.createColorMethod(currentFeature);
				Display.getDefault().asyncExec(() -> refreshColorMethod());

				// Or the list of features may have changed.
				Display.getDefault().asyncExec(() -> {
					ISelection selection = featureCmb.getSelection();
					groupCmb.setInput(ProtocolService.getInstance().getAllFeatureGroups(pc, GroupType.WELL));
					groupCmb.setSelection(new StructuredSelection(ProtocolUIService.getInstance().getCurrentFeatureGroup()));
					groupAutoComplete.setProposals(groupCmb.getCombo().getItems());
					featureCmb.setInput(createFeatureList(pc));
					featureCmb.setSelection(selection);
					featureAutoComplete.setProposals(featureCmb.getCombo().getItems());
				});
			}
		}
	}

	private void handleSelectionEvent(ISelection selection) {
		boolean rebuildColorMethod = false;
		Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
		if (plate != null && !plate.equals(currentPlate)) {
			currentPlate = plate;
			rebuildColorMethod = true;
		} else if (plate == null && currentPlate != null) {
			currentPlate = null;
			rebuildColorMethod = true;
		}
		Well newWell = SelectionUtils.getFirstObject(selection, Well.class);
		if (newWell != currentWell) {
			currentWell = newWell;
			refreshColorMethodHighlight();
		}
		if (rebuildColorMethod)
			refreshColorMethod();
	}

	private void refreshColorMethodHighlight() {
		JobUtils.runBackgroundJob(() -> {
			// Prevent currentWell from changing further down in the method.
			final Well well = currentWell;
			final double[] values;
			if (well == null) {
				values = null;
			} else {
				PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				String n = ProtocolUIService.getInstance().getCurrentNormalization();
				values = new double[] { accessor.getNumericValue(well, f, n) };
			}

			Display.getDefault().asyncExec(() -> {
				if (colorMethodLegend == null || colorMethodLegend.isDisposed()) return;
				colorMethodLegend.setHighlightValues(values);
				colorMethodLegend.setColorMethod(currentColorMethod);
			});
		});
	}

	private void refreshColorMethod() {
		if (currentColorMethod == null)
			return;

		if (currentPlate == null) {
			currentColorMethod.initialize(null);
			colorMethodLegend.setColorMethod(currentColorMethod);
		} else {
			final Feature currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
			final String currentNorm = ProtocolUIService.getInstance().getCurrentNormalization();

			if (loadDataJob != null) {
				loadDataJob.cancel();
			}

			loadDataJob = new Job("Loading data") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (monitor.isCanceled() || currentPlate == null || currentFeature == null)
						return Status.CANCEL_STATUS;

					// Load persistent stats which usually suffice to create
					// color methods.
					StatService.getInstance().loadPersistentPlateStats(currentPlate);

					// Sometimes the plate is set to null during execution of
					// this job.
					if (monitor.isCanceled() || currentPlate == null) return Status.CANCEL_STATUS;

					IColorMethodData data;
					if (ProtocolUIService.getInstance().isExperimentLimit()) {
						data = ColorMethodFactory.createData(currentPlate.getExperiment(), currentFeature, currentNorm);
					} else {
						data = ColorMethodFactory
								.createData(CalculationService.getInstance().getAccessor(currentPlate), currentFeature,
										currentNorm);
					}

					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					if (currentColorMethod != null)
						currentColorMethod.initialize(data);

					Display.getDefault().syncExec(() -> colorMethodLegend.setColorMethod(currentColorMethod));

					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					return Status.OK_STATUS;
				}
			};
			loadDataJob.setRule(new LoadDataJobRule());
			loadDataJob.schedule();
		}
	}

	private void refreshPropertiesTable() {
		propertyTableViewer.setInput(FeaturePropertyProvider.getKeys());
	}

	private ColumnConfiguration[] createPropertyTableColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Property", ColumnDataType.String, 90);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(cell.getElement().toString());
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Value", ColumnDataType.String, 150);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String name = cell.getElement().toString();
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				if (f == null)
					return;
				String value = FeaturePropertyProvider.getValue(name, f);
				cell.setText(value);
			}
		});
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static class LoadDataJobRule implements ISchedulingRule {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return (rule instanceof LoadDataJobRule);
		}
	}

	private Object[] createFeatureList(ProtocolClass pClass) {
		FeatureGroup fg = ProtocolUIService.getInstance().getCurrentFeatureGroup();
		List<Feature> features = ProtocolService.getInstance().getMembers(fg);
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);
		return features.toArray();
	}

	private void setExpLimitIcon() {
		if (ProtocolUIService.getInstance().isExperimentLimit()) {
			expLimitBtn.setImage(IconManager.getIconImage("limit_WP_E.png"));
		} else {
			expLimitBtn.setImage(IconManager.getIconImage("limit_WP_P.png"));
		}
	}

}