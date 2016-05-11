package eu.openanalytics.phaedra.base.ui.util.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import eu.openanalytics.phaedra.base.ui.util.tooltip.AdvancedToolTip;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;

/**
 * This class provides simplified support for annotations on a Text widget.
 * The annotations mark regions of the text (e.g. underline) and provide a hover tooltip.
 * 
 * This is meant as a simplified version of the Eclipse TextMarker concept.
 */
public class AnnotatedTextSupport {
	
	private StyledText text;
	private Runnable postModifyTimer;
	private boolean refreshEnabled;
	
	private Set<TextAnnotation> annotations;
	
	public AnnotatedTextSupport(StyledText text, Runnable annotationCalculator) {
		this.text = text;
		this.annotations = new HashSet<>();
		this.refreshEnabled = true;
		
		this.postModifyTimer = () -> {
			refreshEnabled = false;
			if (annotationCalculator != null) annotationCalculator.run();
			refreshEnabled = true;
			applyAnnotations();
		};
		this.text.addModifyListener(event -> Display.getCurrent().timerExec(1000, postModifyTimer));
		new AnnotationTooltip(text);
	}
	
	public void addAnnotation(TextAnnotation annotation) {
		annotations.add(annotation);
		applyAnnotations();
	}
	
	public void removeAnnotation(TextAnnotation annotation) {
		annotations.remove(annotation);
		applyAnnotations();
	}
	
	public void clearAnnotations() {
		annotations.clear();
		applyAnnotations();
	}
	
	public void setRefresh(boolean refresh) {
		refreshEnabled = refresh;
	}

	/*
	 * Non-public
	 * **********
	 */
	
	private void applyAnnotations() {
		if (!refreshEnabled) return;
		if (text == null || text.isDisposed()) return;
		
		List<StyleRange> ranges = new ArrayList<>();
		for (TextAnnotation annotation: annotations) {
			if (annotation.getStyle() == null) continue;
			StyleRange range = new StyleRange(annotation.getStyle());
			range.start = annotation.getStart();
			range.length = annotation.getEnd() - annotation.getStart();
			ranges.add(range);
		}
		Collections.sort(ranges, (r1,r2) -> r1.start - r2.start);
		text.setStyleRanges(ranges.toArray(new StyleRange[ranges.size()]));
	}
	
	private class AnnotationTooltip extends AdvancedToolTip {

		public AnnotationTooltip(Canvas canvas) {
			super(canvas, ToolTip.NO_RECREATE, false);
			setPopupDelay(500);
			setShift(new Point(6, 3));
			setLabelProvider(new ToolTipLabelProvider(){
				@Override
				public Image getImage(Object element) {
					if (element instanceof TextAnnotation) return ((TextAnnotation)element).getImage();
					return null;
				}
				@Override
				public String getText(Object element) {
					if (element instanceof TextAnnotation) return ((TextAnnotation)element).getText();
					return null;
				}
			});
		}

		@Override
		public Object getData(Event event) {
			Point hoverLocation = new Point(event.x, event.y);
			TextAnnotation annotation = null;
			try {
				int charNr = text.getOffsetAtLocation(hoverLocation);
				annotation = annotations.stream().filter(a -> a.getStart() <= charNr && a.getEnd() >= charNr).findFirst().orElse(null);
			} catch (IllegalArgumentException e) {
				// No character under mouse, do nothing.
			}
			return annotation;
		}

	}
}
