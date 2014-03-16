package client;

import game.GameInput;
import game.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import shared.ServerMessage;

public class GameThread extends Thread {
	
	public GameLogic gameState = new GameLogic(NewClient.log);
	
	boolean isRunning = true;
	int bytes = 1500;
	public InetAddress host;
	
	public int mClientID = 0;
	public int mInput = 0;
	
	public int xOffSet = 0;
	public int yOffSet = 0;
	
	// Counter variables for debugging
	public float normal = 1;
	public float outOfSync = 0;
	
	public int port = 4440;
	
	public GameThread(InetAddress host, int port, int xOffSet, int yOffSet) {
		this.host = host;
		this.xOffSet = xOffSet;
		this.yOffSet = yOffSet;
		this.port = port;
	}

	@Override
	public void run() {

		try {
			host = InetAddress.getByName("127.0.0.1");
			//host = InetAddress.getByName("137.110.53.55");
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		try {
			// get a datagram socket
			DatagramSocket socket = new DatagramSocket();
	
			while (isRunning) {
				if(host == null) {
		        	try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				
				// send request
				byte[] buf;
				int inputNow = mInput; //for sync reasons (key can change mInput async)
				buf = new byte[1+4+((inputNow > 0)?8:0)];
				ByteBuffer wrapper = ByteBuffer.wrap(buf);
				wrapper.put((byte) gameState.checkSum());
				wrapper.putInt(1+4+((inputNow > 0)?8:0));
				if (inputNow > 0) {
					wrapper.putInt(mClientID);
					wrapper.putInt(inputNow);
					mInput = mInput & ~GameInput.FIRE; //TODO decouple this
				}
				
				DatagramPacket packet = new DatagramPacket(buf, buf.length,
						host, port);
				socket.send(packet);
	
				// get response
				buf = new byte[bytes]; //TODO use better size here (should handle any size?)
				handleResponse(packet, socket, buf);
	
				gameState.doPhysics();
	
			}
	
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void handleResponse(DatagramPacket packet,
			DatagramSocket socket, byte[] buf) throws IOException {
		if (outOfSync<.02) {outOfSync=0; } else { outOfSync-=.02; }
		if (normal<.02) {normal=0; } else { normal-=.02; }
		
		// get response
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		//System.out.println("Recieved a packet from sever " + packet.getPort());
		ByteBuffer wPacket = ByteBuffer.wrap(packet.getData());
		byte packetType = wPacket.get();
		switch (packetType) {
		case ServerMessage.NORMALOP:
			// user input is included
			//System.out.println("Normal Operation");
			gameState.updateState(wPacket);
			normal++;
			break;
		case ServerMessage.OUTOFSYNC:
			// im out of sync, full state included
			NewClient.log.println("Out of sync at " + gameState.checkSum());
			byte actualCheckSum = wPacket.get();
			gameState.decodeState(wPacket);
			NewClient.log.println("Synced to " + actualCheckSum);
			outOfSync++;
			break;
		case ServerMessage.IDASSIGN:
			// im just connecting, getting my unique id
			NewClient.log.println("Im getting my ID yay!");
			mClientID = wPacket.getInt();
			NewClient.log.println("Got the id " + mClientID);
			handleResponse(packet, socket, buf);
			break;

		}
	}
    
    public void setClientID(int id) {
    	mClientID = id;
    }
    
    public void setHost(InetAddress host, int port) {
    	this.host = host;
    	this.port = port;
    }

}
