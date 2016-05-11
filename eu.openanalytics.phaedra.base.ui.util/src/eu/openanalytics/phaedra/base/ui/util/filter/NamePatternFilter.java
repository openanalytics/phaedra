package eu.openanalytics.phaedra.base.ui.util.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;

import eu.openanalytics.phaedra.base.ui.util.misc.StringMatcher;

public class NamePatternFilter extends PatternFilter {

	private List<StringMatcher> matchers;

	public NamePatternFilter() {
		super();
	}

	@Override
	public void setPattern(String patternString) {
		matchers = new ArrayList<>();
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
		super.setPattern(patternString);
	}

	// By default, the Filter uses a ILabelProvider. StyledCellLabelProvider is an IBaseLabelProvider and not a ILabelProvider implementation.
	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		String text = "";
		text = element.toString();
		return wordMatches(text);
	}

	@Override
	protected boolean wordMatches(String text) {
		if (text == null) {
			return false;
		}

		//If the whole text matches we are all set
		if (matchFilters(text)) {
			return true;
		}

		return false;
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
