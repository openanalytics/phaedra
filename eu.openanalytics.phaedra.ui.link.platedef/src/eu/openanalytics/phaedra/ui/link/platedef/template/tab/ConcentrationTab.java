package eu.openanalytics.phaedra.ui.link.platedef.template.tab;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.Molar;

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

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.format.ConcentrationFormat;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationValueConverter;
import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;

public class ConcentrationTab extends BaseTemplateTab {
	
	
	private final DataFormatSupport dataFormatSupport;
	private ConcentrationFormat uiConcentrationFormat;
	private ConcentrationFormat uiEditConcentrationFormat;
	private ConcentrationValueConverter uiToModelConverter;
	
	private Label informationControl;
	private Text wellConcentrationText;
	
	
	public ConcentrationTab(final DataFormatSupport dataFormatSupport) {
		this.dataFormatSupport = dataFormatSupport;
		this.dataFormatSupport.addListener(this::updateFormatting);
		updateFormatting();
	}
	
	
	@Override
	public String getName() {
		return "Concentration";
	}
	
	
	private class ConcentrationCellRenderer extends BaseTemplateCellRenderer {
		
		@Override
		protected String[] doGetLabels(WellTemplate well) {
			String s = well.getConcentration();
			if (s != null) {
				final double value = Double.parseDouble(s);
				s = uiConcentrationFormat.format(value, Molar);
			}
			return new String[] { s };
		}
	}
	
	@Override
	public IGridCellRenderer createCellRenderer() {
		return new ConcentrationCellRenderer();
	}

	@Override
	public String getValue(WellTemplate well) {
		String s = well.getConcentration();
		if (s != null) {
			double value = Double.parseDouble(s);
			return this.uiEditConcentrationFormat.format(value, Molar);
		}
		return "";
	}
	
	@Override
	public boolean applyValue(WellTemplate well, String input) {
		if (input.isEmpty()) {
			well.setConcentration(null);
			return true;
		}
		if (NumberUtils.isNumeric(input)) {
			Double value = Double.valueOf(input);
			if (this.uiToModelConverter != null) {
				value = (Double)this.uiToModelConverter.convert(value);
			}
			well.setConcentration(value.toString());
			return true;
		}
		return false;
	}
	
	
	@Override
	public void createEditingFields(Composite parent, PlateTemplate template, Supplier<List<WellTemplate>> selectionSupplier,
			Runnable templateRefresher) {
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
							String.format("Invalid concentration value: %1$s" + conc + "\n\n"
									+ "Please specify the concentration in %2$s, for example: %3$s.",
									conc,
									uiConcentrationFormat.getUnit().getLabel(true),
									uiConcentrationFormat.format(1e-6, Molar) ));
					return;
				}
				for (WellTemplate well: currentSelection) applyValue(well, conc);
				templateRefresher.run();
			}
		});
		GridDataFactory.fillDefaults().hint(65, 21).applyTo(applyBtn);

		new Label(comp, SWT.NONE).setImage(IconManager.getIconImage("information.png"));

		this.informationControl = new Label(comp, SWT.NONE);
		this.informationControl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		updateFormatting();
	}
	
	@Override
	public void selectionChanged(List<WellTemplate> newSelection) {
		WellTemplate sample = newSelection.get(0);
		String conc = (newSelection.stream().allMatch(w -> Objects.equals(sample.getConcentration(), w.getConcentration()))) ?
				getValue(sample) : "";
		wellConcentrationText.setText(conc);
	}
	
	private void updateFormatting() {
		final DataFormatter dataFormatter = this.dataFormatSupport.get();
		final ConcentrationDataDescription dataDescription = (ConcentrationDataDescription)WellProperty.Concentration.getDataDescription();
		this.uiConcentrationFormat = dataFormatter.getConcentrationFormat(dataDescription);
		this.uiEditConcentrationFormat = dataFormatter.getConcentrationEditFormat(dataDescription);
		
		this.uiToModelConverter = (this.uiConcentrationFormat.getUnit() != dataDescription.getConcentrationUnit()) ?
				new ConcentrationValueConverter(this.uiConcentrationFormat.getUnit(), dataDescription.getConcentrationUnit()) :
				null;
		
		if (this.wellConcentrationText == null || this.wellConcentrationText.isDisposed()) {
			return;
		}
		
		this.informationControl.setText(String.format("In %1$s, for example: %2$s.",
				this.uiConcentrationFormat.getUnit().getLabel(true),
				this.uiConcentrationFormat.format(1e-6, Molar) ));
	}
	
}
