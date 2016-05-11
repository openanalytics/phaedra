package eu.openanalytics.phaedra.base.ui.nattable.misc;

import static org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX;
import static org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes.FILTER_COMPARATOR;
import static org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes.FILTER_DISPLAY_CONVERTER;
import static org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes.TEXT_DELIMITER;
import static org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes.TEXT_MATCHING_MODE;
import static org.eclipse.nebula.widgets.nattable.style.DisplayMode.NORMAL;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsStaticFilterStrategy;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.FilterRowUtils;
import org.eclipse.nebula.widgets.nattable.filterrow.ParseResult;
import org.eclipse.nebula.widgets.nattable.filterrow.ParseResult.MatchType;
import org.eclipse.nebula.widgets.nattable.filterrow.TextMatchingMode;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import eu.openanalytics.phaedra.base.ui.nattable.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class PhaedraGlazedListsStaticFilterStrategy<T> extends DefaultGlazedListsStaticFilterStrategy<T> {

	public PhaedraGlazedListsStaticFilterStrategy(FilterList<T> filterList, IColumnAccessor<T> columnAccessor
			, IConfigRegistry configRegistry) {

		super(filterList, columnAccessor, configRegistry);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void applyFilter(Map<Integer, Object> filterIndexToObjectMap) {

		//wait until all listeners had the chance to handle the clear event
		try {
			this.filterLock.writeLock().lock();
			getMatcherEditor().getMatcherEditors().clear();
		}
		finally {
			this.filterLock.writeLock().unlock();
		}

		if (!filterIndexToObjectMap.isEmpty()) {
			try {
				EventList<MatcherEditor<T>> matcherEditors = new BasicEventList<MatcherEditor<T>>();

				if (columnAccessor instanceof IAsyncColumnAccessor) {
					// Make active during filtering so not loaded values are correctly filtered.
					((IAsyncColumnAccessor) columnAccessor).setAsync(false);
				}

				for (Entry<Integer, Object> mapEntry : filterIndexToObjectMap.entrySet()) {
					Integer columnIndex = mapEntry.getKey();
					String filterText = getStringFromColumnObject(columnIndex, mapEntry.getValue());

					String textDelimiter = configRegistry.getConfigAttribute(
							TEXT_DELIMITER, NORMAL, FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex);
					TextMatchingMode textMatchingMode = configRegistry.getConfigAttribute(
							TEXT_MATCHING_MODE, NORMAL, FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex);
					IDisplayConverter displayConverter = configRegistry.getConfigAttribute(
							FILTER_DISPLAY_CONVERTER, NORMAL, FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex);
					Comparator comparator = configRegistry.getConfigAttribute(
							FILTER_COMPARATOR, NORMAL, FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex);
					final Function<T, Object> columnValueProvider = getColumnValueProvider(columnIndex);

					List<ParseResult> parseResults = FilterRowUtils.parse(filterText, textDelimiter, textMatchingMode);

					EventList<MatcherEditor<T>> stringMatcherEditors = new BasicEventList<MatcherEditor<T>>();
					for (ParseResult parseResult : parseResults)
					{
						MatchType matchOperation = parseResult.getMatchOperation();
						if (matchOperation == MatchType.NONE) {
							NumericMatcherEditor numEditor = new NumericMatcherEditor(columnIndex, columnAccessor, displayConverter, parseResult.getValueToMatch());
							stringMatcherEditors.add(numEditor);
						} else {
							Object threshold = displayConverter.displayToCanonicalValue(parseResult.getValueToMatch());
							matcherEditors.add(getThresholdMatcherEditor(columnIndex, threshold, comparator, columnValueProvider, matchOperation));
						}
					}

					if (stringMatcherEditors.size()>0){
						CompositeMatcherEditor<T> stringCompositeMatcherEditor = new CompositeMatcherEditor<T>(stringMatcherEditors);
						stringCompositeMatcherEditor.setMode(CompositeMatcherEditor.OR);
						matcherEditors.add(stringCompositeMatcherEditor);
					}
				}

				//wait until all listeners had the chance to handle the clear event
				try {
					this.filterLock.writeLock().lock();
					getMatcherEditor().getMatcherEditors().addAll(matcherEditors);
				}
				finally {
					this.filterLock.writeLock().unlock();
				}

			} catch (Exception e) {
				EclipseLog.error("Error on applying a filter.", e, Activator.getDefault());
			} finally {
				if (columnAccessor instanceof IAsyncColumnAccessor) {
					((IAsyncColumnAccessor) columnAccessor).setAsync(true);
				}
			}
		}
		getMatcherEditor().getMatcherEditors().addAll(staticMatcherEditor.values());
	}

}
