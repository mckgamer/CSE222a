package test;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class DeleteRoboThreadActionListener implements ActionListener {
	private JPanel roboListPanel;
	private RoboThread myThread;
	private JScrollPane scrollPane;
	
	public DeleteRoboThreadActionListener(JScrollPane scrollPane, JPanel roboListPanel, RoboThread myThread) {
		this.roboListPanel = roboListPanel;
		this.myThread = myThread;
		this.scrollPane = scrollPane;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		myThread.disconnect();
		roboListPanel.remove((Component)e.getSource());
		scrollPane.validate();
	}

}
