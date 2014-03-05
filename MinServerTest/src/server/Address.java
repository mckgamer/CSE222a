package server;
import java.net.InetAddress;


public class Address {
	
	public InetAddress address;
	public int port;
	public int check;
	public byte[] fullData;
	
	public Address(InetAddress address, int port, int check, byte[] fullData) {
		this.address = address;
		this.port = port;
		this.check = check;
		this.fullData = fullData;
	}

}
