package shared;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Date;

public class LogFile {
	private PrintWriter file = null;
	public LogFile(String prefix) {
		//Date date = new Date();
		//Timestamp time = new Timestamp(date.getTime());
		String name = prefix + ".txt";
		try {
			file = new PrintWriter(name, "ASCII");
		} catch(Exception e) {
			System.out.println("ERROR: Failed to open logfile \"" + name + "\"");
			e.printStackTrace();
		}
	}
	
	public void print(String s) {
		synchronized(file) {
			file.print(s);
			file.flush();
		}
		System.out.print(s);
	}
	
	public void println(String s) {
		print("<" + Thread.currentThread().getName() + "> " + s + "\n");
	}
	
	public void close() {
		file.flush();
		file.close();
	}
}
