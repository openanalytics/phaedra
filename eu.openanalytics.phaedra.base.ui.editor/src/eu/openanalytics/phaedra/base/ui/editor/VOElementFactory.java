package eu.openanalytics.phaedra.base.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.environment.GenericEntityService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class VOElementFactory implements IElementFactory {

	@Override
	public IAdaptable createElement(IMemento memento) {
		IMemento dynamic = memento.getChild("dynamic");
		if (dynamic != null) {
			String className = dynamic.getString("class");
			try {
				//TODO Find a better way to do this. This requires Eclipse-BuddyPolicy: global AND requires the collector class to be exported.
				IDynamicVOCollector collector = (IDynamicVOCollector) (Class.forName(className).newInstance());
				return new VOEditorInput(collector);
			} catch (Exception e) {
				EclipseLog.warn("Failed to restore input collector: " + className, e, Activator.getDefault());
			}
		}
		
		IMemento[] voTags = memento.getChildren("vo");
		List<IValueObject> objects = new ArrayList<>();
		for (IMemento tag: voTags) {
			long id = Long.parseLong(tag.getString("id"));
			String className = tag.getString("class");
			try {
				//TODO Find a better way to do this. This requires Eclipse-BuddyPolicy: global.
				IValueObject vo = (IValueObject) GenericEntityService.getInstance().findEntity(Class.forName(className), id);
				if (vo != null) objects.add(vo);
				else EclipseLog.warn("Failed to restore value object: cannot find " + className + " (" + id + ")", Activator.getDefault());
			} catch (Exception e) {
				EclipseLog.warn("Failed to restore value object: " + className + " (" + id + ")", e, Activator.getDefault());
			}
		}
		return new VOEditorInput(objects);
	}

	public static class PersistableVOInput implements IPersistableElement {

		private VOEditorInput input;
		
		public PersistableVOInput(VOEditorInput input) {
			this.input = input;
		}
		
		@Override
		public void saveState(IMemento memento) {
			if (input.getCollector() != null) {
				// Dynamic input. No use trying to save the value objects.
				String className = input.getCollector().getClass().getName();
				IMemento tag = memento.createChild("dynamic");
				tag.putString("class", className);
			} else {
				List<IValueObject> objects = input.getValueObjects();
				for (IValueObject vo: objects) {
					IMemento tag = memento.createChild("vo");
					tag.putString("class", vo.getClass().getName());
					tag.putString("id", ""+vo.getId());
				}
			}
		}

		@Override
		public String getFactoryId() {
			return VOElementFactory.class.getName();
		}
		
	}
}
