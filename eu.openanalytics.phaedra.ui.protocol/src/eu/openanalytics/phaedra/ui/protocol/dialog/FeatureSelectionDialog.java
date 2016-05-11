package eu.openanalytics.phaedra.ui.protocol.dialog;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.util.FeatureSelectionTable;

public class FeatureSelectionDialog<F extends IFeature> extends TitleAreaDialog {

	private FeatureSelectionTable<F> featureSelection;

	private ProtocolClass pClass;
	private Class<F> featureClass;
	private List<F> selectedFeatures;
	private List<String> selectedNormalizations;

	private int minSelectionSize;
	private int maxSelectionSize;

	public FeatureSelectionDialog(Shell parentShell, ProtocolClass pClass, Class<F> featureClass, List<F> selectedFeatures, int minSelectionSize) {
		this(parentShell, pClass, featureClass, selectedFeatures, null, minSelectionSize, Integer.MAX_VALUE);
	}
	
	public FeatureSelectionDialog(Shell parentShell, ProtocolClass pClass, Class<F> featureClass,
			List<F> selectedFeatures, List<String> selectedNormalizations, int minSelectionSize, int maxSelectionSize) {

		super(parentShell);

		this.pClass = pClass;
		this.featureClass = featureClass;
		this.selectedFeatures = selectedFeatures;
		this.selectedNormalizations = selectedNormalizations;
		this.minSelectionSize = minSelectionSize;
		this.maxSelectionSize = maxSelectionSize;
	}

	private FeatureSelectionDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		featureSelection = createFeatureSelection(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(featureSelection);
		GridLayoutFactory.fillDefaults().applyTo(featureSelection);

		setTitle("Select Features");
		setMessage("Select the features in the table below.");

		return parent;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void okPressed() {
		if (selectedFeatures.size() < minSelectionSize) {
			MessageDialog.openWarning(getParentShell(), "Warning!", "Please select " + minSelectionSize + " Features.");
			return;
		}
		if (selectedFeatures.size() > maxSelectionSize) {
			selectedFeatures.removeAll(selectedFeatures.subList(maxSelectionSize, selectedFeatures.size()));
			MessageDialog.openWarning(getParentShell(), "Warning!", "You have selected more than " + maxSelectionSize + " Features.\n\n"
					+ "Only the first " + maxSelectionSize + " Features are kept as a selection.");
		}
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		featureSelection.resetSelection();
		super.cancelPressed();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setSize(600, 400);
		newShell.setText("Select Features");
	}
	
	protected FeatureSelectionTable<F> createFeatureSelection(Composite parent, int style) {
		FeatureSelectionTable<F> table = new FeatureSelectionTable<F>(parent, style, pClass, featureClass, selectedFeatures, selectedNormalizations);
		return table;
	}

}