package server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Neighbor {
	
	public enum Direction {
		TOPLEFT, TOP, TOPRIGHT, LEFT, RIGHT, BOTTOMLEFT, BOTTOM, BOTTOMRIGHT
		
	}
	/*
	public static final byte TOPLEFT = 0;
	public static final byte TOP = 1;
	public static final byte TOPRIGHT = 2;
	public static final byte RIGHT = 3;
	public static final byte BOTTOMRIGHT = 4;
	public static final byte BOTTOM = 5;
	public static final byte BOTTOMLEFT = 6;
	public static final byte LEFT = 7;
	*/
	
	private ServerAddress address;
	private int priority;
	
	public static int ENCODE_SIZE = 4 * 3;	//When you add stuff to encode/decode, change this number
	

	public static byte dirToByte(Direction dir) {
		byte i = 0;
		for(Direction d2 : Direction.values()) {
			if(dir == d2) {
				break;
			}
			++i;
		}
		return i;
	}
	
	public Neighbor(ServerAddress address, int priority) {
		this.address = address;
		this.priority = priority;
	}
	
	public static Neighbor decode(ByteBuffer buf) {
		byte [] ipAddr = new byte[4];
		buf.get(ipAddr);
		int port = buf.getInt();
		int priority = buf.getInt();
		
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
		buf.putInt(priority);
	}
	public ServerAddress getAddress() {
		return address;
	}
	
	public int getPriority() {
		return priority;
	}

	@Override
	public String toString() {
		return "Neighbor {" + priority + ": " + address + "}";
	}
}
