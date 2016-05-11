package eu.openanalytics.phaedra.wellimage.component;

import java.util.Arrays;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

public class ComponentBlender {

	private ImageChannel[] channels;
	
	public ComponentBlender(ImageChannel[] channels) {
		this.channels = channels;
	}
	
	public ImageData blend(ImageData[] components) {

		if (components == null || components.length == 0) return null;
		
		// Start with an opaque, black image.
		ImageData firstComponent = components[0];
		if (firstComponent == null) return null;
		PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
		ImageData output = new ImageData(firstComponent.width, firstComponent.height, 24, palette);
		output.alphaData = new byte[firstComponent.width * firstComponent.height];
		Arrays.fill(output.alphaData, (byte)255);
		
		for (int i=0; i<components.length; i++) {
			IComponentType type = ComponentTypeFactory.getInstance().getComponent(channels[i]);
			
			int[] params = new int[] {
					channels[i].getColorMask(),
					channels[i].getLookupLow(),
					channels[i].getLookupHigh(),
					channels[i].getLevelMin(),
					channels[i].getLevelMax(),
					channels[i].getAlpha()
			};
			
			if (type != null) {
				type.blend(components[i], output, params);
			}
		}
		
		return output;
	}
}
