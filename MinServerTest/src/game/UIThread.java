package game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import test.ConnectToHostActionListener;
import test.EnableRoboModeChangeListener;
import server.Neighbor;
import server.Neighbor.Direction;
import server.ServerAddress;
import client.GameThread;
import client.Client;

public class UIThread extends JPanel {
	
	private static final long serialVersionUID = -6714709327930384294L;
	
	ArrayList<GameThread> gThreads = new ArrayList<GameThread>();
	GameInput input;
			
	GameThread myPlayer;
	int myPlayerIndex = 0;

	private boolean drawGrid = true;
	
	public void updateHosts(HashMap<Direction, Neighbor> neighbors) {
		switch (myPlayerIndex) {
		case 0:
			gThreads.get(1).setHost(neighbors.get(Direction.RIGHT).getAddress().ip, neighbors.get(Direction.RIGHT).getAddress().port-1110);
			gThreads.get(2).setHost(neighbors.get(Direction.BOTTOM).getAddress().ip, neighbors.get(Direction.BOTTOM).getAddress().port-1110);
			gThreads.get(3).setHost(neighbors.get(Direction.BOTTOMRIGHT).getAddress().ip, neighbors.get(Direction.BOTTOMRIGHT).getAddress().port-1110);
			break;
		case 1:
			gThreads.get(0).setHost(neighbors.get(Direction.LEFT).getAddress().ip, neighbors.get(Direction.LEFT).getAddress().port-1110);
			gThreads.get(2).setHost(neighbors.get(Direction.BOTTOMLEFT).getAddress().ip, neighbors.get(Direction.BOTTOMLEFT).getAddress().port-1110);
			gThreads.get(3).setHost(neighbors.get(Direction.BOTTOM).getAddress().ip, neighbors.get(Direction.BOTTOM).getAddress().port-1110);
			break;
		case 2:
			gThreads.get(0).setHost(neighbors.get(Direction.TOP).getAddress().ip, neighbors.get(Direction.TOP).getAddress().port-1110);
			gThreads.get(1).setHost(neighbors.get(Direction.TOPRIGHT).getAddress().ip, neighbors.get(Direction.TOPRIGHT).getAddress().port-1110);
			gThreads.get(3).setHost(neighbors.get(Direction.RIGHT).getAddress().ip, neighbors.get(Direction.RIGHT).getAddress().port-1110);
			break;
		case 3:
			gThreads.get(0).setHost(neighbors.get(Direction.TOPLEFT).getAddress().ip, neighbors.get(Direction.TOPLEFT).getAddress().port-1110);
			gThreads.get(1).setHost(neighbors.get(Direction.TOP).getAddress().ip, neighbors.get(Direction.TOP).getAddress().port-1110);
			gThreads.get(2).setHost(neighbors.get(Direction.LEFT).getAddress().ip, neighbors.get(Direction.LEFT).getAddress().port-1110);
			break;
		}
		
	}
	public void switchMyPlayerChunk(int id) {
		myPlayer.gameState.setInControl(null);
		myPlayer.mInput = 0;
		System.out.println("CAYYYYYYY"+myPlayerIndex);
		myPlayerIndex = id;
		myPlayer = gThreads.get(myPlayerIndex);
		myPlayer.gameState.setInControl(this);
		input.setGameThread(myPlayer);
		System.out.println("YAYYYYYYY"+myPlayerIndex);
	}
	
	public void switchChunk(Direction d) {
		switch (d) {
		case BOTTOM:
			if (myPlayerIndex == 0) {
				switchMyPlayerChunk(2);
			} else if (myPlayerIndex == 1) {
				switchMyPlayerChunk(3);
			}
			break;
		case LEFT:
			if (myPlayerIndex == 1) {
				switchMyPlayerChunk(0);
			} else if (myPlayerIndex == 3) {
				switchMyPlayerChunk(2);
			}
			break;
		case RIGHT:
			if (myPlayerIndex == 0) {
				switchMyPlayerChunk(1);
			} else if (myPlayerIndex == 2) {
				switchMyPlayerChunk(3);
			}
			break;
		case TOP:
			if (myPlayerIndex == 2) {
				switchMyPlayerChunk(0);
			} else if (myPlayerIndex == 3) {
				switchMyPlayerChunk(1);
			}
			break;
		}
	}
	
