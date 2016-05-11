package eu.openanalytics.phaedra.base.ui.util.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import eu.openanalytics.phaedra.base.ui.util.misc.StringMatcher;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class FilterMatcher extends ViewerFilter {

	private List<StringMatcher> matchers;

	public FilterMatcher() {
		matchers = new ArrayList<>();
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		TableViewer tableViewer = (TableViewer) viewer;

		if (matchers.isEmpty()) {
			return true;
		}

		for (int i = 0; i < tableViewer.getTable().getColumnCount(); i++) {
			CellLabelProvider cellLabelProvider = tableViewer.getLabelProvider(i);
			String text = (String) getText(cellLabelProvider, element);
			if (matchFilters(text)) {
				return true;
			}
		}

		return false;
	}

	public void setPattern(String patternString) {
		matchers.clear();
		if (patternString != null) {
			String[] patterns = patternString.split(",");
			for (String pattern : patterns) {
				if (!pattern.isEmpty()) {
					// Remove first space and/or last space if present so e.g. "*cell*ser*4 , *cell*ser*2" also works.
					if (pattern.startsWith(" ")) pattern = pattern.substring(1, pattern.length());
					if (pattern.endsWith(" ")) pattern = pattern.substring(0, pattern.length()-1);
					matchers.add(new StringMatcher(pattern, true, false));
				}
			}
		}
	}

	private Object getText(CellLabelProvider labelProvider, Object element) {
		Object result = null;
		if (labelProvider != null) result = ReflectionUtils.invoke("getText", labelProvider, new Object[] { element }, new Class<?>[] { Object.class });
		return result;
	}

	private boolean matchFilters(String string) {
		for (StringMatcher matcher : matchers) {
			if (matcher.match(string)) {
				return true;
			}
		}
		return false;
	}

}