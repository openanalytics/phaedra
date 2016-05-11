package eu.openanalytics.phaedra.base.ui.colormethod.lchc;

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

public class LcHcColorMethodDialog extends BaseColorMethodDialog {

	private ColorSelector[] colorSelectors;
	private RGB[] originalRGBs;
	private String[] colorNames;
	
	private Label previewLabel;

	private LcHcColorMethod lchc;
	
	public LcHcColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		lchc = (LcHcColorMethod)getColorMethod();
		originalRGBs = new RGB[5];
		originalRGBs[0] = lchc.getMinRGB();
		originalRGBs[1] = lchc.getLcRGB();
		originalRGBs[2] = lchc.getMeanRGB();
		originalRGBs[3] = lchc.getHcRGB();
		originalRGBs[4] = lchc.getMaxRGB();
		
		colorNames = new String[] {
			"Min","LC","Mean","HC","Max"
		};
	}

	@Override
	protected Point getInitialSize() {
		return new Point(350, 350);
	}
	
	@Override
	protected void fillDialogArea(Composite area) {

		colorSelectors = new ColorSelector[5];
		
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
		lchc.setMinRGB(colorSelectors[0].getColorValue());
		lchc.setLcRGB(colorSelectors[1].getColorValue());
		lchc.setMeanRGB(colorSelectors[2].getColorValue());
		lchc.setHcRGB(colorSelectors[3].getColorValue());
		lchc.setMaxRGB(colorSelectors[4].getColorValue());
		
		Image legend = lchc.getLegend(200, 40, SWT.HORIZONTAL, false, null);
		previewLabel.setImage(legend);
	}
	
	@Override
	protected void doCancel() {
		lchc.setMinRGB(originalRGBs[0]);
		lchc.setLcRGB(originalRGBs[1]);
		lchc.setMeanRGB(originalRGBs[2]);
		lchc.setHcRGB(originalRGBs[3]);
		lchc.setMaxRGB(originalRGBs[4]);
	}

}
