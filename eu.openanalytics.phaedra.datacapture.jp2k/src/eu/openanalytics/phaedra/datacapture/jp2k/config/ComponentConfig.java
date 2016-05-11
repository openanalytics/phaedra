package eu.openanalytics.phaedra.datacapture.jp2k.config;

import java.io.Serializable;

public class ComponentConfig implements Serializable{

	private static final long serialVersionUID = -1717529104164371834L;

	public int id;
	
	public boolean convert;
	public String convertArgs;
	public String convertArgsOnFail;
	public String convertFrame;
	
	public CompressionConfig compression;
	
	public ComponentFileConfig[] files;
}
