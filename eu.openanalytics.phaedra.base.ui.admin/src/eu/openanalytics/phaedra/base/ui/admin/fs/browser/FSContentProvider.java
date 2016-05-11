package eu.openanalytics.phaedra.base.ui.admin.fs.browser;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class FSContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {
		// Nothing to dispose.
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Do nothing.
	}

	@Override
	public Object[] getElements(Object inputElement) {
		File file = (File)inputElement;
		File[] children = file.listFiles();
		if (children == null) return new Object[0];
		Arrays.sort(children, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && !o2.isDirectory()) return -1;
				if (o2.isDirectory() && !o1.isDirectory()) return 1;
				return o1.getName().compareTo(o2.getName());
			}
		});
		return children;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		File file = (File)element;
		return file.getParentFile();
	}

	@Override
	public boolean hasChildren(Object element) {
		File file = (File)element;
		return file.isDirectory();
	}

}
