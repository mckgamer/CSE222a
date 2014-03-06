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
	
	//Performs a SoftPull on the current OtherID for sync
	public int softOther() {
		return otherID;
	}
	
	public void setOther(int other) {
		otherID = other;
	}
	
	//TODO transferID stuff getter/setter
	
}
