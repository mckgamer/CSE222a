package server;

import game.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import shared.ServerMessage;


public class ClientListener extends Thread {
	
	public DatagramSocket socket = null;
	public HashMap<String, Long> lastTalked;
	public ArrayList<Address> clientAddresses = new ArrayList<Address>();
	private boolean condition = true;
	public Boolean recieved = false;
	public DatagramPacket packet;
	private GameLogic myLogic;
	
	public ClientListener(GameLogic logic, int port, String name) throws SocketException {
        super(name);
        socket = new DatagramSocket(port);
        
        lastTalked = new HashMap<String,Long>();
        myLogic = logic;
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
                socket.receive(packet);
                recieved = true;
                
                if (lastTalked.containsKey(packet.getAddress().toString()+packet.getPort())) {
                	lastTalked.put(packet.getAddress().toString()+packet.getPort(), System.currentTimeMillis());
                } else {
                	byte[] buftemp = new byte[5];
                	ByteBuffer wrapped = ByteBuffer.wrap(buftemp);
                	wrapped.put(ServerMessage.IDASSIGN);
                	wrapped.putInt(myLogic.mUIDGen.getID()); //TODO fix for multi servers per process
    				DatagramPacket packet2 = new DatagramPacket(buftemp, buftemp.length, packet.getAddress(), packet.getPort());
    				Server.log.println("Sending out ID to new CLient");
    				socket.send(packet2);
                	lastTalked.put(packet.getAddress().toString()+packet.getPort(), System.currentTimeMillis());
                }

                synchronized (clientAddresses) {
                	clientAddresses.add(
            			new Address(
	            			packet.getAddress(),
	            			packet.getPort(),
	            			ByteBuffer.wrap(buf).get(),
	            			buf
            			)
        			);
                }
                

                recieved = false;
 
            } catch (IOException e) {
                e.printStackTrace();
                socket.close();
            }
        } 
        socket.close();
        Server.log.println("Dead Really");
    }

}
