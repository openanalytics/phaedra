package eu.openanalytics.phaedra.ui.protocol.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.TransferData;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.ui.protocol.navigator.FavoritesManager.Favorite;

public class FavoritesHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return element.getId().equals(FavoritesProvider.MY_FAVORITES_GROUP) || element.getId().startsWith("fav_");
	}
	
	@Override
	public void handleDoubleClick(IElement element) {
		IValueObject vo = ((Favorite) element.getData()).resolveValue();
		EditorFactory.getInstance().openEditor(vo);
	}
	
	@Override
	public void createContextMenu(IElement element, IMenuManager mgr) {
		if (!element.getId().startsWith("fav_")) return;
		
		Action action = new Action("Rename...", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Favorite fav = (Favorite) element.getData();
				FavoritesManager.getInstance().renameFavorite(fav);
			}
		};
		mgr.add(action);
		
		action = new Action("Remove", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Favorite fav = (Favorite) element.getData();
				FavoritesManager.getInstance().removeFavorite(fav);
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("delete.png"));
		mgr.add(action);
	}
	
	@Override
	public boolean validateDrop(IElement element, int operation, TransferData transferType) {
		return element.getId().equals(FavoritesProvider.MY_FAVORITES_GROUP);
	}
	
	@Override
	public boolean performDrop(IElement element, Object data) {
		if (!element.getId().equals(FavoritesProvider.MY_FAVORITES_GROUP)) return false;
		FavoritesManager.getInstance().addFavorite(data);
		return true;
	}
}
