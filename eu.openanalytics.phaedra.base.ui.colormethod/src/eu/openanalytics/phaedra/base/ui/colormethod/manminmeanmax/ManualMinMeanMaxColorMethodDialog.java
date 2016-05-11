package eu.openanalytics.phaedra.base.ui.colormethod.manminmeanmax;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
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

public class ManualMinMeanMaxColorMethodDialog extends BaseColorMethodDialog {

	private ColorSelector minColorSelector;
	private ColorSelector meanColorSelector;
	private ColorSelector maxColorSelector;
	private Text minValueTxt;
	private Text maxValueTxt;
	private Button[] minTypeBtn;
	private Button[] meanTypeBtn;
	private Button[] maxTypeBtn;
	
	private Label previewLabel;
	
	ManualMinMeanMaxColorMethod colorMethod;
	
	private RGB originalMin;
	private RGB originalMean;
	private RGB originalMax;
	private double originalMinValue;
	private double originalMaxValue;
	private int originalMinType;
	private int originalMeanType;
	private int originalMaxType;
	
	public ManualMinMeanMaxColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		
		Map<String,String> settings = new HashMap<>();
		colorMethod = (ManualMinMeanMaxColorMethod)getColorMethod();
		colorMethod.getConfiguration(settings);
		
		originalMin = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MIN_RGB, settings, ManualMinMeanMaxColorMethod.DEFAULT_MIN_COLOR);
		originalMean = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MEAN_RGB, settings, ManualMinMeanMaxColorMethod.DEFAULT_MEAN_COLOR);
		originalMax = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MAX_RGB, settings, ManualMinMeanMaxColorMethod.DEFAULT_MAX_COLOR);
		originalMinValue = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MIN_VAL, settings, ManualMinMeanMaxColorMethod.DEFAULT_MIN_VAL);
		originalMaxValue = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MAX_VAL, settings, ManualMinMeanMaxColorMethod.DEFAULT_MAX_VAL);
		originalMinType = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MIN_TYPE, settings, ManualMinMeanMaxColorMethod.DEFAULT_TYPE);
		originalMeanType = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MEAN_TYPE, settings, ManualMinMeanMaxColorMethod.DEFAULT_MEAN_TYPE);
		originalMaxType = BaseColorMethod.getSetting(ManualMinMeanMaxColorMethod.SETTING_MAX_TYPE, settings, ManualMinMeanMaxColorMethod.DEFAULT_TYPE);
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
		minColorSelector.addListener(event -> {
			if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) updatePreview();
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
		if (originalMinType == ManualMinMeanMaxColorMethod.TYPE_ABSOLUTE) minTypeBtn[0].setSelection(true);
		else minTypeBtn[1].setSelection(true);
		
		/* Mean */
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Mean:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		meanColorSelector = new ColorSelector(area);
		meanColorSelector.setColorValue(originalMean);
		meanColorSelector.addListener(event -> {
			if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) updatePreview();
		});
		GridDataFactory.fillDefaults().applyTo(meanColorSelector.getButton());
		
		new Label(area, SWT.NONE);

		Composite meanGrp = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0,0).numColumns(2).applyTo(meanGrp);
		meanTypeBtn = new Button[2];
		meanTypeBtn[0] = new Button(meanGrp, SWT.RADIO);
		meanTypeBtn[0].setText("All Values");
		meanTypeBtn[1] = new Button(meanGrp, SWT.RADIO);
		meanTypeBtn[1].setText("Min & Max");
		if (originalMeanType == ManualMinMeanMaxColorMethod.MEAN_TYPE_ALL) meanTypeBtn[0].setSelection(true);
		else meanTypeBtn[1].setSelection(true);
		
		/* Max */
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Max:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);
		
		maxColorSelector = new ColorSelector(area);
		maxColorSelector.setColorValue(originalMax);
		maxColorSelector.addListener(event -> {
			if (event.getProperty().equals(ColorSelector.PROP_COLORCHANGE)) updatePreview();
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
		if (originalMaxType == ManualMinMeanMaxColorMethod.TYPE_ABSOLUTE) maxTypeBtn[0].setSelection(true);
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
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MIN_RGB, ColorUtils.createRGBString(minColorSelector.getColorValue()));
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MEAN_RGB, ColorUtils.createRGBString(meanColorSelector.getColorValue()));
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MAX_RGB, ColorUtils.createRGBString(maxColorSelector.getColorValue()));
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MIN_VAL, minValueTxt.getText());
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MAX_VAL, maxValueTxt.getText());
		int minType = (minTypeBtn[0].getSelection()) ? ManualMinMeanMaxColorMethod.TYPE_ABSOLUTE : ManualMinMeanMaxColorMethod.TYPE_PERCENTILE;
		int meanType = (meanTypeBtn[0].getSelection()) ? ManualMinMeanMaxColorMethod.MEAN_TYPE_ALL : ManualMinMeanMaxColorMethod.MEAN_TYPE_MINMAX;
		int maxType = (maxTypeBtn[0].getSelection()) ? ManualMinMeanMaxColorMethod.TYPE_ABSOLUTE : ManualMinMeanMaxColorMethod.TYPE_PERCENTILE;
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MIN_TYPE, "" + minType);
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MEAN_TYPE, "" + meanType);
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MAX_TYPE, "" + maxType);
		
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
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MIN_RGB, ColorUtils.createRGBString(originalMin));
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MEAN_RGB, ColorUtils.createRGBString(originalMean));
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MAX_RGB, ColorUtils.createRGBString(originalMax));
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MIN_VAL, "" + originalMinValue);
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MAX_VAL, "" + originalMaxValue);
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MIN_TYPE, "" + originalMinType);
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MEAN_TYPE, "" + originalMeanType);
		settings.put(ManualMinMeanMaxColorMethod.SETTING_MAX_TYPE, "" + originalMaxType);
		colorMethod.configure(settings);
	}

}
