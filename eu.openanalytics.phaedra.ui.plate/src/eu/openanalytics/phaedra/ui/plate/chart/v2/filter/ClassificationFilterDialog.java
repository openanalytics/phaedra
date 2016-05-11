package eu.openanalytics.phaedra.ui.plate.chart.v2.filter;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class ClassificationFilterDialog<FEATURE extends IFeature> extends TitleAreaDialog {

	private CheckboxTableViewer classTableViewer;
	private Button showRejectedCheck;
	private Button showNotClassifiedCheck;

	private ColorStore colorStore;

	private FEATURE feature;
	private List<FEATURE> features;
	private List<FeatureClass> fClasses;
	private boolean showNotClassified;

	// Optional.
	private boolean showRejected;
	private FEATURE rejectionFeature;

	public ClassificationFilterDialog(Shell parentShell, List<FEATURE> features, FEATURE feature
			, List<FeatureClass> fClasses, boolean showNotClassified) {

		this(parentShell, features, feature, fClasses, showNotClassified, true, null);
	}

	public ClassificationFilterDialog(Shell parentShell, List<FEATURE> features, FEATURE feature
			, List<FeatureClass> fClasses, boolean showNotClassified, boolean showRejected, FEATURE rejectionFeature) {

		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
		setBlockOnOpen(true);

		this.features = features;
		this.feature = feature;
		this.fClasses = new ArrayList<>(fClasses);
		this.showNotClassified = showNotClassified;
		this.showRejected = showRejected;
		this.rejectionFeature = rejectionFeature;

		this.colorStore = new ColorStore();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Classification Filter");
		shell.setSize(400, 400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		setTitle("Classification Filter");
		setMessage("Uncheck a classification class to hide it.");

		Composite container = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

		String[] featureNames = CollectionUtils.transformToStringArray(features, ProtocolUtils.FEATURE_NAMES);

		final Combo featuresCombobox = new Combo(container, SWT.READ_ONLY);
		featuresCombobox.setItems(featureNames);
		if (feature == null) feature = features.get(0);
		featuresCombobox.select(features.indexOf(feature));

		GridDataFactory.fillDefaults().grab(true, false).applyTo(featuresCombobox);
		featuresCombobox.addListener(SWT.Selection, e -> {
			feature = features.get(featuresCombobox.getSelectionIndex());
			if (rejectionFeature != null) showRejectedCheck.setEnabled(feature != rejectionFeature);
			fClasses.clear();
			refreshClassTable();
		});

		Composite tableContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(tableContainer);

		classTableViewer = CheckboxTableViewer
				.newCheckList(tableContainer, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		classTableViewer.getTable().setLinesVisible(true);
		classTableViewer.getTable().setHeaderVisible(true);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(classTableViewer.getTable());

		Composite buttonContainer = new Composite(tableContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().applyTo(buttonContainer);

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText("Select All");
		button.addListener(SWT.Selection, e -> classTableViewer.setAllChecked(true));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(button);

		button = new Button(buttonContainer, SWT.PUSH);
		button.setText("Select None");
		button.addListener(SWT.Selection, e -> classTableViewer.setAllChecked(false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(button);

		showNotClassifiedCheck = new Button(container, SWT.CHECK);
		showNotClassifiedCheck.setText("Show unmatched");
		showNotClassifiedCheck.setSelection(showNotClassified);
		showNotClassifiedCheck.setToolTipText("All items where no classification match is found will be shown.");

		if (rejectionFeature != null) {
			showRejectedCheck = new Button(container, SWT.CHECK);
			showRejectedCheck.setText("Show rejected");
			showRejectedCheck.setSelection(showRejected);
			showRejectedCheck.setEnabled(feature != rejectionFeature);
		}

		createColumns();
		classTableViewer.setContentProvider(new ArrayContentProvider());
		refreshClassTable();
		return area;
	}

	@Override
	protected void okPressed() {
		applyConfiguration();
		super.okPressed();
	}

	protected void applyConfiguration() {
		if (rejectionFeature != null) showRejected = showRejectedCheck.getSelection();
		showNotClassified = showNotClassifiedCheck.getSelection();

		fClasses.clear();
		Object[] checkedItems = classTableViewer.getCheckedElements();
		for (Object item : checkedItems) {
			fClasses.add((FeatureClass) item);
		}
	}

	@Override
	public int open() {
		if (features.isEmpty()) {
			MessageDialog.openError(getShell(), "No classification", "No feature classifications defined.");
			return Window.CANCEL;
		} else {
			return super.open();
		}
	}

	@Override
	public boolean close() {
		colorStore.dispose();
		return super.close();
	}

	public FEATURE getFeature() {
		return feature;
	}

	public List<FeatureClass> getFeatureClasses() {
		return fClasses;
	}

	public boolean isShowNotClassified() {
		return showNotClassified;
	}

	public boolean isShowRejected() {
		return showRejected;
	}

	private void refreshClassTable() {
		if (features != null && !classTableViewer.getTable().isDisposed()) {
			List<FeatureClass> featureClassList = new ArrayList<>(feature.getFeatureClasses());

			if (featureClassList != null) {
				classTableViewer.setInput(featureClassList);
				classTableViewer.setAllChecked(true);
				if (fClasses != null && fClasses.size() != 0) {
					classTableViewer.setAllChecked(false);
					for (FeatureClass clazz : fClasses) {
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
				return ((FeatureClass) element).getLabel();
			}
		});

		column = new TableViewerColumn(classTableViewer, SWT.BORDER);
		column.getColumn().setWidth(50);
		column.getColumn().setText("Symbol");
		column.getColumn().setAlignment(SWT.CENTER);
		column.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void paint(Event event, Object element) {
				FeatureClass featureClass = (FeatureClass) element;
				String shape = featureClass.getSymbol();
				if (shape != null && !shape.equals("")) {
					GC gc = event.gc;
					gc.setAntialias(SWT.ON);
					PlotShape ps = PlotShape.valueOf(shape);
					RGB rgb = ColorUtils.hexToRgb(featureClass.getRgbColor());
					Color color = colorStore.get(rgb);
					gc.setForeground(color);
					gc.setBackground(color);
					ps.drawShape(gc, event.x + gc.getClipping().width / 2, event.y + event.height / 2, 5, true);
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
				return ((FeatureClass) element).getDescription();
			}
		});
	}

}