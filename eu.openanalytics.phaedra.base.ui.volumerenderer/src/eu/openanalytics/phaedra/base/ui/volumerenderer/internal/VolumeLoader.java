package eu.openanalytics.phaedra.base.ui.volumerenderer.internal;

import org.eclipse.swt.graphics.ImageData;

public class VolumeLoader {
	
	private byte[] xyz;
	private int width,height,depth,channels;
	
	private DIRECTION currentDir = DIRECTION.XY;
	
	public static enum DIRECTION {
		XY,YZ,XZ
	};
	
	public void load(ImageData[][] image) {
		if (image == null || image.length == 0) {
			throw new RuntimeException("No image provided");
		}
		
		ImageData[] rawImage = image[0];
		ImageData[] overlay = image[1];
		
		width = rawImage[0].width;
		height = rawImage[0].height;
		depth = rawImage.length;
		channels = 4; // Internally we use RGBA

		xyz = new byte[width*height*depth*channels];
		for (int z=0;z<depth;z++) {
			for (int y=0;y<height;y++) {
				for (int x=0;x<width;x++) {
					int index = z*width*height*channels + y*width*channels + x*channels;
					int pixel = rawImage[z].getPixel(x, y);
					int overlayPixel = overlay[z].getPixel(x, y);
					if (pixel > 10) {
						if (overlayPixel > 0) {
							xyz[index] = (byte)255;
							xyz[index+1] = (byte)0;
							xyz[index+2] = (byte)0;
							xyz[index+3] = (byte)254;
						} else {
							xyz[index] = (byte)(pixel >> 16);
							xyz[index+1] = (byte)((pixel >> 8) & 0xFF);
							xyz[index+2] = (byte)(pixel & 0xFF);
							xyz[index+3] = (byte)255;
						}
					} else {
						xyz[index+3] = (byte)0;	
					}
				}
			}
		}
	}
	
	public void setDirection(DIRECTION dir) {
		this.currentDir = dir;
	}
	
	public byte[] get2D(int index) {
		return get2D(currentDir, index);
	}
	
	public byte[] get2D(DIRECTION dir, int index) {
		byte[] slice = null;
		
		switch (dir) {
		case XY:
			// Just return the slice where z = index
			slice = new byte[width*height*channels];
			int offset = index*width*height*channels;
			System.arraycopy(xyz, offset, slice, 0, slice.length);
			break;
		case YZ:
			// Compose slice where x = index
			slice = new byte[depth*height*channels];
			int x = index;
			int i = 0;
			for (int z=0;z<depth;z++) {
				for (int y=0;y<height;y++) {
					int pos = z*width*height*channels + y*width*channels + x*channels;
					slice[i] = xyz[pos];
					slice[i+1] = xyz[pos+1];
					slice[i+2] = xyz[pos+2];
					slice[i+3] = xyz[pos+3];
					i+=4;
				}
			}
			break;
		case XZ:
			// Compose slice where y = index
			slice = new byte[width*depth*channels];
			int y = index;
			i = 0;
			for (int z=0;z<depth;z++) {
				for (x=0;x<width;x++) {
					int pos = z*width*height*channels + y*width*channels + x*channels;
					slice[i] = xyz[pos];
					slice[i+1] = xyz[pos+1];
					slice[i+2] = xyz[pos+2];
					slice[i+3] = xyz[pos+3];
					i+=4;
				}
			}
		}
		return slice;
	}

	public int getSize() {
		return width*height*depth*channels;
	}
	
	public int getWidth() {
		switch (currentDir) {
		case XY: return width;
		case YZ: return height;
		case XZ: return width;
		}
		return width;
	}

	public int getHeight() {
		switch (currentDir) {
		case XY: return height;
		case YZ: return depth;
		case XZ: return depth;
		}
		return height;
	}

	public int getDepth() {
		switch (currentDir) {
		case XY: return depth;
		case YZ: return width;
		case XZ: return height;
		}
		return depth;
	}
}
