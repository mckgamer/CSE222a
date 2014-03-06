package shared;

public class PerChunkUIDGenerator {

	int id = 0;
	int otherID = 0;
	int transferID = 0;
	
	public int getID() {
		return id++;
	}
	
	public int getOtherID() {
		return otherID++;
	}
	
	public int softOther() {
		return otherID;
	}
	
	public void setOther(int other) {
		otherID = other;
	}
	
	//TODO transferID stuff getter/setter
	
}
