package hc.core.util;

import java.util.Vector;

public class ExceptionChecker {
	static final ExceptionChecker instance = new ExceptionChecker();
	
	public static final ExceptionChecker getInstance(){
		return instance;
	}
	
	//一直保留到关机。
	final Vector table = new Vector(12);
	
	public boolean isPosted(final String projectID, final String stackTrace){
		final String key = projectID + stackTrace;
		final boolean isPosted = table.contains(key);
		if(isPosted == false){
			table.addElement(key);
		}
		return isPosted;
	}
}
