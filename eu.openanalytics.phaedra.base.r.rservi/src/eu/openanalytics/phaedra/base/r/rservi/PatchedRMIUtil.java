/*******************************************************************************
 * Copyright (c) 2009-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package eu.openanalytics.phaedra.base.r.rservi;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.statet.ecommons.rmi.core.RMIAddress;
import org.eclipse.statet.ecommons.rmi.core.RMIRegistry;
import org.eclipse.statet.ecommons.runtime.core.ECommonsRuntime;
import org.eclipse.statet.jcommons.lang.Disposable;


/**
 * Patched for Java 8 compatibility (or rather, JDK7.21).
 * See http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/enhancements-7.html
 */
public class PatchedRMIUtil {
	
	
	public static enum StopRule {
		
		/**
		 * Mode to stop the registry always automatically.
		 */
		ALWAYS,
		
		/**
		 * Mode to stop the registry never automatically.
		 */
		NEVER,
		
		/**
		 * Mode to stop the registry if empty.
		 */
		IF_EMPTY,
		
	}
	
	private static final class Port {
		
		private final int port;
		
		public Port(final int port) {
			this.port = port;
		}
		
		@SuppressWarnings("unused")
		public int get() {
			return this.port;
		}
		
		@Override
		public int hashCode() {
			return this.port << 8;
		}
		
		@Override
		public boolean equals(final Object obj) {
			return ((obj instanceof Port) && this.port == ((Port) obj).port);
		}
		
	}
	
	private static final class ManagedRegistry {
		
		final RMIRegistry registry;
		Process process;
		StopRule stopRule;
		
		public ManagedRegistry(final RMIRegistry registry, final StopRule stopRule) {
			this.registry = registry;
			this.stopRule = stopRule;
		}
		
	}
	
	
	private static final int EMBEDDED_PORT_FROM_DEFAULT = 51234;
	private static final int EMBEDDED_PORT_TO_DEFAULT = EMBEDDED_PORT_FROM_DEFAULT + 100;
	
	public static final PatchedRMIUtil INSTANCE = new PatchedRMIUtil(true);
	
	
	private final Map<Port, ManagedRegistry> registries = new HashMap<Port, ManagedRegistry>();
	
	private final Object embeddedLock = new Object();
	private int embeddedPortFrom;
	private int embeddedPortTo;
	private boolean embeddedStartSeparate = true;
	private ManagedRegistry embeddedRegistry;
	private List<String> embeddedClasspathEntries;
	private boolean embeddedClasspathLoadContrib;
	
	
	private PatchedRMIUtil() {
		this(false);
	}
	
