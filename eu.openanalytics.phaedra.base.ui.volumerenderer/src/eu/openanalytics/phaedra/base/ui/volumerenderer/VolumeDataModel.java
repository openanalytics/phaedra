package eu.openanalytics.phaedra.base.ui.volumerenderer;

import java.io.IOException;
import java.util.List;

import loci.formats.IFormatReader;
import loci.formats.in.ICSReader;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import eu.openanalytics.phaedra.base.ui.volumerenderer.internal.IDataModel;
import eu.openanalytics.phaedra.base.ui.volumerenderer.internal.IDataModelRenderer;
import eu.openanalytics.phaedra.base.ui.volumerenderer.internal.VolumeRenderer;

public class VolumeDataModel implements IDataModel {

	private String imagePath;
	private List<VolumeDataItem> items;

	public VolumeDataModel(String imagePath, List<VolumeDataItem> items) {
		this.imagePath = imagePath;
		this.items = items;
	}

	public String getImagePath() {
		return imagePath;
	}

	public ImageData[][] getImage() {

		IFormatReader reader = null;
		try {
			reader = new ICSReader();
			reader.setId(imagePath);

			int slices = reader.getSizeZ();
			int width = reader.getSizeX();
			int height = reader.getSizeY();
			int bpp = reader.getBitsPerPixel();
			
			ImageData[] raw = new ImageData[slices];
			ImageData[] overlay = new ImageData[slices];
			
			for (int z=0; z<slices; z++) {
				byte[] img = reader.openBytes(z);
				
				PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
				raw[z] = new ImageData(width, height, bpp, palette);
				
				for (int i=0; i<width*height; i++) {
					int x = i%width;
					int y = i/width;
					int pixelValue = img[i*4+2] | img[i*4+2]<<8 | img[i*4+2]<<16;
					raw[z].setPixel(x, y, pixelValue);
				}
			}
			for (int z=0; z<slices; z++) {
				byte[] ol = reader.openBytes(z+slices);
				
				PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
				overlay[z] = new ImageData(width, height, bpp, palette);
				
				for (int i=0; i<width*height; i++) {
					int x = i%width;
					int y = i/width;
					int pixelValue = ol[i*4+2] | ol[i*4+2]<<8 | ol[i*4+2]<<16;
					overlay[z].setPixel(x, y, pixelValue);
				}
			}
			
			return new ImageData[][]{raw, overlay};
			
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
	}

	public List<VolumeDataItem> getItems() {
		return items;
	}

	@Override
	public IDataModelRenderer getRenderer() {
		return new VolumeRenderer(this);
	}

	@Override
	public void handleExternalSelection(ISelection sel) {
		// Do nothing. Subclasses can implement.
	}

	@Override
	public ISelection handleInternalSelection(int[] objectNames) {
		// Do nothing. Subclasses can implement.
		return null;
	}

}
