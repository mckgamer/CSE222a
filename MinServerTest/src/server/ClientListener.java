package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import shared.ServerMessage;
import shared.UniqueIDGenerator;


public class ClientListener extends Thread {
	
	public DatagramSocket socket = null;
	public HashMap<String, Long> lastTalked;
	public ArrayList<Address> something = new ArrayList<Address>();
	private boolean condition = true;
	public Boolean recieved = false;
	public DatagramPacket packet;
	
	public ClientListener(int port, String name) throws SocketException {
        super(name);
        socket = new DatagramSocket(port);
        
        lastTalked = new HashMap<String,Long>();
    }
	
	public void kill() {
		condition = false;
	}
	public void run() {
		 
        while (condition) {
            try {
            	byte[] buf = new byte[256];
 
                // receive request
                packet = new DatagramPacket(buf, buf.length);
                //System.out.println("Waiting");
                socket.receive(packet);
                //System.out.println("Got one");
                recieved = true;
                
                if (lastTalked.containsKey(packet.getAddress().toString()+packet.getPort())) {
                	lastTalked.put(packet.getAddress().toString()+packet.getPort(), System.currentTimeMillis());
                } else {
                	byte[] buftemp = new byte[256];
                	ByteBuffer.wrap(buftemp, 0, 4).putInt(ServerMessage.IDASSIGN);
                	ByteBuffer.wrap(buftemp, 4, 4).putInt(UniqueIDGenerator.getID());
    				DatagramPacket packet2 = new DatagramPacket(buftemp, buftemp.length, packet.getAddress(), packet.getPort());
    				System.out.println("Sending out ID to new CLient");
    				socket.send(packet2);
                	lastTalked.put(packet.getAddress().toString()+packet.getPort(), System.currentTimeMillis());
                }

                synchronized (something) {
                	something.add(new Address(packet.getAddress(),packet.getPort(),ByteBuffer.wrap(buf).getInt(),buf));
                }
                

                recieved = false;
 
            } catch (IOException e) {
                e.printStackTrace();
                socket.close();
            }
        } 
        socket.close();
        System.out.println("Dead Really");
    }

}
