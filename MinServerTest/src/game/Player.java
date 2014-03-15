package game;
import java.nio.ByteBuffer;


public class Player {
	
	int entityID;
	public float x=350, y=350, xvel=0, yvel=0;
	public int mwidth=18, mheight=18; 
	public double angle=0.0;
	
	public Player(int id) {
		this.entityID = id;
	}
	
	public void decode(ByteBuffer buf) {
		x = buf.getFloat();
		y = buf.getFloat();
		xvel = buf.getFloat();
		yvel = buf.getFloat();
		angle = buf.getFloat();
	}
	
	public byte[] encode() {
		byte[] buf = new byte[24];
		ByteBuffer wrapped = ByteBuffer.wrap(buf);
		wrapped.putInt(entityID);
		wrapped.putFloat(x);
		wrapped.putFloat(y);
		wrapped.putFloat(xvel);
		wrapped.putFloat(yvel);
		wrapped.putFloat((float)angle);
		return buf;
	}
	
	public static int encodeSize() {
		return 6*8;
	}

}
