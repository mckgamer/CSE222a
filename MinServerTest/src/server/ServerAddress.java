package server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerAddress {

	int port;
	InetAddress ip;
	
	public ServerAddress(String address, int port) throws UnknownHostException {
		this.ip = InetAddress.getByName(address);
		this.port = port;
	}
}
