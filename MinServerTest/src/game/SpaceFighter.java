package game;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import shared.UniqueIDGenerator;

public class SpaceFighter extends JPanel
{
	
	private ArrayList<InputEvent> mInputs = new ArrayList<InputEvent>();
	public int mClientID = 0;
	
	public HashMap<Integer,Player> players = new HashMap<Integer,Player>();
	public HashMap<Integer,Bullet> bullets = new HashMap<Integer,Bullet>();
	
    public SpaceFighter()                       // set up graphics window
    {
        super();
        setBackground(Color.WHITE);
        addKeyListener(new GameInput(this));
        setFocusable(true);
        
        JFrame application = new JFrame();                            // the program itself
        
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   // set frame to exit
                                                                      // when it is closed
        application.add(this);           


        application.setSize(700, 700);         // window is 500 pixels wide, 400 high
        application.setVisible(true); 
    }

    public void paintComponent(Graphics g)  // draw graphics in the panel
    {
        int width = getWidth();             // width of window in pixels
        int height = getHeight();           // height of window in pixels

        super.paintComponent(g);            // call superclass to make panel display correctly


        int cX = -((players.get(mClientID)!=null)?(int)players.get(mClientID).x:0)+width/2;
        int cY = -((players.get(mClientID)!=null)?(int)players.get(mClientID).y:0)+height/2;
        
        g.setColor(new Color(200,200,200));
        for (int l=cX;l<7000;l+=700) {
        	g.drawLine(l+cX, 0, l+cX, height);
        }
        for (int t=0;t<7000;t+=700) {
        	g.drawLine(0, t+cY, width, t+cY);
        }
        g.setColor(new Color(0,0,0));
        
        // Drawing code goes here
        for (Player p: players.values()) {
        	if (mClientID == p.entityID) {
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
        	if (mClientID == p.entityID) {
        		g.setColor(new Color(0,0,0));
        	}
        	p.x+=p.xvel;
	        p.y+=p.yvel;
	        p.xvel/=1.03;
	        p.yvel/=1.03;
        }
        
        ArrayList<Integer> kill = new ArrayList<Integer>();
        for (Bullet b: bullets.values()) {
        	g.drawLine((int)b.x+cX,(int)b.y+cY,(int)(b.x+b.xvel)+cX,(int)(b.y+b.yvel)+cY);
        	b.x+=b.xvel;
	        b.y+=b.yvel;
	        b.life--;
	        if (b.life < 0) {
	        	kill.add(b.entityID);
	        }
        }
        for (Integer b: kill) {
        	bullets.remove(b);
        }
    }

    public void updateState(byte[] buf) {
    	ByteBuffer wrapped = ByteBuffer.wrap(buf);
    	wrapped.getInt(); //throw away first int not useful here
    	int index = 8;
    	int length = wrapped.getInt();
    	while (index+8 <= length) {
    		int id = wrapped.getInt();
    		int command = wrapped.getInt();
    		if (!players.containsKey(id)) {
    			System.out.println("Player id "+id);
    			players.put(id,new Player(id));
    		}
    		switch (command) {
    		case GameInput.UPKEY:
    			players.get(id).yvel+=Math.sin(players.get(id).angle);
				players.get(id).xvel+=Math.cos(players.get(id).angle);
				break;
			case GameInput.DOWNKEY:
				players.get(id).yvel-=Math.sin(players.get(id).angle);
				players.get(id).xvel-=Math.cos(players.get(id).angle);
    			break;
    		case GameInput.LEFTKEY:
    			players.get(id).angle-=0.2;
    			break;
    		case GameInput.RIGHTKEY:
    			players.get(id).angle+=0.2;;
    			break;
    		case GameInput.FIREKEY:
    			int bid = UniqueIDGenerator.getOtherID();
    			bullets.put(bid, new Bullet(bid,players.get(id).x,players.get(id).y,(float)Math.cos(players.get(id).angle)*3,(float)Math.sin(players.get(id).angle)*3));
    			break;
    		}
    		
    		index+=8;
    	}
    }
    
    public byte[] getState() {
    	byte buf[] = new byte[256];
    	ByteBuffer wrapped = ByteBuffer.wrap(buf);
    	wrapped.putInt(UniqueIDGenerator.softOther());
    	wrapped.putInt(players.size());
    	for (Player p : players.values()) {
    		wrapped.put(p.encode());
    	}
    	wrapped.putInt(bullets.size());
    	for (Bullet b : bullets.values()) {
    		wrapped.put(b.encode());
    	}
		return buf;
    }
    
    public void decodeState(ByteBuffer wrapped) {
    	UniqueIDGenerator.setOther(wrapped.getInt());
    	int pCount = wrapped.getInt();
    	//players.clear();
    	for (int p=0;p<pCount;p++) {
    		int id = wrapped.getInt();
    		if (!players.containsKey(id)) {
    			Player temp = new Player(id);
    			temp.decode(wrapped);
    			players.put(id,temp);
    		} else {
    			players.get(id).decode(wrapped);
    		}
    	}
    	int bCount = wrapped.getInt();
    	//players.clear();
    	for (int b=0;b<bCount;b++) {
    		int id = wrapped.getInt();
    		if (!bullets.containsKey(id)) {
    			Bullet temp = new Bullet(id);
    			temp.decode(wrapped);
    			bullets.put(id,temp);
    		} else {
    			bullets.get(id).decode(wrapped);
    		}
    	}
    }
    
    public void setClientID(int id) {
    	mClientID = id;
    }
    
    public ArrayList<InputEvent> getInputs() {
    	return mInputs;
    }

	public int getInputsSize() {
		return 8 * mInputs.size();
	}
	
	public byte[] getInput() {
		byte[] buf;
		synchronized (mInputs) {
			buf = new byte[4+getInputsSize()];
			ByteBuffer wrapper = ByteBuffer.wrap(buf);
			wrapper.putInt(8 + getInputsSize()); // let the
																// server
																// know
																// the
																// length of
																// this

			for (InputEvent in : mInputs) {
				wrapper.putInt(mClientID);
				wrapper.putInt(in.type);
			}
			assert (wrapper.position() == (8 * getInputsSize()));
			mInputs.clear();
		}
		return buf;
	}

}