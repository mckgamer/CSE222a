package test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import client.GameThread;

public class RoboThread {

	private List<GameThread> gThreads = new ArrayList<GameThread>();
	
	public RoboThread(int port, Point pos, int chunkSize) {
        /*
        gThreads.add(new GameThread(port, pos.x - chunkSize, pos.y - chunkSize));
        gThreads.add(new GameThread(port, pos.x + chunkSize, pos.y - chunkSize));
        gThreads.add(new GameThread(port, pos.x - chunkSize, pos.y + chunkSize));
        gThreads.add(new GameThread(port, pos.x + chunkSize, pos.y + chunkSize));
        
        for (GameThread g: gThreads) {
        	g.start();
        }
        */
	}
	
	//Disconnects all client threads
	public void disconnect() {
	}
	

	
	public int getId() {
		return 0;//gThreads.get(0).mClientID;
	}
}
