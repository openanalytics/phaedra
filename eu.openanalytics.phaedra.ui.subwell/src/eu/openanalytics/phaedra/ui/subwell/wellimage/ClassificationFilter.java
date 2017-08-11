package eu.openanalytics.phaedra.ui.subwell.wellimage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class ClassificationFilter {

	private static final String SELECTED_FEATURE = "SELECTED_FEATURE";
	private static final String FILTER_CLASSES = "FILTER_CLASSES";
	private static final String SHOW_CLASSIFIED = "SHOW_CLASSIFIED";
	private static final String SHOW_REJECTED = "SHOW_REJECTED";
	private static final String SHOW_SELECTED = "SHOW_SELECTED";
	private static final String SHOW_ITEMS_WO_CLASS = "SHOW_ITEMS_WO_CLASS";

	private List<FeatureClass> filterClasses;
	private Well currentWell;
	private Canvas canvas;
	private int selectedFeature;
	private SubWellFeature rejectedFeature;
	private FeatureClass rejectedClass;
	private List<SubWellFeature> features;

	private boolean showAllClasses;
	private boolean showSelectedClasses;
	private boolean showRejected;
	private boolean showItemsWithoutClass;

	private static boolean dialogOpen;

	public ClassificationFilter() {
		filterClasses = new ArrayList<FeatureClass>();
		selectedFeature = 0;

		showAllClasses = false;
		showSelectedClasses = true;
		showRejected = false;
		showItemsWithoutClass = false;
	}

	public void createToolbarButton(ToolBar parent, final Canvas canvas) {
		this.canvas = canvas;

		final ToolItem classificationSelectionButton = new ToolItem(parent, SWT.PUSH);
		classificationSelectionButton.setImage(IconManager.getIconImage("funnel.png"));
		classificationSelectionButton.setToolTipText("Filter classification");
		classificationSelectionButton.addListener(SWT.Selection, e -> {
			if (features == null || features.isEmpty()) {
				// No classification available.
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(null, "No classification", "There are no feature classifications defined.");
				});
			} else if (!dialogOpen) {
				// No FilterDialog is currently opened, open one.
				dialogOpen = true;
				FilterDialog dialog = new FilterDialog(Display.getDefault().getActiveShell());
				int retCode = dialog.open();
				dialogOpen = false;
				if (retCode == Window.OK && canvas != null) canvas.redraw();
			}
		});
	}

	public List<Integer> filter(List<Integer> currentSelection, Well well) {
		// Update the current well and features
		if (well != null && currentWell != well) {
			boolean samePClass = PlateUtils.isSameProtocolClass(well, currentWell);
			currentWell = well;
			if (!samePClass) {
				ProtocolClass pClass = PlateUtils.getProtocolClass(currentWell);
				// Get all classification-enabled features.
				features = ClassificationService.getInstance().findSubWellClassificationFeatures(pClass);
				SubWellFeature f = getSelectedFeature();
				if (f != null) filterClasses.addAll(features.get(selectedFeature).getFeatureClasses());
				// See if there's a rejection feature.
				rejectedFeature = ClassificationService.getInstance().findRejectionFeature(pClass);
				if (rejectedFeature == null) showRejected = false;
				else rejectedClass = ClassificationService.getInstance().findRejectionClass(rejectedFeature);
			}
		}

		List<Integer> entityList = new ArrayList<>();
		if (features == null || features.isEmpty()) return entityList;
		SubWellFeature feature = getSelectedFeature();
		if (feature == null) feature = SubWellService.getInstance().getSampleFeature(currentWell);

		Object data = SubWellService.getInstance().getData(well, feature);
		if (data == null) return (showSelectedClasses && currentSelection != null) ? currentSelection : entityList;

		int entityCount = (data instanceof float[]) ? ((float[])data).length : ((String[])data).length;

		for (int entityIndex = 0; entityIndex < entityCount; entityIndex++) {

			boolean addItem = false;

			if (showRejected) {
				// Display all rejected items, regardless of other settings.
				List<FeatureClass> rejectedItemClasses = ClassificationService.getInstance().getFeatureClasses(well, entityIndex, rejectedFeature);
				if (rejectedItemClasses.contains(rejectedClass)) {
					addItem = true;
				}
			}

			if (showAllClasses || (showSelectedClasses && currentSelection.contains(entityIndex))) {
				// Show classification symbols for all items or selected items.
				List<FeatureClass> itemClasses = ClassificationService.getInstance().getFeatureClasses(well, entityIndex, feature);
				for (FeatureClass filterClass: filterClasses) {
					if (itemClasses.contains(filterClass)) {
						addItem = true;
						break;
					}
				}
				if (itemClasses.isEmpty() && showItemsWithoutClass) addItem = true;
			}

			if (addItem) entityList.add(entityIndex);
		}

		return entityList;
	}

	public SubWellFeature getSelectedFeature() {
		if (features == null || features.isEmpty()) return null;
		return features.get(selectedFeature);
	}

	public boolean isShowRejected() {
		return showRejected;
	}

	public SubWellFeature getRejectionFeature() {
		return rejectedFeature;
	}

	public void setShowAllClasses(boolean showAllClasses) {
		this.showAllClasses = showAllClasses;
	}

	public Map<String, Object> createSettingsMap() {
		Map<String, Object> settingsMap = new HashMap<>();
		settingsMap.put(SELECTED_FEATURE, selectedFeature);
		List<Long> featureClassIds = new ArrayList<>();
		for (FeatureClass clazz : filterClasses ) {
			featureClassIds.add(clazz.getId());
		}
		settingsMap.put(FILTER_CLASSES, featureClassIds);
		settingsMap.put(SHOW_CLASSIFIED, showAllClasses);
		settingsMap.put(SHOW_REJECTED, showRejected);
		settingsMap.put(SHOW_SELECTED, showSelectedClasses);
		settingsMap.put(SHOW_ITEMS_WO_CLASS, showItemsWithoutClass);
		return settingsMap;
	}

	@SuppressWarnings("unchecked")
	public void applySettings(Map<String, Object> settingsMap) {
		this.filterClasses.clear();
		selectedFeature = (int) settingsMap.get(SELECTED_FEATURE);
		if (getSelectedFeature() != null) {
			List<FeatureClass> filterClasses = getSelectedFeature().getFeatureClasses();
			List<Long> featureClassIds = (List<Long>) settingsMap.get(FILTER_CLASSES);
			for (FeatureClass clazz : filterClasses)
				if (featureClassIds.contains(clazz.getId())) this.filterClasses.add(clazz);
		}
		showAllClasses = (boolean) settingsMap.get(SHOW_CLASSIFIED);
		showRejected = (boolean) settingsMap.get(SHOW_REJECTED);
		showSelectedClasses = (boolean) settingsMap.get(SHOW_SELECTED);
		showItemsWithoutClass = (boolean) settingsMap.get(SHOW_ITEMS_WO_CLASS);
		if (canvas!=null) canvas.redraw();
	}

	private class FilterDialog extends TitleAreaDialog {

		private CheckboxTableViewer classTableViewer;
		private Button showItemsWithoutClassCheck;
		private Button showClassifiedCheck;
		private Button showSelectedCheck;
		private Button showRejectedCheck;
		private ColorStore colorStore;

		protected FilterDialog(Shell parentShell) {
			super(parentShell);
			setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
			colorStore = new ColorStore();
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setSize(400, 450);
			shell.setText("Classification Filter");
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.PROCEED_ID, "Apply", false);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);

			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
			GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);

			String[] featureNames = new String[features.size()];

			for(int i = 0 ; i < features.size(); i++){
				featureNames[i]  = features.get(i).getName();
			}

			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Classification feature:");

			final Combo featuresCombobox = new Combo(container, SWT.READ_ONLY);
			featuresCombobox.setItems(featureNames);
			featuresCombobox.select(selectedFeature);
			featuresCombobox.addListener(SWT.Selection, e -> {
				selectedFeature = featuresCombobox.getSelectionIndex();
				filterClasses.clear();
				refreshClassTable();
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(featuresCombobox);

			new Label(container, SWT.NONE).setText("Class symbols to show:");

			classTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			classTableViewer.getTable().setLinesVisible(true);
			classTableViewer.getTable().setHeaderVisible(true);
			GridDataFactory.fillDefaults().span(2,1).align(SWT.FILL,SWT.FILL).grab(true,true).applyTo(classTableViewer.getTable());
			classTableViewer.addCheckStateListener(e -> {
				if (e.getChecked()) {
					filterClasses.add((FeatureClass) e.getElement());
				} else {
					filterClasses.remove(e.getElement());
				}
			});

			showItemsWithoutClassCheck = new Button(container, SWT.CHECK);
			showItemsWithoutClassCheck.setText("Show unclassified item symbols");
			showItemsWithoutClassCheck.setSelection(showItemsWithoutClass);
			showItemsWithoutClassCheck.setToolTipText("Show 'unclassified' symbol for items without a classification number.");
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(showItemsWithoutClassCheck);

			showRejectedCheck = new Button(container, SWT.CHECK);
			showRejectedCheck.setText("Show all rejected items");
			showRejectedCheck.setSelection(showRejected);
			showRejectedCheck.setEnabled(rejectedFeature != null);
			showRejectedCheck.setToolTipText("Show the rejection symbol for all rejected items, regardless of class.");
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(showRejectedCheck);

			new Label(container, SWT.NONE).setText("Show these symbols for:");

			showClassifiedCheck = new Button(container, SWT.RADIO);
			showClassifiedCheck.setText("All cells");
			showClassifiedCheck.setSelection(showAllClasses);
			showClassifiedCheck.setToolTipText("The classification symbols will be shown for all items.");
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(showClassifiedCheck);

			showSelectedCheck = new Button(container, SWT.RADIO);
			showSelectedCheck.setText("Selected cells only");
			showSelectedCheck.setSelection(showSelectedClasses);
			showSelectedCheck.setToolTipText("The classification symbols will be shown only for selected items.");
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(showSelectedCheck);

			createColumns();
			classTableViewer.setContentProvider(new ArrayContentProvider());
			refreshClassTable();

			setTitle("Classification Filter");
			setMessage("Choose which classification icons should be shown on the image.");

			return area;
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (IDialogConstants.OK_ID == buttonId) {
				okPressed();
			} else if (IDialogConstants.CANCEL_ID == buttonId) {
				cancelPressed();
			} else if (IDialogConstants.PROCEED_ID == buttonId) {
				applyPressed();
			}
		}

		@Override
		protected void okPressed() {
			applyConfiguration();
			super.okPressed();
		}

		protected void applyPressed() {
			applyConfiguration();
		}

		protected void applyConfiguration(){
			showAllClasses = showClassifiedCheck.getSelection();
			showSelectedClasses = showSelectedCheck.getSelection();
			showRejected = showRejectedCheck.getSelection();
			showItemsWithoutClass = showItemsWithoutClassCheck.getSelection();

			filterClasses.clear();
			Object[] checkedItems = classTableViewer.getCheckedElements();
			for (Object item: checkedItems) {
				filterClasses.add((FeatureClass)item);
			}
			if (canvas != null) canvas.redraw();
		}

		@Override
		public boolean close() {
			colorStore.dispose();
			return super.close();
		}

		public void refreshClassTable() {
			if (features != null && classTableViewer != null && !classTableViewer.getTable().isDisposed()) {
				SubWellFeature f = getSelectedFeature();
				List<FeatureClass> featureClassList = null;
				if (f != null) featureClassList = new ArrayList<>(f.getFeatureClasses());

				if (featureClassList != null) {
					classTableViewer.setInput(featureClassList);
					if (filterClasses != null) {
						for (FeatureClass clazz : filterClasses) {
							classTableViewer.setChecked(clazz, true);
						}
					}
				}
			}
		}

		private void createColumns() {
			TableViewerColumn column = new TableViewerColumn(classTableViewer, SWT.BORDER);
			column.getColumn().setWidth(100);
			column.getColumn().setText("Class");
			column.getColumn().setAlignment(SWT.CENTER);
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((FeatureClass)element).getLabel();
				}
			});

			column = new TableViewerColumn(classTableViewer, SWT.BORDER);
			column.getColumn().setWidth(50);
			column.getColumn().setText("Color");
			column.getColumn().setAlignment(SWT.LEFT);
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return "";
				}
				@Override
				public Color getBackground(Object element) {
					int color = ((FeatureClass)element).getRgbColor();
					return colorStore.get(new RGB(
							(color>>16) & 0xff,
							(color>>8) & 0xff,
							color & 0xff));
				}
			});

			column = new TableViewerColumn(classTableViewer, SWT.BORDER);
			column.getColumn().setWidth(50);
			column.getColumn().setText("Symbol");
			column.getColumn().setAlignment(SWT.CENTER);
			column.setLabelProvider(new OwnerDrawLabelProvider() {
				@Override
				protected void paint(Event event, Object element) {
					String shape = ((FeatureClass) element).getSymbol();
					if (shape != null && !shape.isEmpty()) {
						int color = ((FeatureClass) element).getRgbColor();
						GC gc = event.gc;
						gc.setAntialias(SWT.ON);
						PlotShape ps = PlotShape.valueOf(shape);
						gc.setForeground(colorStore.get(new RGB((color >> 16) & 0xff,
								(color >> 8) & 0xff, color & 0xff)));
						gc.setBackground(colorStore.get(new RGB((color >> 16) & 0xff,
								(color >> 8) & 0xff, color & 0xff)));
						ps.drawShape(
								gc,
								event.x+ gc.getClipping().width/2,
								event.y+ event.height/2,
								5,
								true);
					}
				}

				@Override
				protected void measure(Event event, Object element) {
					// Do nothing.
				}
			});

			column = new TableViewerColumn(classTableViewer, SWT.BORDER);
			column.getColumn().setWidth(200);
			column.getColumn().setText("Description");
			column.getColumn().setAlignment(SWT.LEFT);
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return ((FeatureClass)element).getDescription();
				}
			});
		}
	}
}