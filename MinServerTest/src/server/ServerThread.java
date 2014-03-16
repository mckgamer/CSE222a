package server;

import game.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import shared.ServerMessage;

public class ServerThread extends Thread {

	protected DatagramSocket socket = null;
	private ClientListener clientListener;

	private TransferListener transferListener;
	private TransferSender transferSender;

	private int desiredFR = 25;
	private int windowFR = 5;
	private int stallFR = 20;

	private double desiredMS = 1000 / (desiredFR / windowFR);
	private boolean isRunning = true;

	int bytes = 256;

	private GameLogic dummy;
	
	public void kill() {
		if (isRunning()) {
			isRunning = false;
		}
	}

	public synchronized boolean isRunning() {
		return isRunning;
	}

	public ServerThread(int listenPort, int transferPort) throws IOException {
		this(listenPort, transferPort,"ServerThread-L" + listenPort + "-T"
				+ transferPort);
	}

	public ServerThread(int listenPort, int transferPort, String name)
			throws IOException {
		super(name);

		dummy = new GameLogic(Server.log);

		clientListener = new ClientListener(dummy, listenPort, "ClientListener" + listenPort);
		clientListener.start();

		transferListener = new TransferListener(transferPort, "Transfer Listener");
		transferListener.start();
		transferSender = new TransferSender(dummy, "Transfer Sender");
		transferSender.start();

		socket = new DatagramSocket();
		
		int me = listenPort%4;
		//neighbors.put(Neighbor.TOPLEFT, new ServerAddress("localhost",5550+3-me));
		dummy.neighbors.put(Neighbor.TOP, new ServerAddress("localhost",5550+((2+me)%4)));
		//neighbors.put(Neighbor.TOPRIGHT, new ServerAddress("localhost",5550+3-me));
		dummy.neighbors.put(Neighbor.LEFT, new ServerAddress("localhost",5550+(Math.abs((me-5))%4)));
		dummy.neighbors.put(Neighbor.RIGHT, new ServerAddress("localhost",5550+(Math.abs((me-5))%4)));
		//neighbors.put(Neighbor.BOTTOMLEFT, new ServerAddress("localhost",5550+3-me));
		dummy.neighbors.put(Neighbor.BOTTOM, new ServerAddress("localhost",5550+((2+me)%4)));
		//neighbors.put(Neighbor.BOTTOMRIGHT, new ServerAddress("localhost",5550+3-me));
		
	}

	public void updateClientList(List<String> clients) {
		for (String addr : clientListener.lastTalked.keySet()) {
			clients.add(addr.toString());
		}
	}

	public void run() {

		int windowPackets = 0;
		int totalPackets = 0;
		long myTime = System.currentTimeMillis();
		long longTime = System.currentTimeMillis();
		while (isRunning()) {

			try {
				Thread.sleep(stallFR);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (windowPackets >= windowFR) {
				double observedMS = System.currentTimeMillis() - myTime;
				myTime = System.currentTimeMillis();
				stallFR = (int) (stallFR * desiredMS / observedMS);
				windowPackets = 0;
			}
			if (System.currentTimeMillis() - longTime > 1000) {
				Server.log.println("PPS: " + totalPackets);
				assert (totalPackets > 65);
				totalPackets = 0;
				longTime = System.currentTimeMillis();
			}
			windowPackets++;
			totalPackets++;
			dummy.doPhysics();

			byte[] normalBuf = null;
			if (clientListener.clientAddresses.size() > 0) {
				synchronized (clientListener.clientAddresses) {
					boolean badOne = false;
				 	byte[] goodData = null;
					byte[] aggregate = new byte[bytes]; // TODO use the correct
														// size here
					ByteBuffer aggregator = ByteBuffer.wrap(aggregate);
					int offset = 0;
					for (Address d : clientListener.clientAddresses) {
						if (d.check != dummy.checkSum()) {
							badOne = true;
						}
						aggregator
								.put(d.fullData, 4 + 1,
										ByteBuffer.wrap(d.fullData, 1, 4)
												.getInt() - 4 - 1);
						offset += ByteBuffer.wrap(d.fullData, 1, 4).getInt() - 4 - 1;
					}

					/* Compute the full state buffer once. */
					byte[] fullStateBuf = null;
					if (badOne) {
						goodData = dummy.getState();
						fullStateBuf = new byte[1 + 1 + goodData.length];
						ByteBuffer fswrapper = ByteBuffer.wrap(fullStateBuf);
						fswrapper.put(ServerMessage.OUTOFSYNC);
						if (goodData != null) {
							fswrapper.put((byte) dummy.checkSum());
							fswrapper.put(goodData);
						}
					}

					/* Compute the normal op buffer once. */
					synchronized (transferListener.transfers) {
						normalBuf = new byte[offset + 1 + 2+80]; //TODO fixme
						ByteBuffer nwrapper = ByteBuffer.wrap(normalBuf);
						nwrapper.put(ServerMessage.NORMALOP);
						nwrapper.putShort((short) (offset + 1 + 2)); // length
																		// of
																		// packet
																		// useful
						nwrapper.put(aggregate, 0, offset);

						nwrapper.put((byte)transferListener.transfers.size());
						for (ByteBuffer t : transferListener.transfers) {
							nwrapper.put(t);
						}
						transferListener.transfers.clear();
					}

					/* Send the appropriate packet to each client. */
					for (Address d : clientListener.clientAddresses) {
						DatagramPacket packet;
						if (d.check == dummy.checkSum()) {
							packet = new DatagramPacket(normalBuf,
									normalBuf.length, d.address, d.port);
						} else {
							assert (fullStateBuf != null);
							Server.log.println("Sending out "
									+ dummy.checkSum());
							packet = new DatagramPacket(fullStateBuf,
									fullStateBuf.length, d.address, d.port);
						}
						try {
							clientListener.socket.send(packet);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					clientListener.clientAddresses.clear();
				}
				assert (normalBuf != null);
				dummy.updateState(ByteBuffer.wrap(normalBuf, 1,
						normalBuf.length - 1));
			}
		}

		Server.log.println("Dead");
		clientListener.kill();
	}

}