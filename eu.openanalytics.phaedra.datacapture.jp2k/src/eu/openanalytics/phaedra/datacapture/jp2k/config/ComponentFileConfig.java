package eu.openanalytics.phaedra.datacapture.jp2k.config;

import java.io.Serializable;

public class ComponentFileConfig implements Serializable {

	private static final long serialVersionUID = 5223646961654013693L;
	public String path;
	public String pattern;
	public String patternIdGroups;
	public int idGroup;
}
