package eu.openanalytics.phaedra.base.http.server.jetty.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.log.Log;

public class NewSSLSocketConnector extends SocketConnector {

	public NewSSLSocketConnector() {
		super();
	}

	protected void configure(Socket socket) throws IOException {   
		super.configure(socket);
		try {
			((SSLSocket)socket).startHandshake(); // block until SSL handshaking is done
		} catch (IOException e) {
			throw e;
		}
	}

	protected ServerSocket newServerSocket(String host, int port,int backlog) throws IOException {
		SSLServerSocket socket = null;
		try {
	        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
			if (port == 0) port = 443;
			socket = (SSLServerSocket) (host==null?
					factory.createServerSocket(port,backlog):
					factory.createServerSocket(port,backlog,InetAddress.getByName(host)));
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			Log.warn(Log.EXCEPTION, e);
			throw new IOException("Could not create SSL server socket: " + e.toString());
		}
		return socket;
	}
}
