package hc.server.ui;

import hc.core.HCConfig;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.MobileAgent;
import hc.core.util.StringUtil;
import hc.server.data.screen.PNGCapturer;
import hc.server.ui.design.J2SESession;
import hc.util.PropertiesManager;
import hc.util.StringBuilderCacher;

import java.util.Vector;

public class ClientDesc {

	public final int getClientWidth(){
		return clientWidth;
	}
	
	public final String getClientScale(){
		return agent.getScale();
	}
	
	public final int getClientHeight(){
		return clientHeight;
	}
	
	/*
	 * 是否为竖屏
	 */
	public final boolean isClientPortrait(){
		return clientHeight > clientWidth;
	}
	
	private int clientWidth;
	private int clientHeight;
	private int dpi;
	private float xdpi, ydpi, density;
	private String clientLang;
	private String hcClientVer;//非操作系统的版本
	private MobileAgent agent = new MobileAgent();
	
	public final MobileAgent getAgent(){
		return agent;
	}
	
	public final int getDPI(){
		return dpi;
	}
	
	public final float getXDPI(){
		return xdpi;
	}
	
	public final float getYDPI(){
		return ydpi;
	}
	
	public final float getDensity(){
		return density;
	}
	
	public final String getClientLang(){
		return (clientLang == null) ? "en-US" : clientLang;
	}
	
	public final String getHCClientVer(){
		return hcClientVer;
	}
	
	public final void refreshClientInfo(final J2SESession coreSS, final String serial){
		final Vector v = StringUtil.split(serial, HCConfig.CFG_SPLIT);
		
		clientWidth = HCConfig.getIntProperty(v, (short)0);
		clientHeight = HCConfig.getIntProperty(v, (short)1);
//		clientWidth = 320;
//		clientHeight = 480;
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
			
			PNGCapturer.updateColorBit(coreSS, agent.getColorBit());
			PNGCapturer.updateRefreshMS(coreSS, agent.getRefreshMS());
			
			final String pWifiIsmobileviawifi = PropertiesManager.p_WiFi_isMobileViaWiFi;
			if(PropertiesManager.getValue(pWifiIsmobileviawifi) == null || PropertiesManager.isTrue(pWifiIsmobileviawifi) != agent.ctrlWiFi()){
				PropertiesManager.setValue(pWifiIsmobileviawifi, agent.ctrlWiFi()?IConstant.TRUE:IConstant.FALSE);
				PropertiesManager.saveFile();
			}
		}catch (final Throwable e) {
		}

		final StringBuilder sb = StringBuilderCacher.getFree();
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
		final String sbStr = sb.toString();
		StringBuilderCacher.cycle(sb);
		L.V = L.O ? false : LogManager.log(sbStr);
		L.V = L.O ? false : LogManager.log("Receive client desc, locale:" + clientLang + ",  w:" + clientWidth + ", h:" + clientHeight 
				+ ", dpi:" + dpi + ((dpi==0)?"(unknow)":"") + ", hcClientVer:" + hcClientVer + ", xdpi:" + xdpi + ", ydpi:" + ydpi + ", density:" + density);
		L.V = L.O ? false : LogManager.log("  Important : the w (h) maybe not equal to the real width (height) of mobile in pixels, UI may be scaled to the best size.");
	}
	
	public static final int vgap = 5;
	public static final int hgap = 5;

}
