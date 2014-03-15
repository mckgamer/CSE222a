package game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
import client.GameThread;

public class UIThread extends JPanel {
	
	private static final long serialVersionUID = -6714709327930384294L;
	
	ArrayList<GameThread> gThreads = new ArrayList<GameThread>();
	GameInput input;
			
	GameThread myPlayer;
	int myPlayerIndex = 0;
	
	
	public void updateMain() {
		if (myPlayerIndex == 0) { //TL active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me!=null && me.x < 250) {
				//shift left
				gThreads.remove(1);
				gThreads.add(1,gThreads.get(0));//.setHost(gThreads.get(0).host, gThreads.get(0).port);
				gThreads.get(0).setHost(myPlayer.gameState.neighbors.get(Neighbor.LEFT).ip, myPlayer.gameState.neighbors.get(Neighbor.LEFT).port-1110);
				myPlayer = gThreads.get(1);
				myPlayerIndex = 1;
				input.setGameThread(myPlayer);
			}
			if (me!=null && me.y < 250) {
				//shift up
			}
		} else if (myPlayerIndex == 1) { //TR active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me!=null && me.x > 250) {
				//shift right
			}
			if (me!=null && me.y < 250) {
				//shift up
			}
		} else if (myPlayerIndex == 2) { //BL active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me.x < 250) {
				//shift left
			}
			if (me.y > 250) {
				//shift Down
			}
		} else { //BR active
			Player me = myPlayer.gameState.players.get(myPlayer.mClientID);
			if (me.x > 250) {
				//shift left
			}
			if (me.y > 250) {
				//shift down
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
        
        JLabel ipLabel = new JLabel("Server IP:");
        
        JTextField ipTextField = new JTextField(15);

        JPanel topBarPanel = new JPanel(new FlowLayout());
        topBarPanel.add(enableRoboModeButton);
        topBarPanel.add(ipLabel);
        topBarPanel.add(ipTextField);
        
        //Set up the main application window
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topBarPanel, BorderLayout.NORTH);
        mainPanel.add(this, BorderLayout.CENTER);
        this.requestFocus();
        application.add(mainPanel);


        application.setSize(700, 700);         // window is 500 pixels wide, 400 high
        application.setVisible(true); 
        
        try {
			gThreads.add(new GameThread(InetAddress.getByName("127.0.0.1"),4440,-250,-250));
			gThreads.add(new GameThread(InetAddress.getByName("127.0.0.1"),4441,250,-250));
			gThreads.add(new GameThread(InetAddress.getByName("127.0.0.1"),4442,-250,250));
			gThreads.add(new GameThread(InetAddress.getByName("127.0.0.1"),4443,250,250));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        for (GameThread g: gThreads) {
        	g.start();	
        }
        
        input = new GameInput(gThreads.get(myPlayerIndex));
        this.addKeyListener(input);
        myPlayer = gThreads.get(myPlayerIndex);

        //gThreads.get(1).gameState.neighbors.get(Neighbor.TOP)

        //Add action listeners
        enableRoboModeButton.addChangeListener(new EnableRoboModeChangeListener(this, input));
        ipTextField.addActionListener(new ConnectToHostActionListener(this, gThreads.get(0), ipTextField));

        
        while (true) {
        	repaint();
        	input.generateKeyPresses();
        	try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }

    public void paintComponent(Graphics g)  // draw graphics in the panel
    {
        int width = getWidth();             // width of window in pixels
        int height = getHeight();           // height of window in pixels

        super.paintComponent(g);            // call superclass to make panel display correctly

        //Get position of player
        Player pTemp;
        int pX = 0, pY = 0;
        if ((pTemp = myPlayer.gameState.players.get(myPlayer.mClientID)) != null) {
        	pX = width/2 - (int) pTemp.x - myPlayer.xOffSet;
        	pY = height/2 - (int) pTemp.y - myPlayer.yOffSet;
        }
        
        //Draw Grid 
        g.setColor(new Color(0,0,250));
        for (int l=-250;l<751;l+=500) {
        	g.drawLine(l+pX, 0+pY-250, l+pX, 750+pY);
        }
        for (int t=-250;t<751;t+=500) {
        	g.drawLine(0+pX-250, t+pY, 750+pX, t+pY);
        }
        g.setColor(new Color(0,0,0));
        
			updateMain();
        
        int temp = 1;
        for (GameThread gThread : gThreads) {
        	
        	//Print Stats for each Client
        	g.drawString("Normal: " + (double) 100*gThread.normal / (gThread.normal + gThread.outOfSync)
    				+ "% OOS: " + (double) 100*gThread.outOfSync / (gThread.normal + gThread.outOfSync) + "%", 10, 20*(temp++));
            
        	
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
