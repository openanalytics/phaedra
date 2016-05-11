package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;

public class ThumbnailViewer extends StructuredViewer {

	private Thumbnail thumbnail;

	private int selConfig;

	public ThumbnailViewer(Composite parent) {
		this.thumbnail = new Thumbnail(parent, SWT.BORDER);
	}

	@Override
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		if (labelProvider instanceof AbstractThumbnailLabelProvider) {
			AbstractThumbnailLabelProvider provider = ((AbstractThumbnailLabelProvider) labelProvider);
			thumbnail.setCellRenderer(provider.createCellRenderer());
			thumbnail.hookSelectionListener(() -> handleSelect(null));
		}

		super.setLabelProvider(labelProvider);
	}

	@Override
	protected Widget doFindInputItem(Object element) {
		if (equals(element, getRoot())) {
			return getControl();
		}
		return null;
	}

	@Override
	protected Widget doFindItem(Object element) {
		return thumbnail;
	}

	@Override
	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
		// Do nothing.
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		getControl().setRedraw(false);
		try {
			preservingSelection(() -> internalRefresh(getRoot()));
		} finally {
			getControl().setRedraw(true);
		}
	}

	@Override
	protected void internalRefresh(Object element) {
		thumbnail.setInput(getSortedChildren(element));
		thumbnail.redraw();
	}

	@Override
	public void reveal(Object element) {
		// Do nothing.
	}

	@Override
	public ISelection getSelection() {
		// Send a configurable Structured Selection instead.
		Control control = getControl();
		if (control == null || control.isDisposed()) {
			return new ConfigurableStructuredSelection();
		}
		List<?> list = getSelectionFromWidget();
		ConfigurableStructuredSelection sel = new ConfigurableStructuredSelection(list, getComparer());
		sel.setConfiguration(selConfig);
		return sel;
	}

	@Override
	protected List<Object> getSelectionFromWidget() {
		List<Object> sel = new ArrayList<>();
		for (ThumbnailCell cell : thumbnail.getSelectedCells()) {
			sel.add(cell.getData());
		}
		return sel;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void setSelectionToWidget(List l, boolean reveal) {
		Set<ThumbnailCell> selectedCells = new HashSet();
		// Convert List to Set for performance. For 12 000 rows, a Set is at least 150 times faster (350ms vs 2ms).
		Set set = new HashSet<>(l);
		for (ThumbnailCell cell : thumbnail.getCells()) {
			if (set.contains(cell.getData())) {
				selectedCells.add(cell);
			}
		}
		thumbnail.setSelectedCells(selectedCells);
	}

	@Override
	public Control getControl() {
		return thumbnail;
	}

	public Thumbnail getThumbnail() {
		return thumbnail;
	}

	public void resizeImages() {
		thumbnail.resize(true);
	}

	public int getSelectionConfig() {
		return selConfig;
	}

	public void setSelectionConfig(int selConfig) {
		this.selConfig = selConfig;
	}

	public Object getSettings() {
		Map<String, Integer> map = new HashMap<>();
		map.put("ROWS", getThumbnail().getRows());
		map.put("COLS", getThumbnail().getCols());
		map.put("PADDING", getThumbnail().getPadding());
		map.put("COLOR_R", getThumbnail().getPaddingColor().getRed());
		map.put("COLOR_G", getThumbnail().getPaddingColor().getGreen());
		map.put("COLOR_B", getThumbnail().getPaddingColor().getBlue());
		return map;
	}

	@SuppressWarnings("unchecked")
	public void setSettings(Object settings) {
		if (settings instanceof Map) {
			Map<String, Integer> map = (Map<String, Integer>) settings;
			getThumbnail().setRows(map.getOrDefault("ROWS", getThumbnail().getRows()));
			getThumbnail().setCols(map.getOrDefault("COLS", getThumbnail().getCols()));
			getThumbnail().setPadding(map.getOrDefault("PADDING", getThumbnail().getPadding()));
			int r = map.getOrDefault("COLOR_R", getThumbnail().getPaddingColor().getRed());
			int g = map.getOrDefault("COLOR_G", getThumbnail().getPaddingColor().getGreen());
			int b = map.getOrDefault("COLOR_B", getThumbnail().getPaddingColor().getBlue());
			getThumbnail().setPaddingColor(new Color(null, r, g, b));
		}
	}

}