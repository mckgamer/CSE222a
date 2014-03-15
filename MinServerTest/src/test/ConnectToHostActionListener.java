package test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.swing.JPanel;
import javax.swing.JTextField;

import client.GameThread;

public class ConnectToHostActionListener implements ActionListener {
	private GameThread thread;
	private JPanel panel;
	private JTextField textField;
	
	public ConnectToHostActionListener(JPanel panel, GameThread thread, JTextField textField) {
		this.thread = thread;
		this.panel = panel;
		this.textField = textField;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		//Button click or enter key
		String inetAddress = "";
		int port = 0;
		try {
			StringTokenizer tok = new StringTokenizer(textField.getText(), ":");
			inetAddress = tok.nextToken();
			port = Integer.parseInt(tok.nextToken());
			thread.setHost(InetAddress.getByName(inetAddress), port);
			panel.requestFocus();
		} catch(NumberFormatException e0) {
			textField.setText(inetAddress + ":XXXX");
		} catch(UnknownHostException e1) {
			textField.setText("XXX.XXX.XXX.XXX:" + port);
		} catch(Exception e) {
			textField.setText("XXX.XXX.XXX.XXX:XXXX");
		}
	}

}
