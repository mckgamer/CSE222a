package server;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ServerActionListener implements ActionListener {

	public ServerActionListener(JPanel panel, ServerThread st) {
		thread = st;
		this.panel = panel;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		List<String> clients = new ArrayList<String>();
		thread.updateClientList(clients);
		panel.removeAll();
		panel.add((Component)arg0.getSource());
		for(String client : clients) {
			JLabel lbl = new JLabel(client);
			panel.add(lbl);
		}
		panel.validate();
	}
	private ServerThread thread;
	private JPanel panel;
}
