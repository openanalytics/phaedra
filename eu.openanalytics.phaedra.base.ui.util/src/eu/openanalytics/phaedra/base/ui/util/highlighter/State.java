package eu.openanalytics.phaedra.base.ui.util.highlighter;

public enum State {
	Off,
	On1,
	On2;

	public State getNext() {
		if (this == On1) return On2;
		if (this == On2) return On1;
		return Off;
	}

}