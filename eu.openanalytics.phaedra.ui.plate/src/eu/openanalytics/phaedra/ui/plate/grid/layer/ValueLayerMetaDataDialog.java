package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ValueLayerMetaDataDialog extends TitleAreaDialog {

	private String title;
	private List<String> metadata;
	private String selectedMetadata;
	private org.eclipse.swt.widgets.List listMetaData;

	public ValueLayerMetaDataDialog(Shell parentShell, String title, Set<String> metadata) {
		super(parentShell);
		this.title = title;
		this.metadata = asSortedList(metadata);
		this.selectedMetadata = null;
	}

	public String getSelectedMetadata() {
		return selectedMetadata;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configuration: " + title + " Meta Data");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		listMetaData = new org.eclipse.swt.widgets.List(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.BORDER);
		listMetaData.setItems(metadata.toArray(new String[metadata.size()]));
		GridDataFactory.fillDefaults().hint(400, 400).grab(true,false).applyTo(listMetaData);

		setTitle(title + " Meta Data");
		setMessage("Select which meta data to use as label on the grid.");

		return container;
	}

	@Override
	protected void okPressed() {
		if (listMetaData.getSelection().length < 1)
			super.cancelPressed();
		selectedMetadata = listMetaData.getSelection()[0];
		super.okPressed();
	}

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		Collections.sort(list);
		return list;
	}

}
