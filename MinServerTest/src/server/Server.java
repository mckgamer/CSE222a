package server;
/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import shared.LogFile;
import test.ServerLayoutPanel;
 
public class Server {

    private static JFrame display;
    private static List<ServerThread> servers = new ArrayList<ServerThread>();
    private static int listenPort = 4440;
    private static int transferPort = 5550;
    private static final int NUM_THREADS = 4;
    private static JPanel mainPanel = null;
    private static JPanel btnPanel = null;
    public static ServerLayoutPanel spanel = new ServerLayoutPanel(100,100);
	public static LogFile log = new LogFile("Server");
	
    public static void main(String[] args) throws IOException {
    	//Create a bunch of server threads
    	
    	//This automatically finds good port numbers to use
    	int threadsStarted = 0;
    	while (threadsStarted < NUM_THREADS) {
	    	try {
	    		servers.add(new ServerThread(listenPort, transferPort));
	    		threadsStarted++;
	    	} catch (IOException e) {
	    		listenPort++;
	    		transferPort++;
	    	}
    	}

    	//This sets up thread neighbors
    	if(threadsStarted == 4) {
    		DatagramSocket skt = new DatagramSocket();

    		//Send neighbor messages
    		Neighbor ntl = servers.get(0).toNeighbor();
    		Neighbor ntr = servers.get(1).toNeighbor();
    		Neighbor nbl = servers.get(2).toNeighbor();
    		Neighbor nbr = servers.get(3).toNeighbor();

    		TransferSender.sendNeighborNote(skt, ntl, ntr, Neighbor.Direction.LEFT);
    		TransferSender.sendNeighborNote(skt, ntr, ntl, Neighbor.Direction.RIGHT);

    		TransferSender.sendNeighborNote(skt, ntl, nbl, Neighbor.Direction.TOP);
    		TransferSender.sendNeighborNote(skt, nbl, ntl, Neighbor.Direction.BOTTOM);

    		TransferSender.sendNeighborNote(skt, ntr, nbr, Neighbor.Direction.TOP);
    		TransferSender.sendNeighborNote(skt, nbr, ntr, Neighbor.Direction.BOTTOM);

    		TransferSender.sendNeighborNote(skt, nbl, nbr, Neighbor.Direction.LEFT);
    		TransferSender.sendNeighborNote(skt, nbr, nbl, Neighbor.Direction.RIGHT);

    		skt.close();
    		
    		//Register servers on the server panel
    		spanel.registerPlayer(1);
    		spanel.registerPlayer(2);
    		spanel.registerServer(50, 50, ntl.getPriority(), 1);
    		spanel.registerServer(51, 50, ntr.getPriority(), 1);
    		spanel.registerServer(51, 51, nbr.getPriority(), 1);
    		spanel.registerServer(50, 51, nbl.getPriority(), 1);
    	}
    	
    	//servers.add(new ServerThread(4797, 4446));

    	display = new JFrame();
    	display.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	display.setSize(640, 480);
    	display.addWindowStateListener(
			new WindowStateListener() {
				@Override
				public void windowStateChanged(WindowEvent e) {
					if(e.getID() == WindowEvent.WINDOW_CLOSED) {
						Server.close();
					}
					
				}
			}
		);

    	mainPanel = new JPanel(new BorderLayout());
    	JToggleButton btn = new JToggleButton("Show server buttons");
    	btn.addChangeListener(new ChangeListener() {
			private boolean down = false;
			@Override
			public void stateChanged(ChangeEvent arg0) {
				down = !down;
				mainPanel.remove(1);
				if(down) {
			    	mainPanel.add(btnPanel, BorderLayout.CENTER);
			    	btnPanel.repaint();
				} else {
			    	mainPanel.add(spanel, BorderLayout.CENTER);
			    	spanel.repaint();
				}
				mainPanel.repaint();
			}
    		
    	});
    	mainPanel.add(btn, BorderLayout.NORTH);
    	mainPanel.add(spanel, BorderLayout.CENTER);
    	display.add(mainPanel);
    	
    	
    	btnPanel = new JPanel(new GridLayout(servers.size(), 0));
        for(ServerThread server : servers) {
        	server.start();
        	JPanel serverPanel = new JPanel(new FlowLayout());
        	JButton pingButton = new JButton(server.getName());
        	pingButton.addActionListener(new ServerActionListener(serverPanel, server));
        	serverPanel.add(pingButton);
        	btnPanel.add(serverPanel);
        }

    	display.setVisible(true);
    }
    
    public static void addServer(ServerThread server) {
    	servers.add(server);
    	JPanel serverPanel = new JPanel(new FlowLayout());
    	JButton pingButton = new JButton(server.getName());
    	pingButton.addActionListener(new ServerActionListener(serverPanel, server));
    	serverPanel.add(pingButton);

    	btnPanel.add(serverPanel);
    	btnPanel.validate();
    	server.start();
    }
    
    public static void close() {
    	log.println("Killing servers");
    	for(ServerThread server : servers) {
        	server.kill();
        }
    	log.close();
    }
}