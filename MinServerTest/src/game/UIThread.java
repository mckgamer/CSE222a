package game;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class UIThread extends JPanel {
	
	ArrayList<GameThread> gThreads = new ArrayList<GameThread>();
	GameInput input;
			
	GameThread myPlayer;
	int myPlayerIndex = 0;
	
	public UIThread()                       // set up graphics window
    {
        super();
        setBackground(Color.WHITE);
        setFocusable(true);
        
        JFrame application = new JFrame();                            // the program itself
        
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   // set frame to exit
                                                                      // when it is closed
        application.add(this);           


        application.setSize(700, 700);         // window is 500 pixels wide, 400 high
        application.setVisible(true); 
        
        gThreads.add(new GameThread(4445,-250,-250));
        gThreads.add(new GameThread(4445,250,-250));
        gThreads.add(new GameThread(4445,-250,250));
        gThreads.add(new GameThread(4445,250,250));
        
        for (GameThread g: gThreads) {
        	g.start();
        	
        }
        
        input = new GameInput(gThreads.get(myPlayerIndex));
        addKeyListener(input);
        myPlayer = gThreads.get(myPlayerIndex);
        
        while (true) {
        	repaint();
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
        if ((pTemp = myPlayer.players.get(myPlayer.mClientID)) != null) {
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
        
        
        for (GameThread gThread : gThreads) {
        	
        	//Get Position of Player
            int cX = gThread.xOffSet;//-((gThread.players.get(gThread.mClientID)!=null)?(int)gThread.players.get(gThread.mClientID).x:0)+width/2;
            int cY = gThread.yOffSet;//-((gThread.players.get(gThread.mClientID)!=null)?(int)gThread.players.get(gThread.mClientID).y:0)+height/2;
            
	        // Drawing code goes here
            Player removeP = null;
	        for (Player p: gThread.players.values()) {
	        	if (gThread == myPlayer && gThread.mClientID == p.entityID) {
	        		g.setColor(new Color(255,0,0));
	        	}
	        	if (p.x>500 || p.x<0 || p.y<0 || p.y>500) {
	        		if (gThread == myPlayer && gThread.mClientID == p.entityID) {
	        			myPlayer = gThreads.get(1);
        				input.setGameThread(myPlayer);
	        		}
        			g.setColor(new Color(0,255,0));
        			removeP = p;
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
	        if (removeP != null) {
	        	gThread.players.remove(removeP.entityID);
	        }
	        
	        for (Bullet b: gThread.bullets.values()) {
	        	g.drawLine((int)b.x+cX+pX,(int)b.y+cY+pY,(int)(b.x+b.xvel)+cX+pX,(int)(b.y+b.yvel)+cY+pY);
	        }
        }
    }


}
