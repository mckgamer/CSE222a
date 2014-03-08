package server;

import game.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.List;

import shared.ServerMessage;

public class ServerThread extends Thread {

	protected DatagramSocket socket = null;
	protected int moreQuotes = 0;
	private ClientListener clientListener;

	private int desiredFR = 25;
	private int windowFR = 5;
	private int stallFR = 2000000;
	private double desiredMS = 1000 / (desiredFR / windowFR);
	private boolean isRunning = true;
	
	int bytes = 256;
	
	static GameLogic dummy;
	
	public void kill() {
		if(isRunning()) {
			isRunning = false;
		}
	}
	
	public synchronized boolean isRunning() {
		return isRunning;
	}

	

	public ServerThread(int listenPort, int destPort) throws IOException {
		this(listenPort, destPort, "ServerThread-" + listenPort + "<" + destPort);
	}

	public ServerThread(int listenPort, int destPort, String name) throws IOException {
		super(name);

		dummy = new GameLogic();

		clientListener = new ClientListener(destPort, "ClientListener");
		clientListener.start();
		socket = new DatagramSocket(listenPort);
	}
	
	public void updateClientList(List<String> clients) {
		for(String addr : clientListener.lastTalked.keySet()) {
			clients.add(addr.toString());
		}
	}

	public void run() {

		int windowPackets = 0;
		int totalPackets = 0;
		long myTime = System.currentTimeMillis();
		long longTime = System.currentTimeMillis();
		while (isRunning()) {
			moreQuotes++;
			if (moreQuotes % stallFR == 0) {

				if (windowPackets >= windowFR) {
					double observedMS = System.currentTimeMillis() - myTime;
					myTime = System.currentTimeMillis();
					stallFR = (int) (stallFR * desiredMS / observedMS);
					windowPackets = 0;
					moreQuotes = 0;
				}
				if (System.currentTimeMillis() - longTime > 1000) {
					System.out.println("PPS: " + totalPackets);
					assert (totalPackets > 65);
					totalPackets = 0;
					longTime = System.currentTimeMillis();
				}
				windowPackets++;
				totalPackets++;
				dummy.doPhysics();
				
				byte[] normalBuf = null;
				if (clientListener.something.size() > 0) {
					synchronized (clientListener.something) {
						boolean badOne = false;
						byte[] goodData = null;
						byte[] aggregate = new byte[bytes]; //TODO use the correct size here
						ByteBuffer aggregator = ByteBuffer.wrap(aggregate);
						int offset = 0;
						for (Address d : clientListener.something) {
							if (d.check != dummy.checkSum()) {
								badOne = true;
							}
							aggregator
									.put(d.fullData, 4+1,
											ByteBuffer.wrap(d.fullData, 1, 4)
													.getInt() - 4-1);
							offset += ByteBuffer.wrap(d.fullData, 1, 4)
									.getInt() - 4-1;
						}

						/* Compute the full state buffer once. */
						byte[] fullStateBuf = null;
						if (badOne) {
							goodData = dummy.getState();
							fullStateBuf = new byte[1+1+goodData.length];
							ByteBuffer fswrapper = ByteBuffer
									.wrap(fullStateBuf);
							fswrapper.put(ServerMessage.OUTOFSYNC);
							if (goodData != null) {
								fswrapper.put((byte)dummy.checkSum());
								fswrapper.put(goodData);
							}
						}

						/* Compute the normal op buffer once. */
						normalBuf = new byte[offset + 1+2];
						ByteBuffer nwrapper = ByteBuffer.wrap(normalBuf);
						nwrapper.put(ServerMessage.NORMALOP);
						nwrapper.putShort((short)(offset + 1+2)); // length of packet useful
						nwrapper.put(aggregate, 0, offset);

						/* Send the appropriate packet to each client. */
						for (Address d : clientListener.something) {
							DatagramPacket packet;
							if (d.check == dummy.checkSum()) {
								packet = new DatagramPacket(normalBuf,
										normalBuf.length, d.address, d.port);
							} else {
								assert(fullStateBuf!=null);
								System.out.println("Sending out "
										+ dummy.checkSum());
								packet = new DatagramPacket(fullStateBuf,
										fullStateBuf.length, d.address, d.port);
							}
							try {
								clientListener.socket.send(packet);
							} catch (IOException e) {
								e.printStackTrace();
								moreQuotes = -1;
							}
						}

						clientListener.something.clear();
					}
					assert(normalBuf!=null);
					dummy.updateState(ByteBuffer.wrap(normalBuf,1,normalBuf.length - 1));
				}
			}

		}
		System.out.println("Dead");
		clientListener.kill();
	}

}