package eu.openanalytics.phaedra.base.util.process.comm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import eu.openanalytics.phaedra.base.util.Activator;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import eu.openanalytics.phaedra.base.util.threading.ThreadPool;

/**
 * <p>
 * Manages a set of slave processes.
 * Incoming requests (via <code>sendRequest</code>) are passed to an idle slave and processed there.
 * The response is returned to the caller.
 * </p><p>
 * How the requests are processed, is determined by the ILocalProcessProtocol whose class name is passed
 * to the constructor of this class.
 * </p><p>
 * Note that <code>sendRequest</code> is blocking: it will wait until the response is available,
 * possibly also waiting for a slave to become idle.
 * </p>
 */
public class LocalProcessMaster {

	private SlaveManager[] slaveManagers;
	private BlockingQueue<SlaveManager> idleSlaves;
	private ThreadPool threadPool;

	private String javaPath;
	private String cp;
	private String className;
	private String protocolName;
	private String wd;

	private static String folderSeparator = ProcessUtils.isWindows() ? "\\" : "/";
	private static String javaExecutable = ProcessUtils.isWindows() ? "\\bin\\javaw.exe" : "/bin/java";
	
	public LocalProcessMaster(int slaves, String[] plugins, String[] extraWDFiles, String protocolName) {
		javaPath = getJavaExecutable();
		className = LocalProcessSlave.class.getName();
		if (protocolName == null) protocolName = BaseLocalProcessProtocol.class.getName();
		this.protocolName = protocolName;

		if (plugins == null) plugins = new String[0];
		String[] allPlugins = new String[plugins.length + 1];
		allPlugins[0] = Activator.PLUGIN_ID;
		for (int i = 0; i < plugins.length; i++) allPlugins[i+1] = plugins[i];
		cp = createClassPath(allPlugins);

		wd = FileUtils.generateTempFolder(true);
		copyExtraWDFiles(extraWDFiles, wd);

		slaveManagers = new SlaveManager[slaves];
	}

	public void startup() {
		for (int i = 0; i < slaveManagers.length; i++) {
			slaveManagers[i] = new SlaveManager();
		}
		threadPool = new ThreadPool(slaveManagers.length);
		idleSlaves = new LinkedBlockingQueue<>(slaveManagers.length);

		for (int i = 0; i < slaveManagers.length; i++) {
			threadPool.schedule(slaveManagers[i]);
			idleSlaves.add(slaveManagers[i]);
		}
	}

	public void shutdown() {
		threadPool.stop(false);
	}

	public String sendRequest(String req) {
		SlaveManager idleSlave = null;
		try {
			idleSlave = idleSlaves.take();
			String response = idleSlave.sendRequest(req);
			return response;
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting for an idle slave");
		} finally {
			try { idleSlaves.put(idleSlave); } catch (InterruptedException e) {}
		}
	}

	public int getSlaveCount() {
		return slaveManagers.length;
	}

	/*
	 * Non-public
	 * **********
	 */

