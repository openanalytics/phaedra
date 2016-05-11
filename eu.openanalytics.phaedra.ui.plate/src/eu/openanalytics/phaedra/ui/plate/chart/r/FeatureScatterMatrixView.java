package eu.openanalytics.phaedra.ui.plate.chart.r;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.charting.r.FeatureScatterMatrixPlotter;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.plate.chart.r.data.FeatureScatterMatrixDataProvider;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class FeatureScatterMatrixView extends BaseExperimentChartRView {

	private List<Feature> currentFeatures;
	private ToolItem featuresDropdown;

	@Override
	public void createPartControl(Composite parent) {
		plotLbl = new Label(parent, SWT.BORDER);
		currentFeatures = new ArrayList<>();

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				createButtons(parent);
			}
		};
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(contributionItem);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		super.createPartControl(parent);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewScatterMatrix");
	}

	@Override
	protected void createPlot() {
		if (plotLbl.isDisposed()) return;
		if (currentSelection == null) return;
		try {
			Point size = plotLbl.getSize();
			Image plot = FeatureScatterMatrixPlotter.createPlot(getDataProvider(), size.x, size.y);
			if (plotLbl.getImage() != null && !plotLbl.getImage().isDisposed()) {
				plotLbl.getImage().dispose();
			}
			plotLbl.setImage(plot);
		} catch (Exception e) {
			plotLbl.setImage(null);
			plotLbl.setText(e.getMessage());
		}
	}

	@Override
	protected void featureSelection() {
		fillParameterMenus();
	}

	private FeatureScatterMatrixDataProvider getDataProvider() {
		return new FeatureScatterMatrixDataProvider(PlateService.getInstance().getPlates(currentSelection), currentFeatures);
	}

	private void createButtons(ToolBar parent) {
		featuresDropdown = DropdownToolItemFactory.createDropdown(parent);
		featuresDropdown.setImage(IconManager.getIconImage("chart_X.png"));
		featuresDropdown.setToolTipText("Included Features");
	}

	private void fillParameterMenus() {
		DropdownToolItemFactory.clearChildren(featuresDropdown);

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				MenuItem selected = (MenuItem) event.widget;
				if (selected.getSelection())
					currentFeatures.add(getFeatureByName(selected.getText()));
				else
					for (int i = 0; i < currentFeatures.size(); i++) {
						if (currentFeatures.get(i).getName().equals(selected.getText())) {
							currentFeatures.remove(i);
							break;
						}
					}
				createPlot();
			}
		};

		for (Feature f : getPossibleFeatures()) {
			MenuItem item = DropdownToolItemFactory.createChild(featuresDropdown, f.getName(), SWT.CHECK);
			if (item == null) continue;
			item.addSelectionListener(listener);
			if (currentFeatures.size() < 2) {
				item.setSelection(true);
				currentFeatures.add(f);
			} else {
				item.setSelection(containsFeature(f.getName()));
			}
		}
	}

	private boolean containsFeature(String name) {
		for (Feature f : currentFeatures)
			if (f.getName().equals(name)) return true;
		return false;
	}

	private Feature getFeatureByName(String featureName) {
		Feature feature = ProtocolUtils.getFeatureByName(featureName, currentSelection.getProtocol().getProtocolClass());
		if (feature == null) {
			for (Feature f : ProtocolUIService.getInstance().getCurrentFeature().getProtocolClass().getFeatures())
				if (f.getName().equals(featureName)) return f;
		}
		return feature;
	}

	private List<Feature> getPossibleFeatures() {
		synchronized (this) {
			if (PlateService.getInstance().getPlates(currentSelection) != null &&
					!PlateService.getInstance().getPlates(currentSelection).isEmpty()) {
				return CollectionUtils.findAll(
						PlateUtils.getFeatures(
								PlateService.getInstance().getPlates(currentSelection).get(0)), ProtocolUtils.NUMERIC_FEATURES);
			}
			return new ArrayList<>();
		}
	}

	private Protocol getProtocol() {
		if (currentSelection == null) return null;
		return currentSelection.getProtocol();
	}

	private Properties getProperties() {
		Properties properties = new Properties();
		PartSettingsUtils.setFeatures(properties, currentFeatures);
		return properties;
	}

	private void setProperties(Properties properties) {
		currentFeatures = PartSettingsUtils.getFeatures(properties);
		createPlot();
	}
}