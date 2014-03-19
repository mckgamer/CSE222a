package server;

import game.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import shared.ServerMessage;

public class TransferListener extends Thread {

	public DatagramSocket socket = null;
	public ArrayList<ByteBuffer> transfers = new ArrayList<ByteBuffer>();
	public ArrayList<NewServerMessage> newServerMessages = new ArrayList<NewServerMessage>();
	int tSize = 0;
	private boolean isRunning = true;
	public Boolean recieved = false;
	public DatagramPacket packet;
	private GameLogic myLogic;

	public TransferListener(GameLogic logic, int port, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket(port);
		myLogic = logic;
	}

	public void kill() {
		isRunning = false;
	}

	public void run() {

		while (isRunning) {
			try {
				byte[] buf = new byte[1500];

				// receive request
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				recieved = true;
				Server.log.println("YAY " + socket.getLocalPort() + " got transfers from " + packet.getPort());

				ByteBuffer tData = ByteBuffer.wrap(packet.getData()); //TODO remove 40
				
				//Extract packet header and operate on packet
				byte messageType = tData.get();
				switch(messageType) {
				case ServerMessage.TRANSFEROBJ:
					synchronized (transfers) {
						int mySize = tData.getInt() - 5;
						tSize += mySize; 
						transfers.add(ByteBuffer.wrap(packet.getData(),5,mySize));
					}
					break;
				case ServerMessage.NEIGHBORNOTE:
					Neighbor.Direction dir = Neighbor.Direction.values()[tData.get()];
					Neighbor nbor = Neighbor.decode(tData);
					Server.log.println("Got neighbornote: New neighbor is " + nbor + " to my " + dir);
					synchronized(myLogic) {
						Neighbor oldNbor = myLogic.neighbors.put(dir, nbor);
						if(oldNbor != null) {
							Server.log.println("WARNING: Replacing " + oldNbor + " with " + nbor);
						}
					}
					break;
				case ServerMessage.NEWSERVER:
					NewServerMessage msg = NewServerMessage.decode(tData);
					boolean updateAndForward = true;
					
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
					//Am I a neighbor?
					int dist = msg.xOffset * msg.xOffset + msg.yOffset * msg.yOffset;
					if(dist <= 1) {
						Neighbor.Direction newServerDir = msg.getDirection();
						Neighbor newServerLoc = myLogic.neighbors.get(newServerDir);
						if(newServerLoc != null) {		//We already have a neighbor there!
							updateAndForward = false;	//Stop forwarding this newServerMessage
							boolean serversNotEqual = !newServerLoc.equals(msg.newServer); 
							if(newServerLoc.getPriority() < msg.newServer.getPriority()) {
								//Send killnote to newServerLoc
								//Send neighbornote to newServer
								//neighbors.put(newServer)
							} else if(newServerLoc.getPriority() <= msg.newServer.getPriority() && serversNotEqual) {
								//Send killnote to newServer
								//No need to send neighbornote as newServerLoc is already our neighbor
							}
							//Other case: servers are equal. We have seen this message before, and can stop sending it.
						} else {	//No existing neighbor here
							//put newServer as my neighbor
							//send newServer a neighbornote
						}
					}
					if(msg.ttl <= 0) {
						updateAndForward = false;
					}
					if(updateAndForward) {
						Neighbor.Direction sendDir = msg.updateToNextStep(myLogic);
						//send
					}
					break;
				default:
					Server.log.println("Unknown message type " + messageType);
				}

				recieved = false;

			} catch (IOException e) {
				Server.log.printerr(e);
				socket.close();
			}
		}
		socket.close();
		Server.log.println("Dead Really");
	}

}