	public void updateMain() {
		if (myPlayerIndex == 0) { //TL active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me!=null && me.x < GameLogic.CHUNK_SIZE / 2) {
				//shift left
				gThreads.get(0).xOffSet *= -1;
				gThreads.get(1).xOffSet *= -1;
				gThreads.get(2).xOffSet *= -1;
				gThreads.get(3).xOffSet *= -1;
				gThreads.add(0, gThreads.remove(1));
				gThreads.add(2, gThreads.remove(3));
				if (gThreads.get(1).gameState.neighbors.get(Direction.LEFT) != null) {
					gThreads.get(0).setHost(gThreads.get(1).gameState.neighbors.get(Direction.LEFT).getAddress().ip, gThreads.get(1).gameState.neighbors.get(Direction.LEFT).getAddress().port-1110);
				}
				if (gThreads.get(3).gameState.neighbors.get(Direction.LEFT) != null) {
					gThreads.get(2).setHost(gThreads.get(3).gameState.neighbors.get(Direction.LEFT).getAddress().ip, gThreads.get(3).gameState.neighbors.get(Direction.LEFT).getAddress().port-1110);
				}
				myPlayerIndex = 1;
			}
			if (me!=null && me.y < GameLogic.CHUNK_SIZE / 2) {
				//shift up
				gThreads.get(0).yOffSet *= -1;
				gThreads.get(1).yOffSet *= -1;
				gThreads.get(2).yOffSet *= -1;
				gThreads.get(3).yOffSet *= -1;
				gThreads.add(0, gThreads.remove(2));
				gThreads.add(1, gThreads.remove(3));
				if (gThreads.get(2).gameState.neighbors.get(Direction.TOP) != null) {
					gThreads.get(0).setHost(gThreads.get(2).gameState.neighbors.get(Direction.TOP).getAddress().ip, gThreads.get(2).gameState.neighbors.get(Direction.TOP).getAddress().port-1110);
				}
				if (gThreads.get(3).gameState.neighbors.get(Direction.TOP) != null) {
					gThreads.get(1).setHost(gThreads.get(3).gameState.neighbors.get(Direction.TOP).getAddress().ip, gThreads.get(3).gameState.neighbors.get(Direction.TOP).getAddress().port-1110);
				}
				myPlayerIndex = 2;
			}
		} else if (myPlayerIndex == 1) { //TR active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			
			if (me!=null && me.x > GameLogic.CHUNK_SIZE / 2) {
				//shift right
				gThreads.get(0).xOffSet *= -1;
				gThreads.get(1).xOffSet *= -1;
				gThreads.get(2).xOffSet *= -1;
				gThreads.get(3).xOffSet *= -1;
				gThreads.add(0, gThreads.remove(1));
				gThreads.add(2, gThreads.remove(3));
				if (gThreads.get(0).gameState.neighbors.get(Direction.RIGHT) != null) {
					gThreads.get(1).setHost(gThreads.get(0).gameState.neighbors.get(Direction.RIGHT).getAddress().ip, gThreads.get(0).gameState.neighbors.get(Direction.RIGHT).getAddress().port-1110);
				}
				if (gThreads.get(2).gameState.neighbors.get(Direction.RIGHT) != null) {
					gThreads.get(3).setHost(gThreads.get(2).gameState.neighbors.get(Direction.RIGHT).getAddress().ip, gThreads.get(2).gameState.neighbors.get(Direction.RIGHT).getAddress().port-1110);
				}
				myPlayerIndex = 0;
			}
			if (me!=null && me.y < GameLogic.CHUNK_SIZE / 2) {
				//shift up
				gThreads.get(0).yOffSet *= -1;
				gThreads.get(1).yOffSet *= -1;
				gThreads.get(2).yOffSet *= -1;
				gThreads.get(3).yOffSet *= -1;
				gThreads.add(0, gThreads.remove(2));
				gThreads.add(1, gThreads.remove(3));
				if (gThreads.get(2).gameState.neighbors.get(Direction.TOP) != null) {
					gThreads.get(0).setHost(gThreads.get(2).gameState.neighbors.get(Direction.TOP).getAddress().ip, gThreads.get(2).gameState.neighbors.get(Direction.TOP).getAddress().port-1110);
				}
				if (gThreads.get(3).gameState.neighbors.get(Direction.TOP) != null) {
					gThreads.get(1).setHost(gThreads.get(3).gameState.neighbors.get(Direction.TOP).getAddress().ip, gThreads.get(3).gameState.neighbors.get(Direction.TOP).getAddress().port-1110);
				}
				myPlayerIndex = 3;
			}
		} else if (myPlayerIndex == 2) { //BL active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me!=null && me.x < GameLogic.CHUNK_SIZE / 2) {
				//shift left
				gThreads.get(0).xOffSet *= -1;
				gThreads.get(1).xOffSet *= -1;
				gThreads.get(2).xOffSet *= -1;
				gThreads.get(3).xOffSet *= -1;
				gThreads.add(0, gThreads.remove(1));
				gThreads.add(2, gThreads.remove(3));
				if (gThreads.get(1).gameState.neighbors.get(Direction.LEFT) != null) {
					gThreads.get(0).setHost(gThreads.get(1).gameState.neighbors.get(Direction.LEFT).getAddress().ip, gThreads.get(1).gameState.neighbors.get(Direction.LEFT).getAddress().port-1110);
				}
				if (gThreads.get(3).gameState.neighbors.get(Direction.LEFT) != null) {
					gThreads.get(2).setHost(gThreads.get(3).gameState.neighbors.get(Direction.LEFT).getAddress().ip, gThreads.get(3).gameState.neighbors.get(Direction.LEFT).getAddress().port-1110);
				}
				myPlayerIndex = 3;
			}
			if (me!=null && me.y > GameLogic.CHUNK_SIZE / 2) {
				//shift Down
				gThreads.get(0).yOffSet *= -1;
				gThreads.get(1).yOffSet *= -1;
				gThreads.get(2).yOffSet *= -1;
				gThreads.get(3).yOffSet *= -1;
				gThreads.add(0, gThreads.remove(2));
				gThreads.add(1, gThreads.remove(3));
				if (gThreads.get(0).gameState.neighbors.get(Direction.BOTTOM) != null) {
					gThreads.get(2).setHost(gThreads.get(0).gameState.neighbors.get(Direction.BOTTOM).getAddress().ip, gThreads.get(0).gameState.neighbors.get(Direction.BOTTOM).getAddress().port-1110);
				}
				if (gThreads.get(1).gameState.neighbors.get(Direction.BOTTOM) != null) {
					gThreads.get(3).setHost(gThreads.get(1).gameState.neighbors.get(Direction.BOTTOM).getAddress().ip, gThreads.get(1).gameState.neighbors.get(Direction.BOTTOM).getAddress().port-1110);
				}
				myPlayerIndex = 0;
			}
		} else { //BR active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me!=null && me.x > GameLogic.CHUNK_SIZE / 2) {
				//shift right
				gThreads.get(0).xOffSet *= -1;
				gThreads.get(1).xOffSet *= -1;
				gThreads.get(2).xOffSet *= -1;
				gThreads.get(3).xOffSet *= -1;
				gThreads.add(0, gThreads.remove(1));
				gThreads.add(2, gThreads.remove(3));
				if (gThreads.get(0).gameState.neighbors.get(Direction.RIGHT) != null) {
					gThreads.get(1).setHost(gThreads.get(0).gameState.neighbors.get(Direction.RIGHT).getAddress().ip, gThreads.get(0).gameState.neighbors.get(Direction.RIGHT).getAddress().port-1110);
				}
				if (gThreads.get(2).gameState.neighbors.get(Direction.RIGHT) != null) {
					gThreads.get(3).setHost(gThreads.get(2).gameState.neighbors.get(Direction.RIGHT).getAddress().ip, gThreads.get(2).gameState.neighbors.get(Direction.RIGHT).getAddress().port-1110);
				}
				myPlayerIndex = 2;
			}
			if (me!=null && me.y > GameLogic.CHUNK_SIZE / 2) {
				//shift down
				gThreads.get(0).yOffSet *= -1;
				gThreads.get(1).yOffSet *= -1;
				gThreads.get(2).yOffSet *= -1;
				gThreads.get(3).yOffSet *= -1;
				gThreads.add(0, gThreads.remove(2));
				gThreads.add(1, gThreads.remove(3));
				if (gThreads.get(0).gameState.neighbors.get(Direction.BOTTOM) != null) {
					gThreads.get(2).setHost(gThreads.get(0).gameState.neighbors.get(Direction.BOTTOM).getAddress().ip, gThreads.get(0).gameState.neighbors.get(Direction.BOTTOM).getAddress().port-1110);
				}
				if (gThreads.get(1).gameState.neighbors.get(Direction.BOTTOM) != null) {
					gThreads.get(3).setHost(gThreads.get(1).gameState.neighbors.get(Direction.BOTTOM).getAddress().ip, gThreads.get(1).gameState.neighbors.get(Direction.BOTTOM).getAddress().port-1110);
				}
				myPlayerIndex = 1;
			}
		} 
		//System.out.println(myPlayer.gameState.neighbors.get(Neighbor.RIGHT).ip.toString());
		//System.out.println(myPlayer.gameState.neighbors.get(Neighbor.RIGHT).port-1110);
		//gThreads.get(2).setHost(myPlayer.gameState.neighbors.get(Neighbor.RIGHT).ip, myPlayer.gameState.neighbors.get(Neighbor.LEFT).port-1110);
	}
	
	public UIThread()                       // set up graphics window
    {
        super();
        setBackground(Color.WHITE);
        setFocusable(true);
        
        JFrame application = new JFrame();                            // the program itself
        
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   // set frame to exit
                                                                      // when it is closed
        //Set up the top bar
        JToggleButton enableRoboModeButton = new JToggleButton("Enable Robo-Mode");
        JToggleButton gridButton = new JToggleButton("Grid");
        gridButton.setSelected(true);
        
        JLabel ipLabel = new JLabel("Server IP:");
        
        JTextField ipTextField = new JTextField(15);

        JPanel topBarPanel = new JPanel(new FlowLayout());
        topBarPanel.add(enableRoboModeButton);
        topBarPanel.add(ipLabel);
        topBarPanel.add(ipTextField);
        topBarPanel.add(gridButton);
        
        //Set up the main application window
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topBarPanel, BorderLayout.NORTH);
        mainPanel.add(this, BorderLayout.CENTER);
        application.add(mainPanel);


        application.setSize(700, 700);	//Window sizes
        application.setVisible(true); 
        
        try {
        	String ip = "127.0.0.1";
			gThreads.add(new GameThread(InetAddress.getByName(ip),4440,-GameLogic.CHUNK_SIZE / 2,-GameLogic.CHUNK_SIZE / 2));
			gThreads.add(new GameThread(null,0,GameLogic.CHUNK_SIZE / 2,-GameLogic.CHUNK_SIZE / 2));
			gThreads.add(new GameThread(null,0,-GameLogic.CHUNK_SIZE / 2,GameLogic.CHUNK_SIZE / 2));
			gThreads.add(new GameThread(null,0,GameLogic.CHUNK_SIZE / 2,GameLogic.CHUNK_SIZE / 2));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			Client.log.printerr(e1);
		}
        
        for (GameThread g: gThreads) {
        	g.start();	
        }
        
        input = new GameInput(gThreads.get(myPlayerIndex));
        this.addKeyListener(input);
        myPlayer = gThreads.get(myPlayerIndex);
        myPlayer.gameState.setInControl(this);

        //gThreads.get(1).gameState.neighbors.get(Neighbor.TOP)

        //Add action listeners
        enableRoboModeButton.addChangeListener(new EnableRoboModeChangeListener(this, input));
        gridButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				drawGrid = !drawGrid;
			} });
        ipTextField.addActionListener(new ConnectToHostActionListener(this, gThreads.get(0), ipTextField));

        this.requestFocus();
        
        while (true) {
        	repaint();
        	input.generateKeyPresses();
        	try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				Client.log.printerr(e);
			}
        }
    }

    public void paintComponent(Graphics g)  // draw graphics in the panel
    {
    	Player pTemp;
    	if ((pTemp = myPlayer.gameState.players.get(myPlayer.mClientID)) != null) {
        int width = getWidth();             // width of window in pixels
        int height = getHeight();           // height of window in pixels

        super.paintComponent(g);            // call superclass to make panel display correctly
        
        //Get position of player
        int pX = 0, pY = 0;
        if ((pTemp = myPlayer.gameState.players.get(myPlayer.mClientID)) != null) {
        	pX = width/2 - (int) pTemp.x - myPlayer.xOffSet;
        	pY = height/2 - (int) pTemp.y - myPlayer.yOffSet;
        }
      
		updateMain();
        
        int temp = 1;
        for (GameThread gThread : gThreads) {
        	//Draw Grid
        	if (drawGrid) {
        		drawChunkBorders(g, pX, pY, gThread);
        	}
        	
        	//Print Stats for each Client
        	g.drawString("Normal: " + (double) 100*gThread.normal / (gThread.normal + gThread.outOfSync)
    				+ "% OOS: " + (double) 100*gThread.outOfSync / (gThread.normal + gThread.outOfSync) + "%", 10, 20*(temp++));
            
        	g.drawString("Port: "+gThread.port, 10, 60+20*temp);
        	
        	//Get Position of this Chunk
            int cX = gThread.xOffSet;//-((gThread.players.get(gThread.mClientID)!=null)?(int)gThread.players.get(gThread.mClientID).x:0)+width/2;
            int cY = gThread.yOffSet;//-((gThread.players.get(gThread.mClientID)!=null)?(int)gThread.players.get(gThread.mClientID).y:0)+height/2;

	        // Drawing code goes here
            synchronized (gThread.gameState.players) {
		        for (Player p: gThread.gameState.players.values()) {
		        	if (gThread == myPlayer && gThread.mClientID == p.entityID) {
		        		g.setColor(new Color(255,0,0));
		        		
		        	}
		        	
		        	int brX = (int)(9*Math.cos(p.angle+Math.PI/3));
		        	int brY = (int)(9*Math.sin(p.angle+Math.PI/3));
		        	int blX = (int)(9*Math.cos(p.angle-Math.PI/3));
		        	int blY = (int)(9*Math.sin(p.angle-Math.PI/3));
		        	int tX = (int)(14*Math.cos(p.angle));
		        	int tY = (int)(14*Math.sin(p.angle));
		        	g.drawLine((int)p.x+tX+cX+pX, (int)p.y+tY+cY+pY, (int)p.x+brX+cX+pX, (int)p.y+brY+cY+pY); 
		        	g.drawLine((int)p.x+tX+cX+pX, (int)p.y+tY+cY+pY, (int)p.x+blX+cX+pX, (int)p.y+blY+cY+pY);  
		        	g.drawLine((int)p.x+blX+cX+pX, (int)p.y+blY+cY+pY, (int)p.x+brX+cX+pX, (int)p.y+brY+cY+pY); 
		        	if (gThread == myPlayer && gThread.mClientID == p.entityID) {
		        		g.setColor(new Color(0,0,0));
		        		
		        	}
		        }
            }
	        
            synchronized (gThread.gameState.bullets) {
		        for (Bullet b: gThread.gameState.bullets.values()) {
		        	g.drawLine((int)b.x+cX+pX,(int)b.y+cY+pY,(int)(b.x+b.xvel)+cX+pX,(int)(b.y+b.yvel)+cY+pY);
		        }
            }
        }
		}
    }
    
    //Draw chunk borders.  Red means there is no neighbor set, green means there is a neighbor set
    private void drawChunkBorders(Graphics g, int x, int y, GameThread gt) {
    	Neighbor top = gt.gameState.neighbors.get(Neighbor.Direction.TOP);
    	Neighbor left = gt.gameState.neighbors.get(Neighbor.Direction.LEFT);
    	Neighbor bottom = gt.gameState.neighbors.get(Neighbor.Direction.BOTTOM);
    	Neighbor right = gt.gameState.neighbors.get(Neighbor.Direction.RIGHT);
    	
    	int minX = gt.xOffSet + x;
    	int minY = gt.yOffSet + y;
    	int maxX = gt.xOffSet + x + GameLogic.CHUNK_SIZE;
    	int maxY = gt.yOffSet + y + GameLogic.CHUNK_SIZE;
    	
    	//Top
    	if(top == null) {
    		g.setColor(Color.red);
    	} else {
    		g.setColor(Color.green);
    	}
    	g.drawLine(minX, minY, maxX, minY);
    	
    	//Left
    	if(left == null) {
    		g.setColor(Color.red);
    	} else {
    		g.setColor(Color.green);
    	}
    	g.drawLine(minX, minY, minX, maxY);
    	
    	//Bottom
    	if(bottom == null) {
    		g.setColor(Color.red);
    	} else {
    		g.setColor(Color.green);
    	}
    	g.drawLine(minX, maxY, maxX, maxY);
    	
    	//Right
    	if(right == null) {
    		g.setColor(Color.red);
    	} else {
    		g.setColor(Color.green);
    	}
    	g.drawLine(maxX, minY, maxX, maxY);
    	
    	
    	
    	g.setColor(Color.black);
    	
    	g.drawString("My id: " + gt.port, x + GameLogic.CHUNK_SIZE / 2 + gt.xOffSet, y + GameLogic.CHUNK_SIZE / 2 + gt.yOffSet);
    }


}
