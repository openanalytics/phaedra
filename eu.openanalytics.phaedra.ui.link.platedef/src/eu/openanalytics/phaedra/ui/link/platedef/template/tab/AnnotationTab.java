package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.misc.ColorCache;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.util.AnnotationHelper;

public class AnnotationTab extends BaseTemplateTab {

	private ProtocolClass protocolClass;
	private TableViewer annotationTableViewer;
	private Supplier<List<WellTemplate>> selectionSupplier;
	private Runnable templateRefresher;
	
	@Override
	public String getName() {
		return "Annotations";
	}

	@Override
	public IGridCellRenderer createCellRenderer() {
		return new AnnotationCellRenderer();
	}

	@Override
	public String getValue(WellTemplate well) {
		return StringUtils.createSeparatedString(getValueLabels(well), ",");
	}
	
	@Override
	public boolean applyValue(WellTemplate well, String value) {
		return false;
	}
	
	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher) {
		this.selectionSupplier = selectionSupplier;
		this.templateRefresher = templateRefresher;
		
		new Label(parent, SWT.NONE).setText("Available Annotations:");
		
		annotationTableViewer = AnnotationHelper.createTableViewer(parent, this::getAnnotationValue, this::setAnnotationValue);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(annotationTableViewer.getControl());
		
		AnnotationHelper.createAnnotationLink(parent, () -> protocolClass, () -> protocolClassChanged(protocolClass));		
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		annotationTableViewer.refresh();	
	}

	@Override
	public void protocolClassChanged(ProtocolClass newPClass) {
		this.protocolClass = newPClass;
		List<Feature> annotationFeatures = ProtocolService.streamableList(protocolClass.getFeatures()).stream()
				.filter(ProtocolUtils.ANNOTATION_FEATURES).collect(Collectors.toList());
		annotationTableViewer.setInput(annotationFeatures);
	}
	
	private String getAnnotationValue(Feature feature) {
		List<WellTemplate> currentSelection = selectionSupplier.get();
		if (currentSelection == null || currentSelection.isEmpty()) return "";
		Set<String> values = new HashSet<>();
		for (WellTemplate well: currentSelection) {
			String value = well.getAnnotations().get(feature.getName());
			if (value != null) values.add(value);
		}
		if (values.isEmpty()) return "";
		else if (values.size() == 1) return values.iterator().next();
		else return AnnotationHelper.ANN_MULTIPLE_VALUES;
	}
	
	private void setAnnotationValue(Feature feature, String value) {
		List<WellTemplate> currentSelection = selectionSupplier.get();
		if (currentSelection == null || currentSelection.isEmpty()) return;
		for (WellTemplate well: currentSelection) {
			well.getAnnotations().put(feature.getName(), value);
		}
		templateRefresher.run();
	}
	
	private static String[] getValueLabels(WellTemplate well) {
		return well.getAnnotations().keySet().stream()
				.filter(k -> k != null)
				.sorted()
				.map(k -> well.getAnnotations().get(k))
				.toArray(i -> new String[i]);
	}
	
	public static class AnnotationCellRenderer extends BaseTemplateCellRenderer {
		@Override
		public String[] getLabels(GridCell cell) {
			WellTemplate well = (WellTemplate)cell.getData();
			if (well == null) return super.getLabels(cell);
			return doGetLabels(well);
		}
		
		@Override
		protected String[] doGetLabels(WellTemplate well) {
			return getValueLabels(well);
		}
		
		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			WellTemplate well = (WellTemplate)cell.getData();
			if (well != null && well.isSkip()) gc.setForeground(ColorCache.get(0));
			else gc.setForeground(ColorCache.get(0xFFFFFF));
			super.render(cell, gc, x, y, w, h);
		}
	}
}
