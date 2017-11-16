package eu.openanalytics.phaedra.base.util.process;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

/**
 * A collection of utilities related to OS processes, and their Java representation.
 */
public class ProcessUtils {
	
	private static int physicalCoreCount = -1;
	private static int logicalCoreCount = -1;

	/**
	 * See {@link ProcessUtils#execute(String[], String, String[], OutputStream, OutputStream, int)}
	 */
	public static int execute(String cmd) throws InterruptedException {
		return execute(new String[]{cmd}, null, null, null, null);
	}

	/**
	 * See {@link ProcessUtils#execute(String[], String, String[], OutputStream, OutputStream, int)}
	 */
	public static int execute(String cmd, String workingDir) throws InterruptedException {
		return execute(new String[]{cmd}, workingDir, null, null, null);
	}

	/**
	 * See {@link ProcessUtils#execute(String[], String, String[], OutputStream, OutputStream, int)}
	 */
	public static int execute(String[] cmd, String workingDir) throws InterruptedException {
		return execute(cmd, workingDir, null, null, null);
	}

	/**
	 * See {@link ProcessUtils#execute(String[], String, OutputStream, boolean, boolean, int)}
	 */
	public static int execute(String[] cmd, String workingDir,
			final OutputStream stdOut, boolean failOnStdErr, boolean failOnExitNonZero) throws InterruptedException {
		return execute(cmd, workingDir, stdOut, failOnStdErr, failOnExitNonZero, 0);
	}

	/**
	 * Execute a process, as a child of the current process.
	 * See also {@link ProcessUtils#execute(String[], String, String[], OutputStream, OutputStream, int)}
	 * 
	 * @param failOnStdErr If true, will throw an exception if the error output is not empty.
	 * @param failOnExitNonZero If true, will throw an exception if the return code is not 0. 
	 */
	public static int execute(String[] cmd, String workingDir,
			final OutputStream stdOut, boolean failOnStdErr, boolean failOnExitNonZero, int timeout) throws InterruptedException {

		ByteArrayOutputStream errStream = new ByteArrayOutputStream();
		int exitCode = execute(cmd, workingDir, null, stdOut, errStream, timeout);

		String error = errStream.toString();
		if (failOnExitNonZero && exitCode != 0) {
			throw new RuntimeException(cmd[0] + " failed (exit code " + exitCode + "): " + error);
		}
		if (failOnStdErr && error != null && !error.isEmpty()) {
			throw new RuntimeException(cmd[0] + " failed (exit code " + exitCode + "): " + error);
		}

		return exitCode;
	}

	/**
	 * See {@link ProcessUtils#execute(String[], String, String[], OutputStream, OutputStream, int)}
	 */
	public static int execute(String[] cmd, String workingDir, String[] environment,
			final OutputStream stdOut, final OutputStream stdErr) throws InterruptedException {
		return execute(cmd, workingDir, environment, stdOut, stdErr, 0);
	}

	/**
	 * Execute a native process, as a child of the current process.
	 * 
	 * @param cmd The full command to execute.
	 * @param workingDir The working directory for the process, or null to use the current working directory.
	 * @param environment The environment variables to set, or null to inherit the current environment variables.
	 * @param stdOut An OutputStream to send standard output to (optional).
	 * @param stdErr An OutputStream to send error output to (optional).
	 * @param timeout A timeout value to wait for the process to finish. If set, an exception will be thrown if
	 * the process does not finish in time. Set to 0 to disable.
	 * 
	 * @return The return code of the process.
	 * @throws InterruptedException If an interrupt was raised while waiting for the process to complete.
	 */
	public static int execute(String[] cmd, String workingDir, String[] environment,
			final OutputStream stdOut, final OutputStream stdErr, int timeout) throws InterruptedException {

		Process process = null;

		try	{
			// Set up the working dir.
			File wd = null;
			if (workingDir != null) wd = new File(workingDir);

			// Put quotes around arguments containing whitespace.
			for (int i=0; i<cmd.length; i++) {
				if (cmd[i].contains(" ")) cmd[i] = "\"" + cmd[i] + "\"";
			}

			// Launch the process.
			process = Runtime.getRuntime().exec(cmd,environment,wd);

			// Parse the process output in separate threads.
			startChanneling(process.getInputStream(), stdOut);
			startChanneling(process.getErrorStream(), stdErr);

			// Wait until the process returns.
			if (timeout <= 0) {
				process.waitFor();
			} else {
				boolean finishedInTime = process.waitFor(timeout, TimeUnit.MILLISECONDS);
				if (!finishedInTime) throw new RuntimeException("Process execution exceeded timeout (" + timeout + "ms): " + cmd[0]);
			}

			return process.exitValue();

		} catch (InterruptedException e) {
			if (process != null) try {process.destroy();} catch (Exception ignore) {}
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Process " + cmd[0] + " failed", e);
		}
	}

	/**
	 * Test if the operating system is Windows.
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	/**
	 * Test if the operating system is Mac OSX.
	 */
	public static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().contains("mac");
	}
	
	/**
	 * Get the number of physical CPU cores, as reported by the operating system.
	 */
	public static int getPhysicalCores() {
		if (physicalCoreCount == -1) physicalCoreCount = getNumberOfCores("NumberOfCores");
		return physicalCoreCount;
	}

	/**
	 * Get the number of logical CPU cores, as reported by the operating system.
	 * This may include hyperthreads.
	 */
	public static int getLogicalCores() {
		if (logicalCoreCount == -1) logicalCoreCount = getNumberOfCores("NumberOfLogicalProcessors");
		return logicalCoreCount;
	}

	private static void startChanneling(InputStream in, OutputStream out) {
		if (in != null && out != null) {
			Runnable channeler = new StreamChanneler(in, out);
			new Thread(channeler, "Stream Channel").start();
		}
	}

	private static int getNumberOfCores(String typeOfCores) {
		int cores = 0;
		try {
			//TODO Works on Windows only. Support other operating systems.
			ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
			String[] cmd = { "WMIC", "CPU", "Get", typeOfCores };
			execute(cmd, null, stdOut, true, true, 10000);
			String output = stdOut.toString();

			String[] lines = output.split("\\n");
			for (int i=1; i<lines.length; i++) {
				if (lines[i].trim().isEmpty()) continue;
				cores += Integer.parseInt(lines[i].trim());
			}
		} catch (Exception e) {
			// Failed to detect... fallback to Java cpu count.
		}

		if (cores < 1) return Runtime.getRuntime().availableProcessors();
		return cores;
	}

	private static class StreamChanneler implements Runnable {

		private InputStream in;
		private OutputStream out;

		public StreamChanneler(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			BufferedReader reader = null;
			OutputStreamWriter writer = null;

			try {
				String line;
				reader = new BufferedReader(new InputStreamReader(in));
				writer = new OutputStreamWriter(out);

				while ((line = reader.readLine()) != null) {
					writer.write(line+"\n");
					writer.flush();
				}
			} catch (Exception e) {
				// If an unexpected I/O error occurs, stop channeling.
			} finally {
				// Close the input stream that has no more data to send.
				if (in != null) try {in.close();} catch(IOException e) {}
			}
		}
	}
}
