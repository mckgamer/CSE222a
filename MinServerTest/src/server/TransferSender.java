package server;

import game.Bullet;
import game.GameLogic;
import game.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import shared.ServerMessage;

public class TransferSender extends Thread {
	
	public DatagramSocket socket = null;
	/*
	public HashMap<Neighbor.Direction,ArrayList<Player>> ptransfers;
	public HashMap<Neighbor.Direction,ArrayList<Bullet>> btransfers;
	public HashMap<Neighbor.Direction,ServerAddress> neighbors;
	*/
	private GameLogic myLogic;
	private ServerThread myThread;
	private boolean isRunning = true;
	public Boolean recieved = false;
	public DatagramPacket packet;

	public TransferSender(ServerThread server, GameLogic dummy, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket();
		myLogic = dummy;
		myThread = server;
		/*
		ptransfers = dummy.playerTransfer;
		btransfers = dummy.bulletTransfer;
		neighbors = dummy.neighbors;
		*/
	}

	public void kill() {
		isRunning = false;
	}

	public void run() {

		while (isRunning) {
			try {
				synchronized (myLogic.playerTransfer) {
					
					for (Neighbor.Direction neigh : myLogic.playerTransfer.keySet()) {
						ArrayList<Player> ptrans = myLogic.playerTransfer.get(neigh);
						ArrayList<Bullet> btrans = myLogic.bulletTransfer.get(neigh);
						if ((ptrans != null && btrans != null) && (ptrans.size()>0 || btrans.size()>0)) {
							byte[] buftemp = new byte[1500]; //TODO right size
		                	ByteBuffer wrapped = ByteBuffer.wrap(buftemp);
		                	int startPos = wrapped.position();	//use for measuring buffer size
		                	
		                	//Mark the message type
		                	wrapped.put(ServerMessage.TRANSFEROBJ);
		                	
		                	//Reserve 4 bytes for the message size
		                	wrapped.mark();
		                	wrapped.putInt(0);
		                	
		                	wrapped.putInt(ptrans.size());
		                	for (Player p : ptrans) {
		                		wrapped.put(p.encode());
		                	}
		                	
		                	wrapped.putInt(btrans.size());
		                	for (Bullet b : btrans) {
		                		wrapped.put(b.encode());
		                	}
		                	
		                	//Write the packet length to the buffer
		                	int size = wrapped.position() - startPos;
		                	wrapped.reset();
		                	wrapped.putInt(size);
		                	
		                	if(myLogic.neighbors.get(neigh) == null) {
		                		generateServer(neigh);
		                	}
		                	
		    				DatagramPacket packet2 = new DatagramPacket(
	    						buftemp,
	    						buftemp.length,
	    						myLogic.neighbors.get(neigh).getAddress().ip,
	    						myLogic.neighbors.get(neigh).getAddress().port
    						);
		    				Server.log.println("Sending out transfers to " + myLogic.neighbors.get(neigh).getAddress().port + " from " + socket.getLocalPort());
		    				socket.send(packet2);
		    				
		    				ptrans.clear();
							btrans.clear();
						}
					}
	
					recieved = false;
				}

			} catch (IOException e) {
				Server.log.printerr(e);
				socket.close();
			}
		}
		socket.close();
		Server.log.println("Dead Really");
	}
	
	
	private void generateServer(Neighbor.Direction dir) {
		ServerThread newServer = null;
    	boolean createdServer = false;
    	int listenPort = 4440;
    	int transferPort = 5550;
    	
    	//Find a valid pair of ports
    	while (!createdServer) {
	    	try {
	    		newServer = new ServerThread(listenPort, transferPort);
	    		createdServer = true;
	    	} catch (IOException e) {
	    		listenPort++;
	    		transferPort++;
	    	}
    	}
    	
    	Neighbor newNeighbor = newServer.toNeighbor();
    	myLogic.neighbors.put(dir, newNeighbor);
    	sendNeighborNote(socket, myThread.toNeighbor(), newNeighbor, Neighbor.flip(dir));

		//TODO: Implement NewServer message
	}
	


    //TODO: Put this in a more logical place, like Neighbor or ServerMessage
    public static void sendNeighborNote(DatagramSocket skt, Neighbor from, Neighbor to, Neighbor.Direction dir) {
    	final int neighborMessageSize = Neighbor.ENCODE_SIZE + 8;
		byte [] buf = new byte[neighborMessageSize];
		ByteBuffer wrapped = ByteBuffer.wrap(buf);
		
		wrapped.put(ServerMessage.NEIGHBORNOTE);
		wrapped.put(Neighbor.dirToByte(dir));
		from.encode(wrapped);
		DatagramPacket pkt = new DatagramPacket(buf, neighborMessageSize, to.getAddress().ip, to.getAddress().port);
		
		/*
		String s = "";
		for(int i = 0; i < neighborMessageSize; ++i) {
			s += "{" + buf[i] + "} ";
		}
		log.println(s);
		*/
		try {
			skt.send(pkt);
		} catch (IOException e) {
			Server.log.printerr(e);
			Server.log.println(from + " -> " + to);
		}
    }

}
