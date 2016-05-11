package eu.openanalytics.phaedra.base.util.process.comm;

public class BaseLocalProcessProtocol implements ILocalProcessProtocol {

	public final static String CMD_HELLO = "hello";
	public static final String CMD_SHUTDOWN = "shutdown";
	
	protected static final String RES_OK = "ok";
	protected static final String RES_UNKNOWN_CMD = "unknown_cmd";
	
	@Override
	public String process(String input) {
		if (CMD_HELLO.equals(input)) return RES_OK;
		else if (CMD_SHUTDOWN.equals(input)) return RES_OK;
		else return RES_UNKNOWN_CMD;
	}
}
