package eu.openanalytics.phaedra.ui.curve.grid;

import org.eclipse.ui.IEditorInput;

import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInputFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class CompoundGridEditorFactory extends VOEditorInputFactory {

	@Override
	public IEditorInput createEditorInput(Object input) {
		VOEditorInput editorInput = (VOEditorInput) super.createEditorInput(input);
		
		// Display the number of curves in the tooltip.
		ProtocolClass pClass = SelectionUtils.getFirstAsClass(editorInput.getValueObjects(), ProtocolClass.class);
		int featureCount = (pClass == null) ? 0 : CollectionUtils.findAll(pClass.getFeatures(), ProtocolUtils.FEATURES_WITH_CURVES).size();
		int curveCount = editorInput.getValueObjects().size() * featureCount;
		
		return new VOEditorInput(editorInput.getValueObjects()) {
			@Override
			public String getToolTipText() {
				return getValueObjects().size() + " compounds, " + curveCount + " curves";
			}
		};
	}
	
	@Override
	public String getEditorIdFor(Object input) {
		return CompoundGridEditor.class.getName();
	}
}
