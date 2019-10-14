package eu.openanalytics.phaedra.base.ui.search;

import static java.util.Objects.requireNonNull;

import eu.openanalytics.phaedra.base.datatype.format.ConcentrationValueFormatConverter;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationValueConverter;
import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;


/**
 * Enables conversion and formatting for concentration values.
 */
public class ConcentrationValueQueryValuePanelUnitExtension extends DataFormatSupport implements AbstractRealQueryValuePanelFactory.PanelExtension {
	
	
	private final AbstractRealQueryValuePanelFactory.Panel panel;
	
	private final ConcentrationUnit modelUnit;
	
	
	/**
	 * @param panel the panel to control.
	 * @param unit the unit of the model value.
	 */
	public ConcentrationValueQueryValuePanelUnitExtension(final AbstractRealQueryValuePanelFactory.Panel panel,
			final ConcentrationUnit unit) {
		this.panel = requireNonNull(panel);
		this.modelUnit = requireNonNull(unit);
		
		onConfigChanged();
	}
	
	
	@Override
	protected void onConfigChanged() {
		final DataFormatter dataFormatter = get();
		this.panel.setConverter(
				new ConcentrationValueFormatConverter(this.modelUnit, dataFormatter.getConcentrationEditFormat()),
				(dataFormatter.getConcentrationUnit() != this.modelUnit) ?
						new ConcentrationValueConverter(dataFormatter.getConcentrationUnit(), this.modelUnit) :
						null );
	}
	
}
