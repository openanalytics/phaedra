package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public class OverviewTab extends BaseTemplateTab {

	private Button skipBtn;
	
	@Override
	public String getName() {
		return "Overview";
	}

	@Override
	public IGridCellRenderer createCellRenderer() {
		return new OverviewCellRenderer();
	}

	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(comp);
		
		skipBtn = new Button(comp, SWT.CHECK);
		skipBtn.setText("Skip Wells");
		skipBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<WellTemplate> currentSelection = selectionSupplier.get();
				if (currentSelection == null || currentSelection.isEmpty()) return;
				boolean skip = skipBtn.getSelection();
				for (WellTemplate well: currentSelection) well.setSkip(skip);
				templateRefresher.run();
			}
		});
		GridDataFactory.fillDefaults().applyTo(skipBtn);
		
		new Label(comp, SWT.NONE).setImage(IconManager.getIconImage("information.png"));

		Label lbl = new Label(comp, SWT.NONE);
		lbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		lbl.setText("These wells will be skipped when applying the template to a plate.");
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		WellTemplate sample = newSelection.get(0);
		boolean sameSkip = newSelection.stream().allMatch(w -> sample.isSkip() == w.isSkip()) ? sample.isSkip() : false;
		skipBtn.setSelection(sameSkip);
	}
	
	@Override
	public String getValue(WellTemplate well) {
		return getValueLabels(well)[0];
	}
	
	private static String[] getValueLabels(WellTemplate well) {
		if (ProtocolUtils.isControl(well.getWellType())) {
			// PHA-644
			return new String[] { ProtocolUtils.getCustomHCLCLabel(well.getWellType()) };
		} else {
			String type = well.getCompoundType();
			String nr = well.getCompoundNumber();
			if (type == null && nr == null) return new String[] { "" };
			if (type == null) type = "???";
			if (nr == null) nr = "???";
			String label1 = type + " " + nr;
			
			if (well.getConcentration() == null) return new String[] { label1 };
			else return new String[] { label1, well.getConcentration() };
		}
	}
	
	public static class OverviewCellRenderer extends BaseTemplateCellRenderer {
		@Override
		protected String[] doGetLabels(WellTemplate well) {
			return getValueLabels(well);
		}
	}

}
