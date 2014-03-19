package server;

import game.GameLogic;

import java.nio.ByteBuffer;
import java.util.HashMap;

import server.Neighbor.Direction;


/**
 * This class can decode a new server message and decide on the next appropriate action,
 * given the particular server's neighbors.
 * @author Nathan Heisey
 *
 */
public class NewServerMessage {
	//Information sent in the message
	protected int ttl;			//Maximum number of steps this message can take before dying (killswitch)
	protected byte stepDir;		//Index into the appropriate stepper array
	protected byte circleDir;	//Direction to circle & stepper array initialization
	protected int xOffset, yOffset;
	protected Neighbor newServer;
	protected HashMap<Neighbor.Direction,Neighbor> neighbors = new HashMap<Neighbor.Direction,Neighbor>();
	public static final int SIZE = 
		4 +
		1 +
		1 +
		4 + 4 +
		Neighbor.ENCODE_SIZE +
		Neighbor.ENCODE_SIZE * Neighbor.Direction.values().length;
	
	//Information to determine the next step
	private Stepper [] steppers;
	
	//STEP directions are relative to counterclockwise.  For clockwise, EAST and WEST are flipped
	private static final byte STEP_NORTH = 0;
	private static final byte STEP_EAST = 1;
	private static final byte STEP_SOUTH = 2;
	private static final byte STEP_WEST = 3;

	public static final byte CIRCLE_CCW = 0;
	public static final byte CIRCLE_CW = 1;
	
	//Accessors
	

	/* What actions need to be taken
	 * Cases:
	 * (1) This server is a neighbor, and I don't have an existing neighbor there
	 * 		-Set this server as my neighbor
	 * 		-Send this server a neighbor note
	 * 		-Forward the message (call updateToNextStep, send to neighbor indicated by the return value)
	 * (2) This server is a neighbor, but I have an existing neighbor there
	 * 		-Set the higher-priority server as my neighbor
	 * 		-Send the higher-priority server a neighbor note
	 * 		-Send the lower-priority server a kill note
	 * 		-Stop forwarding the message
	 * (3) This server is a neighbor, but I already know about this neighbor
	 * 		-Stop forwarding the message
	 * (4) This server is not a neighbor
	 * 		-Forward the message (call updateToNextStep, send to neighbor indicated by the return value)
	 * (5) This messages TTL has timed out
	 * 		-Stop forwarding the message
	 */ 
	
	/**
	 * This function changes the properties of NewServerMessage so that an encode() will produce a message
	 * that can be sent to the appropriate recipient.
	 * @return A code indicating actions to be taken by the server thread
	 */
	public Neighbor.Direction updateToNextStep(GameLogic logic) {
		Neighbor.Direction dirToStepNext = Neighbor.Direction.TOPLEFT;	//Arbitrary direction, should always be overridden
		ttl--;
		
		//Determine which way to step next
		for(int i = 0; i < steppers.length; ++i) {
			byte curStepper = (byte) ((i + stepDir) % steppers.length); 
			if(steppers[curStepper].canStep(logic)) {
				//Take a step
				steppers[curStepper].step(this);
				dirToStepNext = steppers[curStepper].toDirection();
				
				//Update the step direction
				stepDir = (byte) (curStepper - 1);
				if(stepDir < 0) {
					stepDir = (byte) (steppers.length - 1);
				}
				break;
			}
		}
		return dirToStepNext;
	}
	
	public Neighbor.Direction getDirection() {	//This is the direction to look for a neighbor
		if(xOffset < 0 && yOffset == 0) {
			return Direction.RIGHT;
		} else if(xOffset < 0 && yOffset < 0) {
			return Direction.BOTTOMRIGHT;
		} else if(xOffset == 0 && yOffset < 0) {
			return Direction.BOTTOM;
		} else if(xOffset > 0 && yOffset < 0) {
			return Direction.BOTTOMLEFT;
		} else if(xOffset > 0 && yOffset == 0) {
			return Direction.LEFT;
		} else if(xOffset > 0 && yOffset > 0) {
			return Direction.TOPLEFT;
		} else if(xOffset == 0 && yOffset > 0) {
			return Direction.TOP;
		} else /*if(xOffset < 0 && yOffset > 0)*/ {
			return Direction.TOPRIGHT;
		}
	}
	
	public void encode(ByteBuffer buf) {	//Encodes the current status of this new server message
		buf.putInt(ttl);
		buf.put(circleDir);
		buf.put(stepDir);
		buf.putInt(xOffset);
		buf.putInt(yOffset);
		
		//If this one is null, we have a serious problem
		newServer.encode(buf);
		
		byte [] nullBytes = new byte[Neighbor.ENCODE_SIZE];
    	
    	for(Neighbor.Direction dir : Neighbor.Direction.values()) {
    		//If the neighbor exists, encode it.
    		Neighbor nbor = neighbors.get(dir);
        	if(nbor != null) {
        		nbor.encode(buf);
        	} else {
        		buf.put(nullBytes);
        	}
    	}
	}
	
	public static NewServerMessage decode(ByteBuffer buf) {
		//Read the appropriate fields from the buffer
		int ttl = buf.getInt();
		byte circleDir = buf.get();
		byte stepDir = buf.get();
		int xOffset = buf.getInt();
		int yOffset = buf.getInt();
		Neighbor newServer = Neighbor.decode(buf);
		
		NewServerMessage msg = new NewServerMessage(ttl, circleDir, stepDir, xOffset, yOffset);
		
		//The new server
		msg.newServer = newServer;
		
		//Known neighbors (null if not known)
		for(Neighbor.Direction dir : Neighbor.Direction.values()) {
    		//If the neighbor exists, encode it.
    		Neighbor nbor = Neighbor.decode(buf);
        	if(nbor != null) {
        		msg.neighbors.put(dir, nbor);
        	}
    	}
		return msg;
	}
	
