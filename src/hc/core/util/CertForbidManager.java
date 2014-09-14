package hc.core.util;

public class CertForbidManager {
	private static boolean hasReceiveUncheckCert = false;
	public static void receiveUncheckCert(){
		hasReceiveUncheckCert = true;
	}
	
	public static boolean hasReceiveUncheckCert(){
		return hasReceiveUncheckCert;
	}
	
	public static void reset(){
		hasReceiveUncheckCert = false;
	}
}
