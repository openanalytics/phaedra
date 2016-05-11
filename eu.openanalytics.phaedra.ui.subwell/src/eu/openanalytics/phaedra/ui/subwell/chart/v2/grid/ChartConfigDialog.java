package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewPart;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;
import eu.openanalytics.phaedra.base.ui.util.view.ViewUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SerializationUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class ChartConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

	private static final String APPLY_LABEL = "Apply";

	private IViewPart view;
	private AbstractSubWellChartLayer layer;
	private List<LayerSettings<Well, Well>> originalLayerSettings;

	private String name;
	private String viewId;

	public ChartConfigDialog(Shell parent, AbstractSubWellChartLayer layer, String name, String viewId) {
		super(parent);
		setShellStyle(SWT.TITLE | SWT.RESIZE | SWT.MIN | SWT.MAX | SWT.TOP);

		this.layer = layer;
		this.name = name;
		this.viewId = viewId;
		this.originalLayerSettings = new ArrayList<>();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(name + " Configuration");
		newShell.setSize(800, 640);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		setTitle(name + " Configuration");
		setMessage("Configure how your chart layer should look like");

		ToolBar toolBar = new ToolBar(container, SWT.RIGHT);

		try {
			view = ViewUtils.constructView(viewId, container, toolBar);
			List<LayerSettings<Well,Well>> layerSettings = layer.getLayerSettings();
			// Store original LayerSettings for cancel.
			originalLayerSettings.addAll(layerSettings);
			if (!layerSettings.isEmpty()) {
				if (view instanceof IDecoratedPart) {
					IDecoratedPart decoratedView = (IDecoratedPart) view;
					SettingsDecorator decorator = decoratedView.hasDecorator(SettingsDecorator.class);
					PartSettings settings = decorator.getCurrentSettings().orElse(null);
					if (settings != null) {
						Properties properties = settings.getProperties();
						for (int i = 1;; i++) {
							String propertyName = String.valueOf(i);
							if (properties.getProperty(propertyName) == null) break;
							properties.removeProperty(propertyName);
						}
						int index = 1;
						for (LayerSettings<Well, Well> layerSetting : layerSettings) {
							try {
								String settingString = SerializationUtils.toString(layerSetting);
								properties.addProperty(index + "", settingString);
							} catch (IOException e) {
								// Do nothing.
							}
							index++;
						}
						decorator.loadPartSettings(Optional.of(settings));
					}
				}
			}
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		}

		return container;
	};

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.PROCEED_ID, APPLY_LABEL, false);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == IDialogConstants.PROCEED_ID) {
			applyPressed();
		}
	}

	@Override
	protected void okPressed() {
		applyLayerSettings();
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		if (!originalLayerSettings.isEmpty()) {
			layer.getLayerSettings().clear();
			layer.getLayerSettings().addAll(originalLayerSettings);
			GridState.saveValue(layer.getProtocolId(), layer.getId(), AbstractSubWellChartLayer.PROPERTY_CONFIG, layer.getLayerSettings());
			layer.triggerLoadDataJob();
		}
		super.cancelPressed();
	}

	protected void applyPressed() {
		applyLayerSettings();
	}

	@Override
	public boolean close() {
		// Properly dispose the View inside the Dialog.
		if (view != null) view.dispose();
		return super.close();
	}

	@Override
	public void applySettings(GridViewer viewer, IGridLayer layer) {
		this.layer.getRenderer().resetRendering();
		if (layer instanceof AbstractSubWellChartLayer) ((AbstractSubWellChartLayer) layer).triggerLoadDataJob();
		viewer.getGrid().redraw();
	}

	@SuppressWarnings("unchecked")
	private void applyLayerSettings() {
		IDecoratedPart decoratedView = (IDecoratedPart) view;
		SettingsDecorator decorator = decoratedView.hasDecorator(SettingsDecorator.class);
		PartSettings settings = decorator.getCurrentSettings().orElse(null);
		if (settings != null) {
			Properties properties = settings.getProperties();
			List<LayerSettings<Well, Well>> layerSettings = new ArrayList<>();
			for (int i = 1;; i++) {
				if (properties.getProperty(String.valueOf(i)) == null) break;
				try {
					String layerSetting = (String) properties.getProperty(i + "");
					layerSettings.add((LayerSettings<Well, Well>) SerializationUtils.fromString(layerSetting));
				} catch (ClassNotFoundException | IOException | RuntimeException e) {
					// Do nothing.
				}
			}
			layer.getLayerSettings().clear();
			layer.getLayerSettings().addAll(layerSettings);
		}
		GridState.saveValue(layer.getProtocolId(), layer.getId(), AbstractSubWellChartLayer.PROPERTY_CONFIG, layer.getLayerSettings());

		layer.triggerLoadDataJob();
	}

}
