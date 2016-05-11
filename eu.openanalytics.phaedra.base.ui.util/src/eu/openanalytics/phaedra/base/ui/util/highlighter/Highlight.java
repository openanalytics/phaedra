package eu.openanalytics.phaedra.base.ui.util.highlighter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

public class Highlight {

	private State currentState;

	public Highlight() {
		currentState = State.Off;
	}

	public void setState(State state) {
		currentState = state;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void paint(GC gc, int x, int y, int w, int h) {
		if (currentState == State.Off || gc.isDisposed()) return;
		int lineWidth = HighlightTimer.getInstance().getLineWidth();
		gc.setLineWidth(lineWidth);

		Color[] colors = HighlightTimer.getInstance().getColors();

		int halfLineWidth = lineWidth/2;
		int newX = x + halfLineWidth;
		int newY = y + halfLineWidth;
		int newH = h - lineWidth;
		int newW = w - lineWidth;

		Color originalFG = gc.getForeground();
		switch (HighlightTimer.getInstance().getStyle()) {
		case FLASH:
			gc.setForeground(currentState == State.On1 ? colors[0] : colors[1]);
			gc.drawRectangle(newX, newY, newW, newH);
			break;
		case ROTATING:
			gc.setForeground(currentState == State.On1 ? colors[0] : colors[1]);
			gc.drawRectangle(newX, newY, newW, newH);
			gc.setLineDash(new int[] { 5, 5 });
			gc.setForeground(currentState == State.On2 ? colors[0] : colors[1]);
			gc.drawRectangle(newX, newY, newW, newH);
			gc.setLineStyle(SWT.LINE_SOLID);
			break;
		case STATIC:
			gc.setForeground(colors[0]);
			gc.drawRectangle(newX, newY, newW, newH);
			gc.setLineDash(new int[] { 5, 5 });
			gc.setForeground(colors[1]);
			gc.drawRectangle(newX, newY, newW, newH);
			gc.setLineStyle(SWT.LINE_SOLID);
			break;
		default:
			break;
		}
		gc.setForeground(originalFG);
	}

}
