package server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Neighbor {
	
	public enum Direction {
		TOPLEFT, TOP, TOPRIGHT, LEFT, RIGHT, BOTTOMLEFT, BOTTOM, BOTTOMRIGHT
	}
	
	private ServerAddress address;
	private int priority;
	
	public static int ENCODE_SIZE = 4 * 2;	//When you add stuff to encode/decode, change this number
	
	
	public Neighbor(ServerAddress address, int priority) {
		this.address = address;
		this.priority = priority;
	}
	
	public static Neighbor decode(ByteBuffer buf) {
		byte [] ipAddr = new byte[4];
		buf.get(ipAddr);
		int port = buf.getInt();
		int priority = 0;//buf.getInt();
		
		try {
			return new Neighbor(new ServerAddress(InetAddress.getByAddress(ipAddr), port), priority);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void encode(ByteBuffer buf) {
		buf.put(address.ip.getAddress(), 0, 4);
		buf.putInt(address.port);
		//buf.putInt(priority);
	}
	public ServerAddress getAddress() {
		return address;
	}
	
	public int getPriority() {
		return priority;
	}

}
