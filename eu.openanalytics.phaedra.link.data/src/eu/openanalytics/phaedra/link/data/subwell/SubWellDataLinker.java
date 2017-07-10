package eu.openanalytics.phaedra.link.data.subwell;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.misc.RetryingUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkException;
import eu.openanalytics.phaedra.link.data.IDataLinkerComponent;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This component links the following items to the target plate:
 * <ul>
 * <li>Subwell data: all subwell-feature values available in the HDF5 file.</li>
 * <li>Subwell data stats: all subwell-feature value statistics available in the HDF5 file.</li>
 * <li>Extra data: extra files found in the HDF5 file such as SVG charts.</li>
 * <li>Meta data: extra well information such as FCS keywords, will be stored as well properties.</li>
 * </ul>
 */
public class SubWellDataLinker implements IDataLinkerComponent {

	private boolean dataAvailable;
	
	private Plate plate;
	private HDF5File dataFile;
	private String dataPath;
	
	@Override
	public void prepareLink(PlateReading reading, HDF5File dataFile, Plate destination, IProgressMonitor monitor) throws DataLinkException {
		this.plate = destination;
		this.dataFile = dataFile;
		this.dataPath = reading.getCapturePath();
		dataAvailable = dataFile.existsSubWellData();
	}

	@Override
	public void executeLink() throws DataLinkException {
		if (dataAvailable) {
			// Attempt to rename, retrying if the file is in use (by the HDF5 sync threads).
			String destination = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
			try {
				RetryingUtils.doRetrying(() -> {
					dataFile.close();
					Screening.getEnvironment().getFileServer().renameAndReplace(dataPath, destination);
				}, 20, 2000);
			} catch (Exception e) {
				throw new DataLinkException("Subwell data link failed for " + plate, e);
			}
		}
		
		plate.setSubWellDataAvailable(dataAvailable || plate.isSubWellDataAvailable());
		PlateService.getInstance().updatePlate(plate);
		
		// If needed, trigger a subwell data calculation.
		boolean triggerSubwellCalculation = false;
		List<Feature> features = PlateUtils.getProtocolClass(plate).getFeatures();
		for (Feature f: features) {
			if (f.isCalculated() && CalculationTrigger.SubwellDataChange.toString().equals(f.getCalculationTrigger())) {
				triggerSubwellCalculation = true;
				break;
			}
		}
		if (triggerSubwellCalculation) CalculationService.getInstance().triggerSubWellCalculation(plate);
	}

	@Override
	public void rollback() {
		// Nothing to roll back.
	}
}
