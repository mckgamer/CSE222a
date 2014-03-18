package test;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JPanel;

import server.Neighbor;
import server.Server;

public class ServerLayoutPanel extends JPanel {

    private ServerInfo [][] grid;
    private int gridWidth;
    private int gridHeight;
    private HashMap<Integer,Color> playerToColor = new HashMap<Integer,Color>();
    private static final int CELL_WIDTH = 5;
    private static final int CELL_HEIGHT = 5;
	
    public ServerLayoutPanel(int width, int height) {
    	gridWidth = width;
    	gridHeight = height;

    	grid = new ServerInfo[width][height];
    	for(int i = 0; i < gridWidth; ++i) {
    		for(int j = 0; j < gridHeight; ++j) {
    			grid[i][j] = new ServerInfo();
    		}
    	}
    }
    
    public void registerPlayer(int id, Color color) {
    	playerToColor.put(id, color);
    }
    
    public void registerServer(int x, int y, int serverId, int playerId) {
    	grid[x][y].playerId = playerId;
    	grid[x][y].serverId = serverId;
    	repaint();
    }
    
    public void registerServer(int neighborId, Neighbor.Direction dirFromNeighbor, int serverId, int playerId) {
    	//Find the known server (default is top-left corner with extra room for a top or left registration)
    	int x = 1, y = 1;
    	for(int i = 0; i < gridWidth; ++i) {
    		for(int j = 0; j < gridHeight; ++j) {
    			if(grid[i][j].serverId == neighborId) {
    				x = i;
    				y = j;
    				
    				//I don't trust break to work in a double-for loop
    				i = gridWidth;
    				j = gridHeight;
    			}
    		}
    	}
    	
    	//Adjust the x/y coords according to the neighbor direction
    	switch(dirFromNeighbor) {
    	case TOPLEFT:
    		x--;
    		y--;
    		break;
    	case TOP:
    		y--;
    		break;
    	case TOPRIGHT:
    		x++;
    		y--;
    		break;
    	case RIGHT:
    		x++;
    		break;
    	case BOTTOMRIGHT:
    		x++;
    		y++;
    		break;
    	case BOTTOM:
    		y++;
    		break;
    	case BOTTOMLEFT:
    		x--;
    		y++;
    		break;
    	case LEFT:
    		x--;
    		break;
		default:
			break;
    	}

    	//Register the server
    	registerServer(x, y, serverId, playerId);
    }
    
	@Override
	public void paintComponent(Graphics g) {
		for(int i = 0; i < gridWidth; ++i) {
			for(int j = 0; j < gridHeight; ++j) {
				Color c = playerToColor.get(grid[i][j].playerId);
				if(c == null) {
					c = Color.black;
				}
				g.setColor(c);
				g.fillRect(i*CELL_WIDTH, j*CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
				
				g.setColor(Color.black);
				g.drawRect(i*CELL_WIDTH, j*CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
			}
		}
	}
	
	private class ServerInfo {
		public int playerId = 0;
		public int serverId = 0;
	}
}
