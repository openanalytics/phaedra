package eu.openanalytics.phaedra.ui.plate.inspector.well;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.WellDataAccessor;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.log.vo.ObjectLog;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.util.FeaturePatternFilter;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

public class WellInspector extends DecoratedView {
	
	
	private DataFormatSupport dataFormatSupport;
	
	private BreadcrumbViewer breadcrumb;

	private FormToolkit formToolkit;

	private Text barcodeTxt, statusTxt, positionTxt, wellTypeTxt, compoundTxt, concTxt, descriptionTxt;
	
	private TreeViewer treeViewer;
	private RichTableViewer historyTableViewer;

	private ISelectionListener selectionListener;
	private IModelEventListener modelEventListener;
	
	private Well currentWell;

	@Override
	public void createPartControl(Composite parent) {
		this.dataFormatSupport = new DataFormatSupport(this::loadWell);
		
		formToolkit = FormEditorUtils.createToolkit();
		
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		final ScrolledForm form = FormEditorUtils.createScrolledForm("Well: <no well selected>", 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);

		// Section 1: Well Properties ------------------------------

		Section section = FormEditorUtils.createSection("Properties", form.getBody(), formToolkit);
		Composite sectionContainer = FormEditorUtils.createComposite(2, section, formToolkit);

		FormEditorUtils.createLabel("Barcode", sectionContainer, formToolkit);
		barcodeTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);
		
