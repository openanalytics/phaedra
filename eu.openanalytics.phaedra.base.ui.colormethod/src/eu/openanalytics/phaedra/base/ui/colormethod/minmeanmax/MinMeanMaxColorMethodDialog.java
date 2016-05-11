package eu.openanalytics.phaedra.base.ui.colormethod.minmeanmax;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;

public class MinMeanMaxColorMethodDialog extends BaseColorMethodDialog {

	private ColorSelector minColorSelector;
	private ColorSelector meanColorSelector;
	private ColorSelector maxColorSelector;
	private Label previewLabel;
	
	MinMeanMaxColorMethod colorMethod;
	private RGB originalMin;
	private RGB originalMean;
	private RGB originalMax;
	
	public MinMeanMaxColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		colorMethod = (MinMeanMaxColorMethod)getColorMethod();
		originalMin = colorMethod.getMinRGB();
		originalMean = colorMethod.getMeanRGB();
		originalMax = colorMethod.getMaxRGB();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(350, 300);
	}
	
	@Override
	protected void fillDialogArea(Composite area) {

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Min:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		minColorSelector = new ColorSelector(area);
		GridDataFactory.fillDefaults().applyTo(minColorSelector.getButton());
		minColorSelector.setColorValue(colorMethod.getMinRGB());
		minColorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					updatePreview();
				}
			}
		});
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Mean:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		meanColorSelector = new ColorSelector(area);
		GridDataFactory.fillDefaults().applyTo(meanColorSelector.getButton());
		meanColorSelector.setColorValue(colorMethod.getMeanRGB());
		meanColorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					updatePreview();
				}
			}
		});
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Max:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		maxColorSelector = new ColorSelector(area);
		GridDataFactory.fillDefaults().applyTo(maxColorSelector.getButton());
		maxColorSelector.setColorValue(colorMethod.getMaxRGB());
		maxColorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					updatePreview();
				}
			}
		});
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Preview:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		previewLabel = new Label(area, SWT.NONE);
		GridDataFactory.fillDefaults().hint(200, 40).applyTo(previewLabel);
		
		updatePreview();
	}

	private void updatePreview() {
		colorMethod.setMinRGB(minColorSelector.getColorValue());
		colorMethod.setMeanRGB(meanColorSelector.getColorValue());
		colorMethod.setMaxRGB(maxColorSelector.getColorValue());
		Image legend = colorMethod.getLegend(200, 40, SWT.HORIZONTAL, false, null);
		previewLabel.setImage(legend);
	}
	
	
	@Override
	protected void doCancel() {
		colorMethod.setMinRGB(originalMin);
		colorMethod.setMeanRGB(originalMean);
		colorMethod.setMaxRGB(originalMax);
	}

}
