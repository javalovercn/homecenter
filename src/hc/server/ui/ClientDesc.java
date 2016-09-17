package hc.server.ui;

import hc.core.HCConfig;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.MobileAgent;
import hc.core.util.StringUtil;
import hc.server.data.screen.PNGCapturer;
import hc.util.PropertiesManager;

import java.util.Hashtable;
import java.util.Vector;

public class ClientDesc {
	//专为不能直接访问projectContext服务的
	static final Hashtable<String, Object> sys_attribute = new Hashtable<String, Object>();

	public static int getClientWidth(){
		return clientWidth;
	}
	
	public static String getClientScale(){
		return agent.getScale();
	}
	
	public static int getClientHeight(){
		return clientHeight;
	}
	
	/*
	 * 是否为竖屏
	 */
	public static boolean isClientPortrait(){
		return clientHeight > clientWidth;
	}
	
	private static int clientWidth;
	private static int clientHeight;
	private static int dpi;
	private static float xdpi, ydpi, density;
	private static String clientLang;
	private static String hcClientVer;//非操作系统的版本
	private static MobileAgent agent = new MobileAgent();
	
	public static MobileAgent getAgent(){
		CCoreUtil.checkAccess();
		return agent;
	}
	
	public static int getDPI(){
		return dpi;
	}
	
	public static float getXDPI(){
		return xdpi;
	}
	
	public static float getYDPI(){
		return ydpi;
	}
	
	public static float getDensity(){
		return density;
	}
	
	public static String getClientLang(){
		return (clientLang == null) ? "en-US" : clientLang;
	}
	
	public static String getHCClientVer(){
		return hcClientVer;
	}
	
	public static void refreshClientInfo(final String serial){
		sys_attribute.clear();
		
		final Vector v = StringUtil.split(serial, HCConfig.CFG_SPLIT);
		
		clientWidth = HCConfig.getIntProperty(v, (short)0);
		clientHeight = HCConfig.getIntProperty(v, (short)1);
		dpi = HCConfig.getIntProperty(v, (short)2);
		clientLang = HCConfig.getProperty(v, (short)3);
		hcClientVer = HCConfig.getProperty(v, (short)4);
	
		String serialMobileAgent = "";
		try{
			xdpi = Float.valueOf(HCConfig.getProperty(v, (short)5));
			ydpi = Float.valueOf(HCConfig.getProperty(v, (short)6));
			density = Float.valueOf(HCConfig.getProperty(v, (short)7));
			
			serialMobileAgent = HCConfig.getProperty(v, (short)8);
			agent = MobileAgent.toObject(serialMobileAgent);
			
			PNGCapturer.updateColorBit(agent.getColorBit());
			PNGCapturer.updateRefreshMS(agent.getRefreshMS());
			
			final String pWifiIsmobileviawifi = PropertiesManager.p_WiFi_isMobileViaWiFi;
			if(PropertiesManager.getValue(pWifiIsmobileviawifi) == null || PropertiesManager.isTrue(pWifiIsmobileviawifi) != agent.ctrlWiFi()){
				PropertiesManager.setValue(pWifiIsmobileviawifi, agent.ctrlWiFi()?IConstant.TRUE:IConstant.FALSE);
				PropertiesManager.saveFile();
			}
		}catch (final Throwable e) {
		}

		final StringBuilder sb = new StringBuilder(1024);
		{
			sb.append("Receive client agent information : ");
			final int size = agent.size();
			final Object[] kv = new Object[2];
			
			for (int i = 0; i < size; i++) {
				agent.get(i, kv);
				
				final String key = (String)kv[0];
				if(key.startsWith(MobileAgent.TAG_HIDE_PREFIX)){//节省log及美观
					continue;
				}

				
				sb.append("\n  [" + key + " = " + kv[1] + "]");
			}
		}
		L.V = L.O ? false : LogManager.log(sb.toString());
		L.V = L.O ? false : LogManager.log("Receive client desc, locale:" + clientLang + ",  w:" + clientWidth + ", h:" + clientHeight 
				+ ", dpi:" + dpi + ((dpi==0)?"(unknow)":"") + ", hcClientVer:" + hcClientVer + ", xdpi:" + xdpi + ", ydpi:" + ydpi + ", density:" + density);
		L.V = L.O ? false : LogManager.log("  Important : the w (h) maybe not equal to the real width (height) of mobile in pixels, UI may be scaled to the best size.");
	}
	public static final int vgap = 5;
	public static final int hgap = 5;

}
