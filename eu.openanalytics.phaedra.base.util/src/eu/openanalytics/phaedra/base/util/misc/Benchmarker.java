package eu.openanalytics.phaedra.base.util.misc;

public class Benchmarker {

	private static long start;
	private static String marker = "--";
	
	public static void start() {
		start = System.nanoTime();
	}

	public static void stop() {
		stop(null);
	}
	
	public static void stop(String msg) {
		long durationNano = System.nanoTime() - start;
		long durationMs = (long) (durationNano / 1e6);
		String line = (marker == null ? "" : marker) + (msg == null ? "" : msg) + " Duration (ms): ";
		System.out.println(line + durationMs);
	}
	
	public static void setMarker(String marker) {
		Benchmarker.marker = marker;
	}

}

