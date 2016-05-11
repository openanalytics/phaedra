package eu.openanalytics.phaedra.ui.plate.classification;

import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;

public class ClassificationBatch<T> {

	private T[] items;
	private FeatureClass featureClass;
	
	public T[] getItems() {
		return items;
	}

	public void setItems(T[] items) {
		this.items = items;
	}

	public FeatureClass getFeatureClass() {
		return featureClass;
	}

	public void setFeatureClass(FeatureClass featureClass) {
		this.featureClass = featureClass;
	}

}
