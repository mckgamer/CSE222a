package game;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import shared.PerChunkUIDGenerator;


public class DummyClient {

	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	static Adler32 checkSumt = new Adler32();
	
	//Each thread has its own UIDGenerator
	PerChunkUIDGenerator mUIDGen = new PerChunkUIDGenerator();
	
	public void doUpdates() {
		
		checkSumt.reset();
        checkSumt.update(getState());
    	
        ArrayList<Integer> playerTransfer = new ArrayList<Integer>();
        
		// Drawing code goes here
	    for (Player p: players.values()) {
	    	p.x+=p.xvel;
	        p.y+=p.yvel;
	        p.xvel/=1.03;
	        p.yvel/=1.03;
	        if (p.x>500 || p.x<0 || p.y<0 || p.y>500) {
	        	playerTransfer.add(p.entityID);
	        }
	    }
	    for (Integer p: playerTransfer) {
        	players.remove(p);
        }
	  //if p leaves my boundaries then transfer it to another server
	    
	    ArrayList<Integer> bulletTransfer = new ArrayList<Integer>();
	    ArrayList<Integer> kill = new ArrayList<Integer>();
        for (Bullet b: bullets.values()) {
        	b.x+=b.xvel;
	        b.y+=b.yvel;
	        b.life--;
	        if (b.life < 0) {
	        	kill.add(b.entityID);
	        }
	        if (b.x>500 || b.x<0 || b.y<0 || b.y>500) {
	        	bulletTransfer.add(b.entityID);
	        }
	        //if b leaves my boundaries then transfer it to another server
        }
        for (Integer b: bulletTransfer) {
        	bullets.remove(b);
        }
        for (Integer b: kill) {
        	bullets.remove(b);
        }
	}
	
	public int checkSum() {
		return (int)checkSumt.getValue();
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
    			int bid = mUIDGen.getOtherID();
    			bullets.put(bid, new Bullet(bid,players.get(id).x,players.get(id).y,(float)Math.cos(players.get(id).angle)*7,(float)Math.sin(players.get(id).angle)*7));
    			break;
    		}
    		
    		index+=8;
    	}
    	recieveTransfer(wrapped);
    }
	
	public void recieveTransfer(ByteBuffer wrapped) {
    	int pCount = wrapped.getInt();
    	for (int p=0;p<pCount;p++) {
    		int id = wrapped.getInt();
    		Player temp = new Player(id);
    		temp.decode(wrapped);
    		players.put(id,temp);
    	}
    	int bCount = wrapped.getInt();
    	for (int b=0;b<bCount;b++) {
    		int id = wrapped.getInt();
			Bullet temp = new Bullet(id);
			temp.decode(wrapped);
			bullets.put(id,temp);
    	}
	}
	
	public byte[] getState() {
		byte buf[] = new byte[256];
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
	
}
