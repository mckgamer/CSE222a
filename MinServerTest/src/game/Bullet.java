package game;
import java.nio.ByteBuffer;


public class Bullet {
	
	public int entityID;
	public float x=30, y=30, xvel=0, yvel=0;
	public int mwidth=18, mheight=18; 
	public int life = 200;
	
	public Bullet(int id) {
		this.entityID = id;
	}
	public Bullet(int id, float x, float y, float xvel, float yvel) {
		this.entityID = id;
		this.x = x;
		this.y = y;
		this.yvel = yvel;
		this.xvel = xvel;
	}
	
	public void decode(ByteBuffer buf) {
		x = buf.getFloat();
		y = buf.getFloat();
		xvel = buf.getFloat();
		yvel = buf.getFloat();
		life = buf.getInt();
	}
	
	public byte[] encode() {
		byte[] buf = new byte[24];
		ByteBuffer wrapped = ByteBuffer.wrap(buf);
		wrapped.putInt(entityID);
		wrapped.putFloat(x);
		wrapped.putFloat(y);
		wrapped.putFloat(xvel);
		wrapped.putFloat(yvel);
		wrapped.putInt(life);
		return buf;
	}
	
	public static int encodeSize() {
		return 6*8;
	}

}
