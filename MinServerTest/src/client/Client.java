package client;

import game.SpaceFighter;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import shared.ServerMessage;

public class Client {

	static boolean isRunning = true;
	static Adler32 checkSum = new Adler32();
	static int bytes = 256;
	static String host;
	static SpaceFighter game;

	// Counter variables for debugging
	static float normal = 1;
	static float outOfSync = 0;

	public static void main(String[] args) throws IOException {

		game = new SpaceFighter(); // The Actual Game

		if (args.length != 1) {
			System.out.println("Usage: java QuoteClient <hostname>");
			return;
		}
		host = args[0];

		// get a datagram socket
		DatagramSocket socket = new DatagramSocket();

		while (isRunning) {
			// send request
			byte[] buf = new byte[bytes];
			ByteBuffer wrapper = ByteBuffer.wrap(buf);
			wrapper.putInt((int) checkSum.getValue());
			wrapper.put(game.getInput());
			
			InetAddress address = InetAddress.getByName(host);
			DatagramPacket packet = new DatagramPacket(buf, buf.length,
					address, 4445);
			socket.send(packet);

			// get response
			handleResponse(packet, socket, buf);

			checkSum.reset();
			checkSum.update(game.getState());

			game.repaint();

		}

		socket.close();
	}

	public static void handleResponse(DatagramPacket packet,
			DatagramSocket socket, byte[] buf) throws IOException {
		outOfSync-=(outOfSync<.01)?outOfSync:.01;
		normal-=(normal<.01)?normal:.01;
		System.out.println("Normal: " + (double) 100*normal / (normal + outOfSync)
				+ "% OOS: " + (double) 100*outOfSync / (normal + outOfSync) + "%");
		// get response
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		//System.out.println("Recieved a packet from sever " + packet.getPort());
		int packetType = ByteBuffer.wrap(packet.getData(), 0, 4).getInt();
		switch (packetType) {
		case ServerMessage.NORMALOP:
			// user input is included
			//System.out.println("Normal Operation");
			game.updateState(packet.getData());
			normal++;
			break;
		case ServerMessage.OUTOFSYNC:
			// im out of sync, full state included
			System.out.println("Out of sync at " + checkSum.getValue());
			int actualCheckSum = ByteBuffer.wrap(packet.getData(), 4, 4)
					.getInt();
			game.decodeState(ByteBuffer.wrap(packet.getData(), 8, 248));
			System.out.println("Synced to " + actualCheckSum);
			outOfSync++;
			break;
		case ServerMessage.STATEREQUEST:
			// someone needs my state
			System.out.println("Sending off my state");
			byte[] buf2 = new byte[bytes];
			ByteBuffer.wrap(buf2, 0, 4).putInt((int) checkSum.getValue());
			ByteBuffer.wrap(buf2, 4, 252).put(game.getState(), 0, 252);
			InetAddress address2 = InetAddress.getByName(host);
			DatagramPacket packet2 = new DatagramPacket(buf2, buf2.length,
					address2, 4786);
			socket.send(packet2);
			System.out.println("Sent off my state of "
					+ ByteBuffer.wrap(buf2, 0, 4).getFloat());
			handleResponse(packet, socket, buf);
			break;
		case ServerMessage.IDASSIGN:
			// im just connecting, getting my unique id
			System.out.println("Im getting my ID yay!");
			game.mClientID = ByteBuffer.wrap(packet.getData(), 4, 4)
					.getInt();
			System.out.println("Got the id " + game.mClientID);
			handleResponse(packet, socket, buf);
			break;

		}

	}

}