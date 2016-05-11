package eu.openanalytics.phaedra.base.util.misc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class RetryingUtils {

	private static Logger log = Logger.getLogger(RetryingUtils.class);

	public static void doRetrying(RetryingBlock runnable, int tries) throws Exception {
		doRetrying(runnable, tries, 0);
	}
	
	public static void doRetrying(RetryingBlock runnable, int tries, int delay) throws Exception {
		int currentTry = 1;
		Exception caughtException = null;
		while (currentTry <= tries) {
			try {
				runnable.run();
				return;
			} catch (Exception e) {
				log.info("RetryingBlock try " + currentTry + "/" + tries + " failed.");
				caughtException = e;
				
				if (delay > 0) {
					try { Thread.sleep(delay); } catch (InterruptedException ie) {}
				}
				
				currentTry++;
			}
		}
		throw caughtException;
	}
	
	public static interface RetryingBlock {
		public void run() throws Exception;
	}
	
	public static class RetryingInputAccessor {
		public InputStream getInput() throws IOException {
			return null;
		}
	}
}
