package game;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import server.Neighbor;
import server.Neighbor.Direction;
import server.ServerAddress;
import shared.LogFile;
import shared.PerChunkUIDGenerator;

public class GameLogic {

	//Each thread has its own UIDGenerator
	public PerChunkUIDGenerator mUIDGen = new PerChunkUIDGenerator();
	
	//Chunk size
	public static final int CHUNK_SIZE = 500;
		
	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	
	public HashMap<Neighbor.Direction,Neighbor> neighbors = new HashMap<Neighbor.Direction,Neighbor>();

	public HashMap<Neighbor.Direction,ArrayList<Player>> playerTransfer = new HashMap<Neighbor.Direction,ArrayList<Player>>();
	public HashMap<Neighbor.Direction,ArrayList<Bullet>> bulletTransfer = new HashMap<Neighbor.Direction,ArrayList<Bullet>>();
	
	public Adler32 checkSumt = new Adler32();
	private LogFile log = null;
	
	private UIThread uiControl = null;
	
	public void setInControl(UIThread uiControl) { //TODO probably better way to do this
		this.uiControl = uiControl;
	}
	
	public GameLogic(LogFile log) {
		this.log = log;
		
		synchronized(playerTransfer) {
			playerTransfer.put(Neighbor.Direction.TOP,new ArrayList<Player>());
			playerTransfer.put(Neighbor.Direction.BOTTOM,new ArrayList<Player>());
			playerTransfer.put(Neighbor.Direction.LEFT,new ArrayList<Player>());
			playerTransfer.put(Neighbor.Direction.RIGHT,new ArrayList<Player>());
			
			bulletTransfer.put(Neighbor.Direction.TOP,new ArrayList<Bullet>());
			bulletTransfer.put(Neighbor.Direction.BOTTOM,new ArrayList<Bullet>());
			bulletTransfer.put(Neighbor.Direction.LEFT,new ArrayList<Bullet>());
			bulletTransfer.put(Neighbor.Direction.RIGHT,new ArrayList<Bullet>());
		}
	}
	public void doPhysics() {

        ArrayList<Integer> killPlayers = new ArrayList<Integer>();
		// Drawing code goes here
		for (Player p : players.values()) {
			p.x += p.xvel;
			p.y += p.yvel;
			p.xvel /= 1.03;
			p.yvel /= 1.03;
			if (p.x > CHUNK_SIZE || p.x < 0 || p.y < 0 || p.y > CHUNK_SIZE) {
				if (p.x > CHUNK_SIZE) { (p).x-= CHUNK_SIZE; playerTransfer.get(Neighbor.Direction.RIGHT).add(p); if (this.uiControl!=null && p.entityID == uiControl.myPlayer.mClientID) {uiControl.switchChunk(Direction.RIGHT);}}
				if ((p).x < 0) { (p).x+= CHUNK_SIZE; playerTransfer.get(Neighbor.Direction.LEFT).add(p); if (this.uiControl!=null && p.entityID == uiControl.myPlayer.mClientID) {uiControl.switchChunk(Direction.LEFT);}}
				if ((p).y > CHUNK_SIZE) { (p).y-= CHUNK_SIZE; playerTransfer.get(Neighbor.Direction.BOTTOM).add(p); if (this.uiControl!=null && p.entityID == uiControl.myPlayer.mClientID) {uiControl.switchChunk(Direction.BOTTOM);}}
				if ((p).y < 0) { (p).y+= CHUNK_SIZE; playerTransfer.get(Neighbor.Direction.TOP).add(p); if (this.uiControl!=null && p.entityID == uiControl.myPlayer.mClientID) {uiControl.switchChunk(Direction.TOP);}}
				killPlayers.add(p.entityID);
			}
		}
		
		synchronized (players) {
			for (Integer p : killPlayers) {
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
			if (b.x > CHUNK_SIZE || b.x < 0 || b.y < 0 || b.y > CHUNK_SIZE) {
				if ((b).x > CHUNK_SIZE) { (b).x-= CHUNK_SIZE; bulletTransfer.get(Neighbor.Direction.RIGHT).add(b); }
				if ((b).x < 0) { (b).x+= CHUNK_SIZE; bulletTransfer.get(Neighbor.Direction.LEFT).add(b); }
				if ((b).y > CHUNK_SIZE) { (b).y-= CHUNK_SIZE; bulletTransfer.get(Neighbor.Direction.BOTTOM).add(b); }
				if ((b).y < 0) { (b).y+= CHUNK_SIZE; bulletTransfer.get(Neighbor.Direction.TOP).add(b); }
				kill.add(b.entityID);
			}
			// if b leaves my boundaries then transfer it to another server
		}

		synchronized (bullets) {
			for (Integer b : kill) {
				bullets.remove(b);
			}
		}
		
		checkSumt.reset();
        checkSumt.update(getState());
	}
	
	public void updateState(ByteBuffer wrapped) {
    	int index = 1+2;
    	short length = wrapped.getShort();
    	//System.out.println("Total update size input "+length);
    	while (index+8 <= length) {
    		int id = wrapped.getInt();
    		
    		int input = wrapped.getInt();
    		
    		if (!players.containsKey(id)) {
    			log.println("Player id "+id);
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
    	byte numServerTransfers = wrapped.get();
    	while (numServerTransfers-- > 0) { //got a transfer
    		//log.println("Client got transfer processing!");
    		int pCount = wrapped.getInt();
        	for (int p=0;p<pCount;p++) {
        		int id = wrapped.getInt();
    			Player temp = new Player(id);
    			temp.decode(wrapped);
    			synchronized (players) {
    				players.put(id,temp);
    			}
        	}
    		
	    	int bCount = wrapped.getInt();
	    	for (int b=0;b<bCount;b++) {
	    		int id = wrapped.getInt();
				Bullet temp = new Bullet(id);
				temp.decode(wrapped);
				//temp.x += temp.xvel;
				//temp.y += temp.yvel;
				synchronized (bullets) {
					bullets.put(id,temp);
				}
	    	}
    	}
    }
	
	public byte[] getState() {
		int uidGenBytes = 4;
		int sizeInfoBytes = 4 * 2;
		int playerBytes = players.size()*Player.encodeSize();
		int bulletBytes = bullets.size()*Bullet.encodeSize();
		int neighborBytes = Neighbor.ENCODE_SIZE * Neighbor.Direction.values().length;
		byte buf[] = new byte[uidGenBytes + sizeInfoBytes + playerBytes + bulletBytes + neighborBytes];
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
    	
    	byte [] nullBytes = new byte[Neighbor.ENCODE_SIZE];
    	
    	for(Neighbor.Direction dir : Neighbor.Direction.values()) {
    		//If the neighbor exists, encode it.
    		Neighbor nbor = neighbors.get(dir);
        	if(nbor != null) {
        		nbor.encode(wrapped);
        	} else {
        		wrapped.put(nullBytes);
        	}
    	}
    	
    	/*
    	wrapped.putInt(ByteBuffer.wrap(neighbors.get(Neighbor.Direction.TOP).getAddress().ip.getAddress()).getInt());
    	wrapped.putInt(neighbors.get(Neighbor.Direction.TOP).getAddress().port);
    	wrapped.putInt(ByteBuffer.wrap(neighbors.get(Neighbor.Direction.LEFT).getAddress().ip.getAddress()).getInt());
    	wrapped.putInt(neighbors.get(Neighbor.Direction.LEFT).getAddress().port);
    	wrapped.putInt(ByteBuffer.wrap(neighbors.get(Neighbor.Direction.RIGHT).getAddress().ip.getAddress()).getInt());
    	wrapped.putInt(neighbors.get(Neighbor.Direction.RIGHT).getAddress().port);
    	wrapped.putInt(ByteBuffer.wrap(neighbors.get(Neighbor.Direction.BOTTOM).getAddress().ip.getAddress()).getInt());
    	wrapped.putInt(neighbors.get(Neighbor.Direction.BOTTOM).getAddress().port);
    	*/
		return buf;
	}
	
	public void decodeState(ByteBuffer wrapped) {
    	mUIDGen.setOther(wrapped.getInt());
    	int pCount = wrapped.getInt();
    	
    	synchronized (players) {
	    	players.clear(); //TODO should just remove ones that don't show up
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
    	}
    	int bCount = wrapped.getInt();
    	
    	synchronized (bullets) {
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
    	
    	//Decode the neighbors
    	neighbors.clear();
		for(Neighbor.Direction dir : Neighbor.Direction.values()) {
    		//If the neighbor exists, encode it.
    		Neighbor nbor = Neighbor.decode(wrapped);
        	if(nbor != null) {
        		neighbors.put(dir, nbor);
        	}
    	}
		/*
    	try {
    		if (neighbors.get(Neighbor.Direction.TOP) == null) { neighbors.put(Neighbor.Direction.TOP,new ServerAddress("127.0.0.1",4444)); }
    		if (neighbors.get(Neighbor.Direction.LEFT) == null) { neighbors.put(Neighbor.Direction.LEFT,new ServerAddress("127.0.0.1",4444)); }
    		if (neighbors.get(Neighbor.Direction.RIGHT) == null) { neighbors.put(Neighbor.Direction.RIGHT,new ServerAddress("127.0.0.1",4444)); }
    		if (neighbors.get(Neighbor.Direction.BOTTOM) == null) { neighbors.put(Neighbor.Direction.BOTTOM,new ServerAddress("127.0.0.1",4444)); }
			neighbors.get(Neighbor.Direction.TOP).ip = InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(wrapped.getInt()).array());
	    	neighbors.get(Neighbor.Direction.TOP).port = wrapped.getInt();
	    	
	    	neighbors.get(Neighbor.Direction.LEFT).ip = InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(wrapped.getInt()).array());
	    	neighbors.get(Neighbor.Direction.LEFT).port = wrapped.getInt();
	    	neighbors.get(Neighbor.Direction.RIGHT).ip = InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(wrapped.getInt()).array());
	    	neighbors.get(Neighbor.Direction.RIGHT).port = wrapped.getInt();
	    	neighbors.get(Neighbor.Direction.BOTTOM).ip = InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(wrapped.getInt()).array());
	    	neighbors.get(Neighbor.Direction.BOTTOM).port = wrapped.getInt();
    	} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		checkSumt.reset(); //TODO remove me, just for debuggin purposes
        checkSumt.update(getState()); //TODO same
        
		//System.out.println("And going to do updates now too!" + wrapped.get());
		updateState(wrapped);
    }
	
	public byte checkSum() {
		return (byte)checkSumt.getValue();
	}
}
