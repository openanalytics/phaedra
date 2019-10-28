package eu.openanalytics.phaedra.base.ui.search;

import static java.util.Objects.requireNonNull;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
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
	
	private final ConcentrationDataDescription dataDescription;
	
	
	/**
	 * @param panel the panel to control.
	 * @param unit the unit of the model value.
	 */
	public ConcentrationValueQueryValuePanelUnitExtension(final AbstractRealQueryValuePanelFactory.Panel panel,
			final ConcentrationDataDescription dataDescription) {
		this.panel = requireNonNull(panel);
		this.dataDescription = requireNonNull(dataDescription);
		
		onConfigChanged();
	}
	
	
	@Override
	protected void onConfigChanged() {
		final DataFormatter dataFormatter = get();
		final ConcentrationUnit uiUnit = dataFormatter.getConcentrationUnit(this.dataDescription);
		this.panel.setConverter(
				new ConcentrationValueFormatConverter(
						this.dataDescription.getConcentrationUnit(),
						dataFormatter.getConcentrationEditFormat(this.dataDescription) ),
				(uiUnit != this.dataDescription.getConcentrationUnit()) ?
						new ConcentrationValueConverter(uiUnit, this.dataDescription.getConcentrationUnit()) :
						null );
	}
	
}
