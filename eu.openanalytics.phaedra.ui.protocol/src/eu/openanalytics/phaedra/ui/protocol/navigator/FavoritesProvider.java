package eu.openanalytics.phaedra.ui.protocol.navigator;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.icons.IconRegistry;
import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.ui.protocol.navigator.FavoritesManager.Favorite;

public class FavoritesProvider implements IElementProvider {

	public final static String MY_FAVORITES_GROUP = "my.favorites";
	
	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			IElement[] elements = new IElement[1];
			elements[0] = new Group("My Favorites", MY_FAVORITES_GROUP, null, true, null);
			return elements;
		} else if (parent.getId().equals(MY_FAVORITES_GROUP)) {
			return loadFavorites(parent);
		}
		return null;
	}

	private IElement[] loadFavorites(IGroup parent) {
		Favorite[] favorites = FavoritesManager.getInstance().getFavorites();
		IElement[] elements = new IElement[favorites.length];
		for (int i = 0; i < favorites.length; i++) {
			Favorite fav = favorites[i];
			IValueObject vo = fav.resolveValue();
			
			String name = fav.name;
			if (name == null || name.isEmpty()) {
				if (vo == null) name = "Unknown " + FileUtils.getExtension(fav.className) + " (" + fav.id + ")";
				else name = vo.toString();
			}
			ImageDescriptor icon = (vo == null) ? null : IconRegistry.getInstance().getDefaultImageDescriptorFor(vo.getClass());
			elements[i] = new Element(name, "fav_" + fav.className + "_" + fav.id, parent.getId(), icon);
			((Element)elements[i]).setData(fav);
		}
		return elements;
	}
}
