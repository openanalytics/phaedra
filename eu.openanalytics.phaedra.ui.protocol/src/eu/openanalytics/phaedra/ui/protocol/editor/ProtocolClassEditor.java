package eu.openanalytics.phaedra.ui.protocol.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.Activator;
import eu.openanalytics.phaedra.ui.protocol.editor.page.FeaturesPage;
import eu.openanalytics.phaedra.ui.protocol.editor.page.ImageSettingsPage;
import eu.openanalytics.phaedra.ui.protocol.editor.page.ProtocolClassPage;
import eu.openanalytics.phaedra.ui.protocol.editor.page.SubWellFeaturesPage;

public class ProtocolClassEditor extends FormEditor implements ISaveablePart {

	private boolean dirty;
	private boolean writeAccess;
	
	private Map<Feature, FormulaRuleset> hitCallRulesets;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);

		if (!(input instanceof ProtocolClassEditorInput)) {
			throw new PartInitException("Input must be an instance of " + ProtocolClassEditorInput.class);
		}
		
		String name = getProtocolClass().getName();
		if (name == null) setPartName("New Protocol Class");
		else setPartName(name);

		setTitleImage(IconManager.getIconImage("struct.png"));

		writeAccess = ProtocolService.getInstance().canEditProtocolClass(getProtocolClass());
		hitCallRulesets = new HashMap<>();
		
		if (((ProtocolClassEditorInput) input).isNewProtocolClass()) markDirty();
	}

	@Override
	protected Composite createPageContainer(Composite parent) {
		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewProtocolClassSettings");
		return super.createPageContainer(parent);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProtocolClassPage(this, "General", "General"));
			addPage(new FeaturesPage(this, "Well Features", "Well Features"));
			addPage(new SubWellFeaturesPage(this, "Sub-Well Features", "Sub-Well Features"));
			addPage(new ImageSettingsPage(this, "Image Settings", "Image Settings"));
			setPageImage(0, IconManager.getIconImage("folder.png"));
			setPageImage(1, IconManager.getIconImage("tag_blue.png"));
			setPageImage(2, IconManager.getIconImage("tag_red.png"));
			setPageImage(3, IconManager.getIconImage("channel.png"));
		} catch (PartInitException e) {}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return writeAccess;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

		if (!isSaveAsAllowed()) {
			if (monitor != null) monitor.done();
			return;
		}

		IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Saving Protocol Class", IProgressMonitor.UNKNOWN);

				EntityManager em = Screening.getEnvironment().getEntityManager();

				// Query JPA. If object is detached (see exception handling below) it will be re-fetched.
				JDBCUtils.lockEntityManager(em);
				ProtocolClass pc = Screening.getEnvironment().getEntityManager().find(ProtocolClass.class, getOriginalProtocolClass().getId());
				JDBCUtils.unlockEntityManager(em);
				// Query may fail if a new protocol class is being edited.
				if (pc == null) pc = getOriginalProtocolClass();

				ProtocolService.getInstance().validateProtocolClass(getProtocolClass());
				// Note: do not copy ID's from the working copy to the original (managed or unmanaged) copy.
				// Existing items (imagesettings, features etc) should retain their existing ID.
				ObjectCopyFactory.copySettings(getProtocolClass(), pc, false);

				
				final ProtocolClass pcToSave = pc;
				// Save in the main thread so that after-save events are handled in the UI thread by default.
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						saveHitCallRulesets();
						ProtocolService.getInstance().updateProtocolClass(pcToSave);
					}
				});

				// Fix: explicitly refresh the image settings because the sequence nr of channels is maintained by JPA.
				JDBCUtils.lockEntityManager(em);
				Screening.getEnvironment().getEntityManager().refresh(pc.getImageSettings());
				JDBCUtils.unlockEntityManager(em);

				// Save succeeded: remove the dirty flag.
				dirty = false;
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						firePropertyChange(IEditorPart.PROP_DIRTY);
					}
				});

				monitor.done();
			}
		};
		try {
			if (monitor == null) monitor = new NullProgressMonitor();
			ModalContext.run(saveRunnable, true, monitor, Display.getCurrent());
		} catch (Exception e) {
			Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to save protocol class", cause);
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Save Failed", null, status);
			EclipseLog.log(status, Activator.getDefault());
		}
	}

	@Override
	public void doSaveAs() {
		// Do nothing
	}

	@Override
	public void setTitleImage(Image titleImage) {
		super.setTitleImage(titleImage);
	}

	@Override
	public void setPartName(String partName) {
		super.setPartName(partName);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		boolean hasAccess = isSaveAsAllowed();
		if (hasAccess) {
			dirty = true;
			firePropertyChange(ISaveablePart.PROP_DIRTY);
		}
	}

	public ProtocolClass getProtocolClass() {
		return ((ProtocolClassEditorInput)getEditorInput()).getProtocolClass();
	}

	public ProtocolClass getOriginalProtocolClass() {
		return ((ProtocolClassEditorInput)getEditorInput()).getOriginalProtocolClass();
	}
	
	public FormulaRuleset getHitCallRuleset(Feature feature) {
		FormulaRuleset ruleset = hitCallRulesets.get(feature);
		if (ruleset == null) {
			ruleset = FormulaService.getInstance().getRulesetForFeature(feature.getId(), RulesetType.HitCalling.getCode());
			if (ruleset != null) ruleset = FormulaService.getInstance().getWorkingCopy(ruleset);
		}
		if (ruleset == null) ruleset = FormulaService.getInstance().createRuleset(feature, RulesetType.HitCalling.getCode());
		hitCallRulesets.put(feature, ruleset);
		return ruleset;
	}
	
	private void saveHitCallRulesets() {
		for (FormulaRuleset workingCopy: hitCallRulesets.values()) {
			boolean isNew = workingCopy.getId() == 0;
			boolean isEmpty = workingCopy.getRules().isEmpty();
			
			if (isNew) {
				if (isEmpty) continue;
				else FormulaService.getInstance().updateRuleset(workingCopy, workingCopy);
			} else {
				FormulaRuleset original = FormulaService.getInstance().getRuleset(workingCopy.getId());
				if (isEmpty) FormulaService.getInstance().deleteRuleset(original);
				else FormulaService.getInstance().updateRuleset(original, workingCopy);
			}
		}
	}

	public Action getSaveAction() {
		Action saveAction = new Action("Save", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				boolean save = MessageDialog.openQuestion(
						Display.getDefault().getActiveShell(),
						"Save Protocol Class",
						"Would you like to save the changes to this Protocol Class?");
				if (!save) return;

				doSave(null);
			}
		};
		saveAction.setToolTipText("Save (Ctrl+S)");
		saveAction.setImageDescriptor(IconManager.getIconDescriptor("disk.png"));
		saveAction.setEnabled(isSaveAsAllowed());
		return saveAction;

	}
}
