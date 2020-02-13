package eu.openanalytics.phaedra.ui.export.wizard;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterMatcher;
import eu.openanalytics.phaedra.base.ui.util.table.FilteredCheckboxTable;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public abstract class AbstractExperimentsPage extends BaseExportWizardPage {

	private final IExportExperimentsSettings settings;
	
	private Protocol protocol;
	private List<Feature> protocolFeatures;
	
	private Text protocolText;
	private org.eclipse.swt.widgets.List experimentList;
	
	private Collection<Feature> featureSelection;
	private CheckboxTableViewer featureTableViewer;
	
	
	public AbstractExperimentsPage(String pageName, IExportExperimentsSettings settings) {
		super(pageName);
		
		this.settings = settings;
	}
	
	public AbstractExperimentsPage(IExportExperimentsSettings settings, int stepNum, int stepTotal) {
		this("Select Features", settings);
		
		setDescription(String.format("Step %1$s/%2$s: Select the features to export.", stepNum, stepTotal));
	}


	public Protocol getProtocol() {
		return protocol;
	}
	
	protected List<Feature> getProtocolFeatures() {
		return protocolFeatures;
	}


	protected void addExperimentsInfo(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText("Protocol:");
		GridDataFactory.defaultsFor(label).applyTo(label);

		protocolText = new Text(container, SWT.BORDER);
		protocolText.setEditable(false);
		GridDataFactory.defaultsFor(protocolText).applyTo(protocolText);

		label = new Label(container, SWT.NONE);
		label.setText("Experiments:");
		GridDataFactory.fillDefaults().applyTo(label);

		experimentList = new org.eclipse.swt.widgets.List(container, SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true,false).hint(SWT.DEFAULT,120).applyTo(experimentList);
	}
	
	protected void addFeatureSelection(Composite container, Collection<Feature> model) {
		this.featureSelection = model;
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Features:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
		
		Composite fContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(fContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fContainer);
		
		FilteredCheckboxTable table = new FilteredCheckboxTable(fContainer, SWT.V_SCROLL | SWT.BORDER, new FilterMatcher(), true);
		featureTableViewer = table.getCheckboxViewer();
		featureTableViewer.getTable().setHeaderVisible(true);
		GridDataFactory.fillDefaults().grab(true,true).hint(SWT.DEFAULT,200).applyTo(featureTableViewer.getControl());
		featureTableViewer.setContentProvider(new ArrayContentProvider());
		featureTableViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(Object element) { return featureSelection.contains(element); };
			@Override
			public boolean isGrayed(Object element) { return false; };
		});
		featureTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				CollectionUtils.setContains(featureSelection, (Feature)event.getElement(), event.getChecked());
				checkPageComplete();
			}
		});
		
		TableViewerColumn col = new TableViewerColumn(featureTableViewer, SWT.LEFT);
		col.getColumn().setText("Well Feature");
		col.getColumn().setWidth(250);
		col.getColumn().setResizable(true);
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
		addButton(btnComposite, "Select All", (feature) -> true);
		addButton(btnComposite, "Select None", (feature) -> false);
		addButton(btnComposite, "Select Key", (feature) -> feature.isKey());
		addButton(btnComposite, "Select Num.", (feature) -> feature.isNumeric())
				.setToolTipText("Select All Numeric");
	}
	
	private Button addButton(Composite parent, String label, Predicate<Feature> select) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				for (TableItem item : featureTableViewer.getTable().getItems()) {
					if (item.getData() instanceof Feature) {
						Feature feature = (Feature) item.getData();
						CollectionUtils.setContains(featureSelection, feature, select.test(feature));
					}
				}
//				for (Feature feature : protocolFeatures) {
//					CollectionUtils.setContains(featureSelection, feature, select.test(feature));
//				}
				featureTableViewer.refresh();
				checkPageComplete();
			}
		});
		GridDataFactory.defaultsFor(button).applyTo(button);
		return button;
	}
	
	protected void setFeatureSelection(Collection<Feature> selection) {
		for (Feature feature : protocolFeatures) {
			CollectionUtils.setContains(featureSelection, feature, selection.contains(feature));
		}
		featureTableViewer.refresh();
		checkPageComplete();
	}
	
	
	@Override
	protected void pageAboutToShow(boolean firstTime) {
		super.pageAboutToShow(firstTime);
		
		if (!firstTime) return;
		
		java.util.List<Experiment> experiments = settings.getExperiments();
		protocol = (experiments != null && !experiments.isEmpty()) ? experiments.get(0).getProtocol() : null;
		protocolFeatures = (protocol != null) ? PlateUtils.getFeatures(protocol) : Collections.emptyList();
		
		protocolText.setText((protocol != null) ? protocol.getName() : "<MISSING>");
		
		for (Experiment exp: experiments) experimentList.add(exp.getName());
		
		if (featureSelection != null) {
			featureTableViewer.setInput(protocolFeatures);
		}
	}
	
}
