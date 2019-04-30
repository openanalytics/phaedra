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
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ImageChannelPool extends GenericKeyedObjectPool<Plate, SeekableByteChannel> {

	private static final long EVICTION_RUN_INTERVAL = 900000L; // Check for eviction every 15 minutes.
	
	public ImageChannelPool(int maxChannelsPerPlate, long evictionDelay) {
		super(new ImageChannelFactory(), getConfig(maxChannelsPerPlate, evictionDelay));
	}

	private static GenericKeyedObjectPoolConfig getConfig(int maxChannelsPerPlate, long evictionDelay) {
		GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
		config.setMinEvictableIdleTimeMillis(evictionDelay);
		config.setMaxTotalPerKey(maxChannelsPerPlate);
		config.setTimeBetweenEvictionRunsMillis(EVICTION_RUN_INTERVAL);
		return config;
	}
	
	private static class ImageChannelFactory extends BaseKeyedPooledObjectFactory<Plate, SeekableByteChannel> {

		@Override
		public SeekableByteChannel create(Plate plate) throws Exception {
			ProtocolClass pClass = PlateUtils.getProtocolClass(plate);
			List<ImageChannel> imageChannels = pClass.getImageSettings().getImageChannels();
			if (imageChannels == null || imageChannels.isEmpty()) throw new IOException("Cannot render image: no image channels configured for protocol class" + pClass);

			String imagePath = PlateService.getInstance().getImageFSPath(plate);
			SeekableByteChannel byteChannel = null;
			if (imagePath != null) byteChannel = Screening.getEnvironment().getFileServer().getChannel(imagePath, "r");
			if (byteChannel == null) throw new IOException("Cannot render image: plate " + plate + " has no image data available");
			
			return byteChannel;
		}

		@Override
		public PooledObject<SeekableByteChannel> wrap(SeekableByteChannel channel) {
			return new DefaultPooledObject<SeekableByteChannel>(channel);
		}
		
		@Override
		public void destroyObject(Plate plate, PooledObject<SeekableByteChannel> channel) throws Exception {
			channel.getObject().close();
		}
	}
}