	private static void copyExtraWDFiles(String[] files, String wd) {
		if (files == null || files.length == 0) return;
		try {
			for (String file: files) {
				String plugin = Activator.PLUGIN_ID;
				if (file.startsWith("plugin://")) {
					String subPath = file.substring("plugin://".length());
					plugin = subPath.split("/")[0];
					file = subPath.substring(subPath.indexOf('/'));
				}
				FileUtils.copyFromPlugin(file, plugin, wd);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to prepare multiprocess working directory", e);
		}
	}

	private static String getJavaExecutable() {
		String javaPath = "jre" + javaExecutable;
		if (!new File(javaPath).exists()) {
			javaPath = System.getProperty("java.home") + javaExecutable;
		}
		javaPath = new File(javaPath).getAbsolutePath();
		return javaPath;
	}

	private static String createClassPath(String[] pluginIds) {
		StringBuilder cpBuilder = new StringBuilder();
		for (String id: pluginIds) {
			cpBuilder.append(resolvePluginClassPath(id) + ";");
		}
		return cpBuilder.toString();
	}

	private static String resolvePluginClassPath(String id) {
		Bundle bundle = Platform.getBundle(id);
		String location = bundle.getLocation();
		String prefix = "reference:file:";
		if (location.startsWith(prefix)) {
			location = location.substring(prefix.length());
		}
		location = location.replace("/", folderSeparator);
		if (ProcessUtils.isWindows() && location.startsWith(folderSeparator)) {
			location = location.substring(1);
		}
		if (location.endsWith("\\")) {
			location = location.substring(0, location.length()-1);
		}

		if (ProcessUtils.isMac()) {
			//Workaround for Mac: bundle.getLocation() returns a path relative to the install area.
			try {
				String eclipsePath = new URL(System.getProperty("osgi.install.area")).getFile();
				location = eclipsePath + location;
			} catch (MalformedURLException e) {}
		}
		
		if (location.endsWith(".jar")) {
			// Workaround: Extract the jar to a temporary location.
			// Otherwise, the process' class loader (which is not a BundleClassLoader)
			// will miss things such as extra class folders and nested jars.
			String tempPath = FileUtils.generateTempFolder(true);
			try {
				FileUtils.unzip(location, tempPath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to prepare analysis files", e);
			}
			location = tempPath;
		}

		List<String> cpDirs = new ArrayList<String>();

		// If there are fragments, prepend them to the classpath (priority over host)
		Bundle[] fragments = Platform.getFragments(bundle);
		if (fragments != null) {
			for (Bundle fragment: fragments) {
				String fragmentCp = resolvePluginClassPath(fragment.getSymbolicName());
				cpDirs.add(fragmentCp);
			}
		}

		cpDirs.add(new File(location).getAbsolutePath());

		File[] children = new File(location).listFiles();
		for (File child: children) {
			String name = child.getName().toLowerCase();
			if (name.equals("bin")) cpDirs.add(child.getAbsolutePath());
			else if (name.endsWith("classes")) cpDirs.add(child.getAbsolutePath());
			else if (name.equals("lib")) cpDirs.add(child.getAbsolutePath() + "\\*");
		}

		if (!cpDirs.isEmpty()) {
			String cp = "";
			for (String dir: cpDirs) {
				cp += dir + ";";
			}
			location = cp.substring(0, cp.length()-1);
		}

		return location;
	}

	private class SlaveManager implements Runnable {

		private int portNr;
		private BlockingQueue<String> requestQueue;
		private BlockingQueue<String> responseQueue;
		private volatile boolean idle;

		public SlaveManager() {
			requestQueue = new LinkedBlockingQueue<>(1);
			responseQueue = new LinkedBlockingQueue<>(1);
		}

		@Override
		public void run() {

			String[] cmd = new String[5];
			cmd[0] = javaPath;
			cmd[1] = "-cp";
			cmd[2] = cp;
			cmd[3] = className;
			cmd[4] = protocolName;

			Process p = null;
			try {
				p = Runtime.getRuntime().exec(cmd, null, new File(wd));
			} catch (Exception e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				throw new RuntimeException("Failed to launch slave process", e);
			}

			try (BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				portNr = Integer.parseInt(stdOutReader.readLine());
			} catch (IOException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				throw new RuntimeException("Failed to determine slave TCP port", e);
			}

			// Give the slave some time to set up a socket.
			try { Thread.sleep(2000); } catch (InterruptedException e) {}

			try (Socket conn = new Socket(InetAddress.getByName(null), portNr)) {
				PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

				// Send a hello command to see if the slave is alive and listening
				out.println(BaseLocalProcessProtocol.CMD_HELLO);
				in.readLine();

				boolean running = true;
				idle = true;
				while (running) {
					// Wait for an incoming request.
					String request = requestQueue.take();
					// Get to work and process the request.
					out.println(request);
					String response = in.readLine();
					if (response == null) response = "";
					if (request.equals(BaseLocalProcessProtocol.CMD_SHUTDOWN)) running = false;
					responseQueue.put(response);
				}
			} catch (Exception e) {
				try { responseQueue.put("ERROR: " + e.getMessage()); } catch (InterruptedException e1) {}
				restart();
				throw new RuntimeException("Failed to connect to slave process", e);
			}
		}

		public String sendRequest(String req) {
			if (!idle) throw new RuntimeException("Cannot send request: slave is not idle");
			try {
				idle = false;
				requestQueue.put(req);
				return responseQueue.take();
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for response", e);
			} finally {
				idle = true;
			}
		}

		private void restart() {
			threadPool.schedule(this);
			requestQueue.clear();
			responseQueue.clear();
		}
	}
}
