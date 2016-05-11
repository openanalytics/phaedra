package eu.openanalytics.phaedra.base.imaging.jp2k.comp;

import java.util.Map;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;

/**
 * Represents a type of image component (in JPEG2000 terminology,
 * a component is a "channel" or "layer" of an image).
 */

// params[0] = color mask
// params[1] = lookup low
// params[2] = lookup high
// params[3] = level min
// params[4] = level max
// params[5] = alpha

public interface IComponentType {

	public String getName();
	
	public int getId();
	
	public String getDescription();

	public void loadConfig(Map<String,String> config);
	public void saveConfig(Map<String,String> config);
	
	public void blend(ImageData source, ImageData target, int... params);

	public Image createIcon(Device device);
	
	public void createConfigArea(Composite parent, Map<String,String> config, ISelectionChangedListener changeListener);
}
