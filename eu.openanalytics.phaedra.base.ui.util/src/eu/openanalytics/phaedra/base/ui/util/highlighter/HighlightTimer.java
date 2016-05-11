package eu.openanalytics.phaedra.base.ui.util.highlighter;

import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_COLOR_1;
import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_COLOR_2;
import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_LINE_WIDTH;
import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_STYLE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.util.Activator;

public class HighlightTimer implements Runnable {

	private static HighlightTimer instance;

	private volatile boolean running;
	private int delay;

	private State state;

	private List<HighlightListener> listeners;

	// Highlighting options.
	private HightlightStyle style;
	private int lineWidth;
	private Color[] colors;

	private HighlightTimer() {
		this.running = false;
		this.delay = 500;
		this.state = State.On1;

		this.listeners = new ArrayList<>();

		this.style = HightlightStyle.getStyle(getStore().getString(HIGHTLIGHT_STYLE));
		this.lineWidth = getStore().getInt(HIGHTLIGHT_LINE_WIDTH);
		this.colors = new Color[] {
			new Color(null, PreferenceConverter.getColor(getStore(), HIGHTLIGHT_COLOR_1))
			, new Color(null, PreferenceConverter.getColor(getStore(), HIGHTLIGHT_COLOR_2))
		};

		IPropertyChangeListener listener = (event) -> {
			if (event.getProperty().equals(HIGHTLIGHT_STYLE)) {
				String newStyle = (String) event.getNewValue();
				style = HightlightStyle.getStyle(newStyle);
			} else if (event.getProperty().equals(HIGHTLIGHT_COLOR_1)) {
				RGB rgb = (RGB) event.getNewValue();
				colors[0] = new Color(null, rgb);
			} else if (event.getProperty().equals(HIGHTLIGHT_COLOR_2)) {
				RGB rgb = (RGB) event.getNewValue();
				colors[1] = new Color(null, rgb);
			} else if (event.getProperty().equals(HIGHTLIGHT_LINE_WIDTH)) {
				lineWidth = (int) event.getNewValue();
			}
			setRunning(running);
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
	}

	public static HighlightTimer getInstance() {
		if (instance == null) instance = new HighlightTimer();
		return instance;
	}

	public void addListener(HighlightListener listener) {
		listeners.add(listener);
		if (!running && !listeners.isEmpty()) setRunning(true);
	}

	public void removeListener(HighlightListener listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) setRunning(false);
	}

	@Override
	public void run() {
		highlight();
		if (running && style.isRunning()) {
			Display.getDefault().timerExec(delay, this);
		}
	}

	public HightlightStyle getStyle() {
		return style;
	}

	public Color[] getColors() {
		return colors;
	}

	public int getLineWidth() {
		return lineWidth;
	}

	private void setRunning(boolean running) {
		this.running = running;
		if (running) Display.getDefault().timerExec(0, this);
	}

	private void highlight() {
		state = state.getNext();
		listeners.forEach(l -> l.stateChanged(state));
	}

	private static IPreferenceStore getStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}