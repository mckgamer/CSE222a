package game;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class UIThread extends JPanel {
	
	ArrayList<GameThread> gThreads = new ArrayList<GameThread>();
	
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
        
        gThreads.add(new GameThread(-250,-250));
        gThreads.add(new GameThread(250,-250));
        gThreads.add(new GameThread(-250,250));
        gThreads.add(new GameThread(250,250));
        
        for (GameThread g: gThreads) {
        	g.start();
        	
        }
        
        GameInput input = new GameInput(gThreads.get(0));
        addKeyListener(input);
        
        int test=0;
        int now=2;
        while (true) {
        	repaint();
        	
        	/*if (test++%1000000 == 0) {
        		now=(now+1)%4;
        		input.setGameThread(gThreads.get(now));
        	}*/
        }
    }

    public void paintComponent(Graphics g)  // draw graphics in the panel
    {
        int width = getWidth();             // width of window in pixels
        int height = getHeight();           // height of window in pixels

        super.paintComponent(g);            // call superclass to make panel display correctly

        //Draw Grid
        /*
        g.setColor(new Color(200,200,200));
        for (int l=cX;l<7000;l+=700) {
        	g.drawLine(l+cX, 0, l+cX, height);
        }
        for (int t=0;t<7000;t+=700) {
        	g.drawLine(0, t+cY, width, t+cY);
        }
        g.setColor(new Color(0,0,0));
        */
        
        for (GameThread gThread : gThreads) {
        	
        	//Get Position of Player
            int cX = gThread.xOffSet;//-((gThread.players.get(gThread.mClientID)!=null)?(int)gThread.players.get(gThread.mClientID).x:0)+width/2;
            int cY = gThread.yOffSet;//-((gThread.players.get(gThread.mClientID)!=null)?(int)gThread.players.get(gThread.mClientID).y:0)+height/2;
            
	        // Drawing code goes here
	        for (Player p: gThread.players.values()) {
	        	if (gThread.mClientID == p.entityID) {
	        		g.setColor(new Color(255,0,0));
	        	}
	        	int brX = (int)(9*Math.cos(p.angle+Math.PI/3));
	        	int brY = (int)(9*Math.sin(p.angle+Math.PI/3));
	        	int blX = (int)(9*Math.cos(p.angle-Math.PI/3));
	        	int blY = (int)(9*Math.sin(p.angle-Math.PI/3));
	        	int tX = (int)(14*Math.cos(p.angle));
	        	int tY = (int)(14*Math.sin(p.angle));
	        	g.drawLine((int)p.x+tX+cX, (int)p.y+tY+cY, (int)p.x+brX+cX, (int)p.y+brY+cY); 
	        	g.drawLine((int)p.x+tX+cX, (int)p.y+tY+cY, (int)p.x+blX+cX, (int)p.y+blY+cY);  
	        	g.drawLine((int)p.x+blX+cX, (int)p.y+blY+cY, (int)p.x+brX+cX, (int)p.y+brY+cY); 
	        	if (gThread.mClientID == p.entityID) {
	        		g.setColor(new Color(0,0,0));
	        	}
	        }
	        
	        for (Bullet b: gThread.bullets.values()) {
	        	g.drawLine((int)b.x+cX,(int)b.y+cY,(int)(b.x+b.xvel)+cX,(int)(b.y+b.yvel)+cY);
	        }
        }
    }


}
