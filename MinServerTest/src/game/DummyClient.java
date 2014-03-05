package game;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import shared.UniqueIDGenerator;


public class DummyClient {

	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	static Adler32 checkSumt = new Adler32();
	
	public void doUpdates() {
		
		checkSumt.reset();
        checkSumt.update(getState());
    	
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
    			int bid = UniqueIDGenerator.getOtherID();
    			bullets.put(bid, new Bullet(bid,players.get(id).x,players.get(id).y,(float)Math.cos(players.get(id).angle)*3,(float)Math.sin(players.get(id).angle)*3));
    			break;
    		}
    		
    		index+=8;
    	}
    }
	
	public byte[] getState() {
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
	
}
