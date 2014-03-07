package test;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MakeRoboThreadActionListener implements ActionListener {
	private JPanel roboListPanel;
	private JScrollPane scrollPane;
	public MakeRoboThreadActionListener(JScrollPane scrollPane, JPanel roboListPanel) {
		this.roboListPanel = roboListPanel;
		this.scrollPane = scrollPane;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		RoboThread thread = new RoboThread(4445, new Point(0,0), 270);
		JButton button = new JButton("Remove robo" + thread.getId());
		button.addActionListener(
			new DeleteRoboThreadActionListener(scrollPane, roboListPanel, thread)
		);
		roboListPanel.add(button);
		scrollPane.validate();
	}

}
