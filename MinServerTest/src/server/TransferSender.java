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
	private boolean isPaused = false;
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
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					Server.log.printerr(e);
				}
				
				//Ensure that all servers exist if a client might be able to see them
				if(myLogic.players.size() > 0) {
					//There is a player in this chunk, make sure all the surrounding servers exist
					for(Neighbor.Direction dir : Neighbor.Direction.values()) {
						Neighbor nbor = myLogic.neighbors.get(dir);
						if(nbor == null) {
							Server.log.println("Generating my neighbor " + dir);
	                		generateServer(dir);
						}
					}
				}
				synchronized (myLogic.playerTransfer) {
					
					for (Neighbor.Direction neigh : myLogic.playerTransfer.keySet()) {
						ArrayList<Player> ptrans = myLogic.playerTransfer.get(neigh);
						ArrayList<Bullet> btrans = myLogic.bulletTransfer.get(neigh);
						if ((ptrans != null && btrans != null) && (ptrans.size()>0 || btrans.size()>0)) {
							byte[] buftemp = new byte[1+4+4+4+(ptrans.size()*Player.encodeSize())+(btrans.size()*Bullet.encodeSize())]; //TODO right size
		                	ByteBuffer wrapped = ByteBuffer.wrap(buftemp);
		                	int startPos = wrapped.position();	//use for measuring buffer size
		                	
		                	//Mark the message type
		                	wrapped.put(ServerMessage.TRANSFEROBJ);
		                	
		                	//Reserve 4 bytes for the message size
		                	wrapped.mark();
		                	wrapped.putInt(1+4+4+4+(ptrans.size()*Player.encodeSize())+(btrans.size()*Bullet.encodeSize()));
		                	
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
		                		if(ptrans.size() > 0) {
		                			Server.log.println("ERROR: Somehow a server did not exist in direction " + neigh + " when " + ptrans.size() + " players are in this chunk!");
		                		}
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
    	
    	//Find a valid pair of ports/create the server
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
    	Server.addServer(newServer);
    	

		//Prepare the NewServer message
    	NewServerMessage msgCcw = NewServerMessage.create(100, NewServerMessage.CIRCLE_CCW, dir, newNeighbor, myThread.toNeighbor());
    	NewServerMessage msgCw  = NewServerMessage.create(100, NewServerMessage.CIRCLE_CW,  dir, newNeighbor, myThread.toNeighbor());
    	Server.log.println("New CCW msg: " + msgCcw);
    	Server.log.println("New CW msg: " + msgCw);
    	Neighbor.Direction ccwSendTo = msgCcw.updateToNextStep(myLogic);
    	Neighbor.Direction cwSendTo = msgCw.updateToNextStep(myLogic);
    	
    	//Send the neighbor note
    	myLogic.neighbors.put(dir, newNeighbor);
    	sendNeighborNote(socket, myThread.toNeighbor(), newNeighbor, Neighbor.flip(dir));
    	
    	//TODO: Remove debug?
    	Server.spanel.registerServer(myThread.toNeighbor().getPriority(), dir, newNeighbor.getPriority(), 2);
    	
    	sendNewServerMessage(socket, msgCcw, myLogic.neighbors.get(ccwSendTo));
    	sendNewServerMessage(socket, msgCw, myLogic.neighbors.get(cwSendTo));
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

    public static void sendNewServerMessage(DatagramSocket skt, NewServerMessage msg, Neighbor to) {
    	int msgSize = NewServerMessage.SIZE + 1;
		byte [] buf = new byte[msgSize];
		ByteBuffer wrapped = ByteBuffer.wrap(buf);
		
		wrapped.put(ServerMessage.NEWSERVER);
		msg.encode(wrapped);
		DatagramPacket pkt = new DatagramPacket(buf, msgSize, to.getAddress().ip, to.getAddress().port);
		try {
			skt.send(pkt);
		} catch (IOException e) {
			Server.log.printerr(e);
		}
    }
}
