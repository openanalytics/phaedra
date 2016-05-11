package eu.openanalytics.phaedra.ui.protocol.editor.page;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.editor.ProtocolClassEditor;

public class FeaturesPage extends FormPage {

	private FeaturesMasterBlock featuresMasterBlock;

	private IMessageManager msgManager;

	private ProtocolClass protocolClass;
	private ProtocolClassEditor editor;

	private boolean dirty;
	private boolean curvesChanged;
	
	public FeaturesPage(FormEditor editor, String id, String title) {
		super(editor, id, title);

		this.editor = (ProtocolClassEditor) editor;
		protocolClass = this.editor.getProtocolClass();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);

		ScrolledForm form = managedForm.getForm();
		form.setText("Features");
		form.setImage(IconManager.getIconImage("tag_blue.png"));

		msgManager = form.getMessageManager();
		msgManager.setDecorationPosition(SWT.TOP);

		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.decorateFormHeading(form.getForm());

		featuresMasterBlock = new FeaturesMasterBlock(protocolClass, this);
		featuresMasterBlock.createContent(managedForm);

		Action action = getEditor().getSaveAction();
		action.setEnabled(getEditor().isSaveAsAllowed());
		managedForm.getForm().getToolBarManager().add(action);
		managedForm.getForm().getToolBarManager().update(true);
	}

	@Override
	public ProtocolClassEditor getEditor() {
		return editor;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
		getEditor().markDirty();
	}
	
	public boolean isCurvesChanged() {
		return curvesChanged;
	}
	
	public void setCurvesChanged(boolean curvesChanged) {
		this.curvesChanged = curvesChanged;
	}
}