	private PatchedRMIUtil(final boolean instance) {
		initDispose();
		
		embeddedPortFrom = EMBEDDED_PORT_FROM_DEFAULT;
		embeddedPortTo = EMBEDDED_PORT_TO_DEFAULT;
		if (instance) {
			embeddedClasspathLoadContrib = true;
			String s = System.getProperty("de.walware.ecommons.net.rmi.eRegistryPortrange");
			if (s != null && s.length() > 0) {
				final String from;
				final String to;
				final int idx = s.indexOf('-');
				if (idx >= 0) {
					from = s.substring(0, idx);
					to = s.substring(idx+1);
				}
				else {
					from = to = s;
				}
				try {
					embeddedPortFrom = Integer.parseInt(from);
					embeddedPortTo = Integer.parseInt(to);
					if (embeddedPortFrom > embeddedPortTo) {
						throw new IllegalArgumentException("from > to");
					}
				}
				catch (final IllegalArgumentException e) {
					embeddedPortFrom = EMBEDDED_PORT_FROM_DEFAULT;
					embeddedPortTo = EMBEDDED_PORT_TO_DEFAULT;
					ECommonsRuntime.getEnv().log(new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID,
							"The value of the Java property 'de.walware.ecommons.net.rmi.eRegistryPortrange' is invalid.", e ));
				}
			}
		}
	}
	
	protected void initDispose() {
		ECommonsRuntime.getEnv().addStoppingListener(new Disposable() {
			public void dispose() {
				PatchedRMIUtil.this.dispose();
			}
		});
	}
	
	
	/**
	 * Returns a handler for the RMI registry at the local host and the given port.
	 * 
	 * The registry must be started by this util instance.
	 * 
	 * @param port the registry port
	 * @return the registry handler or <code>null</code>
	 */
	public RMIRegistry getRegistry(int port) {
		if (port <= 0) {
			port = Registry.REGISTRY_PORT;
		}
		
		final Port key = new Port(port);
		final ManagedRegistry r;
		synchronized (this) {
			r = this.registries.get(key);
		}
		return (r != null) ? r.registry : null;
	}
	
	/**
	 * Sets the port for the managed embedded private RMI registry.
	 * 
	 * @param port the registry port
	 */
	public void setEmbeddedPrivatePort(final int port) {
		setEmbeddedPrivatePortDynamic(port, port);
	}
	
	/**
	 * Sets the valid port range for the managed embedded private RMI registry.
	 * An unused port for the registry is search inside this range.
	 * 
	 * @param from lowest valid registry port
	 * @param to highest valid the registry port
	 */
	public void setEmbeddedPrivatePortDynamic(final int from, final int to) {
		if (from > to) {
			throw new IllegalArgumentException("from > to");
		}
		synchronized (this.embeddedLock) {
			this.embeddedPortFrom = (from > 0) ? from : EMBEDDED_PORT_FROM_DEFAULT;
			this.embeddedPortTo = (to > 0) ? to : EMBEDDED_PORT_TO_DEFAULT;
		}
	}
	
	/**
	 * Sets the start mode for the managed embedded private RMI registry.
	 * 
	 * @param separate start registry in separate process
	 */
	public void setEmbeddedPrivateMode(final boolean separate) {
		synchronized (this.embeddedLock) {
			this.embeddedStartSeparate = separate;
		}
	}
	
	public void addEmbeddedClasspathEntry(final String entry) {
		synchronized (this.embeddedLock) {
			if (this.embeddedClasspathEntries == null) {
				this.embeddedClasspathEntries = new ArrayList<String>();
			}
			this.embeddedClasspathEntries.add(entry);
		}
	}
	
	
	/**
	 * Returns the managed embedded private RMI registry.
	 * 
	 * @return the registry or <code>null</code> if not available
	 */
	public RMIRegistry getEmbeddedPrivateRegistry(final IProgressMonitor monitor)
			throws CoreException {
		try {
			ManagedRegistry r = null;
			IStatus status = null;
			synchronized (this.embeddedLock) {
				if (this.embeddedRegistry != null) {
					return this.embeddedRegistry.registry;
				}
				
				monitor.beginTask("Starting registry (RMI)...", IProgressMonitor.UNKNOWN);
				
				if (this.embeddedClasspathLoadContrib && this.embeddedStartSeparate) {
					loadClasspathContrib();
				}
				
				int loop = 1;
				for (int port = this.embeddedPortFrom; ; ) {
					if (monitor.isCanceled()) {
						throw new CoreException(Status.CANCEL_STATUS);
					}
					try {
						final RMIAddress rmiAddress = new RMIAddress(RMIAddress.LOOPBACK, port, null);
						if (this.embeddedStartSeparate) {
							status = startSeparateRegistry(rmiAddress, false,
									(this.embeddedPortFrom == this.embeddedPortTo),
									StopRule.ALWAYS, this.embeddedClasspathEntries );
							if (status.getSeverity() < IStatus.ERROR) {
								r = this.registries.get(new Port(port));
							}
						}
						else {
							final Registry javaRegistry = LocateRegistry.createRegistry(port);
							final RMIRegistry registry = new RMIRegistry(rmiAddress, javaRegistry, false);
							r = new ManagedRegistry(registry, StopRule.NEVER);
							r.stopRule = StopRule.ALWAYS;
						}
						if (r != null) {
							this.embeddedRegistry = r;
							break;
						}
					}
					catch (final Exception e) {
						if (!(e.getCause() instanceof BindException)) {
							status = new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID,
									"An unknown exception was thrown when starting the embedded registry.", e);
						}
					}
					port++;
					if (port % 10 == 0) {
						port += 10;
					}
					if (port > this.embeddedPortTo) {
						if (loop == 1) {
							loop = 2;
							port = this.embeddedPortFrom + 10;
							if (port <= this.embeddedPortTo) {
								continue;
							}
						}
						break;
					}
				}
			}
			if (r != null) {
				final Port key = new Port(r.registry.getAddress().getPortNum());
				synchronized (this) {
					this.registries.put(key, r);
				}
				return r.registry;
			}
			if (status != null) {
				throw new CoreException(status);
			}
			return null;
		}
		finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}
	
	private void loadClasspathContrib() {
		// not available outside Eclipse
		this.embeddedClasspathLoadContrib = false;
	}
	
	
	public IStatus startSeparateRegistry(final RMIAddress address, final boolean allowExisting,
			final StopRule stopRule, final List<String> classpathEntries) {
		return startSeparateRegistry(address, allowExisting, false, stopRule, classpathEntries);
	}
	private IStatus startSeparateRegistry(RMIAddress address, final boolean allowExisting,
			final boolean noCheck,
			final StopRule stopRule, final List<String> classpathEntries) {
		if (allowExisting && classpathEntries != null && !classpathEntries.isEmpty()) {
			throw new IllegalArgumentException("allow existing not valid in combination with classpath entries");
		}
		final InetAddress hostAddress = address.getHostAddress();
		if (!(hostAddress.isLinkLocalAddress() || hostAddress.isLoopbackAddress())) {
			throw new IllegalArgumentException("address not local");
		}
		if (address.getName() != null) {
			try {
				address = new RMIAddress(address, null);
			} catch (final MalformedURLException e) {}
		}
		if (!noCheck) {
			try {
				checkRegistryAccess(address);
				if (allowExisting) {
					try {
						final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNum());
						registry.list();
						final Port key = new Port(address.getPortNum());
						synchronized (this) {
							if (!this.registries.containsKey(key)) {
								final ManagedRegistry r = new ManagedRegistry(new RMIRegistry(address, registry, false), StopRule.NEVER);
								this.registries.put(key, r);
							}
						}
						return new Status(IStatus.INFO, ECommonsRuntime.BUNDLE_ID,
								MessageFormat.format("RegistryAlreadyStarted", address.getPort()) );
					}
					catch (final RemoteException e) {}
				}
				return new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID,
						MessageFormat.format("RegistryStartFailedPortAlreadyUsed", address.getPort()) );
			}
			catch (final RemoteException e) {}
		}
		final Process process;
		try {
			final List<String> command = new ArrayList<String>();
			final StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append(System.getProperty("java.home")); //$NON-NLS-1$
			sb.append(File.separator).append("bin"); //$NON-NLS-1$
			sb.append(File.separator).append("rmiregistry"); //$NON-NLS-1$
			command.add(sb.toString());
			command.add(address.getPort());
			if (classpathEntries != null && !classpathEntries.isEmpty()) {
				command.add("-J-classpath");
				sb.setLength(0);
				sb.append("-J");
				sb.append(classpathEntries.get(0));
				for (int i = 1; i < classpathEntries.size(); i++) {
					sb.append(File.pathSeparatorChar);
					sb.append(classpathEntries.get(i));
				}
				command.add(sb.toString());
			}
			if (address.getHost() != null) {
				sb.setLength(0);
				sb.append("-J-Djava.rmi.server.hostname=");
				sb.append(address.getHost());
				command.add(sb.toString());
			}
			// Patched:
			command.add("-J-Djava.rmi.server.useCodebaseOnly=false");
			process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
		}
		catch (final Exception e) {
			return new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID, MessageFormat.format("RegistryStartFailed", address.getPort()), e);
		}
		
		RemoteException lastException = null;
		for (int i = 1; ; i++) {
			try {
				final int exit = process.exitValue();
				return new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID, MessageFormat.format("RegistryStartFailedWithExitValue", address.getPort(), exit));
			}
			catch (final IllegalThreadStateException e) {
			}
			if (i > 1) {
				try {
					checkRegistryAccess(address);
					final Registry registry = LocateRegistry.getRegistry(address.getHost(), address.getPortNum());
					registry.list();
					final ManagedRegistry r = new ManagedRegistry(new RMIRegistry(address, registry, true),
							(stopRule != null) ? stopRule : StopRule.IF_EMPTY);
					r.process = process;
					final Port key = new Port(address.getPortNum());
					synchronized (this) {
						this.registries.put(key, r);
					}
					return Status.OK_STATUS;
				}
				catch (final RemoteException e) {
					lastException = e;
				}
			}
			
			if (Thread.interrupted()) {
				process.destroy();
				return Status.CANCEL_STATUS;
			}
			if (i >= 25) {
				process.destroy();
				return new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID, MessageFormat.format("RegistryStartFailed", address.getPort()), lastException);
			}
			try {
				Thread.sleep(50);
				continue;
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	public IStatus stopSeparateRegistry(final RMIAddress address) {
		return stopSeparateRegistry(address.getPortNum());
	}
	
	public IStatus stopSeparateRegistry(int port) {
		if (port <= 0) {
			port = Registry.REGISTRY_PORT;
		}
		
		final Port key = new Port(port);
		final ManagedRegistry r;
		synchronized (this) {
			r = this.registries.get(key);
			if (r == null || r.process == null) {
				return new Status(IStatus.ERROR, ECommonsRuntime.BUNDLE_ID, MessageFormat.format("RegistryStopFailedNotFound", port));
			}
			this.registries.remove(key);
		}
		
		r.process.destroy();
		return Status.OK_STATUS;
	}
	
	
	protected void dispose() {
		synchronized(this) {
			for (final ManagedRegistry r : this.registries.values()) {
				if (r.process == null) {
					continue;
				}
				switch (r.stopRule) {
				case ALWAYS:
					break;
				case NEVER:
					continue;
				case IF_EMPTY:
					try {
						final Registry registry = LocateRegistry.getRegistry(r.registry.getAddress().getPortNum());
						if (registry.list().length > 0) {
							continue;
						}
					}
					catch (final RemoteException e) {}
					break;
				}
				r.process.destroy();
				r.process = null;
			}
			this.registries.clear();
		}
	}
	
	
	public static final int ACCESS_TIMEOUT;
	
	public static void checkRegistryAccess(final RMIAddress address) throws RemoteException {
		Socket socket = null;
		try {
			socket = new Socket();
			try {
				socket.setSoTimeout(ACCESS_TIMEOUT);
			}
			catch (final SocketException e) {}
			socket.connect(new InetSocketAddress(address.getHostAddress(), address.getPortNum()),
					ACCESS_TIMEOUT);
		}
		catch (final IOException e) {
			throw new RemoteException("Cannot connect to RMI registry.", e);
		}
		finally {
			if (socket != null && !socket.isClosed()) {
				try {
					socket.close();
				}
				catch (final IOException e) {}
			}
		}
	}
	
	
	static {
		int timeout = 15000;
		String s = System.getProperty("de.walware.ecommons.net.rmi.accessTimeout");
		if (s != null) {
			try {
				timeout = Integer.parseInt(s);
			} catch (Exception e) {}
		}
		ACCESS_TIMEOUT = timeout;
	}
	
}