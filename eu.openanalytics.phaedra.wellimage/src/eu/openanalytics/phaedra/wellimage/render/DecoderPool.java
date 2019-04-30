package eu.openanalytics.phaedra.wellimage.render;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import eu.openanalytics.phaedra.base.imaging.jp2k.CodecFactory;
import eu.openanalytics.phaedra.base.imaging.jp2k.IDecodeAPI;

public class DecoderPool extends GenericObjectPool<IDecodeAPI> {

	private static final long EVICTION_RUN_INTERVAL = 900000L; // Check for eviction every 15 minutes.
	
	public DecoderPool(long evictionDelay) {
		super(new DecoderFactory(), getConfig(evictionDelay));
	}
	
	private static GenericObjectPoolConfig getConfig(long evictionDelay) {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMinEvictableIdleTimeMillis(evictionDelay);
		config.setTimeBetweenEvictionRunsMillis(EVICTION_RUN_INTERVAL);
		return config;
	}
	
	private static class DecoderFactory extends BasePooledObjectFactory<IDecodeAPI> {

		@Override
		public IDecodeAPI create() throws Exception {
			IDecodeAPI decoder = CodecFactory.getDecoder();
			if (decoder != null) decoder.open();
			return decoder;
		}

		@Override
		public PooledObject<IDecodeAPI> wrap(IDecodeAPI decoder) {
			return new DefaultPooledObject<IDecodeAPI>(decoder);
		}
		
		@Override
		public void destroyObject(PooledObject<IDecodeAPI> decoder) throws Exception {
			decoder.getObject().close();
		}
	}
}
