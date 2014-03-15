package server;

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
	private boolean condition = true;
	public Boolean recieved = false;
	public DatagramPacket packet;

	public TransferListener(int port, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket(port);
	}

	public void kill() {
		condition = false;
	}

	public void run() {

		while (condition) {
			try {
				byte[] buf = new byte[1500];

				// receive request
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				recieved = true;
				Server.log.println("YAY got transfers from " + packet.getPort());

				ByteBuffer tData = ByteBuffer.wrap(packet.getData(),0,40); //TODO remove 40
				
				//Extract packet header and operate on packet
				byte messageType = tData.get();
				switch(messageType) {
				case ServerMessage.TRANSFEROBJ:
					synchronized (transfers) {
						transfers.add(tData);
					}
					break;
				case ServerMessage.NEIGHBORNOTE:
					//We have a new neighbor!
					break;
				default:
					Server.log.println("Unknown message type " + messageType);
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