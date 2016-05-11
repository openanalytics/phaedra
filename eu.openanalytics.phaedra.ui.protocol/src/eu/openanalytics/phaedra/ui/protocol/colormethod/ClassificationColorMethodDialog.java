package eu.openanalytics.phaedra.ui.protocol.colormethod;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class ClassificationColorMethodDialog extends BaseColorMethodDialog {

	private ProtocolClass pClass;
	private ClassificationColorMethod cm;
	
	private ComboViewer classificationFeatureCmb;
	
	public ClassificationColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell, cm);
		this.pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
		this.cm = (ClassificationColorMethod)cm;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 200);
	}
	
	@Override
	protected void fillDialogArea(Composite area) {
		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Classification feature:");
		
		classificationFeatureCmb = new ComboViewer(area);
		classificationFeatureCmb.setContentProvider(new ArrayContentProvider());
		classificationFeatureCmb.setLabelProvider(new LabelProvider());
		List<Feature> features = ClassificationService.getInstance().findWellClassificationFeatures(pClass);
		classificationFeatureCmb.setInput(features);
		
		if (cm.getClassificationFeature() != null) {
			classificationFeatureCmb.setSelection(new StructuredSelection(cm.getClassificationFeature()));
		} else if (features.size() > 0) {
			classificationFeatureCmb.setSelection(new StructuredSelection(features.get(0)));
		}
	}

	@Override
	protected void okPressed() {
		// Transfer new ruleset to the color method.
		Feature classificationFeature = SelectionUtils.getFirstObject(classificationFeatureCmb.getSelection(), Feature.class);
		cm.setClassificationFeature(classificationFeature);
		super.okPressed();
	}
}