	//This will create a new NewServerMessage for a particular circle
	// direction, neighbor direction, and other specified inputs.
	// To send, call updateToNextStep(), then encode().  Make sure to prefix
	// the buffer with the ServerMessage.NEWSERVER byte before encoding.
	public static NewServerMessage create(int ttl, byte circleDir, Neighbor.Direction dirToNewServer, Neighbor newServer, Neighbor neighbor) {
		int x = 0, y = 0;	//offsets
		byte stepDir = 0;
		Neighbor.Direction dirFromNewServer = Neighbor.flip(dirToNewServer);
    	switch(dirFromNewServer) {
    	case TOPLEFT:
    		x--;
    		y--;
    		break;
    	case TOP:
    		y--;
    		stepDir = STEP_WEST;	//This is equivalent to east for clockwise.
    		break;
    	case TOPRIGHT:
    		x++;
    		y--;
    		break;
    	case RIGHT:
    		x++;
    		stepDir = (circleDir == CIRCLE_CCW) ? STEP_NORTH : STEP_SOUTH;
    		break;
    	case BOTTOMRIGHT:
    		x++;
    		y++;
    		break;
    	case BOTTOM:
    		y++;
    		stepDir = STEP_EAST;	//This is equivalent to west for clockwise.
    		break;
    	case BOTTOMLEFT:
    		x--;
    		y++;
    		break;
    	case LEFT:
    		x--;
    		stepDir = (circleDir == CIRCLE_CCW) ? STEP_SOUTH : STEP_NORTH;
    		break;
		default:
			break;
    	}
    	
    	NewServerMessage msg = new NewServerMessage(ttl, circleDir, stepDir, x, y);
    	msg.newServer = newServer;
    	msg.neighbors.put(dirFromNewServer, neighbor);
    	return msg;
	}
	
	private NewServerMessage(int ttl, byte circleDir, byte stepDir, int xOffset, int yOffset) {
		this.ttl = ttl;
		this.circleDir = circleDir;
		this.stepDir = stepDir;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		//The neighbors are set by direct variable modification
		
		//The steppers are used to determine the next step; basically, a stand-in for function pointers.
		steppers = new Stepper[4];
		if(circleDir == CIRCLE_CCW) {
			steppers[0] = new NorthStepper();
			steppers[1] = new EastStepper();
			steppers[2] = new SouthStepper();
			steppers[3] = new WestStepper();
		} else {
			steppers[0] = new NorthStepper();
			steppers[1] = new WestStepper();
			steppers[2] = new SouthStepper();
			steppers[3] = new EastStepper();
		}
	}
	
	private interface Stepper {
		public boolean canStep(GameLogic logic);
		public int step(NewServerMessage msg);		//Performs a step and returns the direction to step
		public Neighbor.Direction toDirection();
	}
	
	private class NorthStepper implements Stepper {

		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.TOP) != null;
		}

		@Override
		public int step(NewServerMessage msg) {
			msg.yOffset--;	//Change the offset
			Server.log.println("Stepped north");
			return STEP_NORTH;
		}

		@Override
		public Direction toDirection() {
			return Neighbor.Direction.TOP;
		}
	}
	
	private class EastStepper implements Stepper {
		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.RIGHT) != null;
		}

		@Override
		public int step(NewServerMessage msg) {
			msg.xOffset++;
			Server.log.println("Stepped east");
			return STEP_EAST;
		}

		@Override
		public Direction toDirection() {
			return Neighbor.Direction.RIGHT;
		}
	}

	private class SouthStepper implements Stepper {
		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.BOTTOM) != null;
		}

		@Override
		public int step(NewServerMessage msg) {
			msg.yOffset++;
			Server.log.println("Stepped south");
			return STEP_SOUTH;
		}

		@Override
		public Direction toDirection() {
			return Neighbor.Direction.BOTTOM;
		}
	}
	
	private class WestStepper implements Stepper {
		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.LEFT) != null;
		}

		@Override
		public int step(NewServerMessage msg) {
			msg.xOffset--;
			Server.log.println("Stepped west");
			return STEP_WEST;
		}

		@Override
		public Direction toDirection() {
			return Neighbor.Direction.LEFT;
		}
	}
	
	@Override
	public String toString() {
		String sdir = "";
		switch(stepDir) {
		case STEP_NORTH:
			sdir = "STEP_NORTH";
			break;

		case STEP_EAST:
			if(circleDir == CIRCLE_CCW) {
				sdir = "STEP_EAST (CCW)";
			} else {
				sdir = "STEP_WEST (CW)";
			}
			break;
		case STEP_SOUTH:
			sdir = "STEP_SOUTH";
			break;
		case STEP_WEST:
			if(circleDir == CIRCLE_CCW) {
				sdir = "STEP_WEST (CCW)";
			} else {
				sdir = "STEP_EAST (CW)";
			}
			break;
		}
		String cdir = (circleDir == CIRCLE_CCW) ? "CIRCLE_CCW" : "CIRCLE_CW";
		String r = "New server msg: TTL(" + ttl + "), " + sdir + ", " + cdir + ", (" + xOffset + "," + yOffset + "), " + newServer + "; ";
		for(Neighbor.Direction dir : Neighbor.Direction.values()) {
			Neighbor nbor = neighbors.get(dir);
			if(nbor == null) {
				r += dir + "=?, ";
			} else {
				r += dir + "=" + nbor + ", ";
			}
		}
		return r;
	}
}
