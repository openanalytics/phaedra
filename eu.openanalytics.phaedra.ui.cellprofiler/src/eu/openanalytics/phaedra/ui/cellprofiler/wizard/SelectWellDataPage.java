package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.cellprofiler.wizard.CellprofilerProtocolWizard.WizardState;

public class SelectWellDataPage extends BaseStatefulWizardPage {

	private TableViewer wellDataFileTableViewer;
	private TableViewer wellDataHeadersTableViewer;
	private WizardState wizardState;
	
	protected SelectWellDataPage() {
		super("Select Well Data");
	}

	@Override
	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(area);
		
		wellDataFileTableViewer = new TableViewer(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 120).applyTo(wellDataFileTableViewer.getControl());
		wellDataFileTableViewer.addSelectionChangedListener(e -> analyzeSelectedFile());
		wellDataFileTableViewer.setContentProvider(new ArrayContentProvider());
		
		TableViewerColumn col = new TableViewerColumn(wellDataFileTableViewer, SWT.NONE);
		col.getColumn().setWidth(350);
		col.getColumn().setText("File");
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() == null) return;
				Path relative = wizardState.selectedFolder.relativize((Path) cell.getElement());
				cell.setText(relative.toString());
			}
		});
		
		new Label(area, SWT.NONE).setText("Select the column(s) identifying the well:");
		
		wellDataHeadersTableViewer = new TableViewer(area, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 150).applyTo(wellDataHeadersTableViewer.getControl());
		wellDataHeadersTableViewer.addSelectionChangedListener(e -> setSelectedHeaders());
		wellDataHeadersTableViewer.setContentProvider(new ArrayContentProvider());
		
		col = new TableViewerColumn(wellDataHeadersTableViewer, SWT.NONE);
		col.getColumn().setWidth(350);
		col.getColumn().setText("Header");
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() != null) cell.setText(cell.getElement().toString());
			}
		});
		
		setTitle("Select Well Data");
    	setDescription("Select the well data file (containing aggregated values per well).");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		wizardState = (WizardState) state;
		wellDataFileTableViewer.setInput(wizardState.wellDataCandidates);
	}
	
	@Override
	public void collectState(IWizardState state) {
		// Nothing to do.
	}
	
	private void analyzeSelectedFile() {
		wizardState.selectedWellDataFile = SelectionUtils.getFirstObject(wellDataFileTableViewer.getSelection(), Path.class);
		
		String errMessage = null;
		try {
			getContainer().run(true, false, (monitor) -> {
				new CellprofilerAnalyzer().analyzeWelldataFile(wizardState, monitor);
				if (wizardState.wellDataHeaders != null) {
					Display.getDefault().asyncExec(() -> wellDataHeadersTableViewer.setInput(wizardState.wellDataHeaders));
				}
			});
		} catch (Exception e) {
			errMessage = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
		}
		setErrorMessage(errMessage);
		setPageComplete(false);
	}
	
	private void setSelectedHeaders() {
		wizardState.selectedWellDataHeaders = SelectionUtils.getObjects(wellDataHeadersTableViewer.getSelection(), String.class).toArray(new String[0]);
		setPageComplete(wizardState.selectedWellDataHeaders != null && wizardState.selectedWellDataHeaders.length > 0);	
	}
}
