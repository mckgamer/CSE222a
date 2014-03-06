package shared;

@Deprecated
public class UniqueIDGenerator {
	
	static int id = 0;
	static int otherID = 0;
	
	public static int getID() {
		return id++;
	}
	
	public static int getOtherID() {
		return otherID++;
	}
	
	public static int softOther() {
		return otherID;
	}
	
	public static void setOther(int other) {
		otherID = other;
	}

}
