package hc.server.ui;

import hc.core.ConfigManager;
import hc.core.HCConfig;
import hc.core.IConstant;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;
import hc.core.util.MobileAgent;
import hc.core.util.StringUtil;
import hc.server.data.screen.PNGCapturer;
import hc.server.ui.design.J2SESession;
import hc.util.BaseResponsor;
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
	private int iosDrawWidth, iosDrawHeight;
	
	private MobileAgent agent = new MobileAgent();
	
	public final MobileAgent getAgent(){
		return agent;
	}
	
	Boolean isIOS;
	
	public final boolean isIOS(){
		if(isIOS == null){
			isIOS = ProjectContext.OS_IOS.equals(agent.getOS());
		}
		return isIOS;
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
		return (clientLang == null) ? LangUtil.EN_US : clientLang;
	}
	
	public final String getHCClientVer(){
		return hcClientVer;
	}
	
	public final int getIOSDrawWidth(){
		return iosDrawWidth;
	}
	
	public final int getIOSDrawHeight(){
		return iosDrawHeight;
	}
	
	public final void refreshClientInfo(final J2SESession coreSS, final String serial){
		final Vector v = StringUtil.split(serial, HCConfig.CFG_SPLIT);
		
		clientWidth = HCConfig.getIntProperty(v, (short)0);
		clientHeight = HCConfig.getIntProperty(v, (short)1);
//		clientWidth = 320;
//		clientHeight = 480;
		dpi = HCConfig.getIntProperty(v, (short)2);
		clientLang = HCConfig.getProperty(v, (short)3);
		final String testClientLocale = PropertiesManager.getValue(PropertiesManager.t_testClientLocale);
		if(testClientLocale != null && testClientLocale.length() > 0){
			LogManager.log("use testClientLocale : " + testClientLocale);
			clientLang = testClientLocale;
		}
		//clientLang = "ar";
		hcClientVer = HCConfig.getProperty(v, (short)4);
	
		String serialMobileAgent = "";
		try{
			xdpi = Float.valueOf(HCConfig.getProperty(v, (short)5));
			ydpi = Float.valueOf(HCConfig.getProperty(v, (short)6));
			density = Float.valueOf(HCConfig.getProperty(v, (short)7));
			
			serialMobileAgent = HCConfig.getProperty(v, (short)8);
			agent = MobileAgent.toObject(serialMobileAgent);
			
			final BaseResponsor br = coreSS.getBaseResponsor();
			if(br != null){
				br.addCacheSoftUID(agent.getSoftUID());
			}else{
				LogManager.err("BaseResponsor is null for J2SESession!");
			}
			
			final int iw = (int)Float.parseFloat(agent.get(ConfigManager.IOS_DRAW_WIDTH, "0"));
			final int ih = (int)Float.parseFloat(agent.get(ConfigManager.IOS_DRAW_HEIGHT, "0"));
			
			if(clientHeight > clientWidth){
				iosDrawWidth = Math.min(iw, ih);
				iosDrawHeight = Math.max(iw, ih);
			}else{
				iosDrawHeight = Math.min(iw, ih);
				iosDrawWidth = Math.max(iw, ih);
			}
			
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
				if(key.startsWith(MobileAgent.TAG_HIDE_PREFIX)//节省log及美观
						|| key.startsWith("hc.", 0)){//hc.ui.ios.draw.width
					continue;
				}

				Object value = kv[1];
				if("encryptionStrength".equals(key)){
					if("0".equals(value)){
						value = "low";
					}else if("1".equals(value)){
						value = "middle";
					}else{
						value = "high";
					}
				}
				sb.append("\n  [" + key + " = " + value + "]");
			}
		}
		final String sbStr = sb.toString();
		StringBuilderCacher.cycle(sb);
		LogManager.log(sbStr);
		LogManager.log("Receive client desc, locale:" + clientLang + ",  w:" + clientWidth + ", h:" + clientHeight 
				+ ", dpi:" + dpi + ((dpi==0)?"(unknow)":"") + ", hcClientVer:" + hcClientVer + ", xdpi:" + xdpi + ", ydpi:" + ydpi + ", density:" + density);
		LogManager.log("  Important : the w (h) maybe not equal to the real width (height) of mobile in pixels, UI may be scaled to the best size.");
	}
	
	public static final int vgap = 5;
	public static final int hgap = 5;

}
