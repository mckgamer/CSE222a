package game;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import shared.PerChunkUIDGenerator;
import shared.ServerMessage;

public class GameThread extends Thread {
	
	//Each thread has its own UIDGenerator
	PerChunkUIDGenerator mUIDGen = new PerChunkUIDGenerator();
	
	boolean isRunning = true;
	Adler32 checkSum = new Adler32();
	int bytes = 1500;
	String host;
	
	public int mClientID = 0;
	public int mInput = 0;
	
	int xOffSet = 0;
	int yOffSet = 0;
	
	// Counter variables for debugging
	float normal = 1;
	float outOfSync = 0;
	
	int port = 4445;
	
	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	
	public GameThread(int port, int xOffSet, int yOffSet) {
		this.xOffSet = xOffSet;
		this.yOffSet = yOffSet;
		this.port = port;
	}
	
	@Override
	public void run() {
		host = "localhost";

		try {
			// get a datagram socket
			DatagramSocket socket = new DatagramSocket();
	
			while (isRunning) {
				// send request
				byte[] buf;
				int inputNow = mInput; //for sync reasons (key can change mInput async)
				buf = new byte[8+((inputNow > 0)?8:0)];
				ByteBuffer wrapper = ByteBuffer.wrap(buf);
				wrapper.putInt((int) checkSum.getValue());
				wrapper.putInt(8+((inputNow > 0)?8:0));
				if (inputNow > 0) {
					wrapper.putInt(mClientID);
					wrapper.putInt(inputNow);
					mInput = mInput & ~GameInput.FIRE;
				}
				
				
				InetAddress address = InetAddress.getByName(host);
				DatagramPacket packet = new DatagramPacket(buf, buf.length,
						address, port);
				socket.send(packet);
	
				// get response
				buf = new byte[bytes]; //TODO use better size here (should handle any size?)
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
	        if (b.life < 0 || b.x>500 || b.x<0 || b.y<0 || b.y>500) {
	        	kill.add(b.entityID);
	        }
        }
        for (Integer b: kill) {
        	bullets.remove(b);
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
			decodeState(ByteBuffer.wrap(packet.getData(), 8, packet.getData().length-8));
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
    		
    		int input = wrapped.getInt();
    		
    		if (!players.containsKey(id)) {
    			System.out.println("Player id "+id);
    			players.put(id,new Player(id));
    		}
    		
    		if ((input & GameInput.UP) != 0){
    			players.get(id).yvel+=Math.sin(players.get(id).angle);
				players.get(id).xvel+=Math.cos(players.get(id).angle);
    		}
    		if ((input & GameInput.DOWN) != 0){
    			players.get(id).yvel-=Math.sin(players.get(id).angle);
				players.get(id).xvel-=Math.cos(players.get(id).angle);
    		}
    		if ((input & GameInput.LEFT) != 0){
    			players.get(id).angle-=0.2;
    		}
    		if ((input & GameInput.RIGHT) != 0){
    			players.get(id).angle+=0.2;
    		}
    		if ((input & GameInput.FIRE) != 0){
    			int bid = mUIDGen.getOtherID();
    			bullets.put(bid, new Bullet(bid,players.get(id).x,players.get(id).y,(float)Math.cos(players.get(id).angle)*7,(float)Math.sin(players.get(id).angle)*7));
    		}
    		
    		index+=8;
    	}
    }
    
    public byte[] getChunkState() {
    	byte buf[] = new byte[12+players.size()*Player.encodeSize()+bullets.size()*Bullet.encodeSize()];
    	ByteBuffer wrapped = ByteBuffer.wrap(buf);
    	wrapped.putInt(mUIDGen.softOther());
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
    	mUIDGen.setOther(wrapped.getInt());
    	int pCount = wrapped.getInt();
    	//players.clear(); //TODO should just remove ones that don't show up
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
    	bullets.clear(); //TODO should just remove ones that don't show up
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

}
