package eu.openanalytics.phaedra.link.platedef.source;

import java.util.List;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.link.platedef.Activator;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettingsDialog;
import eu.openanalytics.phaedra.model.plate.vo.Plate;


public interface IPlateDefinitionSource extends IExecutableExtension {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".plateDefinitionSource";
	
	/**
	 * Get the id of the extension that provided this link source.
	 * 
	 * @return The link source id.
	 */
	public String getId();
	
	/**
	 * If this source requires additional configuration before a plate
	 * can be linked, this method should return true.
	 * It also implies that the method createSettingsDialog() can be
	 * used to let the user provide this configuration via a dialog.
	 * 
	 * @return True if additional settings apply to this source.
	 */
	public boolean requiresSettings();
	
	/**
	 * Get the default settings for this link source. The defaults
	 * may depend on the provided Plate (or its protocol class).
	 * 
	 * This method is used in a non-GUI link operation, where the 
	 * user is not asked for configuration via createSettingsDialog(),
	 * and defaults have to be used instead.
	 * 
	 * @param plate The Plate for which the settings will be used.
	 * @return A default settings object. It is not required to set the Plate.
	 */
	public PlateLinkSettings getDefaultSettings(Plate plate);
	
	/**
	 * Create an instance of a dialog that allows the user to provide
	 * link configuration.
	 * 
	 * @param parentShell The parent shell hosting the dialog.
	 * @param plates The selected plates, if any (may be empty or null).
	 * @return A new dialog, ready for use.
	 */
	public PlateLinkSettingsDialog createSettingsDialog(Shell parentShell, List<Plate> plates);
	
	/**
	 * Test whether this source is currently available.
	 * 
	 * @return True if the source is available.
	 */
	public boolean isAvailable();
	
	/**
	 * Test the link operation using the current settings.
	 * The link is tested but the results are discarded, instead of being
	 * saved into the application database.
	 * 
	 * @param settings The link settings.
	 * @return The link status outcome.
	 * @throws PlateLinkException If the link fails for an unexpected reason.
	 */
	public String test(PlateLinkSettings settings) throws PlateLinkException;
	
	/**
	 * Perform the link operation using the current settings.
	 * If the link succeeds, the retrieved information is stored
	 * in the application database.
	 * 
	 * @param settings The link settings.
	 * @return The link status outcome.
	 * @throws PlateLinkException If the link fails for an unexpected reason.
	 */
	public String link(PlateLinkSettings settings) throws PlateLinkException;
}
