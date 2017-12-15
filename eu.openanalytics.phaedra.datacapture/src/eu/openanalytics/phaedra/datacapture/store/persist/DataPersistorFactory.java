package eu.openanalytics.phaedra.datacapture.store.persist;

public class DataPersistorFactory {

	public static IDataPersistor[] createPersistors() {
		return new IDataPersistor[] {
			new PlateDataPersistor(),
			new WellDataPersistor()
		};
	}
}
