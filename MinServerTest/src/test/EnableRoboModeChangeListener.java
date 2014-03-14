package test;

import game.GameInput;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class EnableRoboModeChangeListener implements ChangeListener {
	private GameInput input;
	private JPanel panel;
	
	public EnableRoboModeChangeListener(JPanel panel, GameInput input) {
		this.input = input;
		this.panel = panel;
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		input.setRoboMode(!input.getRoboMode());
		panel.requestFocus();
	}

}
