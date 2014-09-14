package hc.server.ui;

import hc.core.HCConfig;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;

import java.util.Vector;

public class ClientDesc {

	public static int clientWidth;
	public static int clientHeight;
	public static int dpi;
	public static String clientLang;
	public static String clientVer;
	public static void refreshClientInfo(String serial){
		Vector v = StringUtil.split(serial, HCConfig.CFG_SPLIT);
		clientWidth = HCConfig.getIntProperty(v, (short)0);
		clientHeight = HCConfig.getIntProperty(v, (short)1);
		dpi = HCConfig.getIntProperty(v, (short)2);
		clientLang = HCConfig.getProperty(v, (short)3);
		clientVer = HCConfig.getProperty(v, (short)4);
	
		hc.core.L.V=hc.core.L.O?false:LogManager.log("Receive client desc, w:" + clientWidth + ", h:" + clientHeight + ", dpi:" + dpi + ", ver:" + clientVer);
	}
	public static final int vgap = 5;
	public static final int hgap = 5;

}
