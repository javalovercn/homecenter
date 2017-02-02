package hc.server;

import hc.server.util.ExceptionViewer;
import hc.util.ThreadConfig;

public class HCSecurityException extends SecurityException {
	public HCSecurityException(final String s){
		super(s);
		
		final boolean isAuto = ThreadConfig.isTrue(ThreadConfig.AUTO_PUSH_EXCEPTION, true, true);
		if(isAuto){
			ExceptionViewer.pushIn(s);
		}
	}
}
