package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ColumnEditingConfiguration {

	public Function<Object, Object> valueGetter;
	public BiConsumer<Object, Object> valueSetter;
	public Function<Object, Boolean> editableChecker;

}
