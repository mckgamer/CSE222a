package game;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class GameInput implements KeyListener {
	
	public final static int FIREKEY = 5, LEFTKEY = 3, RIGHTKEY  = 4, UPKEY = 1, DOWNKEY = 2;
	public boolean[] pressed = new boolean[6];
	private GameThread game;
	
	public GameInput(GameThread game) {
		this.game = game;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		
		synchronized (pressed) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			pressed[FIREKEY] = true;
			synchronized (game.getInputs()) {
				game.getInputs().add(new InputEvent(game.mClientID, FIREKEY));
			}
			System.out.println("Shoot Event");
			break;
		case KeyEvent.VK_LEFT:
			pressed[LEFTKEY] = true;
			System.out.println("Rotate Event");
			synchronized (game.getInputs()) {
				game.getInputs().add(new InputEvent(game.mClientID, LEFTKEY));
				}
			break;
		case KeyEvent.VK_RIGHT:
			pressed[RIGHTKEY] = true;
			System.out.println("Rotate Event");
			synchronized (game.getInputs()) {
				game.getInputs().add(new InputEvent(game.mClientID, RIGHTKEY));
				}
			break;
		case KeyEvent.VK_UP:
			pressed[UPKEY] = true;
			System.out.println("Move Event");
			synchronized (game.getInputs()) {
				game.getInputs().add(new InputEvent(game.mClientID, UPKEY));
			}
			break;
		case KeyEvent.VK_DOWN:
			pressed[DOWNKEY] = true;
			System.out.println("Move Event");
			synchronized (game.getInputs()) {
				game.getInputs().add(new InputEvent(game.mClientID, DOWNKEY));
			}
			break;
		}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		synchronized (pressed) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			pressed[FIREKEY] = false;
			System.out.println("Shoot Event");
			break;
		case KeyEvent.VK_LEFT:
			pressed[LEFTKEY] = false;
			System.out.println("Rotate Event");
			break;
		case KeyEvent.VK_RIGHT:
			pressed[RIGHTKEY] = false;
			System.out.println("Rotate Event");
			break;
		case KeyEvent.VK_UP:
			pressed[UPKEY] = false;
			System.out.println("Move Event");
			break;
		case KeyEvent.VK_DOWN:
			pressed[DOWNKEY] = false;
			System.out.println("Move Event");
			break;
		}
		}
	}
	
	public void setGameThread(GameThread game) {
		this.game = game;
	}
	
}

