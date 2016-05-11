package eu.openanalytics.phaedra.base.cache.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.sf.ehcache.Cache;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import eu.openanalytics.phaedra.base.cache.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class EhCacheDisk extends EhCacheHeap {

	public EhCacheDisk(String name, Cache cache) {
		super(name, cache);
	}

	@Override
	public Object get(Object key) {
		return convertToHeap(super.get(key));
	}
	
	@Override
	public Object put(Object key, Object value) {
		return super.put(key, convertToDisk(value));
	}
	
	private Object convertToDisk(Object o) {
		if (o == null || o instanceof Serializable) return o;
		if (o instanceof ImageData) return new ImageDataSerializable((ImageData) o);
		// Other types cannot be cached to disk.
		EclipseLog.warn("Cannot cache non-serializable object in disk cache: " + o, Activator.getDefault());
		return null;
	}
	
	private Object convertToHeap(Object o) {
		if (o instanceof ImageDataSerializable) return ((ImageDataSerializable) o).imageData;
		return o;
	}
	
	// Note: public class and exported package, because ehcache needs access to this class.
	public static class ImageDataSerializable implements Serializable {

		private static final long serialVersionUID = 4275079518703362583L;

		private transient ImageData imageData;

		public ImageDataSerializable(ImageData imageData) {
			this.imageData = imageData;
		}

		private byte[] imageDataToByteArray() {
			if (imageData != null) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try (DataOutputStream writeOut = new DataOutputStream(out)) {
					ImageLoader loader = new ImageLoader();
					loader.data = new ImageData[] { imageData };
					loader.save(writeOut, SWT.IMAGE_PNG);
					return out.toByteArray();
				} catch (IOException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
			}
			return new byte[0];
		}

		private ImageData byteArrayToImageData(byte[] buffer) {
			ByteArrayInputStream in = new ByteArrayInputStream(buffer);
			ImageLoader loader = new ImageLoader();
			ImageData[] imageDatas = loader.load(in);
			return imageDatas[0];
		}

		private void writeObject(ObjectOutputStream oos) throws IOException {
			byte[] buffer = imageDataToByteArray();
			oos.writeObject(buffer);
		}

		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			byte[] buffer = (byte[]) ois.readObject();
			if (buffer.length == 0) return;
			this.imageData = byteArrayToImageData(buffer);
		}
	}

}
