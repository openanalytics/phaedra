package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

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

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.cellprofiler.widget.ChannelComposer;
import eu.openanalytics.phaedra.ui.cellprofiler.wizard.CellprofilerProtocolWizard.WizardState;

public class SelectImageDataPage extends BaseStatefulWizardPage {

	private TableViewer imageFolderTableViewer;
	private ChannelComposer channelComposer;
	private WizardState wizardState;
	
	protected SelectImageDataPage() {
		super("Select Image Data");
	}

	@Override
	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(area);
		
		imageFolderTableViewer = new TableViewer(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(400, 120).applyTo(imageFolderTableViewer.getControl());
		imageFolderTableViewer.addSelectionChangedListener(e -> setSelectedFolder());
		imageFolderTableViewer.setContentProvider(new ArrayContentProvider());
		
		TableViewerColumn col = new TableViewerColumn(imageFolderTableViewer, SWT.NONE);
		col.getColumn().setWidth(500);
		col.getColumn().setText("Folder");
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() == null) return;
				Path relative = wizardState.selectedFolder.relativize((Path) cell.getElement());
				cell.setText(relative.toString());
			}
		});
		
		channelComposer = new ChannelComposer(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(channelComposer);
		
		setTitle("Select Image Data");
    	setDescription("Select the folder containing the images.");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		wizardState = (WizardState) state;
		imageFolderTableViewer.setInput(wizardState.imageFolderCandidates);
	}
	
	@Override
	public void collectState(IWizardState state) {
		// Nothing to do.
	}
	
	private void setSelectedFolder() {
		wizardState.selectedImageFolder = SelectionUtils.getFirstObject(imageFolderTableViewer.getSelection(), Path.class);
		/*
		 * TODO
		 * -inspect folder
		 * -ask user to select patterns for each channel
		 * -if montage is required, ask user to select?
		 */
	}
}

