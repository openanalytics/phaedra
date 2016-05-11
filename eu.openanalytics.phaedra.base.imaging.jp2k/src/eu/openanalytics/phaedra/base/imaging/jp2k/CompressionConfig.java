package eu.openanalytics.phaedra.base.imaging.jp2k;

public class CompressionConfig {

	public int slope;
	public int psnr;
	public int resolutionLevels;
	public String order;
	public boolean reversible;
	public String precincts;
	
	public CompressionConfig() {
		// Defaults
		slope = 40000;
		psnr = 70;
		resolutionLevels = 9;
		order = "RPCL";
		reversible = false;
		precincts = "{256,256},{256,256},{128,128},{64,128},{32,64},{16,64},{8,32},{4,32},{2,16}";
	}

}
