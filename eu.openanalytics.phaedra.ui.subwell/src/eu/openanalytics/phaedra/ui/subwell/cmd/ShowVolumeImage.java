package eu.openanalytics.phaedra.ui.subwell.cmd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.ui.volumerenderer.VolumeDataItem;
import eu.openanalytics.phaedra.base.ui.volumerenderer.VolumeDataModel;
import eu.openanalytics.phaedra.base.ui.volumerenderer.VolumeViewEditor;
import eu.openanalytics.phaedra.base.ui.volumerenderer.VolumeViewEditorInput;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.Activator;

public class ShowVolumeImage extends AbstractHandler implements IHandler {


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();

		ISelection selection = page.getSelection();
		final Well well = SelectionUtils.getFirstObject(selection, Well.class);

		// Obtain the volume image for this well, or ask the user to select one.
		String imgPath = null;
		if (well != null) {
			try {
				String hdf5Path = PlateService.getInstance().getPlateFSPath(well.getPlate()) + "/" + well.getPlate().getId() + ".h5";
				if (Screening.getEnvironment().getFileServer().exists(hdf5Path)) {
					try (HDF5File dataFile = HDF5File.openForRead(hdf5Path)) {
						DecimalFormat wellNrFormat = new DecimalFormat("0000");
						String wellNr = wellNrFormat.format(PlateUtils.getWellNr(well));
						String dataPath = "/ExtraFiles/Well_" + wellNr + ".ics";
						if (dataFile.exists(dataPath)) {
							InputStream input = dataFile.getBinaryData(dataPath);
							imgPath = FileUtils.generateTempFolder(true) + "/volimage.ics";
							StreamUtils.copyAndClose(input, new FileOutputStream(imgPath));
						}
					} catch (IOException e) {
						throw new ExecutionException("Failed to open HDF5 file", e);
					}
				}
			} catch (IOException e) {
				EclipseLog.error("Failed to access HDF5 file", e, Activator.getDefault());
			}
		}
		if (imgPath == null) {
			FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN | SWT.MULTI);
			dialog.setText("Please select an ICS image");
			dialog.setFilterExtensions(new String[] { "*.ics" });
			imgPath = dialog.open();
		}
		if (imgPath == null) {
			return null;
		}

		// Build a list of data items representing the subwell data.
		List<VolumeDataItem> items = new ArrayList<VolumeDataItem>();
//		for (int i=0; i<100; i++) {
//			VolumeDataItem item = new VolumeDataItem();
//			item.setLabel(i);
//			item.setSize(10);
//			float x = (float)Math.random()*200;
//			float y = (float)Math.random()*50;
//			float z = (float)Math.random()*100;
//			item.setCoordinates(new float[] { x, y, z });
//			items.add(item);
//		}

		VolumeDataModel model = new VolumeDataModel(imgPath, items) {
			public void handleExternalSelection(ISelection sel) {
				//TODO
			}

			@Override
			public ISelection handleInternalSelection(int[] objectNames) {
				for (VolumeDataItem item : getItems()) {
					for (int name : objectNames) {
						item.setSelected(false);
						if (name == item.getLabel()) {
							item.setSelected(true);
							//TODO
							break;
						}
					}
				}
				return new StructuredSelection();
			}
		};

		try {
			VolumeViewEditorInput input = new VolumeViewEditorInput(model);
			page.openEditor(input, VolumeViewEditor.class.getName());
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to launch the editor " + VolumeViewEditor.class.getName(), e);
		}
		return null;
	}

}
