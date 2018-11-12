package eu.openanalytics.phaedra.ui.perspective;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.PerspectiveDescriptor;
import org.eclipse.ui.internal.registry.PerspectiveRegistry;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.partsettings.service.PartSettingsService;
import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPartReference;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

/**
 * <p>
 * Service for working with Saved Perspectives.
 * </p><p>
 * A Saved Perspective is a 'snapshot' of the workbench, and contains the following state:
 * </p><ul>
 * <li>The active Feature</li>
 * <li>The layout of open views</li>
 * <li>The state of each open view that supports Save & Restore</li>
 * </ul>
 * <p>
 * Any user can create new Saved Perspectives, which are private by default.
 * The users who can see, modify or delete the Saved Perspective, are determined by the access scope of the Saved Perspective.
 * </p>
 */
@SuppressWarnings("restriction")
public class PerspectiveService extends BaseJPAService {

	private static PerspectiveService instance;

	private PerspectiveService() {
		// Hidden constructor
	}

	public static synchronized PerspectiveService getInstance() {
		if (instance == null) instance = new PerspectiveService();
		return instance;
	}

	public List<SavedPerspective> getPrivatePerspectives() {
		List<SavedPerspective> allPerspectives = getList(SavedPerspective.class);
		return allPerspectives.stream()
				.filter(p -> p.getAccessScope() == AccessScope.PRIVATE)
				.filter(p -> SecurityService.getInstance().checkPersonalObject(Action.READ, p))
				.filter(p -> p.getOwner().equalsIgnoreCase(SecurityService.getInstance().getCurrentUserName()))
				.collect(Collectors.toList());
	}

	public List<SavedPerspective> getTeamPerspectives() {
		List<SavedPerspective> allPerspectives = getList(SavedPerspective.class);
		return allPerspectives.stream()
				.filter(p -> p.getAccessScope() == AccessScope.TEAM)
				.filter(p -> SecurityService.getInstance().checkPersonalObject(Action.READ, p))
				.collect(Collectors.toList());
	}

	public List<SavedPerspective> getPublicPerspectives() {
		List<SavedPerspective> allPerspectives = getList(SavedPerspective.class);
		return allPerspectives.stream()
				.filter(p -> p.getAccessScope().isLessRestrictiveThan(AccessScope.PUBLIC_R))
				.filter(p -> SecurityService.getInstance().checkPersonalObject(Action.READ, p))
				.collect(Collectors.toList());
	}

	public SavedPerspective getPerspective(long id) {
		SavedPerspective p = getEntity(SavedPerspective.class, id);
		if (!SecurityService.getInstance().checkPersonalObject(Action.READ, p)) return null;
		return p;
	}

	public SavedPerspective createPerspective(String name) {
		SavedPerspective p = new SavedPerspective();
		p.setName(name);
		p.setAccessScope(AccessScope.PRIVATE);
		p.setOwner(SecurityService.getInstance().getCurrentUserName());
		return p;
	}

