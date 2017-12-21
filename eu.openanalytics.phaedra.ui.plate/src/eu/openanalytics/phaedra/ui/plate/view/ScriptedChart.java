package eu.openanalytics.phaedra.ui.plate.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class ScriptedChart extends DecoratedView {

	private Combo scriptSrcCmb;
	private Button runScriptBtn;
	private Canvas chartCanvas;
	
	private ISelectionListener selectionListener;
	private Protocol currentProtocol;
	private ImageData currentChart;
	private String errorMsg;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 5).applyTo(parent);
		
		new Label(parent, SWT.NONE).setText("Script:");
		
		scriptSrcCmb = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scriptSrcCmb);
		try {
			List<String> scripts = ScriptService.getInstance().getCatalog().getAvailableScripts("");
			scriptSrcCmb.setItems(scripts.toArray(new String[scripts.size()]));
		} catch (IOException e) {
			EclipseLog.warn("Failed to list scripts from catalog", e, Activator.PLUGIN_ID);
		}
		
		runScriptBtn = new Button(parent, SWT.PUSH);
		runScriptBtn.setText("Run");
		runScriptBtn.addListener(SWT.Selection, e -> createChart());
		GridDataFactory.fillDefaults().applyTo(runScriptBtn);
		
		chartCanvas = new Canvas(parent, SWT.NONE);
		chartCanvas.addPaintListener(e -> drawChart());
		GridDataFactory.fillDefaults().span(3, 1).grab(true, true).applyTo(chartCanvas);
		
		selectionListener = (p, s) -> {
			Protocol protocol = SelectionUtils.getFirstObject(s, Protocol.class);
			if (protocol != null) currentProtocol = protocol;
		};
		getSite().getPage().addSelectionListener(selectionListener);
		SelectionUtils.triggerActiveSelection(selectionListener);
		
		chartCanvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				createChart();
			}
		});
		
		addDecorator(new SettingsDecorator(() -> currentProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		initDecorators(parent);
	}

	@Override
	public void setFocus() {
		scriptSrcCmb.setFocus();
	}
	
	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

	public static ImageData createChart(String scriptSrc, int w, int h) throws ScriptException {
		if (scriptSrc == null || scriptSrc.isEmpty()) return null;
		
		Map<String, Object> args = new HashMap<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		args.put("output", bos);
		args.put("width", Math.max(w, 100));
		args.put("height", Math.max(h, 100));
		ScriptService.getInstance().getCatalog().run(scriptSrc, args);
		
		byte[] img = bos.toByteArray();
		ImageLoader loader = new ImageLoader();
		ImageData[] data = loader.load(new ByteArrayInputStream(img));
		return data[0];
	}
	
	private void createChart() {
		currentChart = null;
		errorMsg = null;
		try {
			String scriptSrc = scriptSrcCmb.getText();
			int w = chartCanvas.getBounds().width;
			int h = chartCanvas.getBounds().height;
			currentChart = createChart(scriptSrc, w, h);
		} catch (Exception e) {
			errorMsg = StringUtils.getStackTrace(e);
		}
		chartCanvas.redraw();
	}
	
	private void drawChart() {
		if (currentChart == null && errorMsg == null) return;
		GC gc = new GC(chartCanvas);
		try {
			if (errorMsg == null) {
				Image image = new Image(gc.getDevice(), currentChart);
				gc.drawImage(image, 0, 0);
				image.dispose();
			} else {
				gc.drawText(errorMsg, 5, 5);
			}
		} finally {
			gc.dispose();
		}
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty("scriptSrc", scriptSrcCmb.getText());
		return properties;
	}

	private void setProperties(Properties properties) {
		String scriptSrc = (String) properties.getProperty("scriptSrc");
		if (scriptSrc != null) scriptSrcCmb.setText(scriptSrc);
		createChart();
	}
}
