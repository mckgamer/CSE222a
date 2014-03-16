package server;

import game.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import shared.ServerMessage;

public class TransferListener extends Thread {

	public DatagramSocket socket = null;
	public ArrayList<ByteBuffer> transfers = new ArrayList<ByteBuffer>();
	private boolean isRunning = true;
	public Boolean recieved = false;
	public DatagramPacket packet;
	private GameLogic myLogic;

	public TransferListener(GameLogic logic, int port, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket(port);
		myLogic = logic;
	}

	public void kill() {
		isRunning = false;
	}

	public void run() {

		while (isRunning) {
			try {
				byte[] buf = new byte[1500];

				// receive request
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				recieved = true;
				Server.log.println("YAY " + socket.getLocalPort() + " got transfers from " + packet.getPort());

				ByteBuffer tData = ByteBuffer.wrap(packet.getData()); //TODO remove 40
				
				//Extract packet header and operate on packet
				byte messageType = tData.get();
				switch(messageType) {
				case ServerMessage.TRANSFEROBJ:
					synchronized (transfers) {
						transfers.add(tData);
					}
					break;
				case ServerMessage.NEIGHBORNOTE:
					Neighbor.Direction dir = Neighbor.Direction.values()[tData.getInt()];
					Neighbor nbor = Neighbor.decode(tData);
					synchronized(myLogic) {
						Neighbor oldNbor = myLogic.neighbors.put(dir, nbor);
						if(oldNbor != null) {
							Server.log.println("WARNING: Replacing " + oldNbor + " with " + nbor);
						}
					}
					break;
				default:
					Server.log.println("Unknown message type " + messageType);
				}

				recieved = false;

			} catch (IOException e) {
				Server.log.printerr(e);
				socket.close();
			}
		}
		socket.close();
		Server.log.println("Dead Really");
	}

}