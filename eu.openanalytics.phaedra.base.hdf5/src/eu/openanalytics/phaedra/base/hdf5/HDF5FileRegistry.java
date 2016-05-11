package eu.openanalytics.phaedra.base.hdf5;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import eu.openanalytics.phaedra.base.util.CollectionUtils;

public class HDF5FileRegistry {

	private static List<BaseHDF5File> registeredFiles = new ArrayList<>();
	private static List<BaseHDF5File> closedFiles = new ArrayList<>();
	private static Lock registerLock = new ReentrantLock();
	
	public static void add(BaseHDF5File file) {
		registerLock.lock();
		CollectionUtils.addUnique(registeredFiles, file);
		registerLock.unlock();
	}
	
	public static void remove(BaseHDF5File file) {
		registerLock.lock();
		registeredFiles.remove(file);
		registerLock.unlock();
	}
	
	public static void closeAll(String path) {
		registerLock.lock();
		BaseHDF5File[] files = registeredFiles.toArray(new BaseHDF5File[registeredFiles.size()]);
		for (BaseHDF5File file: files) {
			if (file.getFilePath().endsWith(path)) {
				file.close();
				CollectionUtils.addUnique(closedFiles, file);
			}
		}
		registerLock.unlock();
	}
	
	public static void reopenAll(String path) {
		registerLock.lock();
		BaseHDF5File[] files = closedFiles.toArray(new BaseHDF5File[closedFiles.size()]);
		for (BaseHDF5File file: files) {
			if (file.getFilePath().endsWith(path)) {
				file.open();
				closedFiles.remove(file);
			}
		}
		registerLock.unlock();
	}
}
