package eu.openanalytics.phaedra.ui.silo.editor;

import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInputFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloEditorFactory extends VOEditorInputFactory {

	@Override
	public String getEditorIdFor(Object input) {
		Silo silo = SelectionUtils.getFirstAsClass(((VOEditorInput)input).getValueObjects(), Silo.class);
		String editorId = (silo.getType() == GroupType.WELL.getType()) ? WellSiloEditor.class.getName() : SubWellSiloEditor.class.getName();
		return editorId;
	}
}
