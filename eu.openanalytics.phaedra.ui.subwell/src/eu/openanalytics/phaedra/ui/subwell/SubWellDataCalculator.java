package eu.openanalytics.phaedra.ui.subwell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class SubWellDataCalculator {

	public static final String[] SIGNAL_PLOTS = new String[] {"acf_ALL", "acf_PRE", "acf_POST", "acf_FIRST", "acf_SECOND"};

	private List<Well> currentWells;

	private float scale;
	private boolean[] channels;

	public SubWellDataCalculator() {
		this.scale = 1f;
		this.currentWells = new ArrayList<>();
	}

	public Well getCurrentWell() {
		return currentWells.isEmpty() ? null : currentWells.get(0);
	}

	public void setCurrentWell(Well currentWell) {
		setCurrentWells(Arrays.asList(currentWell));
	}

	public List<Well> getCurrentWells() {
		return currentWells;
	}

	public void setCurrentWells(List<Well> currentWells) {
		this.currentWells = currentWells;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public boolean[] getChannels() {
		return channels;
	}

	public void setChannels(boolean[] channels) {
		this.channels = channels;
	}

	public ImageData createSignalPlot(Well well, SubWellFeature feature, int cellNr) {
		//TODO
//		float[][] data = SubWellService.getInstance().getNumericData2D(well, feature);
		float[][] data = new float[0][0];
		if (data == null) return null;
		if (cellNr >= data.length) return null;
		float[] tpData = data[cellNr];
		float min = 5000;
		float max = 0;
		for (float d: tpData) {
			if (d > max) max = d;
			if (d < min) min = d;
		}

		int w = 100;
		int h = 48;
		Image img = new Image(null, w, h);
		GC gc = new GC(img);
		try {
			int x = 0;
			int y = h;
			float yFactor = h/(max-min);
			for (int i=0; i<tpData.length; i++) {
				int newX = (int)(i*(((float)w)/tpData.length));
				int newY = h - (int)((tpData[i]-min)*yFactor);
				gc.drawLine(x, y-1, newX, newY-1);
				x = newX;
				y = newY;
			}
			return img.getImageData();
		} finally {
			gc.dispose();
			img.dispose();
		}
	}

}