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
	private ServerThread myServer;

	public TransferListener(ServerThread server, GameLogic logic, int port, String name) throws SocketException {
		super(name);
		socket = new DatagramSocket(port);
		myLogic = logic;
		myServer = server;
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
					boolean forward = true;
					Neighbor.Direction sendDir = Neighbor.Direction.TOPLEFT;
					
					//Am I a neighbor?
					int dist = msg.xOffset * msg.xOffset + msg.yOffset * msg.yOffset;
					if(dist <= 2) {
						Neighbor.Direction newServerDir = msg.getDirection();
						Neighbor newServerLoc = myLogic.neighbors.get(newServerDir);
						if(newServerLoc != null) {		//We already have a neighbor there!
							forward = false;	//Stop forwarding this newServerMessage
							boolean serversNotEqual = !newServerLoc.equals(msg.newServer); 
							if(newServerLoc.getPriority() < msg.newServer.getPriority()) {
								//put newServer as my neighbor
								myLogic.neighbors.put(newServerDir, msg.newServer);
								
								//Send killnote to newServerLoc
								
								//Send neighbornote to newServer
								TransferSender.sendNeighborNote(
									myServer.getTransferSender().socket,
									myServer.toNeighbor(),
									msg.newServer,
									Neighbor.flip(newServerDir)
								);
								Server.log.println("(2) This server is a neighbor, but I have an existing neighbor there (new server won, neighbor " + newServerLoc + " lost) " + msg);
							} else if(newServerLoc.getPriority() <= msg.newServer.getPriority() && serversNotEqual) {
								//Send killnote to newServer
								//No need to send neighbornote as newServerLoc is already our neighbor
								Server.log.println("(2) This server is a neighbor, but I have an existing neighbor there (my neighbor " + newServerLoc + " won) " + msg);
							} else {
								Server.log.println("(3) This server is a neighbor, but I already know about this neighbor " + msg);
							}
							//Other case: servers are equal. We have seen this message before, and can stop sending it.
						} else {	//No existing neighbor here
							//Update before setting neighbors
							sendDir = msg.updateToNextStep(myLogic);
							
							//put newServer as my neighbor
							myLogic.neighbors.put(newServerDir, msg.newServer);
							
							//send newServer a neighbornote
							TransferSender.sendNeighborNote(
								myServer.getTransferSender().socket,
								myServer.toNeighbor(),
								msg.newServer,
								Neighbor.flip(newServerDir)
							);
							
							//The message may get sent, so continue to update the neighbors in the message
							msg.neighbors.put(Neighbor.flip(newServerDir), myServer.toNeighbor());
							
							Server.log.println("(1) This server is a neighbor, and I don't have an existing neighbor there " + msg);
						}
					} else {	//Not a neighbor
						//Update message to prepare for forwarding
						sendDir = msg.updateToNextStep(myLogic);
						
						Server.log.println("(4) This server is not a neighbor " + msg);
					}
					if(msg.ttl <= 0) {
						forward = false;
						Server.log.println("(5) This messages TTL has timed out " + msg);
					}
					if(forward) {
						Neighbor to = myLogic.neighbors.get(sendDir);
						
						if(to == null) {
							Server.log.println("ERROR: Message has the wrong direction " + sendDir);
						} else {
							TransferSender.sendNewServerMessage(myServer.getTransferSender().socket, msg, to);
						}
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