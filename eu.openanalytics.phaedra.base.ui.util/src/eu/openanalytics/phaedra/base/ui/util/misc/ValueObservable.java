package eu.openanalytics.phaedra.base.ui.util.misc;

import java.util.Observable;

public class ValueObservable extends Observable {
	public void valueChanged() {
		setChanged();
		notifyObservers();
	}
	
	public void valueChanged(Object o) {
		setChanged();
		notifyObservers(o);
	}
}