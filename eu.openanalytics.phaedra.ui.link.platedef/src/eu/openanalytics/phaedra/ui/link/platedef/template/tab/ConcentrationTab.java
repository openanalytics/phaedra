package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;

public class ConcentrationTab extends BaseTemplateTab {

	private Text wellConcentrationText;
	
	@Override
	public String getName() {
		return "Concentration";
	}

	@Override
	public IGridCellRenderer createCellRenderer() {
		return new ConcentrationCellRenderer();
	}

	@Override
	public String getValue(WellTemplate well) {
		return well.getConcentration() == null ? "" : well.getConcentration();
	}
	
	@Override
	public boolean applyValue(WellTemplate well, String value) {
		if (NumberUtils.isNumeric(value)) {
			well.setConcentration(value);
			return true;
		}
		return false;
	}
	
	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier, Runnable templateRefresher) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(comp);
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText("Concentration:");

		wellConcentrationText = new Text(comp, SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(wellConcentrationText);

		Button applyBtn = new Button(comp, SWT.PUSH);
		applyBtn.setText("Apply");
		applyBtn.setImage(IconManager.getIconImage("tick.png"));
		applyBtn.setToolTipText("Apply this concentration to the selected well(s)");
		applyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<WellTemplate> currentSelection = selectionSupplier.get();
				if (currentSelection == null || currentSelection.isEmpty()) return;
				String conc = wellConcentrationText.getText();
				if (!NumberUtils.isDouble(conc)) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Invalid concentration",
							"Invalid concentration: " + conc + "\nPlease specify the concentration in mol/L, for example: 1.5e-8");
					return;
				}
				for (WellTemplate well: currentSelection) applyValue(well, conc);
				templateRefresher.run();
			}
		});
		GridDataFactory.fillDefaults().hint(65, 21).applyTo(applyBtn);

		new Label(comp, SWT.NONE).setImage(IconManager.getIconImage("information.png"));

		lbl = new Label(comp, SWT.NONE);
		lbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		lbl.setText("In mol/L, for example: 1e-8");		
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		WellTemplate sample = newSelection.get(0);
		String conc = newSelection.stream().allMatch(w -> Objects.equals(sample.getConcentration(), w.getConcentration()))
				? StringUtils.nonNull(sample.getConcentration()) : "";
		wellConcentrationText.setText(conc);
	}
	
	public static class ConcentrationCellRenderer extends BaseTemplateCellRenderer {
		@Override
		protected String[] doGetLabels(WellTemplate well) {
			return new String[] { well.getConcentration() };
		}
	}
}
