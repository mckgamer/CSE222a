package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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

				ByteBuffer tData = ByteBuffer.wrap(packet.getData(),0,40); //TODO remove 40
				synchronized (transfers) {
					transfers.add(tData);
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