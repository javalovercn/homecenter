package hc.core.util;

import hc.core.RootConfig;

import java.util.Random;

public class CCoreUtil {

	public static final int CERT_KEY_LEN = 64;

	public static void generateRandomKey(final byte[] data, final int offset, final int len){
		final Random r = new Random(System.currentTimeMillis());
		
		for (int i = offset, endIdx = offset + len; i < endIdx; i++) {
			data[i] = (byte) (r.nextInt() & 0xFF);
		}
	}

	public static final String RECEIVE_CERT_OK = "OK";
	public static final String RECEIVE_CERT_FORBID = "FORBID";
	public final static String FORBID_UPDATE_CERT = "Forbid update certification in (unsafe) net";
	public static final int ICON_WIDTH = 64;
	public static final Boolean GLOBAL_LOCK = new Boolean(true);

	public static final void globalExit(){
		synchronized (GLOBAL_LOCK) {
			System.exit(0);
		}
	}

	public static int resetFactor(){
		CUtil.factor = RootConfig.getInstance().getIntProperty(RootConfig.p_Encrypt_Factor);
		return CUtil.factor;
	}

}
