package eu.openanalytics.phaedra.base.ui.colormethod.minmax;

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

public class MinMaxColorMethodDialog extends BaseColorMethodDialog {

	private ColorSelector minColorSelector;
	private ColorSelector maxColorSelector;
	private Label previewLabel;
	
	MinMaxColorMethod minmax;
	private RGB originalMin;
	private RGB originalMax;
	
	public MinMaxColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		minmax = (MinMaxColorMethod)getColorMethod();
		originalMin = minmax.getMinRGB();
		originalMax = minmax.getMaxRGB();
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
		minColorSelector.setColorValue(minmax.getMinRGB());
		minColorSelector.addListener(new IPropertyChangeListener() {
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
		maxColorSelector.setColorValue(minmax.getMaxRGB());
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
		minmax.setMinRGB(minColorSelector.getColorValue());
		minmax.setMaxRGB(maxColorSelector.getColorValue());
		Image legend = minmax.getLegend(200, 40, SWT.HORIZONTAL, false, null);
		previewLabel.setImage(legend);
	}
	
	
	@Override
	protected void doCancel() {
		minmax.setMinRGB(originalMin);
		minmax.setMaxRGB(originalMax);
	}

}
