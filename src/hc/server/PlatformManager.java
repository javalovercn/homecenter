package hc.server;

import hc.util.ResourceUtil;

public class PlatformManager {
	final static PlatformService service = buildService();
	
	private static PlatformService buildService(){
		try{
			if(ResourceUtil.isAndroidServerPlatform()){
				Class c = Class.forName("hc.android.AndroidPlatformService");
				return (PlatformService)c.newInstance();
			}else{
				Class c = Class.forName("hc.server.j2se.J2SEPlatformService");
				return (PlatformService)c.newInstance();
			}
		}catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PlatformService getService(){
		return service;
	}
}
