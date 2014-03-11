package game;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import server.Neighbor;
import server.ServerAddress;
import shared.PerChunkUIDGenerator;

public class GameLogic {

	//Each thread has its own UIDGenerator
	public PerChunkUIDGenerator mUIDGen = new PerChunkUIDGenerator();
		
	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	
	public HashMap<Neighbor,ServerAddress> neighbors = new HashMap<Neighbor,ServerAddress>();

	public HashMap<Neighbor,ArrayList<Player>> playerTransfer = new HashMap<Neighbor,ArrayList<Player>>();
	public HashMap<Neighbor,ArrayList<Bullet>> bulletTransfer = new HashMap<Neighbor,ArrayList<Bullet>>();
	
	private Adler32 checkSumt = new Adler32();
	
	
	public GameLogic() {
		
		
		synchronized(playerTransfer) {
			playerTransfer.put(Neighbor.TOP,new ArrayList<Player>());
			playerTransfer.put(Neighbor.BOTTOM,new ArrayList<Player>());
			playerTransfer.put(Neighbor.LEFT,new ArrayList<Player>());
			playerTransfer.put(Neighbor.RIGHT,new ArrayList<Player>());
			
			bulletTransfer.put(Neighbor.TOP,new ArrayList<Bullet>());
			bulletTransfer.put(Neighbor.BOTTOM,new ArrayList<Bullet>());
			bulletTransfer.put(Neighbor.LEFT,new ArrayList<Bullet>());
			bulletTransfer.put(Neighbor.RIGHT,new ArrayList<Bullet>());
		}
	}
	public void doPhysics() {
		
		checkSumt.reset();
        checkSumt.update(getState());

		// Drawing code goes here
		for (Player p : players.values()) {
			p.x += p.xvel;
			p.y += p.yvel;
			p.xvel /= 1.03;
			p.yvel /= 1.03;
			if (p.x > 500 || p.x < 0 || p.y < 0 || p.y > 500) {
				if (p.x > 500) { (p).x-= 500; playerTransfer.get(Neighbor.RIGHT).add(p); }
				if ((p).x < 0) { (p).x+= 500; playerTransfer.get(Neighbor.LEFT).add(p); }
				if ((p).y > 500) { (p).y-= 500; playerTransfer.get(Neighbor.BOTTOM).add(p); }
				if ((p).y < 0) { (p).y+= 500; playerTransfer.get(Neighbor.TOP).add(p); }
				players.remove(p);
			}
		}

		// if p leaves my boundaries then transfer it to another server

		ArrayList<Integer> kill = new ArrayList<Integer>();
		for (Bullet b : bullets.values()) {
			b.x += b.xvel;
			b.y += b.yvel;
			b.life--;
			if (b.life < 0) {
				kill.add(b.entityID);
			}
			if (b.x > 500 || b.x < 0 || b.y < 0 || b.y > 500) {
				if ((b).x > 500) { (b).x-= 500; bulletTransfer.get(Neighbor.RIGHT).add(b); }
				if ((b).x < 0) { (b).x+= 500; bulletTransfer.get(Neighbor.LEFT).add(b); }
				if ((b).y > 500) { (b).y-= 500; bulletTransfer.get(Neighbor.BOTTOM).add(b); }
				if ((b).y < 0) { (b).y+= 500; bulletTransfer.get(Neighbor.TOP).add(b); }
				kill.add(b.entityID);
			}
			// if b leaves my boundaries then transfer it to another server
		}

		synchronized (bullets) {
			for (Integer b : kill) {
				bullets.remove(b);
			}
		}
	}
	
	public void updateState(ByteBuffer wrapped) {
    	int index = 1+2;
    	short length = wrapped.getShort();
    	while (index+8 <= length) {
    		int id = wrapped.getInt();
    		
    		int input = wrapped.getInt();
    		
    		if (!players.containsKey(id)) {
    			System.out.println("Player id "+id);
    			synchronized (players) {
    				players.put(id,new Player(id));
    			}
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
    			synchronized (bullets) {
    				bullets.put(bid, new Bullet(bid,players.get(id).x,players.get(id).y,(float)Math.cos(players.get(id).angle)*7,(float)Math.sin(players.get(id).angle)*7));
    			}
			}
    		
    		index+=8;
    	}
    	
    	//Recieve Transfer
    	if (wrapped.get() > 0) { //got a transfer
    		System.out.println("Client got transfer processing!");
    		int pCount = wrapped.getInt();
        	for (int p=0;p<pCount;p++) {
        		System.out.println("PLAYA!");
        		int id = wrapped.getInt();
    			Player temp = new Player(id);
    			temp.decode(wrapped);
    			synchronized (players) {
    				players.put(id,temp);
    			}
        	}
    		
	    	int bCount = wrapped.getInt();
	    	for (int b=0;b<bCount;b++) {
	    		System.out.println("BULLET!");
	    		int id = wrapped.getInt();
				Bullet temp = new Bullet(id);
				temp.decode(wrapped);
				temp.x += temp.xvel;
				temp.y += temp.yvel;
				synchronized (bullets) {
					bullets.put(id,temp);
				}
	    	}
    	}
    }
	
	public byte[] getState() {
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
    			synchronized (players) {
    				players.put(id,temp);
    			}
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
    			synchronized (bullets) {
    				bullets.put(id,temp);
    			}
    		} else {
    			bullets.get(id).decode(wrapped);
    		}
    	}
    }
	
	public byte checkSum() {
		return (byte)checkSumt.getValue();
	}
}
