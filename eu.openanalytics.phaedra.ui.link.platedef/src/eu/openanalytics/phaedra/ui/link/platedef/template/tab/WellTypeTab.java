package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class WellTypeTab extends BaseTemplateTab {

	private Combo wellTypeCombo;
	
	@Override
	public String getName() {
		return "Well Type";
	}

	@Override
	public IGridCellRenderer createCellRenderer() {
		return new WellTypeCellRenderer();
	}

	@Override
	public String getValue(WellTemplate well) {
		return well.getWellType();
	}
	
	@Override
	public boolean applyValue(WellTemplate well, String value) {
		well.setWellType(value);
		if (ProtocolUtils.isControl(value)) {
			well.setCompoundType(null);
			well.setCompoundNumber(null);
			well.setConcentration(null);
		}
		return true;
	}

	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(comp);
		
		new Label(comp, SWT.NONE).setText("Well type:");
		
		wellTypeCombo = new Combo(comp, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).applyTo(wellTypeCombo);
		for (WellType type: ProtocolService.getInstance().getWellTypes()) {
			wellTypeCombo.add(type.getCode());
		}
		wellTypeCombo.select(0);

		Button applyBtn = new Button(comp, SWT.PUSH);
		applyBtn.setText("Apply");
		applyBtn.setImage(IconManager.getIconImage("tick.png"));
		applyBtn.setToolTipText("Apply this control to the selected well(s)");
		applyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<WellTemplate> currentSelection = selectionSupplier.get();
				if (currentSelection == null || currentSelection.isEmpty()) return;
				String wellType = wellTypeCombo.getItem(wellTypeCombo.getSelectionIndex());
				for (WellTemplate well: currentSelection) applyValue(well, wellType);
				templateRefresher.run();
			}
		});
		GridDataFactory.fillDefaults().hint(65, 21).applyTo(applyBtn);
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		WellTemplate sample = newSelection.get(0);
		String wellType = newSelection.stream().allMatch(w -> Objects.equals(sample.getWellType(), w.getWellType())) ? sample.getWellType() : "";
		wellTypeCombo.select(wellTypeCombo.indexOf(wellType));
	}
	
	public static class WellTypeCellRenderer extends BaseTemplateCellRenderer {
		@Override
		protected String[] doGetLabels(WellTemplate well) {
			return new String[] { well.getWellType() };
		}
	}
}
