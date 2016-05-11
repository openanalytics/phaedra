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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessUtils {

	/*
	 * *****************
	 * Process execution
	 * *****************
	 */

	public static int execute(String cmd) throws InterruptedException {
		return execute(new String[]{cmd}, null, null, null, null);
	}

	public static int execute(String cmd, String workingDir) throws InterruptedException {
		return execute(new String[]{cmd}, workingDir, null, null, null);
	}

	public static int execute(String[] cmd, String workingDir) throws InterruptedException {
		return execute(cmd, workingDir, null, null, null);
	}

	public static int execute(String[] cmd, String workingDir,
			final OutputStream stdOut, boolean failOnStdErr, boolean failOnExitNonZero) throws InterruptedException {

		return execute(cmd, workingDir, stdOut, failOnStdErr, failOnExitNonZero, 0);
	}

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

	public static int execute(String[] cmd, String workingDir, String[] environment,
			final OutputStream stdOut, final OutputStream stdErr) throws InterruptedException {

		return execute(cmd, workingDir, environment, stdOut, stdErr, 0);
	}

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

	/*
	 * ***************************
	 * System-dependent properties
	 * ***************************
	 *
	 * Note that most of these are Windows-specific.
	 */

	private static int isSystem64bit = -1;
	private static int physicalCoreCount = -1;
	private static int logicalCoreCount = -1;

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static boolean isSystem64Bit() {
		if (isSystem64bit == -1) {
			String output = null;
			if (isWindows()) {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					execute(new String[]{"cmd.exe", "/c", "systeminfo"}, null, out, true, true);
					output = out.toString();
					Pattern pattern = Pattern.compile("System Type: *(.*)");
					for (String line: output.split("\n")) {
						Matcher matcher = pattern.matcher(line);
						if (matcher.matches()) {
							output = matcher.group(1);
							break;
						}
					}
				} catch (Throwable ignore) {
					ignore.printStackTrace();
				}
			} else {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					execute(new String[]{"uname", "-m"}, null, out, true, true);
					output = out.toString();
				} catch (Throwable ignore) {}
			}
			if (output != null && output.contains("64")) {
				isSystem64bit = 1;
			} else {
				isSystem64bit = 0;
			}
		}
		return isSystem64bit == 1;
	}

	public static boolean isPlatform64Bit() {
		String arch = System.getProperty("os.arch");
		if (arch == null) return isSystem64Bit();
		return arch.contains("64");
	}

	public static int getPhysicalCores() {
		if (physicalCoreCount == -1) physicalCoreCount = getNumberOfCores("NumberOfCores");
		return physicalCoreCount;
	}

	public static int getLogicalCores() {
		if (logicalCoreCount == -1) logicalCoreCount = getNumberOfCores("NumberOfLogicalProcessors");
		return logicalCoreCount;
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private static void startChanneling(InputStream in, OutputStream out) {
		if (in != null && out != null) {
			Runnable channeler = new StreamChanneler(in, out);
			new Thread(channeler, "Stream Channel").start();
		}
	}

	/**
	 * Get the number of cores for the given type.
	 *
	 * @param typeOfCores NumberOfCores or NumberOfLogicalProcessors
	 * @return
	 */
	private static int getNumberOfCores(String typeOfCores) {
		int cores = 0;
		try {
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
