package test;

import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RoboClient {
	public static void main(String [] args) {
		JFrame managerUi = new JFrame();
		managerUi.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		managerUi.setSize(300, 300);
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel scrollPanel = new JPanel();
		scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(scrollPanel);
		//scrollPane.setPreferredSize(new Dimension(300,300));
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		
		JButton makeButton = new JButton("New robo-thread");
		makeButton.addActionListener(
			new MakeRoboThreadActionListener(scrollPane, scrollPanel)
		);
		mainPanel.add(makeButton, BorderLayout.NORTH);
		
		managerUi.add(mainPanel);
		managerUi.setVisible(true);
	}
}
