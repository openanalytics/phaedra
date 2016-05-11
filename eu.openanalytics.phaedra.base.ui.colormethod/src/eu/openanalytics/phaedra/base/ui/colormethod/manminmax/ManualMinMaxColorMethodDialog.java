package eu.openanalytics.phaedra.base.ui.colormethod.manminmax;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;

public class ManualMinMaxColorMethodDialog extends BaseColorMethodDialog {

	private ColorSelector minColorSelector;
	private ColorSelector maxColorSelector;
	private Text minValueTxt;
	private Text maxValueTxt;
	private Button[] minTypeBtn;
	private Button[] maxTypeBtn;
	
	private Label previewLabel;
	
	ManualMinMaxColorMethod colorMethod;
	
	private RGB originalMin;
	private RGB originalMax;
	private double originalMinValue;
	private double originalMaxValue;
	private int originalMinType;
	private int originalMaxType;
	
	public ManualMinMaxColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		Map<String,String> settings = new HashMap<>();
		colorMethod = (ManualMinMaxColorMethod)getColorMethod();
		colorMethod.getConfiguration(settings);
		
		originalMin = BaseColorMethod.getSetting(ManualMinMaxColorMethod.SETTING_MIN_RGB, settings, ManualMinMaxColorMethod.DEFAULT_MIN_COLOR);
		originalMax = BaseColorMethod.getSetting(ManualMinMaxColorMethod.SETTING_MAX_RGB, settings, ManualMinMaxColorMethod.DEFAULT_MAX_COLOR);
		originalMinValue = BaseColorMethod.getSetting(ManualMinMaxColorMethod.SETTING_MIN_VAL, settings, ManualMinMaxColorMethod.DEFAULT_MIN_VAL);
		originalMaxValue = BaseColorMethod.getSetting(ManualMinMaxColorMethod.SETTING_MAX_VAL, settings, ManualMinMaxColorMethod.DEFAULT_MAX_VAL);
		originalMinType = BaseColorMethod.getSetting(ManualMinMaxColorMethod.SETTING_MIN_TYPE, settings, ManualMinMaxColorMethod.DEFAULT_TYPE);
		originalMaxType = BaseColorMethod.getSetting(ManualMinMaxColorMethod.SETTING_MAX_TYPE, settings, ManualMinMaxColorMethod.DEFAULT_TYPE);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 300);
	}
	
	@Override
	protected void fillDialogArea(Composite area) {

		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(4).applyTo(area);
		
		/* Min */
		
		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Min:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		minColorSelector = new ColorSelector(area);
		minColorSelector.setColorValue(originalMin);
		minColorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					updatePreview();
				}
			}
		});
		GridDataFactory.fillDefaults().applyTo(minColorSelector.getButton());
		
		minValueTxt = new Text(area, SWT.BORDER);
		minValueTxt.setText("" + originalMinValue);
		GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(minValueTxt);
		
		Composite minGrp = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0,0).numColumns(2).applyTo(minGrp);
		minTypeBtn = new Button[2];
		minTypeBtn[0] = new Button(minGrp, SWT.RADIO);
		minTypeBtn[0].setText("Absolute");
		minTypeBtn[1] = new Button(minGrp, SWT.RADIO);
		minTypeBtn[1].setText("Percentile");
		if (originalMinType == ManualMinMaxColorMethod.TYPE_ABSOLUTE) minTypeBtn[0].setSelection(true);
		else minTypeBtn[1].setSelection(true);
		
		/* Max */
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Max:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		maxColorSelector = new ColorSelector(area);
		maxColorSelector.setColorValue(originalMax);
		maxColorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) {
					updatePreview();
				}
			}
		});
		GridDataFactory.fillDefaults().applyTo(maxColorSelector.getButton());
		
		maxValueTxt = new Text(area, SWT.BORDER);
		maxValueTxt.setText("" + originalMaxValue);
		GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(maxValueTxt);
		
		Composite maxGrp = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0,0).numColumns(2).applyTo(maxGrp);
		maxTypeBtn = new Button[2];
		maxTypeBtn[0] = new Button(maxGrp, SWT.RADIO);
		maxTypeBtn[0].setText("Absolute");
		maxTypeBtn[1] = new Button(maxGrp, SWT.RADIO);
		maxTypeBtn[1].setText("Percentile");
		if (originalMaxType == ManualMinMaxColorMethod.TYPE_ABSOLUTE) maxTypeBtn[0].setSelection(true);
		else maxTypeBtn[1].setSelection(true);
		
		/* Legend preview */
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Preview:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		previewLabel = new Label(area, SWT.NONE);
		GridDataFactory.fillDefaults().span(3,1).grab(true,false).applyTo(previewLabel);
		
		updatePreview();
	}

	private void updatePreview() {
		Map<String,String> settings = new HashMap<>();
		settings.put(ManualMinMaxColorMethod.SETTING_MIN_RGB, ColorUtils.createRGBString(minColorSelector.getColorValue()));
		settings.put(ManualMinMaxColorMethod.SETTING_MAX_RGB, ColorUtils.createRGBString(maxColorSelector.getColorValue()));
		settings.put(ManualMinMaxColorMethod.SETTING_MIN_VAL, minValueTxt.getText());
		settings.put(ManualMinMaxColorMethod.SETTING_MAX_VAL, maxValueTxt.getText());
		int minType = (minTypeBtn[0].getSelection()) ? ManualMinMaxColorMethod.TYPE_ABSOLUTE : ManualMinMaxColorMethod.TYPE_PERCENTILE;
		int maxType = (maxTypeBtn[0].getSelection()) ? ManualMinMaxColorMethod.TYPE_ABSOLUTE : ManualMinMaxColorMethod.TYPE_PERCENTILE;
		settings.put(ManualMinMaxColorMethod.SETTING_MIN_TYPE, "" + minType);
		settings.put(ManualMinMaxColorMethod.SETTING_MAX_TYPE, "" + maxType);
		
		colorMethod.configure(settings);
		Image legend = colorMethod.getLegend(200, 40, SWT.HORIZONTAL, false, null);
		previewLabel.setImage(legend);
	}
	
	@Override
	protected void doApply() {
		updatePreview();
	}
	
	@Override
	protected void doCancel() {
		Map<String,String> settings = new HashMap<>();
		settings.put(ManualMinMaxColorMethod.SETTING_MIN_RGB, ColorUtils.createRGBString(originalMin));
		settings.put(ManualMinMaxColorMethod.SETTING_MAX_RGB, ColorUtils.createRGBString(originalMax));
		settings.put(ManualMinMaxColorMethod.SETTING_MIN_VAL, "" + originalMinValue);
		settings.put(ManualMinMaxColorMethod.SETTING_MAX_VAL, "" + originalMaxValue);
		settings.put(ManualMinMaxColorMethod.SETTING_MIN_TYPE, "" + originalMinType);
		settings.put(ManualMinMaxColorMethod.SETTING_MAX_TYPE, "" + originalMaxType);
		colorMethod.configure(settings);
	}

}
