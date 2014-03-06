package game;

import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import shared.ServerMessage;
import shared.UniqueIDGenerator;

public class GameThread extends Thread {
	
	boolean isRunning = true;
	Adler32 checkSum = new Adler32();
	int bytes = 256;
	String host;
	
	public int mClientID = 0;
	private ArrayList<InputEvent> mInputs = new ArrayList<InputEvent>();
	
	int xOffSet = 0;
	int yOffSet = 0;
	
	// Counter variables for debugging
	float normal = 1;
	float outOfSync = 0;
	
	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	
	public GameThread(int xOffSet, int yOffSet) {
		this.xOffSet = xOffSet;
		this.yOffSet = yOffSet;
	}
	
	@Override
	public void run() {
		host = "localhost";

		try {
			// get a datagram socket
			DatagramSocket socket = new DatagramSocket();
	
			while (isRunning) {
				// send request
				byte[] buf = new byte[bytes];
				ByteBuffer wrapper = ByteBuffer.wrap(buf);
				wrapper.putInt((int) checkSum.getValue());
				wrapper.put(getInput());
				
				InetAddress address = InetAddress.getByName(host);
				DatagramPacket packet = new DatagramPacket(buf, buf.length,
						address, 4445);
				socket.send(packet);
	
				// get response
				handleResponse(packet, socket, buf);
	
				checkSum.reset();
				checkSum.update(getChunkState());
	
				doPhysics();
	
			}
	
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void doPhysics() {
		// Drawing code goes here
        for (Player p: players.values()) {
        	p.x+=p.xvel;
	        p.y+=p.yvel;
	        p.xvel/=1.03;
	        p.yvel/=1.03;
        }
        
        ArrayList<Integer> kill = new ArrayList<Integer>();
        for (Bullet b: bullets.values()) {
        	b.x+=b.xvel;
	        b.y+=b.yvel;
	        b.life--;
	        if (b.life < 0) {
	        	kill.add(b.entityID);
	        }
        }
        for (Integer b: kill) {
        	bullets.remove(b);
        }
	}
	
	public void handleResponse(DatagramPacket packet,
			DatagramSocket socket, byte[] buf) throws IOException {
		outOfSync-=(outOfSync<.01)?outOfSync:.01;
		normal-=(normal<.01)?normal:.01;
		System.out.println("Normal: " + (double) 100*normal / (normal + outOfSync)
				+ "% OOS: " + (double) 100*outOfSync / (normal + outOfSync) + "%");
		// get response
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		//System.out.println("Recieved a packet from sever " + packet.getPort());
		int packetType = ByteBuffer.wrap(packet.getData(), 0, 4).getInt();
		switch (packetType) {
		case ServerMessage.NORMALOP:
			// user input is included
			//System.out.println("Normal Operation");
			updateState(packet.getData());
			normal++;
			break;
		case ServerMessage.OUTOFSYNC:
			// im out of sync, full state included
			System.out.println("Out of sync at " + checkSum.getValue());
			int actualCheckSum = ByteBuffer.wrap(packet.getData(), 4, 4)
					.getInt();
			decodeState(ByteBuffer.wrap(packet.getData(), 8, 248));
			System.out.println("Synced to " + actualCheckSum);
			outOfSync++;
			break;
		case ServerMessage.STATEREQUEST:
			// someone needs my state
			System.out.println("Sending off my state");
			byte[] buf2 = new byte[bytes];
			ByteBuffer.wrap(buf2, 0, 4).putInt((int) checkSum.getValue());
			ByteBuffer.wrap(buf2, 4, 252).put(getChunkState(), 0, 252);
			InetAddress address2 = InetAddress.getByName(host);
			DatagramPacket packet2 = new DatagramPacket(buf2, buf2.length,
					address2, 4786);
			socket.send(packet2);
			System.out.println("Sent off my state of "
					+ ByteBuffer.wrap(buf2, 0, 4).getFloat());
			handleResponse(packet, socket, buf);
			break;
		case ServerMessage.IDASSIGN:
			// im just connecting, getting my unique id
			System.out.println("Im getting my ID yay!");
			mClientID = ByteBuffer.wrap(packet.getData(), 4, 4)
					.getInt();
			System.out.println("Got the id " + mClientID);
			handleResponse(packet, socket, buf);
			break;

		}
	}
	
	public void updateState(byte[] buf) {
    	ByteBuffer wrapped = ByteBuffer.wrap(buf);
    	wrapped.getInt(); //throw away first int not useful here
    	int index = 8;
    	int length = wrapped.getInt();
    	while (index+8 <= length) {
    		int id = wrapped.getInt();
    		int command = wrapped.getInt();
    		if (!players.containsKey(id)) {
    			System.out.println("Player id "+id);
    			players.put(id,new Player(id));
    		}
    		switch (command) {
    		case GameInput.UPKEY:
    			players.get(id).yvel+=Math.sin(players.get(id).angle);
				players.get(id).xvel+=Math.cos(players.get(id).angle);
				break;
			case GameInput.DOWNKEY:
				players.get(id).yvel-=Math.sin(players.get(id).angle);
				players.get(id).xvel-=Math.cos(players.get(id).angle);
    			break;
    		case GameInput.LEFTKEY:
    			players.get(id).angle-=0.2;
    			break;
    		case GameInput.RIGHTKEY:
    			players.get(id).angle+=0.2;;
    			break;
    		case GameInput.FIREKEY:
    			int bid = UniqueIDGenerator.getOtherID();
    			bullets.put(bid, new Bullet(bid,players.get(id).x,players.get(id).y,(float)Math.cos(players.get(id).angle)*3,(float)Math.sin(players.get(id).angle)*3));
    			break;
    		}
    		
    		index+=8;
    	}
    }
    
    public byte[] getChunkState() {
    	byte buf[] = new byte[256];
    	ByteBuffer wrapped = ByteBuffer.wrap(buf);
    	wrapped.putInt(UniqueIDGenerator.softOther());
    	wrapped.putInt(players.size());
    	for (Player p : players.values()) {
    		wrapped.put(p.encode());
    	}
    	wrapped.putInt(bullets.size());
    	for (Bullet b : bullets.values()) {
    		wrapped.put(b.encode());
    	}
		return buf;
    }
    
    public void decodeState(ByteBuffer wrapped) {
    	UniqueIDGenerator.setOther(wrapped.getInt());
    	int pCount = wrapped.getInt();
    	//players.clear();
    	for (int p=0;p<pCount;p++) {
    		int id = wrapped.getInt();
    		if (!players.containsKey(id)) {
    			Player temp = new Player(id);
    			temp.decode(wrapped);
    			players.put(id,temp);
    		} else {
    			players.get(id).decode(wrapped);
    		}
    	}
    	int bCount = wrapped.getInt();
    	//players.clear();
    	for (int b=0;b<bCount;b++) {
    		int id = wrapped.getInt();
    		if (!bullets.containsKey(id)) {
    			Bullet temp = new Bullet(id);
    			temp.decode(wrapped);
    			bullets.put(id,temp);
    		} else {
    			bullets.get(id).decode(wrapped);
    		}
    	}
    }
    
    
    public void setClientID(int id) {
    	mClientID = id;
    }
    
    public ArrayList<InputEvent> getInputs() {
    	return mInputs;
    }

	public int getInputsSize() {
		return 8 * mInputs.size();
	}
	
	public byte[] getInput() {
		byte[] buf;
		synchronized (mInputs) {
			buf = new byte[4+getInputsSize()];
			ByteBuffer wrapper = ByteBuffer.wrap(buf);
			wrapper.putInt(8 + getInputsSize()); // let the
																// server
																// know
																// the
																// length of
																// this

			for (InputEvent in : mInputs) {
				wrapper.putInt(mClientID);
				wrapper.putInt(in.type);
			}
			assert (wrapper.position() == (8 * getInputsSize()));
			mInputs.clear();
		}
		return buf;
	}

}
