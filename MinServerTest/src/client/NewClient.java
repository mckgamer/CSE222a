package client;

import shared.LogFile;
import game.UIThread;

public class NewClient {

	public static LogFile log = new LogFile("Client");
	
    public static void main(String[] args) {
        new UIThread();
    }
}
