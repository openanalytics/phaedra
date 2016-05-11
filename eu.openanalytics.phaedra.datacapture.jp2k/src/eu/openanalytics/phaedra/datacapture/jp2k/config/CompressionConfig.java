package eu.openanalytics.phaedra.datacapture.jp2k.config;

import java.io.Serializable;

public class CompressionConfig implements Serializable {

	private static final long serialVersionUID = -7121446744703599564L;
	
	public String type;
	public int levels;
	public String order;
	public int slope;
	public int psnr;
	public String precincts;
}
