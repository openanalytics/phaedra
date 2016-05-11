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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;

public class CompoundTab extends BaseTemplateTab {

	private Text wellCompoundTypeText;
	private Text wellCompoundNumberText;
	
	@Override
	public String getName() {
		return "Compound";
	}

	@Override
	public IGridCellRenderer createCellRenderer() {
		return new CompoundCellRenderer();
	}

	@Override
	public String getValue(WellTemplate well) {
		return getValueLabel(well);
	}
	
	@Override
	public boolean applyValue(WellTemplate well, String value) {
		int splitIndex = value.indexOf(" ");
		if (splitIndex > 0) {
			well.setCompoundType(value.substring(0, splitIndex));
			well.setCompoundNumber(value.substring(splitIndex + 1));
		} else {
			well.setCompoundNumber(value);
		}
		return true;
	}
	
	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(comp);
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText("Compound Type:");

		wellCompoundTypeText = new Text(comp, SWT.BORDER);
		wellCompoundTypeText.setTextLimit(10);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(wellCompoundTypeText);

		Button applyBtn = new Button(comp, SWT.PUSH);
		applyBtn.setText("Apply");
		applyBtn.setImage(IconManager.getIconImage("tick.png"));
		applyBtn.setToolTipText("Apply this compound type to the selected well(s)");
		applyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<WellTemplate> currentSelection = selectionSupplier.get();
				if (currentSelection == null || currentSelection.isEmpty()) return;
				String compoundType = wellCompoundTypeText.getText();
				for (WellTemplate well: currentSelection) well.setCompoundType(compoundType);
				templateRefresher.run();
			}
		});
		GridDataFactory.fillDefaults().hint(65, 21).applyTo(applyBtn);

		lbl = new Label(comp, SWT.NONE);
		lbl.setText("Compound Number:");

		wellCompoundNumberText = new Text(comp, SWT.BORDER);
		wellCompoundNumberText.setTextLimit(25);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(wellCompoundNumberText);

		applyBtn = new Button(comp, SWT.PUSH);
		applyBtn.setText("Apply");
		applyBtn.setImage(IconManager.getIconImage("tick.png"));
		applyBtn.setToolTipText("Apply this compound number to the selected well(s)");
		applyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<WellTemplate> currentSelection = selectionSupplier.get();
				if (currentSelection == null || currentSelection.isEmpty()) return;
				String compoundNumber = wellCompoundNumberText.getText();
				for (WellTemplate well: currentSelection) well.setCompoundNumber(compoundNumber);
				templateRefresher.run();
			}
		});
		GridDataFactory.fillDefaults().hint(65, 21).applyTo(applyBtn);
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		WellTemplate sample = newSelection.get(0);
		String compType = newSelection.stream().allMatch(w -> Objects.equals(sample.getCompoundType(), w.getCompoundType()))
				? StringUtils.nonNull(sample.getCompoundType()) : "";
		String compNr = newSelection.stream().allMatch(w -> Objects.equals(sample.getCompoundNumber(), w.getCompoundNumber()))
				? StringUtils.nonNull(sample.getCompoundNumber()) : "";
				
		wellCompoundTypeText.setText(compType);
		wellCompoundNumberText.setText(compNr);
	}
	
	private static String getValueLabel(WellTemplate well) {
		String type = well.getCompoundType();
		String nr = well.getCompoundNumber();
		if (type == null && nr == null) return "";
		if (type == null) type = "???";
		if (nr == null) nr = "???";
		return type + " " + nr;
	}
	
	public static class CompoundCellRenderer extends BaseTemplateCellRenderer {
		@Override
		protected String[] doGetLabels(WellTemplate well) {
			return new String[] { getValueLabel(well) };
		}
	}
}
