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

import shared.ServerMessage;

public class TransferSender extends Thread {
	
	public DatagramSocket socket = null;
	public ArrayList<Player> ptransfers;
	public ArrayList<Bullet> btransfers;
	private boolean condition = true;
	public Boolean recieved = false;
	public DatagramPacket packet;

	public TransferSender(int port, GameLogic dummy, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket(port);
		ptransfers = dummy.playerTransfer;
		btransfers = dummy.bulletTransfer;
	}

	public void kill() {
		condition = false;
	}

	public void run() {

		while (condition) {
			try {
				synchronized (ptransfers) {
					
					if (ptransfers.size()>0 || btransfers.size()>0) {
						byte[] buftemp = new byte[1500]; //TODO right size
	                	ByteBuffer wrapped = ByteBuffer.wrap(buftemp);
	                	
	                	wrapped.putInt(ptransfers.size());
	                	for (Player p : ptransfers) {
	                		wrapped.put(p.encode());
	                	}
	                	
	                	wrapped.putInt(btransfers.size());
	                	for (Bullet b : btransfers) {
	                		wrapped.put(b.encode());
	                	}
	    				DatagramPacket packet2 = new DatagramPacket(buftemp, buftemp.length, InetAddress.getByName("localhost"), 5555);
	    				System.out.println("Sending out transfers");
	    				socket.send(packet2);
					}
					ptransfers.clear();
					btransfers.clear();
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
