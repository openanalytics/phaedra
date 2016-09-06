package eu.openanalytics.phaedra.ui.silo.navigator;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchActionConstants;

import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloGroup;
import eu.openanalytics.phaedra.ui.silo.dialog.SiloGroupDialog;

public class GroupHandler extends BaseHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().startsWith(SiloProvider.GROUP)) || (element.getId().startsWith(SiloProvider.PCLASS));
	}

	@Override
	public void createContextMenu(final IElement element, IMenuManager mgr) {
		if (!(element.getData() instanceof SiloGroup)) return;
		Action action = new Action("Edit Silo Group", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				SiloGroup siloGroup = (SiloGroup) element.getData();
				SiloGroupDialog dialog = new SiloGroupDialog(Display.getDefault().getActiveShell(), siloGroup);
				dialog.open();
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("folder_edit.png"));
		mgr.add(action);

		action = new Action("Delete Silo Group", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				SiloGroup siloGroup = (SiloGroup) element.getData();
				if (siloGroup.getSilos().isEmpty()) {
					boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
							"Delete Silo Group", "Are you sure you want to delete the silo group \"" + siloGroup.getName() + "\" ?");
					if (confirmed) {
						try {
							SiloService.getInstance().deleteSiloGroup(siloGroup);
						} catch (PermissionDeniedException ex) {
							MessageDialog.openError(Display.getDefault().getActiveShell(),
									"Delete Error", ex.getMessage());
						}
					}
				} else {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Delete Error", "Silo Group \"" + siloGroup.getName() + "\" cannot be deleted because it still contains Silos.\n\n"
									+ "Please remove the Silos from this group first.");
				}
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("folder_delete.png"));
		mgr.add(action);
		
		mgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		
		addCreateCmds(element, mgr);
	}

	@Override
	public boolean validateDrop(IElement element, int operation, TransferData transferType) {
		return element.getData() instanceof SiloGroup || element.getData() instanceof ProtocolClass;
	}

	@Override
	public boolean performDrop(IElement element, Object data) {
		Object elementData = element.getData();
		StructuredSelection selection = (StructuredSelection) data;
		Object selectionData = selection.getFirstElement();
		if (selectionData instanceof Silo) {
			Silo silo = (Silo) selectionData;

			// Silo was dragged on a Silo Group, attempt to move it to that Silo Group.
			if (elementData instanceof SiloGroup) {
				SiloGroup group = (SiloGroup) elementData;

				// Check if the user has permission to move the Silo to a Group.
				if (!SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, silo)
						|| !SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, group)) {
					MessageDialog.openError(Display.getDefault().getActiveShell()
							, "Silo Move Error", "Insufficient permissions for moving Silo " + silo.getName() + " to Silo Group " + group.getName() + ".");
					return false;
				}
				// Check if the ProtocolClass of the Silo and Group matches.
				if (silo.getProtocolClass() != group.getProtocolClass()) {
					MessageDialog.openError(Display.getDefault().getActiveShell()
							, "Silo Move Error", "Silo and Silo Group are not of the same Protocol Class.");
					return false;
				}
				// Check if the type of the Silo and Group matches.
				if (silo.getType() != group.getType()) {
					MessageDialog.openError(Display.getDefault().getActiveShell()
							, "Silo Move Error", "Silo and Silo Group are not of the same type (e.g. Well).");
					return false;
				}
				// Check if the access scope of the Silo and Group matches.
				AccessScope siloScope = silo.getAccessScope();
				AccessScope groupScope = group.getAccessScope();
				if (siloScope != groupScope) {
					boolean updateSilo = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Silo Move Error"
							, "Cannot move a Silo with share option \"" + siloScope.getName() + "\" to a Silo Group with share option \"" + groupScope.getName() + "\".\n\n"
									+ "Would you like to change the share option of the Silo to \"" + groupScope.getName() + "\"?");
					if (updateSilo) {
						// Update the AccessScope of the Silo that is being moved.
						silo.setAccessScope(groupScope);
					} else {
						return false;
					}
				}

				removeSiloFromGroups(silo);
				silo.getSiloGroups().add(group);
				group.getSilos().add(silo);
				SiloService.getInstance().updateSilo(silo);
				SiloService.getInstance().updateSiloGroup(group);
				return true;
			}

			// Silo was dropped on a Protocol Class. Remove the Silo from its current Group.
			if (elementData instanceof ProtocolClass) {
				// Check if the user has permission to move the Silo to a Group.
				if (!SecurityService.getInstance().checkPersonalObject(SecurityService.Action.UPDATE, silo)) {
					MessageDialog.openError(Display.getDefault().getActiveShell()
							, "Silo Move Error", "Insufficient permissions for removing Silo " + silo.getName() + " from its Silo Group.");
					return false;
				}

				removeSiloFromGroups(silo);
				SiloService.getInstance().updateSilo(silo);
				return true;
			}
		}
		return false;
	}

	private void removeSiloFromGroups(Silo silo) {
		// Check if the Silo belongs to one or more Groups. If so, remove it from those groups.
		Set<SiloGroup> siloGroups = silo.getSiloGroups();
		if (siloGroups != null && !siloGroups.isEmpty()) {
			// Silo belonged to another Group. Remove it from the previous group.
			for (SiloGroup prevGroup : siloGroups) {
				if (prevGroup.getSilos().remove(silo)) {
					SiloService.getInstance().updateSiloGroup(prevGroup);
				}
			}
		}
		// Remove references to possible groups.
		silo.setSiloGroups(new HashSet<SiloGroup>());
	}

}