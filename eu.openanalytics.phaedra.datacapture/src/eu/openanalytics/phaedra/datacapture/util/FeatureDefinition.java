package eu.openanalytics.phaedra.datacapture.util;

public class FeatureDefinition {

	public boolean addFeatureToProtocolClass;
	public String name;
	public boolean isKey;
	public boolean isNumeric;
	public boolean isLogarithmic;
	
	public FeatureDefinition(String name)
	{
		this.addFeatureToProtocolClass 	= true;
		this.name 						= name;
		this.isKey 						= false;
		this.isNumeric 					= true;
		this.isLogarithmic 				= false;
	}
	
		
	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */
	
	@Override
	public String toString()
	{
		String f = name+" (isKey="+isKey+"; isNumeric="+isNumeric+"; isLogarithmic="+isLogarithmic+")";
		if(addFeatureToProtocolClass)
			return "ADD: "+f;
		else
			return "DO NOT ADD: "+f;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		FeatureDefinition other = (FeatureDefinition) obj;
		return this.name.equals(other.name);
	}
}
