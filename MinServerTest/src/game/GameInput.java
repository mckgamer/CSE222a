package game;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class GameInput implements KeyListener {
	
	private GameThread game;
	
	public final static int FIRE = 1, LEFT = 2, RIGHT = 4, UP = 8, DOWN = 16;
	
	public GameInput(GameThread game) {
		this.game = game;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			game.mInput = game.mInput | FIRE;
			System.out.println("Shoot Event");
			break;
		case KeyEvent.VK_LEFT:
			game.mInput = game.mInput | LEFT;
			System.out.println("Rotate Event");
			break;
		case KeyEvent.VK_RIGHT:
			game.mInput = game.mInput | RIGHT;
			System.out.println("Rotate Event");
			break;
		case KeyEvent.VK_UP:
			game.mInput = game.mInput | UP;
			System.out.println("Move Event");
			break;
		case KeyEvent.VK_DOWN:
			game.mInput = game.mInput | DOWN;
			System.out.println("Move Event");
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			game.mInput = game.mInput & ~FIRE;
			System.out.println("Shoot Event");
			break;
		case KeyEvent.VK_LEFT:
			game.mInput = game.mInput & ~LEFT;
			System.out.println("Rotate Event");
			break;
		case KeyEvent.VK_RIGHT:
			game.mInput = game.mInput & ~RIGHT;
			System.out.println("Rotate Event");
			break;
		case KeyEvent.VK_UP:
			game.mInput = game.mInput & ~UP;
			System.out.println("Move Event");
			break;
		case KeyEvent.VK_DOWN:
			game.mInput = game.mInput & ~DOWN;
			System.out.println("Move Event");
			break;
		}
	}
	
	public void setGameThread(GameThread game) {
		this.game = game;
	}
	
}

