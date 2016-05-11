package eu.openanalytics.phaedra.base.hook;

/**
 * <p>
 * Interface describing a hook into a hook point.
 * Hook points can be defined using the HookPoint extension point.
 * Hooks can be registered to specific hook points using the Hook extension point.
 * </p>
 * A hook point consists of a pre-hook call, an operation, and a post-hook call.
 */
public interface IHook {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".hook";
	public final static String ATTR_HOOK_ID = "hookPointId";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_PRIORITY = "priority";
	
	/**
	 * Called right before a hook point.
	 * The hook can prohibit the execution of the hook point by throwing a PreHookException.
	 * 
	 * @param args The arguments describing the hook point.
	 * @throws PreHookException If the execution of the hook point should be prohibited.
	 */
	public void pre(IHookArguments args) throws PreHookException;
	
	/**
	 * Called right after a hook point.
	 * Any exception thrown here is ignored by the hook point.
	 * 
	 * @param args The arguments describing the hook point.
	 */
	public void post(IHookArguments args);
}
