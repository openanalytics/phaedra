package eu.openanalytics.phaedra.base.ui.volumerenderer.internal;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Rectangle;

public class PickHelper {

	public static int[] createPickMatrix(Rectangle rect, IntBuffer viewPort, boolean multi) {
		int[] output = new int[4];
		output[2] = multi ? rect.width : 5;
		output[3] = multi ? rect.height : 5;
		// Calc the center of the rectangle.
		output[0] = rect.x + rect.width/2;
		int yZeroOnTop = rect.y + rect.height/2;
		output[1] = viewPort.get(3) - yZeroOnTop;
		return output;
	}
	
	public static int[] pick(int hits, IntBuffer selectBuffer, boolean multi) {
		List<Integer> pickedNames = new ArrayList<Integer>();
		int ptr = 0;
		if (multi) {
			// Collect all hits with a valid name.
			for (int i=0;i<hits;i++) {
				int names = selectBuffer.get(ptr++);
				ptr++; // Mindepth value, ignore.
				ptr++; // Maxdepth value, ignore.
				int name = -1;
				for (int j=0;j<names;j++) {
					name = selectBuffer.get(ptr++);
				}
				if (name != -1) pickedNames.add(name);
			}
		} else {
			// Collect only the hit with the nearest depth value.
			int nearestHitName = -1;
			int nearestHitDepth = Integer.MAX_VALUE;
			for (int i=0;i<hits;i++) {
				int names = selectBuffer.get(ptr++);
				int minDepth = selectBuffer.get(ptr++);
				ptr++; // Maxdepth value, ignore.
				int name = -1;
				for (int j=0;j<names;j++) {
					name = selectBuffer.get(ptr++);
				}
				if (minDepth < nearestHitDepth) {
					nearestHitDepth = minDepth;
					nearestHitName = name;
				}
			}
			if (nearestHitName != -1) pickedNames.add(nearestHitName);
		}
		int[] output = new int[pickedNames.size()];
		for (int i=0; i<pickedNames.size(); i++) {
			output[i] = pickedNames.get(i);
		}
		return output;
	}
}
