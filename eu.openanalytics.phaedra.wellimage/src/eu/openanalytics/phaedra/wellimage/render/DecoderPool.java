package eu.openanalytics.phaedra.wellimage.render;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.List;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class DecoderPool extends GenericKeyedObjectPool<Plate, IDecodeAPI> {

	private static final long EVICTION_RUN_INTERVAL = 900000L; // Check for eviction every 15 minutes.
	
	public DecoderPool(int maxDecodersPerPlate, long evictionDelay) {
		super(new DecoderFactory(), getConfig(maxDecodersPerPlate, evictionDelay));
	}
	
	private static GenericKeyedObjectPoolConfig getConfig(int maxDecodersPerPlate, long evictionDelay) {
		GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
		config.setMaxTotalPerKey(maxDecodersPerPlate);
		config.setMinEvictableIdleTimeMillis(evictionDelay);
		config.setTimeBetweenEvictionRunsMillis(EVICTION_RUN_INTERVAL);
		return config;
	}
	
	private static class DecoderFactory extends BaseKeyedPooledObjectFactory<Plate, IDecodeAPI> {

		@Override
		public IDecodeAPI create(Plate plate) throws Exception {
			ProtocolClass pClass = PlateUtils.getProtocolClass(plate);
			List<ImageChannel> imageChannels = pClass.getImageSettings().getImageChannels();
			if (imageChannels == null || imageChannels.isEmpty()) throw new IOException("Cannot render image: no image channels configured for protocol class" + pClass);

			String imagePath = PlateService.getInstance().getImageFSPath(plate);
			SeekableByteChannel byteChannel = Screening.getEnvironment().getFileServer().getChannel(imagePath, "r");
			if (byteChannel == null) throw new IOException("Cannot render image: plate " + plate + " has no image data available");
			
			IDecodeAPI decoder = CodecFactory.getDecoder(byteChannel, PlateUtils.getWellCount(plate), imageChannels.size());
			if (decoder != null) decoder.open();
			return decoder;
		}

		@Override
		public PooledObject<IDecodeAPI> wrap(IDecodeAPI decoder) {
			return new DefaultPooledObject<IDecodeAPI>(decoder);
		}
		
		@Override
		public void destroyObject(Plate key, PooledObject<IDecodeAPI> decoder) throws Exception {
			decoder.getObject().close();
		}
	}
}
