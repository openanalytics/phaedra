package eu.openanalytics.phaedra.ui.export.wizard.pages;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterMatcher;
import eu.openanalytics.phaedra.base.ui.util.table.FilteredCheckboxTable;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.export.wizard.BaseExportWizardPage;

public class SelectFeaturePage extends BaseExportWizardPage {

	private Text protocolText;
	private List experimentList;
	private CheckboxTableViewer featureTableViewer;

	private java.util.List<Feature> allFeatures;
	private java.util.List<Feature> selectedFeatures;

	public SelectFeaturePage() {
		super("Select Features");
		setDescription("Step 1/4: Select the features you want to export.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);
		setControl(container);

		Label label = new Label(container, SWT.NONE);
		label.setText("Protocol:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(label);

		protocolText = new Text(container, SWT.BORDER);
		protocolText.setEditable(false);
		GridDataFactory.fillDefaults().applyTo(protocolText);

		label = new Label(container, SWT.NONE);
		label.setText("Experiments:");
		GridDataFactory.fillDefaults().applyTo(label);

		experimentList = new List(container, SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true,false).hint(SWT.DEFAULT,120).applyTo(experimentList);

		label = new Label(container, SWT.NONE);
		label.setText("Features:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

		Composite fContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(fContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fContainer);

		FilteredCheckboxTable table = new FilteredCheckboxTable(fContainer, SWT.V_SCROLL | SWT.BORDER, new FilterMatcher(), true);
		featureTableViewer = table.getCheckboxViewer();
		featureTableViewer.setContentProvider(new ArrayContentProvider());
		featureTableViewer.getTable().addListener(SWT.Selection, e -> checkPageComplete());
		GridDataFactory.fillDefaults().grab(true,true).hint(SWT.DEFAULT,200).applyTo(featureTableViewer.getControl());

		featureTableViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(Object element) { return selectedFeatures.contains(element); };
			@Override
			public boolean isGrayed(Object element) { return false; };
		});
		featureTableViewer.addCheckStateListener(e -> {
			boolean checked = e.getChecked();
			if (checked) selectedFeatures.add((Feature) e.getElement());
			else selectedFeatures.remove(e.getElement());
			featureTableViewer.refresh();
		});

		TableViewerColumn col = new TableViewerColumn(featureTableViewer, SWT.LEFT);
		col.getColumn().setWidth(250);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Feature)element).getDisplayName();
			}
		});

		Composite btnComposite = new Composite(fContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false,false).applyTo(btnComposite);
		GridLayoutFactory.fillDefaults().applyTo(btnComposite);

		new Label(btnComposite, SWT.NONE);

		Button button = new Button(btnComposite, SWT.PUSH);
		button.setText("Select All");
		button.addListener(SWT.Selection, e -> {
			toggle(f -> true);
			checkPageComplete();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(button);

		button = new Button(btnComposite, SWT.PUSH);
		button.setText("Select None");
		button.addListener(SWT.Selection, e -> {
			toggle(f -> false);
			checkPageComplete();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(button);

		button = new Button(btnComposite, SWT.PUSH);
		button.setText("Select Key");
		button.addListener(SWT.Selection, e -> {
			toggle(f -> f.isKey());
			checkPageComplete();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(button);

		button = new Button(btnComposite, SWT.PUSH);
		button.setText("Select Num.");
		button.addListener(SWT.Selection, e -> {
			toggle(f -> f.isNumeric());
			checkPageComplete();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(button);

		setPageComplete(false);
	}

	private void checkPageComplete() {
		boolean complete = !selectedFeatures.isEmpty();
		setPageComplete(complete);
	}

	private void toggle(Predicate<Feature> filter) {
		for (TableItem item : featureTableViewer.getTable().getItems()) {
			if (item.getData() instanceof Feature) {
				Feature f = (Feature) item.getData();
				if (filter.test(f)) CollectionUtils.addUnique(selectedFeatures, f);
				else selectedFeatures.remove(f);
			}
		}
		//selectedFeatures.clear();
		//for (Feature f: allFeatures) {
		//	if (filter.test(f)) selectedFeatures.add(f);
		//}
		featureTableViewer.refresh();
	}

	@Override
	protected void pageAboutToShow(ExportSettings settings, boolean firstTime) {
		if (settings.experiments == null || settings.experiments.isEmpty()) return;
		if (!firstTime) return;

		Protocol p = settings.experiments.get(0).getProtocol();
		protocolText.setText(p.getName());

		for (Experiment exp: settings.experiments) experimentList.add(exp.getName());

		allFeatures = PlateUtils.getFeatures(p);
		selectedFeatures = new ArrayList<>();
		featureTableViewer.setInput(allFeatures);
	}

	@Override
	public void collectSettings(ExportSettings settings) {
		settings.features = selectedFeatures;
	}
}
