package eu.openanalytics.phaedra.base.search;

public final class QueryNameGenerator {
	private static int queryIndex = 1;
	
	private QueryNameGenerator() {		
	}
	
	public static String generateQueryName() {
		String queryName = "Query " + queryIndex;
		queryIndex++;
		return queryName;
	}
}
