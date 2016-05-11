package eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider;

import java.io.File;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class TreeSorter extends ViewerSorter {

	public int category(Object obj) {
		return super.category(obj);
	}

	public int compare(Viewer viewer, Object obj1, Object obj2) {
		if ((obj1 instanceof File) && (obj2 instanceof File)) {
			File f1 = (File) obj1;
			File f2 = (File) obj2;
			if (f1.isDirectory() && !f2.isDirectory()) return -1;
			if (!f1.isDirectory() && f2.isDirectory()) return 1;
			return f1.compareTo(f2);
		}
		return super.compare(viewer, obj1, obj2);
	}
}