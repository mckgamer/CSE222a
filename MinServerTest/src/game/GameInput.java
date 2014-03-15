package game;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

import client.GameThread;
import client.NewClient;


public class GameInput implements KeyListener {
	
	private GameThread game;
	private boolean roboModeEnabled = false;
	private int keyGenTimer = 0;
	private int fireTimer = 0;
	Random rand = new Random();
	
	private static final int MAX_ROTATE_TIMER = 90;
	private static final int MAX_MOVE_TIMER = 100;
	private static final int MAX_FIRE_TIMER = 10;
	
	public final static int FIRE = 1, LEFT = 2, RIGHT = 4, UP = 8, DOWN = 16;
	
	public GameInput(GameThread game) {
		this.game = game;
	}
	
	//our "AI"
	public void generateKeyPresses() {
		if(!roboModeEnabled) {
			return;
		}
		if(--keyGenTimer < 0) {
			//Move either forward or backwards
			if(rand.nextBoolean()) {
				game.mInput = UP;
			} else {
				game.mInput = 0;//DOWN;
			}
			
			//Turn left, right, or not at all
			int turn = rand.nextInt(3);
			switch(turn) {
			case 0:
				keyGenTimer = MAX_ROTATE_TIMER;
				game.mInput |= LEFT;
				break;
			case 1:
				keyGenTimer = MAX_ROTATE_TIMER;
				game.mInput |= RIGHT;
				break;
			default:
				keyGenTimer = MAX_MOVE_TIMER;
			}
		}
		
		//Fire or not
		if(--fireTimer < 0) {
			fireTimer = MAX_FIRE_TIMER;
			if(rand.nextBoolean()) {
				game.mInput |= FIRE;
			}
		} else {
			game.mInput &= ~FIRE;
		}
	}
	
	public void setRoboMode(boolean enabled) {
		game.mInput = 0;
		roboModeEnabled = enabled;
	}
	
	public boolean getRoboMode() { 
		return roboModeEnabled;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		if(roboModeEnabled) {
			return;
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(roboModeEnabled) {
			return;
		}
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			game.mInput = game.mInput | FIRE;
			NewClient.log.println("Shoot Event");
			break;
		case KeyEvent.VK_LEFT:
			game.mInput = game.mInput | LEFT;
			NewClient.log.println("Rotate Event");
			break;
		case KeyEvent.VK_RIGHT:
			game.mInput = game.mInput | RIGHT;
			NewClient.log.println("Rotate Event");
			break;
		case KeyEvent.VK_UP:
			game.mInput = game.mInput | UP;
			NewClient.log.println("Move Event");
			break;
		case KeyEvent.VK_DOWN:
			game.mInput = game.mInput | DOWN;
			NewClient.log.println("Move Event");
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if(roboModeEnabled) {
			return;
		}
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			game.mInput = game.mInput & ~FIRE;
			NewClient.log.println("Shoot Event");
			break;
		case KeyEvent.VK_LEFT:
			game.mInput = game.mInput & ~LEFT;
			NewClient.log.println("Rotate Event");
			break;
		case KeyEvent.VK_RIGHT:
			game.mInput = game.mInput & ~RIGHT;
			NewClient.log.println("Rotate Event");
			break;
		case KeyEvent.VK_UP:
			game.mInput = game.mInput & ~UP;
			NewClient.log.println("Move Event");
			break;
		case KeyEvent.VK_DOWN:
			game.mInput = game.mInput & ~DOWN;
			NewClient.log.println("Move Event");
			break;
		}
	}
	
	public void setGameThread(GameThread game) {
		this.game = game;
	}
	
}

