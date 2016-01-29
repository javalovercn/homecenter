package hc.server;

import hc.server.util.ExceptionViewer;

public class HCSecurityException extends SecurityException {
	public HCSecurityException(String s){
		super(s);
		
		ExceptionViewer.pushIn(s);
	}
}
