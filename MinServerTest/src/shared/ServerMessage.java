package shared;

public class ServerMessage {
	//Communication with client
	public static final byte NORMALOP = 0;
	public static final byte OUTOFSYNC = 1;
	public static final byte STATEREQUEST = 2;
	public static final byte IDASSIGN = 3;
	
	//Communication with server
	public static final byte NEWSERVER = 4;		//New server notification
	public static final byte NEIGHBORNOTE = 5;	//New neighbor notification
	public static final byte KILLNOTE = 6;		//Kill this server thread notification (contains replacement server)
	public static final byte TRANSFEROBJ = 7;

}
