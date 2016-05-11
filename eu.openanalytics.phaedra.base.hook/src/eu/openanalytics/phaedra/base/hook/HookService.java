package eu.openanalytics.phaedra.base.hook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class HookService {

	private static HookService instance;
	
	private Map<String, HookPoint> hookIdMap;
	private Map<HookPoint, HookList> hookMap;
	
	private HookService() {
		// Hidden constructor.
		loadHooks();
	}
	
	public static synchronized HookService getInstance() {
		if (instance == null) instance = new HookService();
		return instance;
	}
	
	public void runPre(String hookPointId, IHookArguments args) throws PreHookException {
		IHook[] hooks = getHooks(hookPointId);
		for (IHook hook: hooks) {
			try {
				hook.pre(args);
			} catch (PreHookException e) {
				throw e;
			} catch (Throwable e) {
				logWarning("Pre-hook " + hook.getClass().getName() + " failed: " + e.getMessage(), e);
			}
		}
	}
	
	public void runPost(String hookPointId, IHookArguments args) {
		IHook[] hooks = getHooks(hookPointId);
		for (IHook hook: hooks) {
			try {
				hook.post(args);
			} catch (Throwable e) {
				logWarning("Post-hook " + hook.getClass().getName() + " failed: " + e.getMessage(), e);
			}
		}
	}
	
	public void startBatch(String hookPointId, IBatchedHookArguments args) {
		IHook[] hooks = getHooks(hookPointId);
		for (IHook hook: hooks) {
			if (hook instanceof IBatchedHook) {
				try {
					((IBatchedHook)hook).startBatch(args);
				} catch (Throwable e) {
					logWarning("Batch start of hook " + hook.getClass().getName() + " failed: " + e.getMessage(), e);
				}
			}
		}
	}
	
	public void endBatch(String hookPointId) {
		endBatch(hookPointId, true);
	}
	
	public void endBatch(String hookPointId, boolean successful) {
		IHook[] hooks = getHooks(hookPointId);
		for (IHook hook: hooks) {
			if (hook instanceof IBatchedHook) {
				try {
					((IBatchedHook)hook).endBatch(successful);
				} catch (Throwable e) {
					logWarning("Batch end of hook " + hook.getClass().getName() + " failed: " + e.getMessage(), e);
				}
			}
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private final static IHook[] NO_HOOKS = new IHook[0];
	
	private IHook[] getHooks(String hookPointId) {
		HookPoint hookPoint = hookIdMap.get(hookPointId);
		if (hookPoint == null) return NO_HOOKS;
		HookList list = hookMap.get(hookPoint);
		IHook[] hooks = list.getHooks();
		if (hooks == null) return NO_HOOKS;
		return hooks;
	}
	
	private void loadHooks() {
		hookIdMap = new HashMap<>();
		hookMap = new HashMap<>();
		
		// Fetch all hook points.
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(HookPoint.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			String id = el.getAttribute(HookPoint.ATTR_ID);
			HookPoint hookPoint = new HookPoint();
			hookPoint.setId(id);
			hookIdMap.put(id, hookPoint);
			hookMap.put(hookPoint, new HookList());
		}
		
		// Fetch all hooks.
		config = Platform.getExtensionRegistry().getConfigurationElementsFor(IHook.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IHook.ATTR_CLASS);
				if (o instanceof IHook) {
					IHook hook = (IHook)o;
					String hookPointId = el.getAttribute(IHook.ATTR_HOOK_ID);
					String priorityString = el.getAttribute(IHook.ATTR_PRIORITY);
					int priority = 10;
					if (priorityString != null && NumberUtils.isDouble(priorityString)) {
						priority = Integer.parseInt(priorityString);
					}
					HookPoint hookPoint = hookIdMap.get(hookPointId);
					if (hookPoint != null) {
						HookList list = hookMap.get(hookPoint);
						list.add(hook, priority);
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		
		// Sort hooks by priority.
		for (HookList list: hookMap.values()) {
			list.sort();
		}
	}
	
	private void logWarning(String message, Throwable cause) {
		Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, message, cause));
	}
	
	private static class HookList {
		private List<IHook> hooks;
		private Map<IHook, Integer> priorities;
		private IHook[] sortedHooks;
		
		public HookList() {
			hooks = new ArrayList<>();
			priorities = new HashMap<>();
			sortedHooks = new IHook[0];
		}
		
		public void add(IHook hook, int priority) {
			hooks.add(hook);
			priorities.put(hook, priority);
		}
		
		public void sort() {
			List<IHook> sortedHookList = new ArrayList<>(hooks);
			Collections.sort(sortedHookList, new Comparator<IHook>() {
				@Override
				public int compare(IHook o1, IHook o2) {
					if (o1 == null) return -1;
					if (o2 == null) return 1;
					int p1 = priorities.get(o1);
					int p2 = priorities.get(o2);
					return p1 - p2;
				}
			});
			sortedHooks = sortedHookList.toArray(new IHook[sortedHookList.size()]);
		}
		
		public IHook[] getHooks() {
			return sortedHooks;
		}
	}
}
