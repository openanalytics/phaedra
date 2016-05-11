package eu.openanalytics.phaedra.ui.protocol.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.annotation.AnnotationService;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.Activator;
import eu.openanalytics.phaedra.ui.protocol.util.ClassificationTableFactory;

/**
 * A dialog for creating a new annotation feature.
 */
public class CreateAnnotationDialog extends TitleAreaDialog {

	private Text nameTxt;
	private TableViewer valuesTableViewer;
	
	private String name;
	private ProtocolClass pClass;
	private List<FeatureClass> values;
	
	public CreateAnnotationDialog(Shell parentShell, String name, ProtocolClass pClass, List<String> values) {
		super(parentShell);
		this.name = name;
		this.pClass = pClass;
		this.values = new ArrayList<>();
		if (values != null) {
			for (String value: values) createValueClass(value);
		}
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Create New Annotation");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);
		GridDataFactory.fillDefaults().applyTo(container);
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Name:");
		
		nameTxt = new Text(container, SWT.BORDER);
		nameTxt.addModifyListener(e -> validateAnnotation());
		if (name != null) nameTxt.setText(name);
		GridDataFactory.fillDefaults().grab(true,false).align(SWT.FILL, SWT.CENTER).applyTo(nameTxt);
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Protocol Class:");
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText(pClass.getName());
		GridDataFactory.fillDefaults().grab(true,false).align(SWT.FILL, SWT.CENTER).applyTo(lbl);
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Values:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		Composite tableContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(tableContainer);
		
		valuesTableViewer = ClassificationTableFactory.createTableViewer(tableContainer, true, null);
		valuesTableViewer.setInput(values);
		GridDataFactory.fillDefaults().span(4, 1).grab(true, true).hint(SWT.DEFAULT, 150).applyTo(valuesTableViewer.getControl());
		ClassificationTableFactory.createAddClassLink(tableContainer, e -> createValueClass(null));
		ClassificationTableFactory.createRemoveClassLink(tableContainer, e -> removeValueClass());
		
		setTitle("Create New Annotation");
		setMessage("Specify a name for the annotation below.\nOptionally, specify a list of possible values for the annotation.");
		
		return container;
	}
	
	@Override
	protected void okPressed() {
		try {
			name = nameTxt.getText();
			boolean numeric = true;
			if (values.isEmpty()) numeric = false;
			else numeric = !values.stream().map(fc -> fc.getPattern()).anyMatch(p -> !NumberUtils.isDigit(p));
			AnnotationService.getInstance().createAnnotation(name, numeric, pClass, values);
			super.okPressed();	
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Error Creating Annotation", "An error occurred while creating the annotation", status);
		}
	}
	
	private void validateAnnotation() {
		name = nameTxt.getText();
		boolean valid = !name.isEmpty() && !ProtocolService.streamableList(pClass.getFeatures()).stream().anyMatch(f -> f.getName().equalsIgnoreCase(name));
		
		Button okBtn = getButton(IDialogConstants.OK_ID);
		if (okBtn != null) okBtn.setEnabled(valid);
		
		String msg = valid ? null : "Invalid annotation name";
		setErrorMessage(msg);
	}
	
	private void createValueClass(String name) {
		if (name == null) name = "" + (values.size() + 1);
		values.add(AnnotationService.getInstance().createValueClass(name));
		valuesTableViewer.refresh();
	}
	
	private void removeValueClass() {
		values.removeAll(SelectionUtils.getObjects(valuesTableViewer.getSelection(), FeatureClass.class));
		valuesTableViewer.refresh();
	}
}