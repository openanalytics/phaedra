package eu.openanalytics.phaedra.base.ui.nattable.selection;

public interface ISelectionDataColumnAccessor<T> {

	Object getSelectionValue(T data, int column);

}