	public void deletePerspective(SavedPerspective perspective) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, perspective);
		deleteSavedViews(perspective);
		deleteWorkbenchState(perspective);
		delete(perspective);
	}

	public void savePerspectiveSettings(SavedPerspective perspective) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, perspective);
		save(perspective);
	}

	public void savePerspectiveLayout(SavedPerspective perspective) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, perspective);
		try {
			saveFeatureState(perspective);
			saveWorkbenchState(perspective);
			saveViewStates(perspective);
		} catch (IOException e) {
			throw new RuntimeException("Failed to save workbench state", e);
		}
		save(perspective);
	}

	public void openPerspective(SavedPerspective perspective) {
		if (Display.getCurrent() == null) throw new RuntimeException("Perspectives can only be opened from the UI thread");
		try {
			GridState.clear();
			loadFeatureState(perspective);
			loadWorkbenchState(perspective);
			loadViewStates(perspective);
		} catch (WorkbenchException e) {
			throw new RuntimeException("Failed to restore workbench state", e);
		}
	}

	public boolean canUpdatePerspective(SavedPerspective perspective) {
		return SecurityService.getInstance().checkPersonalObject(Action.UPDATE, perspective);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void saveFeatureState(SavedPerspective perspective) {
		Feature currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
		perspective.setFeature(currentFeature);
	}

	private void saveWorkbenchState(SavedPerspective perspective) throws IOException {
		String name = perspective.getWorkbenchState();
		if (name == null || name.isEmpty()) {
			name = UUID.randomUUID().toString();
			perspective.setWorkbenchState(name);
		}
		
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		PerspectiveRegistry reg = (PerspectiveRegistry) WorkbenchPlugin.getDefault().getPerspectiveRegistry();
		
		IPerspectiveDescriptor currentPerspective = page.getPerspective();
		if (currentPerspective.getLabel().equals(name)) {
			// Already exists, and it's currently open
			page.savePerspectiveAs(currentPerspective);
		} else if (reg.findPerspectiveWithLabel(name) != null) {
			// Already exists
			page.savePerspectiveAs(currentPerspective);
		} else {
			// Doesn't exist yet: create one now
			IPerspectiveDescriptor newPerspective = reg.createPerspective(name, (PerspectiveDescriptor) page.getPerspective());
			page.savePerspectiveAs(newPerspective);
		}
	}

	private void deleteWorkbenchState(SavedPerspective perspective) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		PerspectiveRegistry reg = (PerspectiveRegistry) WorkbenchPlugin.getDefault().getPerspectiveRegistry();
		IPerspectiveDescriptor desc = reg.findPerspectiveWithLabel(perspective.getWorkbenchState());
		if (desc == null) return;
		if (page.getPerspective() == desc) page.closeAllPerspectives(true, true);
		reg.deletePerspective(desc);
	}
	
	private void saveViewStates(SavedPerspective perspective) {
		List<SavedPartReference> refs = perspective.getSavedParts();
		if (refs == null) {
			refs = new ArrayList<>();
			perspective.setSavedParts(refs);
		} else {
			deleteSavedViews(perspective);
			refs.clear();
		}

		IViewReference[] viewRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (IViewReference viewRef: viewRefs) {
			IViewPart view = viewRef.getView(false);
			if (view == null) continue;

			String name = "Perspective View " + perspective.getWorkbenchState();
			PartSettings settings = PartSettingsService.getInstance().createPartSettings(view, name);
			if (settings == null) continue;

			SavedPartReference ref = new SavedPartReference();
			ref.setPerspective(perspective);
			ref.setPartId(viewRef.getId());
			ref.setSecondaryId(viewRef.getSecondaryId());
			ref.setPartSettings(settings);
			refs.add(ref);
		}
	}

	private void loadFeatureState(SavedPerspective perspective) {
		ProtocolUIService.getInstance().setCurrentFeature(perspective.getFeature());
	}

	private void loadWorkbenchState(SavedPerspective perspective) throws WorkbenchException {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		PerspectiveRegistry reg = (PerspectiveRegistry) WorkbenchPlugin.getDefault().getPerspectiveRegistry();		
		IPerspectiveDescriptor pDesc = reg.findPerspectiveWithLabel(perspective.getWorkbenchState());
		
		// Open the default perspective so that when closePerspective is called, not all perspectives are closed,
		// which would trigger closing of all editors as well.
		String defaultPerspId = reg.getDefaultPerspective();
		if (defaultPerspId != null) {
			IPerspectiveDescriptor defaultPersp = reg.findPerspectiveWithId(defaultPerspId);
			page.setPerspective(defaultPersp);
		}
		page.closePerspective(pDesc, true, false);
		page.setPerspective(pDesc);
	}

	private void loadViewStates(SavedPerspective perspective) {
		List<SavedPartReference> refs = perspective.getSavedParts();
		if (refs == null || refs.isEmpty()) return;

		for (SavedPartReference ref: refs) {
			IViewReference viewRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(ref.getPartId(), ref.getSecondaryId());
			if (viewRef == null) continue;
			IViewPart view = viewRef.getView(false);
			
			PartSettings settings = ref.getPartSettings();
			if (settings == null) continue;
			
			try {
				PartSettingsService.getInstance().applySettings(view, settings);
			} catch (Exception e) {
				throw new RuntimeException("Failed to restore saved view", e);
			}
		}
	}

	private void deleteSavedViews(SavedPerspective perspective) {
		for (SavedPartReference ref: perspective.getSavedParts()) {
			PartSettings settings = ref.getPartSettings();
			if (settings != null) PartSettingsService.getInstance().deletePartSettings(settings);
		}
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	/*
	 * **************
	 * Event handling
	 * **************
	 */

	protected void fire(ModelEventType type, Object object, int status) {
		ModelEvent event = new ModelEvent(object, type, status);
		ModelEventService.getInstance().fireEvent(event);
	}

	@Override
	protected void afterSave(Object o) {
		fire(ModelEventType.ObjectChanged, o, 0);
	}

	@Override
	protected void beforeDelete(Object o) {
		fire(ModelEventType.ObjectAboutToBeRemoved, o, 0);
	}

	@Override
	protected void afterDelete(Object o) {
		fire(ModelEventType.ObjectRemoved, o, 0);
	}
}
