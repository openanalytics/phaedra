package eu.openanalytics.phaedra.base.ui.charting.v2.data;

public interface IJEPAwareDataProvider {

	public String[] getJepExpressions();
	
	public void setJepExpressions(String[] newExpressions);

}