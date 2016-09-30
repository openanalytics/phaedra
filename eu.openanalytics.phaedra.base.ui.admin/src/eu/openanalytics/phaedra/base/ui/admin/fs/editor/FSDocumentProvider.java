package eu.openanalytics.phaedra.base.ui.admin.fs.editor;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.base.ui.admin.Activator;

public class FSDocumentProvider extends AbstractDocumentProvider {

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createDocument(java.lang.Object)
	 */
	@Override
    protected IDocument createDocument(Object element) throws CoreException {
		String fsPath = getFSPath(element);
		try {
			String contents = Screening.getEnvironment().getFileServer().getContentsAsString(fsPath);
			IDocument document = new Document();
			document.set(contents);
			setupDocument(document);
			return document;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to open file " + fsPath, e));
		}
	}

	/**
	 * Set up the document - default implementation does nothing.
	 * 
	 * @param document the new document
	 */
	protected void setupDocument(IDocument document) {
		// Do nothing.
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createAnnotationModel(java.lang.Object)
	 */
	@Override
    protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		return null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doSaveDocument(org.eclipse.core.runtime.IProgressMonitor, java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
	 */
	@Override
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
		//TODO Do a better security check: admins or protocol team members.
		SecurityService.getInstance().checkWithException(Roles.USER, null);
		String fsPath = getFSPath(element);
		try {
			Screening.getEnvironment().getFileServer().putContents(fsPath, document.get().getBytes());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to save file " + fsPath, e));
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getOperationRunner(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
    protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		return null;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#isModifiable(java.lang.Object)
	 */
	@Override
    public boolean isModifiable(Object element) {
		try {
			return Screening.getEnvironment().getFileServer().exists(getFSPath(element));
		} catch (IOException e) {
			return false;
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#isReadOnly(java.lang.Object)
	 */
	@Override
    public boolean isReadOnly(Object element) {
		return !isModifiable(element);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#isStateValidated(java.lang.Object)
	 */
	@Override
    public boolean isStateValidated(Object element) {
		return true;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#isDeleted(java.lang.Object)
	 */
	@Override
    public boolean isDeleted(Object element) {
		try {
			return !Screening.getEnvironment().getFileServer().exists(getFSPath(element));
		} catch (IOException e) {
			return true;
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getModificationStamp(java.lang.Object)
	 */
	@Override
    public long getModificationStamp(Object element) {
		try {
			return Screening.getEnvironment().getFileServer().getLastModified(getFSPath(element));
		} catch (IOException e) {
			return 0;
		}
	}

	private String getFSPath(Object element) {
		if (element instanceof FSPathEditorInput) {
			return ((FSPathEditorInput) element).getPath().toString();
		}
		return null;
	}
}