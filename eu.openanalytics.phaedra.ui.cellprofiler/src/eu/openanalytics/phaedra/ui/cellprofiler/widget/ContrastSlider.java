package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class ContrastSlider {

	private int low, high;
	private ContrastChangeListener listener;
	
	private Composite control;
	private Slider contrastMinSlider;
	private Slider contrastMaxSlider;
	private Text contrastMin;
	private Text contrastMax;
	
	public ContrastSlider(Composite parent, int low, int high, int depth, ContrastChangeListener listener) {
		this.low = low;
		this.high = high;
		this.listener = listener;
		
		int maxContrast = (int) Math.pow(2, depth); // 1 too high, because the slider has an excluding upper bound.
		int contrastStep = (int) maxContrast / 40;
		
		control = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(control);
		
		new Label(control, SWT.NONE).setImage(IconManager.getIconImage("contrast_low.png"));
		contrastMinSlider = new Slider(control, SWT.HORIZONTAL);
		contrastMinSlider.setValues(low, 0, maxContrast, 1, 10, contrastStep);
		contrastMinSlider.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lowChanged(contrastMinSlider.getSelection());
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(contrastMinSlider);
		
		contrastMin = new Text(control, SWT.BORDER);
		contrastMin.setText("" + low);
		contrastMin.addModifyListener(e -> lowChanged(Integer.parseInt(contrastMin.getText())));
		GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(contrastMin);
		
		new Label(control, SWT.NONE).setImage(IconManager.getIconImage("contrast_high.png"));
		contrastMaxSlider = new Slider(control, SWT.HORIZONTAL);
		contrastMaxSlider.setValues(high, 0, maxContrast, 1, 10, contrastStep);
		contrastMaxSlider.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				highChanged(contrastMaxSlider.getSelection());
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(contrastMaxSlider);
		
		contrastMax = new Text(control, SWT.BORDER);
		contrastMax.setText("" + high);
		contrastMax.addModifyListener(e -> highChanged(Integer.parseInt(contrastMax.getText())));
		GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(contrastMax);
	}
	
	public Composite getControl() {
		return control;
	}
	
	public static interface ContrastChangeListener {
		public void contrastChanged(int low, int high);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void lowChanged(int newLow) {
		int maxLow = high;
		if (newLow > maxLow) newLow = maxLow;
		if (newLow < 0) newLow = 0;
		if (contrastMinSlider.getSelection() != newLow) contrastMinSlider.setSelection(newLow);
		if (Integer.parseInt(contrastMin.getText()) != newLow) contrastMin.setText("" + newLow);
		low = newLow;
		if (listener != null) listener.contrastChanged(low, high);
	}
	
	private void highChanged(int newHigh) {
		int minHigh = low;
		if (newHigh < minHigh) newHigh = minHigh;
		if (newHigh >= contrastMaxSlider.getMaximum()) newHigh = contrastMaxSlider.getMaximum() - 1;
		if (contrastMaxSlider.getSelection() != newHigh) contrastMaxSlider.setSelection(newHigh);
		if (Integer.parseInt(contrastMax.getText()) != newHigh) contrastMax.setText("" + newHigh);
		high = newHigh;
		if (listener != null) listener.contrastChanged(low, high);
	}
}
