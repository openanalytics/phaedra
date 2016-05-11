package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class AggregationMenuFactory {

	public static <ENTITY, ITEM> void createFor(final ToolBar toolBar, final AbstractChartLayer<ENTITY, ITEM> layer, final ValueObservable observable) {

		// The data provider and data calculator must be compatible.
		IDataProvider<ENTITY, ITEM> dataProvider = layer.getDataProvider();
		if (!(dataProvider instanceof BaseDataProvider<?, ?>))
			return;
		IDataCalculator<ENTITY, ITEM> dataCalculator = ((BaseDataProvider<ENTITY, ITEM>) dataProvider).getDataCalculator();
		if (!(dataCalculator instanceof AggregationDataCalculator<?, ?>))
			return;
		final AggregationDataCalculator<ENTITY, ITEM> aggCalculator = (AggregationDataCalculator<ENTITY, ITEM>) dataCalculator;

		final ToolItem menuButton = new ToolItem(toolBar, SWT.DROP_DOWN);
		menuButton.setImage(IconManager.getIconImage("aggregation.gif"));
		menuButton.setToolTipText("Apply data aggregation");

		final Menu menu = new Menu(toolBar.getShell(), SWT.POP_UP);

		MenuItem methodSubMenuItem = new MenuItem(menu, SWT.CASCADE);
		methodSubMenuItem.setText("Aggregation Method");
		Menu methodSubMenu = new Menu(menu);
		methodSubMenuItem.setMenu(methodSubMenu);

		String[] aggMethods = AggregationDataCalculator.AGGREGATION_METHODS;
		for (String aggMethod : aggMethods) {
			MenuItem item = new MenuItem(methodSubMenu, SWT.RADIO);
			item.setText(aggMethod);

			if (aggMethod.equals(aggCalculator.getAggregationMethod()))
				item.setSelection(true);

			item.addListener(SWT.Selection, e -> {
				String aggregationMethod = ((MenuItem) e.widget).getText();
				aggCalculator.setAggregationMethod(aggregationMethod);
				layer.dataChanged();
				if (observable != null) observable.valueChanged(layer);
			});
		}

		MenuItem featureSubMenuItem = new MenuItem(menu, SWT.CASCADE);
		featureSubMenuItem.setText("Aggregation Feature");
		Menu featureSubMenu = new Menu(menu);
		featureSubMenuItem.setMenu(featureSubMenu);

		List<String> features = layer.getDataProvider().getFeatures();
		if (features != null) {
			String[] aggFeatures = new String[features.size() + 1];
			aggFeatures[0] = AggregationDataCalculator.NONE;
			for (int i = 0; i < features.size(); i++)
				aggFeatures[i + 1] = features.get(i);
			for (String feature : aggFeatures) {
				MenuItem item = new MenuItem(featureSubMenu, SWT.RADIO);
				item.setText(feature);

				if (feature.equals(aggCalculator.getAggregationFeature()))
					item.setSelection(true);

				item.addListener(SWT.Selection, e -> {
					String aggregationFeature = ((MenuItem) e.widget).getText();
					aggCalculator.setAggregationFeature(aggregationFeature);
					layer.dataChanged();
					if (observable != null) observable.valueChanged(layer);
				});
			}
		}

		menuButton.addListener(SWT.Selection, e -> {
			if (e.detail == SWT.ARROW) {
				Rectangle rect = menuButton.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = toolBar.toDisplay(pt);
				menu.setLocation(pt.x, pt.y);
				menu.setVisible(true);
			}
		});
		menuButton.addListener(SWT.Dispose, e -> {
			if (menu != null && !menu.isDisposed()) {
				menu.dispose();
			}
		});
	}
}
