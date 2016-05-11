package eu.openanalytics.phaedra.base.ui.colormethod;

import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

public interface IColorMethod {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".colorMethod";
	public final static String ATTR_ID = "id";
	public final static String ATTR_NAME = "name";
	public final static String ATTR_CLASS = "class";
	
	/**
	 * Return the unique id of this color method.
	 * 
	 * @return The color method's id.
	 */
	public String getId();
	
	/**
	 * Return the name of this color method.
	 * Should be unique, but not enforced.
	 * 
	 * @return The color method's name.
	 */
	public String getName();
	
	/**
	 * Configure the color method with a map of settings.
	 * Afterwards, the color method is ready to render legends
	 * and open dialogs.
	 * 
	 * If null is passed, the color method is configured with defaults.
	 * 
	 * @param settings The settings to apply to the color method.
	 */
	public void configure(Map<String,String> settings);
	
	/**
	 * Copy this color method's settings into the settings Map.
	 * This is usually called after a dialog has been closed (see
	 * createDialog).
	 * 
	 * @param settings The Map to copy the settings into.
	 */
	public void getConfiguration(Map<String,String> settings);
	
	/**
	 * Get a legend image for this color method.
	 * The method configure must have been called at this point, or an Exception will occur.
	 * 
	 * @param width The width of the legend image.
	 * @param height The height of the legend image.
	 * @param orientation The orientation of the legend image, SWT.HORIZONTAL or SWT.VERTICAL.
	 * @param labels True to display value labels on the legend (requires that initialize has been called).
	 * @param highlightValues An optional array of value to highlight on the legend.
	 * @return A legend Image, ready to be displayed.
	 */
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues);
	
	/**
	 * 
	 * Get a legend image for this color method.
	 * The method configure must have been called at this point, or an Exception will occur.
	 * 
	 * @param width The width of the legend image.
	 * @param height The height of the legend image.
	 * @param orientation The orientation of the legend image, SWT.HORIZONTAL or SWT.VERTICAL.
	 * @param labels True to display value labels on the legend (requires that initialize has been called).
	 * @param highlightValues An optional array of value to highlight on the legend.
	 * @param isWhiteBackground True for a white background (e.g. reporting).
	 * @return A legend Image, ready to be displayed.
	 */
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues, boolean isWhiteBackground);
	
	/**
	 * Create a Dialog that can adjust the color method settings.
	 * If OK is clicked, the adjusted settings are applied to the color method.
	 * 
	 * @param shell The parent Shell to open the Dialog in.
	 * @return A Dialog, ready to open.
	 */
	public BaseColorMethodDialog createDialog(Shell shell);
	
	/**
	 * Initialize the color method with a data set.
	 * The method configure must have been called by now.
	 * Afterwards, values can be converted to colors using the getColor method.
	 * 
	 * @param dataset The dataset to initialize with.
	 */
	public void initialize(IColorMethodData dataset);
	
	/**
	 * Translate a value to a color.
	 * The method initialize must have been called by now.
	 * 
	 * @param v The value to translate.
	 * @return The value's corresponding color.
	 */
	public RGB getColor(double v);
	
	/**
	 * Translate a value to a color.
	 * The method initialize must have been called by now.
	 * 
	 * @param v The value to translate.
	 * @return The value's corresponding color.
	 */
	public RGB getColor(String v);
}
