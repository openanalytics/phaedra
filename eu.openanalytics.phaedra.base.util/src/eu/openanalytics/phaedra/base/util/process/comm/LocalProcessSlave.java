package eu.openanalytics.phaedra.base.util.process.comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p>
 * A slave process that can be managed by a LocalProcessMaster.
 * When this process starts, it does the following:
 * </p>
 * <ol>
 * <li>Instantiate a ILocalProcessProtocol that was given via a launch argument</li>
 * <li>Open a localhost TCP socket using a random available port</li>
 * <li>Send this port number to StdOut (to be picked up by the LocalProcessMaster)</li>
 * <li>Start listening to connections on the socket</li>
 * <li>When a connection is established, process incoming requests using the ILocalProcessProtocol</li>
 * </ol>
 **/
public class LocalProcessSlave {

	private ILocalProcessProtocol protocol;

	public LocalProcessSlave(ILocalProcessProtocol protocol) {
		this.protocol = protocol;
	}

	public static void main(String[] args) {
		try {
			ILocalProcessProtocol protocol = (ILocalProcessProtocol) Class.forName(args[0]).newInstance();
			new LocalProcessSlave(protocol).run();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	public void run() {
		try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName(null))) {
			System.out.println(socket.getLocalPort());

			Socket conn = socket.accept();
			PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String input;
			String output;
			while ((input = in.readLine()) != null) {
				try {
					output = protocol.process(input);
				} catch (Exception e) {
					output = "ERROR: " + (e.getMessage() == null ? "" : e.getMessage().replace("\n", " ").replace("\r", " "));
				}
				out.println(output);
				if (input.equals(BaseLocalProcessProtocol.CMD_SHUTDOWN)) break;
			}
		} catch (IOException e) {
			throw new RuntimeException("Slave socket error", e);
		}
	}
}
