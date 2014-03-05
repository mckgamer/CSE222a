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
import game.DummyClient;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import shared.ServerMessage;
 
public class ServerThread extends Thread {
 
    protected DatagramSocket socket = null;
    protected int moreQuotes = 1;
    private ClientListener clientListener;
    
    private int desiredFR = 60;
    private int windowFR = 5;
    private int stallFR = 2000000;
    private double desiredMS = 1000/(desiredFR/windowFR);
 
    private HashMap<String, Integer> lastTalked;
    DummyClient dummy;
    
    public ServerThread() throws IOException {
    this("ServerThread");
    }
 
    public ServerThread(String name) throws IOException {
        super(name);
        
        lastTalked = new HashMap<String,Integer>();
        dummy = new DummyClient();
        
        clientListener = new ClientListener("ClientListener");
        clientListener.start();
        
        socket = new DatagramSocket(4786);
    }
 
    public void run() {
 
    	int windowPackets = 0;
    	int totalPackets = 0;
    	long myTime = System.currentTimeMillis();
    	long longTime = System.currentTimeMillis();
    	long totalSpinTime = 0;
        while (moreQuotes != -1.1) {
        	
        	long spinTime = System.currentTimeMillis();
        	while (moreQuotes%stallFR!=0) {
        		moreQuotes++;
        	}
        	spinTime =  spinTime-System.currentTimeMillis();
        	totalSpinTime += spinTime;
        	if (windowPackets > windowFR) {
    			double observedMS = System.currentTimeMillis()-myTime;
    			myTime = System.currentTimeMillis();
    			stallFR = (int)(stallFR*(observedMS/totalSpinTime)*desiredMS/observedMS);
    			windowPackets = 0;
    			moreQuotes=1;
    			totalSpinTime = 0;
    		}
        	if (System.currentTimeMillis()-longTime > 1000) {
    			System.out.println("PPS: "+totalPackets);
    			assert(totalPackets>65);
    			totalPackets=0;
    			longTime = System.currentTimeMillis();
    		}
    		windowPackets++;
    		totalPackets++;
    		dummy.doUpdates();
    		if (clientListener.something.size() > 0) {
        		synchronized(clientListener.something) {
        			boolean badOne = false;
        			byte[] goodData = null;
        			byte[] aggregate = new byte[256];
        			ByteBuffer aggregator = ByteBuffer.wrap(aggregate);
        			int offset = 0;
        			for (Address d :clientListener.something) {
        				if (d.check != dummy.checkSum()) { badOne = true; }
        				aggregator.put(d.fullData,8,ByteBuffer.wrap(d.fullData, 4, 4).getInt()-8);
        				offset+=ByteBuffer.wrap(d.fullData, 4, 4).getInt()-8;
        			}
        			
        			/*Compute the full state buffer once. */
        			byte[] fullStateBuf = new byte[256];
        			if (badOne) {
        				goodData = dummy.getState();
        				ByteBuffer fswrapper = ByteBuffer.wrap(fullStateBuf);
        				fswrapper.putInt(ServerMessage.OUTOFSYNC);
    					if (goodData != null) {
    						fswrapper.putInt(dummy.checkSum());
    						fswrapper.put(goodData,0,248);
    					}
        			}
        			
        			/*Compute the normal op buffer once. */
        			byte[] normalBuf = new byte[256];
    				ByteBuffer nwrapper = ByteBuffer.wrap(normalBuf);
    				nwrapper.putInt(ServerMessage.NORMALOP);
    				nwrapper.putInt(offset+8); //length of packet useful
    				nwrapper.put(aggregate,0,offset);
        			
    				/*Send the appropriate packet to each client. */
        			for (Address d :clientListener.something) {
        				System.out.println(d.address);
        				DatagramPacket packet;
        				if (d.check == dummy.checkSum()) {
        					packet = new DatagramPacket(normalBuf, normalBuf.length, d.address, d.port);
        				} else {
        					System.out.println("Sending out "+dummy.checkSum());
        					packet = new DatagramPacket(fullStateBuf, fullStateBuf.length, d.address, d.port);
        				}
        				try {
							clientListener.socket.send(packet);
						} catch (IOException e) {
							e.printStackTrace();
							moreQuotes = -1;
						}
        			}

        			dummy.updateState(normalBuf);
        			clientListener.something.clear();
        		}
        		System.out.println("----------------------------");
    		}
    	
        }
        System.out.println("Dead");
        clientListener.kill();
    }
 
}