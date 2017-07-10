package eu.openanalytics.phaedra.link.data.image;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkException;
import eu.openanalytics.phaedra.link.data.IDataLinkerComponent;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

/**
 * Moves the captured image from the capture store to the plate data store.
 * Also sets the 'Image Available' flag on the plate.
 */
public class ImageDataLinker implements IDataLinkerComponent {

	private boolean imgDataAvailable;
	
	private Plate plate;
	private String imageFile;
	
	@Override
	public void prepareLink(PlateReading reading, HDF5File dataFile, Plate destination, IProgressMonitor monitor) throws DataLinkException {
		this.plate = destination;
		try {
			imageFile = DataCaptureService.getInstance().getImagePath(reading);
			imgDataAvailable = (imageFile != null);
		} catch (IOException e) {
			throw new DataLinkException("Failed to verify image " + imageFile, e);
		}
	}

	@Override
	public void executeLink() throws DataLinkException {
		if (imgDataAvailable) {
			String ext = FileUtils.getExtension(imageFile);
			String destination = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + "." + ext;
			try {
				Screening.getEnvironment().getFileServer().renameAndReplace(imageFile, destination);
			} catch (IOException e) {
				throw new DataLinkException("Image upload failed for " + plate, e);
			}
		}
		
		plate.setImageAvailable(imgDataAvailable || plate.isImageAvailable());
		PlateService.getInstance().updatePlate(plate);
	}

	@Override
	public void rollback() {
		// Nothing to roll back.
	}

}
