package eu.openanalytics.phaedra.base.ui.charting.v2.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.filter.FilterMenu;

public class ChartAxesMenuFactory {

	private static final String[] PARAMETERS = new String[] { "X", "Y", "Z" };

	public static final String DIMENSION = "DIMENSION";
	public static final String IS_RADIO = "IS_RADIO";
	public static final String LISTENER = "LISTENER";

	public static ToolItem[] initializeAxisButtons(ToolBar parent, int dim, boolean isRadio) {
		if (dim > 0) {
			ToolItem[] featureDropdowns = new ToolItem[dim];
			for (int i = 0; i < featureDropdowns.length; i++) {
				String name = PARAMETERS[i % 3];
				featureDropdowns[i] = new ToolItem(parent, SWT.PUSH);
				featureDropdowns[i].setImage(IconManager.getIconImage("chart_" + name.toLowerCase() + ".png"));
				featureDropdowns[i].setToolTipText(name + " value");
				featureDropdowns[i].setData(IS_RADIO, isRadio);
				featureDropdowns[i].setData(DIMENSION, i);
			}

			return featureDropdowns;
		}

		return null;
	}

	/**
	 * Creates a menu of Feature Groups with their Features in a submenu.
	 * When only one group is present the Features will be shown in the menu instead.
	 * Special entries that should be displayed below the groups (e.g. JEP Expressions), add a group UNGROUPED_MENU_OPTIONS.
	 * @param toolItems The ToolItems to which you want to add the MenuItems.
	 * @param groupedFeatures Key values are the groups, List values are the Feature names
	 * @param selectedFeatures List of Feature names that should be selected by default
	 * @param listener The listener for when a Feature name is selected
	 */
	public static void updateAxisButtons(final ToolItem[] toolItems, final Map<String, List<String>> groupedFeatures
			, final List<String> selectedFeatures, final AxisChangedListener listener) {
		if (toolItems == null || selectedFeatures == null || selectedFeatures.isEmpty()) return;
		for (ToolItem item : toolItems) {
			Listener itemListener = e -> {
				int dimension = (int) item.getData(DIMENSION);
				ChartAxesLabelProvider labelProvider;
				boolean isRadio = (boolean) item.getData(IS_RADIO);
				if (isRadio) {
					labelProvider = new ChartAxesLabelProvider(selectedFeatures, dimension, groupedFeatures);
				} else {
					labelProvider = new ChartAxesLabelProvider(selectedFeatures, groupedFeatures);
				}
				boolean changeOnFocus = isRadio && Activator.getDefault().getPreferenceStore().getBoolean(Prefs.UPDATE_FEATURE_ON_FOCUS);
				FilterMenu menu = new FilterMenu(new ChartAxesContentProvider(groupedFeatures), labelProvider, changeOnFocus);
				menu.setInput(groupedFeatures);
				menu.addFilterMenuSelectionListener(element -> {
					listener.axisChanged(element.toString(), dimension);
				});
				Rectangle bounds = item.getBounds();
				Point displayCoordinates = item.getParent().toDisplay(new Point(bounds.x, bounds.y + bounds.height));
				Point size = menu.computeSizeHint();
				int x = displayCoordinates.x;
				int y = displayCoordinates.y;

				Monitor mon = item.getParent().getMonitor();
				bounds = mon.getClientArea();
				if (x + size.x > bounds.x + bounds.width) {
					x = bounds.x + bounds.width - size.x;
				}
				if (y + size.y > bounds.y + bounds.height) {
					y = bounds.y + bounds.height - size.y;
				}
				menu.setLocation(new Point(x, y));
				menu.setFocus();
			};
			Display.getDefault().syncExec(() -> {
				if (item.isDisposed()) return;
				// Remove previous listener if any.
				Object data = item.getData(LISTENER);
				if (data instanceof Listener) item.removeListener(SWT.Selection, (Listener) data);
				item.addListener(SWT.Selection, itemListener);
				item.setData(LISTENER, itemListener);
			});
		}
	}

	public static class ChartAxesLabelProvider extends LabelProvider implements IFontProvider {

		private int dimension;
		private List<String> selectedFeatures;
		private Map<String, List<String>> groupedFeatures;

		public ChartAxesLabelProvider(List<String> selectedFeatures, Map<String, List<String>> groupedFeatures) {
			this(selectedFeatures, -1, groupedFeatures);
		}

		public ChartAxesLabelProvider(List<String> selectedFeatures, int dimension, Map<String, List<String>> groupedFeatures) {
			this.selectedFeatures = selectedFeatures;
			this.dimension = dimension;
			this.groupedFeatures = groupedFeatures;
		}

		@Override
		public Font getFont(Object element) {
			Font font = getFont2(element);
			if (font == null && groupedFeatures.containsKey(element)) {
				List<String> list = groupedFeatures.get(element);
				for (String item : list) {
					font = getFont2(item);
					if (font != null) break;
				}
			}
			return font;
		}

		private Font getFont2(Object element) {
			if (dimension > -1 && dimension < selectedFeatures.size()) {
				if (selectedFeatures.get(dimension).equalsIgnoreCase(element.toString())) {
					return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
				}
			} else {
				if (selectedFeatures.contains(element)) {
					return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
				}
			}
			return null;
		}
	}

	public static class ChartAxesContentProvider implements ITreeContentProvider {

		private Map<String, List<String>> groupedFeatures;

		public ChartAxesContentProvider(Map<String, List<String>> groupedFeatures) {
			this.groupedFeatures = groupedFeatures;
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
		public boolean hasChildren(Object element) {
			return !groupedFeatures.getOrDefault(element, new ArrayList<String>()).isEmpty();
		}

		@Override
		public Object getParent(Object element) {
			for (String key : groupedFeatures.keySet()) {
				if (key == null) continue;
				List<String> list = groupedFeatures.get(key);
				if (list.contains(element)) {
					return key;
				}
			}
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			List<String> elements = new ArrayList<>();
			for (String key : groupedFeatures.keySet()) {
				if (key != null) {
					elements.add(key);
				} else {
					for (String elem : groupedFeatures.get(key)) {
						elements.add(elem);
					}
				}
			}
			return elements.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (hasChildren(parentElement)) {
				return groupedFeatures.get(parentElement).toArray();
			}
			return null;
		}
	}

	public interface AxisChangedListener {
		public void axisChanged(String axis, int dimension);
	}

}