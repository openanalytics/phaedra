package eu.openanalytics.phaedra.ui.plate.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.stat.util.PlateStatisticsProvider;
import eu.openanalytics.phaedra.calculation.stat.util.PlateStatisticsProvider.PlateStatistic;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class PlateStatisticsView extends DecoratedView {

	private Label currentFeatureLbl;
	private RichTableViewer tableViewer;
	
	private Plate currentPlate;
	private Feature currentFeature;
	private PlateStatistic[] currentStatistics;
	
	private ISelectionListener selectionListener;
	private IUIEventListener featureListener;
	
	@Override
	public void createPartControl(Composite parent) {
		
		FormToolkit formToolkit = FormEditorUtils.createToolkit();
		
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(parent);
		
		Label separator = formToolkit.createSeparator(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);
		
		final Form form = FormEditorUtils.createForm("Plate: <no plate selected>", 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		
		// Section 1: Plate Statistics ------------------------------
		
		Section section = FormEditorUtils.createSection("Statistics", form.getBody(), formToolkit);
		Composite container = FormEditorUtils.createComposite(2, section, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
		
		currentFeatureLbl = FormEditorUtils.createLabelPair("Feature:", container, formToolkit);
		
		Table t = formToolkit.createTable(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(t);
		
		tableViewer = new RichTableViewer(t);
		tableViewer.setContentProvider(new ArrayContentProvider());
		
		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
				if (plate != null && !plate.equals(currentPlate)) {
					boolean rebuidColumns = !PlateUtils.isSameProtocolClass(plate, currentPlate);
					currentPlate = plate;
					if (currentFeature != null) currentStatistics = PlateStatisticsProvider.getAvailableStats(plate);
					form.setText("Plate: " + currentPlate.getBarcode());
					
					if (rebuidColumns) tableViewer.applyColumnConfig(configureColumns());
					tableViewer.setInput(currentStatistics);
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);
		
		featureListener = new IUIEventListener() {
			@Override
			public void handle(UIEvent event) {
				if (event.type == EventType.FeatureSelectionChanged) {
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					if (f != null && !f.equals(currentFeature)) {
						currentFeature = f;
						currentFeatureLbl.setText(f.getDisplayName());
						if (currentPlate != null)  currentStatistics = PlateStatisticsProvider.getAvailableStats(currentPlate);
						
						tableViewer.applyColumnConfig(configureColumns());
						tableViewer.setInput(currentStatistics);
					}
				}
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureListener);

		addDecorator(new CopyableDecorator());
		initDecorators(parent);
		
		currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
		if (currentFeature != null) currentFeatureLbl.setText(currentFeature.getDisplayName());
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewPlateStatistics");
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(featureListener);
		super.dispose();
	}
	
	private ColumnConfiguration[] configureColumns() {
		
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;
		
		config = ColumnConfigFactory.create("Statistic", ColumnDataType.String, 100);
		config.setLabelProvider(new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				PlateStatistic statistic = (PlateStatistic)element;
				return statistic.label;
			}
		});
		config.setSorter(new Comparator<PlateStatistic>(){
			@Override
			public int compare(PlateStatistic s1, PlateStatistic s2) {
				if (s1 == null && s2 != null) return -1;
				if (s2 == null) return 1;
				return s1.label.compareTo(s2.label);
			}
		});
		configs.add(config);

		config = ColumnConfigFactory.create("Value", ColumnDataType.String, 100);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				PlateStatistic statistic = (PlateStatistic)element;
				if (currentFeature == null) return "";
				return statistic.getFormattedValue(currentPlate, currentFeature);
			}
		};
		config.setSorter(new Comparator<PlateStatistic>(){
			@Override
			public int compare(PlateStatistic s1, PlateStatistic s2) {
				if (s1 == null && s2 != null) return -1;
				if (s2 == null) return 1;
				double v1 = s1.getValue(currentPlate, currentFeature);
				double v2 = s2.getValue(currentPlate, currentFeature);
				if (v1 < v2) return -1;
				if (v1 > v2) return 1;
				return 0;
			}
		});
		config.setLabelProvider(labelProvider);
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
