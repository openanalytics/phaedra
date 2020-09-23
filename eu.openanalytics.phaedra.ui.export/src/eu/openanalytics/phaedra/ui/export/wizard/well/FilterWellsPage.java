package eu.openanalytics.phaedra.ui.export.wizard.well;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.filter.CompoundFilter;
import eu.openanalytics.phaedra.export.core.filter.CompoundFilter.CompoundNr;
import eu.openanalytics.phaedra.export.core.filter.WellFeatureFilter;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.export.widget.ShoppingCart;
import eu.openanalytics.phaedra.ui.export.widget.ShoppingCart.AddItemAction;
import eu.openanalytics.phaedra.ui.export.wizard.BaseExportWizardPage;

public class FilterWellsPage extends BaseExportWizardPage {
	
	private final ExportSettings settings;
	
	private Button filterWellResultsChk;
	private Combo wellResultFeatureCombo;
	private Combo wellResultNormCombo;
	private Combo wellResultOperatorCombo;
	private Text wellResultValueTxt;
	
	private Button filterCompoundsChk;
	private Text compTypeTxt;
	private Text compNrTxt;
	private ShoppingCart compoundList;

	private Button rejectedWellsChk;
	private Button invalidatedCompoundsChk;

	private CheckboxTableViewer wellTypeTableViewer;
	
	private WellFeatureFilter wellFeatureFilter;
	private CompoundFilter compoundFilter;
	
	public FilterWellsPage(ExportSettings settings) {
		super("Filter Wells and Compounds");
		setDescription("Step 3/4:  Select filters for the wells and compounds to export.");
		
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);
		setControl(container);
	
		filterWellResultsChk = new Button(container, SWT.CHECK);
		filterWellResultsChk.setText("Filter on Well Results:");
		GridDataFactory.fillDefaults().span(2,1).applyTo(filterWellResultsChk);

		filterWellResultsChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = filterWellResultsChk.getSelection();
				wellResultFeatureCombo.setEnabled(checked);
				wellResultNormCombo.setEnabled(checked);
				wellResultOperatorCombo.setEnabled(checked);
				wellResultValueTxt.setEnabled(checked);
			}
		});
		
		Composite subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(2,1).grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(subContainer);
		
		wellResultFeatureCombo = new Combo(subContainer, SWT.NONE | SWT.READ_ONLY);
		wellResultFeatureCombo.setEnabled(false);
		wellResultFeatureCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = wellResultFeatureCombo.getSelectionIndex();
				wellResultNormCombo.setItems(wellFeatureFilter.getNormalizations(index));
				if (wellResultNormCombo.getItemCount() > 0) wellResultNormCombo.select(0);
			}
		});
		GridDataFactory.fillDefaults().hint(120,SWT.DEFAULT).applyTo(wellResultFeatureCombo);
		
		wellResultNormCombo = new Combo(subContainer, SWT.NONE | SWT.READ_ONLY);
		wellResultNormCombo.setEnabled(false);
		GridDataFactory.fillDefaults().hint(100,SWT.DEFAULT).applyTo(wellResultNormCombo);
		
		wellResultOperatorCombo = new Combo(subContainer, SWT.NONE | SWT.READ_ONLY);
		wellResultOperatorCombo.setEnabled(false);
		GridDataFactory.fillDefaults().hint(20,SWT.DEFAULT).applyTo(wellResultOperatorCombo);
		
		wellResultValueTxt = new Text(subContainer, SWT.BORDER);
		wellResultValueTxt.setEnabled(false);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint(SWT.DEFAULT,15).applyTo(wellResultValueTxt);
		
		Label label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		filterCompoundsChk = new Button(container, SWT.CHECK);
		filterCompoundsChk.setText("Filter on Compounds:");
		GridDataFactory.fillDefaults().span(2,1).applyTo(filterCompoundsChk);
		
		filterCompoundsChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = filterCompoundsChk.getSelection();
				compTypeTxt.setEnabled(checked);
				compNrTxt.setEnabled(checked);
				compoundList.setEnabled(checked);
			}
		});
		
		subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(2,1).grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(subContainer);

		compoundList = new ShoppingCart(subContainer, SWT.NONE);
		compoundList.setEnabled(false);
		compoundList.setAddItemAction(new AddItemAction() {
			@Override
			public String getName() {
				return compTypeTxt.getText() + " " + compNrTxt.getText();
			}
			@Override
			public Object getValue() {
				return compoundFilter.getNr(compTypeTxt.getText(), compNrTxt.getText());
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).span(1,3).applyTo(compoundList);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);

		rejectedWellsChk = new Button(container, SWT.CHECK);
		rejectedWellsChk.setText("Include rejected wells");
		GridDataFactory.fillDefaults().span(2,1).applyTo(rejectedWellsChk);
		
		invalidatedCompoundsChk = new Button(container, SWT.CHECK);
		invalidatedCompoundsChk.setText("Include invalidated compounds");
		GridDataFactory.fillDefaults().span(2,1).applyTo(invalidatedCompoundsChk);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		label = new Label(container, SWT.NONE);
		label.setText("Filter on Well Type:");
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		String[] allWellTypes = ProtocolService.getInstance().getWellTypes()
				.stream().map(wt -> wt.getCode()).toArray(i -> new String[i]);
		
		Table table = new Table(container, SWT.BORDER | SWT.CHECK);
		table.setLinesVisible(true);
		wellTypeTableViewer = new CheckboxTableViewer(table);
		wellTypeTableViewer.setContentProvider(new ArrayContentProvider());
		//PHA-644
		wellTypeTableViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String wellType = (String)element;
				return ProtocolUtils.getCustomHCLCLabel(wellType);
			}
		});
		wellTypeTableViewer.setInput(allWellTypes);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).applyTo(table);
		
		IDialogSettings settings = getDialogSettings();
		rejectedWellsChk.setSelection(settings.getBoolean("rejectedWellsChk"));
		invalidatedCompoundsChk.setSelection(settings.getBoolean("invalidatedCompoundsChk"));
		
		String[] wellTypes = settings.getArray("wellTypes");
		if (wellTypes == null) wellTypes = allWellTypes;
		wellTypeTableViewer.setCheckedElements(wellTypes);
	}
	
	@Override
	protected void pageAboutToShow(boolean firstTime) {
		
		if (!firstTime) return;

		List<Feature> allFeatures = new ArrayList<Feature>();
		if (settings.experiments != null && !settings.experiments.isEmpty()) {
			ProtocolClass pc = settings.experiments.get(0).getProtocol().getProtocolClass();
			allFeatures = pc.getFeatures();
		}
		
		wellFeatureFilter = new WellFeatureFilter(allFeatures);
		String[] names = wellFeatureFilter.getFeatureNames();
		wellResultFeatureCombo.setItems(names);
		if (wellResultFeatureCombo.getItemCount() > 0) wellResultFeatureCombo.select(0);
	
		if (wellResultFeatureCombo.getItemCount() > 0) {
			wellResultNormCombo.setItems(wellFeatureFilter.getNormalizations(0));
			if (wellResultNormCombo.getItemCount() > 0) wellResultNormCombo.select(0);
		}
		
		wellResultOperatorCombo.setItems(wellFeatureFilter.getOperators());
		if (wellResultOperatorCombo.getItemCount() > 0) wellResultOperatorCombo.select(0);

		compoundFilter = new CompoundFilter(settings.experiments);
	}

	@Override
	public void collectSettings() {
		settings.filterWellResults = filterWellResultsChk.getSelection();
		if (settings.filterWellResults) {
			int index = wellResultFeatureCombo.getSelectionIndex();
			if (index != -1) settings.wellResultFeature = wellFeatureFilter.getFeature(index);
			index = wellResultNormCombo.getSelectionIndex();
			if (index != -1) settings.wellResultNormalization = wellResultNormCombo.getText();
			index = wellResultOperatorCombo.getSelectionIndex();
			if (index != -1) settings.wellResultOperator = wellResultOperatorCombo.getItem(index);
			settings.wellResultValue = wellResultValueTxt.getText();
		}
		
		settings.filterCompound = filterCompoundsChk.getSelection();
		if (settings.filterCompound) {
			Object[] values = compoundList.getItemValues();
			settings.compoundTypes = new String[values.length];
			settings.compoundNumbers = new String[values.length];
			for (int i=0;i<values.length;i++) {
				if (values[i] == null) {
					String msg = "The compound " + compoundList.getItemNames()[i] + " is invalid.";
					MessageDialog.open(MessageDialog.ERROR, getShell(),
							"Invalid compound", msg, SWT.NONE);
					throw new RuntimeException(msg);
				}
				settings.compoundTypes[i] = ((CompoundNr)values[i]).type;
				settings.compoundNumbers[i] = ((CompoundNr)values[i]).number;
			}
		}
		
		settings.includeRejectedWells = rejectedWellsChk.getSelection();
		settings.includeInvalidatedCompounds = invalidatedCompoundsChk.getSelection();
		settings.wellTypes = Arrays.stream(wellTypeTableViewer.getCheckedElements()).map(e -> e.toString()).toArray(i -> new String[i]);
	}

	@Override
	public void saveDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		
		dialogSettings.put("rejectedWellsChk", settings.includeRejectedWells);
		dialogSettings.put("invalidatedCompoundsChk", settings.includeInvalidatedCompounds);
		dialogSettings.put("wellTypes", settings.wellTypes);
	}
	
}
