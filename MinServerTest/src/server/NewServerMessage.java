package server;

import game.GameLogic;

import java.nio.ByteBuffer;
import java.util.HashMap;


/**
 * This class can decode a new server message and decide on the next appropriate action,
 * given the particular server's neighbors.
 * @author Nathan Heisey
 *
 */
public class NewServerMessage {
	protected byte stepDir;
	protected byte circleDir;
	protected int xOffset, yOffset;
	protected Neighbor newServer;
	public HashMap<Neighbor.Direction,Neighbor> neighbors = new HashMap<Neighbor.Direction,Neighbor>();
	
	private Stepper [] steppers;
	/*
	private static final byte STEP_NORTH = 0;
	private static final byte STEP_EAST = 1;
	private static final byte STEP_SOUTH = 2;
	private static final byte STEP_WEST = 3;
	*/
	private static final byte CIRCLE_CCW = 0;
	private static final byte CIRCLE_CW = 1;
	
	//Accessors
	
	/**
	 * This function changes the properties of NewServerMessage so that an encode() will produce a message
	 * that can be sent to the appropriate recipient.
	 * @return A code indicating actions to be taken by the server thread
	 */
	public int updateToNextStep(GameLogic logic) {
		/*
		if(circleDir == CIRCLE_CCW) {
			return stepCounterClockwise(logic);
		} else {
			return stepClockwise(logic);
		}
		*/
		
		for(int i = 0; i < steppers.length; ++i) {
			byte curStepper = (byte) ((i + stepDir) % steppers.length); 
			if(steppers[curStepper].canStep(logic)) {
				//Take a step
				steppers[curStepper].step(this);
				
				//Update the step direction
				stepDir = (byte) (curStepper - 1);
				if(stepDir < 0) {
					stepDir = (byte) (steppers.length - 1);
				}
				break;
			}
		}
		
		return 0;
	}
	
	public void encode(ByteBuffer buf) {	//Encodes the current status of this new server message
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
		byte circleDir = buf.get();
		byte stepDir = buf.get();
		int xOffset = buf.getInt();
		int yOffset = buf.getInt();
		Neighbor newServer = Neighbor.decode(buf);
		
		NewServerMessage msg = new NewServerMessage(circleDir, stepDir, xOffset, yOffset);
		
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
	
	private NewServerMessage(byte circleDir, byte stepDir, int xOffset, int yOffset) {
		this.circleDir = circleDir;
		this.stepDir = stepDir;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		//The neighbors are set by direct variable modification
		
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
		public void step(NewServerMessage msg);
	}
	
	private class NorthStepper implements Stepper {

		@Override
		public void step(NewServerMessage msg) {
			msg.yOffset--;	//Change the offset
			//dir = WEST;
			System.out.println("North clear, aim west");
		}

		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.TOP) != null;
		}
	}
	
	private class EastStepper implements Stepper {
		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.RIGHT) != null;
		}

		@Override
		public void step(NewServerMessage msg) {
			msg.xOffset++;
			//dir = NORTH;
			System.out.println("East clear, aim north");
		}
	}

	private class SouthStepper implements Stepper {
		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.BOTTOM) != null;
		}

		@Override
		public void step(NewServerMessage msg) {
			msg.yOffset++;
			//dir = EAST;
			System.out.println("South clear, aim east");
		}
	}
	
	private class WestStepper implements Stepper {
		@Override
		public boolean canStep(GameLogic logic) {
			return logic.neighbors.get(Neighbor.Direction.LEFT) != null;
		}

		@Override
		public void step(NewServerMessage msg) {
			msg.xOffset--;
			//dir = SOUTH;
			System.out.println("West clear, aim south");
		}
	}
	
}
