package eu.openanalytics.phaedra.base.ui.colormethod.lc;

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

public class LcColorMethodDialog extends BaseColorMethodDialog {

	private ColorSelector[] colorSelectors;
	private RGB[] originalRGBs;
	private String[] colorNames;
	
	private Label previewLabel;

	private LcColorMethod colorMethod;
	
	public LcColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		colorMethod = (LcColorMethod)getColorMethod();
		originalRGBs = new RGB[3];
		originalRGBs[0] = colorMethod.getMinRGB();
		originalRGBs[1] = colorMethod.getLcRGB();
		originalRGBs[2] = colorMethod.getMaxRGB();
		
		colorNames = new String[] {
			"Min","LC","Max"
		};
	}

	@Override
	protected Point getInitialSize() {
		return new Point(350, 350);
	}
	
	@Override
	protected void fillDialogArea(Composite area) {

		colorSelectors = new ColorSelector[3];
		
		for (int i=0; i<colorSelectors.length; i++) {
			createColorSelector(i, area);
		}
				
		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Preview:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		previewLabel = new Label(area, SWT.NONE);
		GridDataFactory.fillDefaults().hint(200, 40).applyTo(previewLabel);
		
		updatePreview();
	}

	private void createColorSelector(int index, Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(colorNames[index] + ":");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		colorSelectors[index] = new ColorSelector(parent);
		GridDataFactory.fillDefaults().applyTo(colorSelectors[index].getButton());
		colorSelectors[index].setColorValue(originalRGBs[index]);
		colorSelectors[index].addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					updatePreview();
				}
			}
		});
	}
	
	private void updatePreview() {
		colorMethod.setMinRGB(colorSelectors[0].getColorValue());
		colorMethod.setLcRGB(colorSelectors[1].getColorValue());
		colorMethod.setMaxRGB(colorSelectors[2].getColorValue());
		
		Image legend = colorMethod.getLegend(200, 40, SWT.HORIZONTAL, false, null);
		previewLabel.setImage(legend);
	}
	
	@Override
	protected void doCancel() {
		colorMethod.setMinRGB(originalRGBs[0]);
		colorMethod.setLcRGB(originalRGBs[1]);
		colorMethod.setMaxRGB(originalRGBs[2]);
	}

}