		FormEditorUtils.createLabel("Status", sectionContainer, formToolkit);
		statusTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);
		
		FormEditorUtils.createLabel("Position", sectionContainer, formToolkit);
		positionTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Well Type", sectionContainer, formToolkit);
		wellTypeTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Compound", sectionContainer, formToolkit);
		compoundTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Concentration", sectionContainer, formToolkit);
		concTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		FormEditorUtils.createLabel("Description", sectionContainer, formToolkit);
		descriptionTxt = FormEditorUtils.createReadOnlyText("", sectionContainer, formToolkit);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(sectionContainer);

		// Section 2: Feature Values ------------------------------

		section = FormEditorUtils.createSection("Features", form.getBody(), formToolkit, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
		sectionContainer = FormEditorUtils.createComposite(1, section, formToolkit);

		FilteredTree filteredTree = new FilteredTree(sectionContainer
				, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
				, new FeaturePatternFilter(), true);
		treeViewer = filteredTree.getViewer();
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.stateMask == SWT.CTRL && e.keyCode == 'c') {
					List<Feature> features = SelectionUtils.getObjects(treeViewer.getSelection(), Feature.class);
					String value = features.stream().map(f -> {
						return IntStream.range(0, 3)
							.mapToObj(i -> getFeatureValue(i, currentWell, f))
							.map(v -> { if (v == null) return ""; else return v; })
							.collect(Collectors.joining("\t"));
					}).collect(Collectors.joining("\n"));
					CopyItems.execute(value);
				}
			}
		});
		
		createFeatureTableColumns();
		treeViewer.setContentProvider(new WellFeatureContentProvider());
		treeViewer.setLabelProvider(new WellFeatureLabelProvider());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(filteredTree);

		// Section 3: History ------------------------------

		section = FormEditorUtils.createSection("History", form.getBody(), formToolkit, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
		sectionContainer = FormEditorUtils.createComposite(1, section, formToolkit);

		Table t = formToolkit.createTable(sectionContainer
				, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(t);

		historyTableViewer = new RichTableViewer(t);
		historyTableViewer.setContentProvider(new ArrayContentProvider());
		historyTableViewer.applyColumnConfig(createHistoryTableColumns());

		// Selection handling -----------------------------------
		selectionListener = (part, selection) -> {
			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well != null && !well.equals(currentWell)) {
				currentWell = well;
				String pos = NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn());
				form.setText("Well: " + pos);
				loadWell();
				form.reflow(true);
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		modelEventListener = event -> {
			boolean refreshWell = false;
			if (event.type == ModelEventType.ValidationChanged || event.type == ModelEventType.Calculated) {
				if (event.source instanceof Well) {
					Well well = (Well)event.source;
					if (well.equals(currentWell)) refreshWell = true;
				} else if (event.source instanceof Plate) {
					Plate plate = (Plate)event.source;
					if (currentWell != null && plate.equals(currentWell.getPlate())) refreshWell = true;
				}
			}
			if (refreshWell) {
				Display.getDefault().asyncExec(() -> loadWell());
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ModelEventService.getInstance().removeEventListener(modelEventListener);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					ModelEventService.getInstance().addEventListener(modelEventListener);
				}
			}
		});
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewWellInspector");
	}

	@Override
	public void setFocus() {
		treeViewer.getTree().setFocus();
	}

	@Override
	public void dispose() {
		if (this.dataFormatSupport != null) this.dataFormatSupport.dispose();
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
		super.dispose();
	}

	private void loadWell() {
		if (treeViewer == null || treeViewer.getControl().isDisposed()
				|| currentWell == null) {
			return;
		}

		breadcrumb.setInput(currentWell);
		breadcrumb.getControl().getParent().layout();

		barcodeTxt.setText(currentWell.getPlate().getBarcode());

		WellStatus status = WellStatus.getByCode(currentWell.getStatus());
		if (status == null) statusTxt.setText("" + currentWell.getStatus());
		else statusTxt.setText(status.getLabel());

		String position = NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn());
		position += " (" + currentWell.getRow() + ", " + currentWell.getColumn() + ")";
		positionTxt.setText(position);

		wellTypeTxt.setText(currentWell.getWellType());

		Compound c = currentWell.getCompound();
		if (c == null) {
			compoundTxt.setText("<None>");
		} else {
			compoundTxt.setText(c.toString());
		}
		{	final WellProperty property = WellProperty.Concentration;
			final ConcentrationDataDescription dataDescription = (ConcentrationDataDescription)property.getDataDescription();
			concTxt.setText(this.dataFormatSupport.get().getConcentrationEditFormat(dataDescription)
					.format(property.getValue(currentWell), dataDescription.getConcentrationUnit()) );
		}

		descriptionTxt.setText(currentWell.getDescription() == null ? "" : currentWell.getDescription());

		TreePath[] expandedTreePaths = treeViewer.getExpandedTreePaths();
		treeViewer.setInput("Loading...");

		JobUtils.runUserJob(monitor -> {
			// All feature values will be listed in the table, so load the data eagerly.
			List<Feature> features = PlateUtils.getFeatures(currentWell);
			WellDataAccessor.fetchFeatureValues(currentWell, features, true);

			if (monitor.isCanceled()) return;
			Display.getDefault().asyncExec(() -> {
				if (treeViewer.getTree().isDisposed()) return;
				treeViewer.setInput(currentWell);
				treeViewer.setExpandedTreePaths(expandedTreePaths);
			});
		}, "Loading Well Feature Data", 100, toString(), null);

		historyTableViewer.setInput(ObjectLogService.getInstance().getHistory(currentWell));
	}

	private static String getFeatureValue(int colIndex, Well well, Feature feature) {
		String text = null;
		if (colIndex == 0) {
			text = feature.getName();
		} else if (colIndex == 1) {
			if (feature.isNumeric() && ProtocolUtils.isNormalized(feature)) {
				String norm = feature.getNormalization();
				text = getFormattedFeatureValue(well, feature, norm);
			}
		} else {
			text = getFormattedFeatureValue(well, feature, null);
		}
		return text;
	}
	private static String getFormattedFeatureValue(Well well, Feature feature, String norm) {
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
		if (feature.isNumeric()) {
			double numericValue = accessor.getNumericValue(well, feature, norm);
			return Formatters.getInstance().format(numericValue, feature);
		}
		return accessor.getStringValue(well, feature);
	}

	private class WellFeatureLabelProvider extends CellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			String text = "";
			if (element instanceof Feature) {
				Feature feature = (Feature) element;
				text = getFeatureValue(cell.getColumnIndex(), currentWell, feature);
			}
			if (element instanceof FeatureGroup) {
				if (cell.getColumnIndex() == 0) {
					text = ((FeatureGroup) element).getName();
				}
			}
			if (element instanceof String) {
				if (cell.getColumnIndex() == 0) {
					text = (String) element;
				}
			}
			cell.setText(text);
		}
	}

	private class WellFeatureContentProvider implements ITreeContentProvider {
		private ProtocolClass pClass;
		@Override
		public void dispose() {
			// Do nothing.
		}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Well) {
				pClass = PlateUtils.getProtocolClass((Well) inputElement);
				List<FeatureGroup> fgs = ProtocolService.getInstance().getAllFeatureGroups(pClass, GroupType.WELL);
				return fgs.toArray();
			}
			if (inputElement instanceof String) {
				return new Object[] { inputElement };
			}
			return null;
		}
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof FeatureGroup) {
				List<Feature> featuresByGroup = ProtocolService.getInstance().getMembers((FeatureGroup) parentElement);
				Collections.sort(featuresByGroup, ProtocolUtils.FEATURE_NAME_SORTER);
				return featuresByGroup.toArray();
			}
			return null;
		}
		@Override
		public Object getParent(Object element) {
			return null;
		}
		@Override
		public boolean hasChildren(Object element) {
			return element instanceof FeatureGroup;
		}
	}

	private void createFeatureTableColumns() {
		TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		TreeColumn col = column.getColumn();
		col.setAlignment(SWT.LEFT);
		col.setText("Feature");
		col.setToolTipText("Feature (Group)");
		col.setWidth(180);
		new ColumnViewerSorter<>(column, (o1, o2) -> {
			if (o1 instanceof FeatureGroup && o2 instanceof FeatureGroup) {
				FeatureGroup f1 = (FeatureGroup) o1;
				FeatureGroup f2 = (FeatureGroup) o2;
				return f1.getName().compareTo(f2.getName());
			}
			if (o1 instanceof IFeature && o2 instanceof IFeature) {
				IFeature f1 = (IFeature) o1;
				IFeature f2 = (IFeature) o2;
				return f1.getName().compareTo(f2.getName());
			}
			return 0;
		});
		column = new TreeViewerColumn(treeViewer, SWT.RIGHT);
		col = column.getColumn();
		col.setAlignment(SWT.RIGHT);
		col.setText("Normalized");
		col.setToolTipText("Normalized Feature Value");
		col.setWidth(100);
		new ColumnViewerSorter<>(column, (o1, o2) -> {
			if (o1 instanceof FeatureGroup && o2 instanceof Feature) {
				Feature f1 = (Feature) o1;
				Feature f2 = (Feature) o2;
				String norm1 = f1.getNormalization();
				String norm2 = f2.getNormalization();
				String t1 = "";
				String t2 = "";
				if (f1.isNumeric() && norm1 != null && !norm1.equals(NormalizationService.NORMALIZATION_NONE)) {
					t1 = getFormattedFeatureValue(currentWell, f1, norm1);
				}
				if (f2.isNumeric() && norm2 != null && !norm2.equals(NormalizationService.NORMALIZATION_NONE)) {
					t2 = getFormattedFeatureValue(currentWell, f2, norm2);
				}
				return StringUtils.compareToNumericStrings(t1, t2);
			}
			return 0;
		});
		column = new TreeViewerColumn(treeViewer, SWT.RIGHT);
		col = column.getColumn();
		col.setAlignment(SWT.RIGHT);
		col.setText("Raw");
		col.setToolTipText("Raw Feature Value");
		col.setWidth(100);
		new ColumnViewerSorter<>(column, (o1, o2) -> {
			if (o1 instanceof FeatureGroup && o2 instanceof Feature) {
				Feature f1 = (Feature) o1;
				Feature f2 = (Feature) o2;
				String t1 = getFormattedFeatureValue(currentWell, f1, null);
				String t2 = getFormattedFeatureValue(currentWell, f2, null);
				return StringUtils.compareToNumericStrings(t1, t2);
			}
			return 0;
		});
	}

	private ColumnConfiguration[] createHistoryTableColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Timestamp", DataType.DateTime, 120);
		ColumnConfigFactory.createLabelProvider(config, "getTimestamp", "dd/MM/yyyy HH:mm:ss");
		config.setSortComparator((ObjectLog o1, ObjectLog o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			return o1.getTimestamp().compareTo(o2.getTimestamp());
		});
		configs.add(config);

		config = ColumnConfigFactory.create("User", DataType.String, 70);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				ObjectLog log = (ObjectLog)cell.getElement();
				cell.setText(log.getUserCode());
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Field", DataType.String, 80);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				ObjectLog log = (ObjectLog)cell.getElement();
				String p1 = log.getObjectProperty1();
				String p2 = log.getObjectProperty2();
				String field = (p2 == null) ? p1 : p1 + "::" + p2;
				cell.setText(field);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Old Value", DataType.String, 70);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				ObjectLog log = (ObjectLog)cell.getElement();
				cell.setText(getValueLabel(log.getOldValue(), log));
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("New Value", DataType.String, 70);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				ObjectLog log = (ObjectLog)cell.getElement();
				cell.setText(getValueLabel(log.getNewValue(), log));
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Remark", DataType.String, 80);
		config.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				ObjectLog log = (ObjectLog)cell.getElement();
				cell.setText(log.getRemark());
			}
		});
		configs.add(config);

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private String getValueLabel(String value, ObjectLog logItem) {
		if (value == null) return "";
		if (!"status".equalsIgnoreCase(logItem.getObjectProperty1())) return value;
		if (NumberUtils.isNumeric(value)) {
			int code;
			if (NumberUtils.isDigit(value)) {
				code = Integer.valueOf(value);
			} else {
				code = Double.valueOf(value).intValue();
			}
			WellStatus status = WellStatus.getByCode(code);
			return status.getLabel();
		}
		return value;
	}

}
