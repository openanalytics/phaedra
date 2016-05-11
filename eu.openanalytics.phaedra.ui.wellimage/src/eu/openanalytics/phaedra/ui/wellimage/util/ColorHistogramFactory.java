package eu.openanalytics.phaedra.ui.wellimage.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

public class ColorHistogramFactory {

	public static Image createHistogram(int w, int h, ImageData data, ImageChannel channel, float gamma, boolean log) {

		if (data == null) return null;

		Image image = new Image(null, w, h);
		GC gc = new GC(image);
		gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, w, h);

		int[] histogramBuf = new int[256];
		float highest = 0;
		for (int i = 0; i < data.data.length; i += 3) {
			int val =  data.data[i]; //only one byte needed since it's all greyscale
			if (val < 0 ){
				int tmp = val;
				val = tmp & 0x80;
				val += tmp & 0x7F;
			}
			histogramBuf[val]++;
		}

		boolean isLookupChannel = channel.getType() == ImageChannel.CHANNEL_TYPE_LOOKUP;
		boolean isRawChannel = channel.getType() == ImageChannel.CHANNEL_TYPE_RAW;
		
		int beginCount;
		if (isLookupChannel) {
			//the value 0 will be highest on the lookup type, but is not needed
			beginCount = 1;
		} else {
			beginCount = 0;
		}

		for (int i = beginCount; i < histogramBuf.length; i++) {
			if (histogramBuf[i] > highest) {
				highest = histogramBuf[i];
			}
		}
		highest = highest / (data.data.length/3) * 256;

		//old color
		//gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));

		//scale histogrambuffer
		for (int i = 0; i < histogramBuf.length; i++) {
			histogramBuf[i] = (int)((float)histogramBuf[i] / (data.data.length/3) * 256 * (256/highest));
		}

		ColorStore colorStore = new ColorStore();
		
		for (int i = 0; i < histogramBuf.length; i++) {
			int scaledTo256 = histogramBuf[i];

			if (scaledTo256 == 0)
				//no value to plot: skip!
				continue;

			//new color system
			int r, g, b, iLevel, diff;

			if (i < channel.getLevelMin()) {
				//this value falls out of the range on the left side

				if (isLookupChannel) {
					//these values get the low lookup color
					r = channel.getLookupLow() >> 16;
					g = (channel.getLookupLow() >> 8) & 0xFF;
					b = channel.getLookupLow() & 0xFF;

				} else if (channel.getSequence() == 0){
					//these values become black
					r = 0;
					g = 0;
					b = 0;

				} else {
					//these values disappear
					gc.setLineDash(new int[]{2,1});
					r = 128;
					g = 128;
					b = 128;
				}

			} else {
				//regular paint
				gc.setLineDash(null);

				diff = channel.getLevelMax() - channel.getLevelMin();
				if (diff == 0) diff = 1;
				iLevel = (i - channel.getLevelMin())* 256 / diff;
				if (iLevel > 255) iLevel = 255;
				if (iLevel < 0) iLevel = 0;

				if (isRawChannel) {
					r = (channel.getColorMask() >> 16) * iLevel / 256;
					g = ((channel.getColorMask() >> 8) & 0xFF) * iLevel / 256;
					b = (channel.getColorMask() & 0xFF) * iLevel / 256;
				} else {
					//lookup type with 2 colors
					r = ((channel.getLookupLow() >> 16) * (255 - iLevel) +
							(channel.getLookupHigh() >> 16) * (iLevel))
							/ 256;
					g = (((channel.getLookupLow() >> 8) & 0xFF) * (255 - iLevel) +
							((channel.getLookupHigh() >> 8) & 0xFF) * (iLevel))
							/ 256;
					b = ((channel.getLookupLow() & 0xFF) * (255 - iLevel) +
							(channel.getLookupHigh() & 0xFF) * (iLevel))
							/ 256;
				}
			}

			if (gamma != 1.0 && i > channel.getLevelMin()){
				//only apply gamma when needed
				r = (int)(Math.pow(r/256.0, 1.0/gamma) * 256);
				g = (int)(Math.pow(g/256.0, 1.0/gamma) * 256);
				b = (int)(Math.pow(b/256.0, 1.0/gamma) * 256);
			}
			gc.setForeground(colorStore.get(new RGB(r, g, b)));

			if (!log) {
				gc.drawLine(translate(i, w), translate(256, h), translate(i, w),  translate(256 - scaledTo256, h));

				if (i != 0){
					//put a line on top
					gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
					gc.drawLine(translate(i-1, w), translate(256-histogramBuf[i-1], h), translate(i, w), translate(255 - scaledTo256, h));
				}
			} else {
				//we do the log value temporary +1 and later -1 one, since log(1) == 0
				//the 106.301699 constant is needed to bring all values back to the range 0 -> 255
				int logValue = 0;
				if (scaledTo256 != 0)
					logValue = (int)(Math.log(scaledTo256+1)/Math.log(10) * 106.301699) - 1;
				gc.drawLine(translate(i, w), translate(256, h), translate(i, w), translate(256 - logValue, h));

				if (i != 0){
					//put a line on top
					int secondVal = 0;
					if (histogramBuf[i-1] != 0)
						secondVal = (int)(Math.log(histogramBuf[i-1]+1)
								/Math.log(10) * 106.301699) - 1;

					gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
					gc.drawLine(translate(i-1, w), translate(256 - secondVal, h), translate(i, w), translate(255 - logValue, h));
				}
			}

			histogramBuf[i] = scaledTo256;
		}
		
		colorStore.dispose();
		gc.dispose();
		
		return image;
	}
	
	public static int translate(int value, int maxValue) {
		return (int) ((value / 256f) * maxValue);
	}
	
}