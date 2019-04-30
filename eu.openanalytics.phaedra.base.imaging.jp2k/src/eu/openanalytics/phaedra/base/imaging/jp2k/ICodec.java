package eu.openanalytics.phaedra.base.imaging.jp2k;

public interface ICodec {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".codec";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_PREFERRED = "preferred";
	
	public String getName();
	
	public IEncodeAPI getEncoder();
	
	public IDecodeAPI getDecoder();
}
