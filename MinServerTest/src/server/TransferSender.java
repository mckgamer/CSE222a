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
	private boolean isRunning = true;
	public Boolean recieved = false;
	public DatagramPacket packet;

	public TransferSender(GameLogic dummy, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket();
		myLogic = dummy;
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
		                	//TODO encode total size somehow
		                	wrapped.put(ServerMessage.TRANSFEROBJ);
		                	
		                	wrapped.putInt(ptrans.size());
		                	for (Player p : ptrans) {
		                		wrapped.put(p.encode());
		                	}
		                	
		                	wrapped.putInt(btrans.size());
		                	for (Bullet b : btrans) {
		                		wrapped.put(b.encode());
		                	}
		    				DatagramPacket packet2 = new DatagramPacket(
	    						buftemp,
	    						buftemp.length,
	    						myLogic.neighbors.get(neigh).getAddress().ip,
	    						myLogic.neighbors.get(neigh).getAddress().port
    						);
		    				Server.log.println("Sending out transfers to " + myLogic.neighbors.get(neigh).getAddress().port);
		    				socket.send(packet2);
		    				
		    				ptrans.clear();
							btrans.clear();
						}
					}
	
					recieved = false;
				}

			} catch (IOException e) {
				e.printStackTrace();
				socket.close();
			}
		}
		socket.close();
		Server.log.println("Dead Really");
	}

}
