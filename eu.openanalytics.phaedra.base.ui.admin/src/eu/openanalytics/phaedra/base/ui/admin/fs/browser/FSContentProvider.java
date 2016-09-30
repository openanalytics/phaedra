package eu.openanalytics.phaedra.base.ui.admin.fs.browser;

import java.io.IOException;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.ui.admin.Activator;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

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
		String path = inputElement.toString();
		if (isDirectory(path)) {
			try {
				return Screening.getEnvironment().getFileServer().dir(path).stream()
						.map(c -> path + (path.endsWith("/") ? "" : "/") + c)
						.sorted((c1, c2) -> {
							if (isDirectory(c1) && !isDirectory(c2)) return -1;
							if (isDirectory(c2) && !isDirectory(c1)) return 1;
							return getName(c1).compareTo(getName(c2));
						}).toArray(i -> new String[i]);
			} catch (IOException e) {
				EclipseLog.error("Failed to list contents of " + path, e, Activator.getDefault());
			}
		}
		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		return FileUtils.getPath(element.toString());
	}

	@Override
	public boolean hasChildren(Object element) {
		return isDirectory(element.toString());
	}

	public static boolean isDirectory(String path) {
		try {
			return Screening.getEnvironment().getFileServer().isDirectory(path);
		} catch (IOException e) {
			return false;
		}
	}
	
	public static String getName(String path) {
		return FileUtils.getName(path);
	}
}
