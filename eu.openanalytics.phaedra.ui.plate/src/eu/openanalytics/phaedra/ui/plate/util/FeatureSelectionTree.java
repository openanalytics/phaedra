package eu.openanalytics.phaedra.ui.plate.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Point;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterMenu;
import eu.openanalytics.phaedra.base.ui.util.filter.FilterMenu.FilterMenuSelectionListener;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider.ValueKey;

public class FeatureSelectionTree {

	public static final String ITEM_ACTIVE_FEATURE = ValueProvider.VALUE_TYPE_ACTIVE_FEATURE;
	public static final String ITEM_NONE = ValueProvider.VALUE_TYPE_NONE;
	public static final String GROUP_FEATURES = "Features";
	public static final String GROUP_ANNOTATIONS = "Annotations";
	public static final String GROUP_PROPERTIES = "Other Properties";
	
	public static FilterMenu open(ProtocolClass pClass, Point location, FilterMenuSelectionListener listener) {
		FilterMenu menu = new FilterMenu(new FilterContentProvider(pClass), new FilterLabelProvider(), false);
		menu.setInput(createInput());
		if (location != null) menu.setLocation(location);
		if (listener != null) menu.addFilterMenuSelectionListener(listener);
		menu.setFocus();
		return menu;
	}
	
	public static ValueKey toKey(Object selectedObject) {
		if (ITEM_NONE.equals(selectedObject)) return ValueKey.create(ITEM_NONE);
		else if (ITEM_ACTIVE_FEATURE.equals(selectedObject)) return ValueKey.create(ITEM_ACTIVE_FEATURE);
		else if (selectedObject instanceof IFeature) return ValueKey.create(ValueProvider.VALUE_TYPE_FEATURE, selectedObject);
		else return ValueKey.create(ValueProvider.VALUE_TYPE_PROPERTY, selectedObject);
	}
	
	private static Object createInput() {
		List<String> input = new ArrayList<>();
		input.add(ITEM_ACTIVE_FEATURE);
		input.add(GROUP_FEATURES);
		input.add(GROUP_ANNOTATIONS);
		input.add(GROUP_PROPERTIES);
		input.add(ITEM_NONE);
		return input;
	}
	
	public static class FilterContentProvider implements ITreeContentProvider {

		private ProtocolClass pClass;
		private IFeature[] annotationsFeatures;
		
		public FilterContentProvider(ProtocolClass pClass) {
			this.pClass = pClass;
			this.annotationsFeatures = ProtocolService.streamableList(pClass.getFeatures()).stream()
					.filter(f -> f.isAnnotation()).toArray(i -> new IFeature[i]);
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}

		@Override
		public void dispose() {
			// Do nothing.
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public boolean hasChildren(Object element) {
			return !(element instanceof IFeature)
					&& !(element instanceof WellProperty)
					&& !ITEM_ACTIVE_FEATURE.equals(element)
					&& !ITEM_NONE.equals(element);
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			if (GROUP_FEATURES.equals(parentElement)) {
				return ProtocolService.getInstance().getAllFeatureGroups(pClass, GroupType.WELL).toArray();
			} else if (GROUP_ANNOTATIONS.equals(parentElement)) {
				return annotationsFeatures;
			} else if (GROUP_PROPERTIES.equals(parentElement)) {
				return WellProperty.values();
			} else if (parentElement instanceof FeatureGroup) {
				return ProtocolService.getInstance().getMembers((FeatureGroup) parentElement).toArray();
			} else if (parentElement instanceof List) {
				return ((List<?>) parentElement).toArray();
			} else {
				return new Object[0];
			}
		}
	}
	
	public static class FilterLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof FeatureGroup) {
				return ((FeatureGroup) element).getName();
			} else if (element instanceof IFeature) {
				return ((IFeature) element).getName();
			} else if (element instanceof WellProperty) {
				return ((WellProperty) element).getLabel();
			}
			return super.getText(element);
		}
	}
}
