package eu.openanalytics.phaedra.base.ui.admin.fs.editor;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.environment.Screening;

/**
 * Editor input that stores a path.
 */
public class FSPathEditorInput implements IPathEditorInput {
	
	private IPath fPath;
	
	public FSPathEditorInput(IPath path) {
		if (path == null) throw new IllegalArgumentException();
		this.fPath = path;
	}
	
	/*
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		try {
			return Screening.getEnvironment().getFileServer().exists(fPath.toString());
		} catch (IOException e) {
			return false;
		}
	}
	
	/*
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(fPath.toString());
	}
	
	/*
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return fPath.lastSegment();
	}
	
	/*
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return fPath.toString();
	}
	
	/*
	 * @see org.eclipse.ui.IPathEditorInput#getPath()
	 */
	public IPath getPath() {
		return fPath;
	}

	/*
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
    public int hashCode() {
		return fPath.hashCode();
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
    public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FSPathEditorInput))
			return false;
		FSPathEditorInput other = (FSPathEditorInput) obj;
		return fPath.equals(other.fPath);
	}
}
