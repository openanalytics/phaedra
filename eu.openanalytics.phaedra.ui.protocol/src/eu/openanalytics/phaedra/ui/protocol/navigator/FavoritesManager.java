package eu.openanalytics.phaedra.ui.protocol.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.db.jpa.EntityClassManager;
import eu.openanalytics.phaedra.base.environment.GenericEntityService;
import eu.openanalytics.phaedra.base.ui.navigator.Navigator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.ui.protocol.Activator;

public class FavoritesManager {

	private static FavoritesManager instance;
	
	private List<Favorite> currentFavorites;
	
	private FavoritesManager() {
		// Hidden constructor
	}
	
	public static synchronized FavoritesManager getInstance() {
		if (instance == null) instance = new FavoritesManager();
		return instance;
	}
	
	public Favorite[] getFavorites() {
		if (currentFavorites == null) loadFavorites();
		return currentFavorites.toArray(new Favorite[currentFavorites.size()]);
	}
	
	public void addFavorite(Object data) {
		IValueObject vo = null;
		if (data instanceof ISelection) vo = SelectionUtils.getFirstObject((ISelection)data, IValueObject.class);
		else vo = SelectionUtils.getAsClass(data, IValueObject.class, false);
		if (vo == null) return;
		
		currentFavorites.add(new Favorite(null, vo.getClass().getName(), vo.getId()));
		saveFavorites();
		refreshNavigator();
	}
	
	public void removeFavorite(Favorite favorite) {
		currentFavorites.remove(favorite);
		saveFavorites();
		refreshNavigator();
	}
	
	public void renameFavorite(Favorite favorite) {
		InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), "Rename item",
				"Enter a new name for this favorite item", favorite.name, null);
		if (dialog.open() == Window.CANCEL) return;
		favorite.name = dialog.getValue();
		saveFavorites();
		refreshNavigator();
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private IValueObject resolveFavorite(Favorite favorite) {
		IValueObject vo = null;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(favorite.className);
		} catch (ClassNotFoundException e) {
			Class<?>[] classes = EntityClassManager.getRegisteredEntityClasses();
			clazz = Arrays.stream(classes).filter(c -> c.getName().equals(favorite.className)).findFirst().orElse(null);
		}

		if (clazz == null) {
			EclipseLog.warn("Unable to resolve favorite item: " + favorite.className + ", id: " + favorite.id, Activator.getDefault());
			return null;
		}
		
		Object instance = GenericEntityService.getInstance().findEntity(clazz, favorite.id);
		if (instance instanceof IValueObject) vo = (IValueObject)instance;
		return vo;
	}
	
	private void loadFavorites() {
		currentFavorites = new ArrayList<>();
		
		try {
			String value = UserService.getInstance().getPreferenceValue("Memento", "NavigatorFavorites");
			if (value == null || value.isEmpty()) return;
			
			Document doc = XmlUtils.parse(value);
			NodeList favoriteTags = XmlUtils.findTags("/favorites/fav", doc);
			for (int i=0; i<favoriteTags.getLength(); i++) {
				Element tag = (Element)favoriteTags.item(i);
				String name = tag.getAttribute("name");
				String className = tag.getAttribute("class");
				long id = Long.parseLong(tag.getAttribute("id"));
				currentFavorites.add(new Favorite(name, className, id));
			}
		} catch (Exception e) {
			EclipseLog.warn("Unable to load navigator favorite item: " + e.getMessage(), Activator.getDefault());
		}
	}
	
	private void saveFavorites() {
		try {
			Document doc = XmlUtils.createEmptyDoc();
			Element favs = XmlUtils.createTag(doc, doc, "favorites");
			for (Favorite fav: currentFavorites) {
				Element e = XmlUtils.createTag(doc, favs, "fav");
				e.setAttribute("class", fav.className);
				e.setAttribute("id", ""+fav.id);
				if (fav.name != null) e.setAttribute("name", fav.name);
			}
			String value = XmlUtils.writeToString(doc);
			UserService.getInstance().setPreferenceValue("Memento", "NavigatorFavorites", value);
		} catch (Exception e) {
			EclipseLog.error("Unable to save navigator favorites", e, Activator.getDefault());
		}
	}
	
	private void refreshNavigator() {
		try {
			Navigator view = (Navigator)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(Navigator.class.getName());
			view.refreshTree(null);
		} catch (Exception e) {}
	}
	
	public static class Favorite {
		
		public String name;
		public String className;
		public long id;
		
		private IValueObject resolvedValue;
		
		public Favorite(String name, String className, long id) {
			this.name = name;
			this.className = className;
			this.id = id;
		}
		
		public IValueObject resolveValue() {
			if (resolvedValue == null) resolvedValue = FavoritesManager.getInstance().resolveFavorite(this);
			return resolvedValue;
		}
	}
}
