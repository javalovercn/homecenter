package hc.server;

import hc.util.ResourceUtil;

public class PlatformManager {
	final static PlatformService service = buildService();
	
	private static PlatformService buildService(){
		try{
			if(ResourceUtil.isAndroidServerPlatform()){
				final Class c = Class.forName("hc.android.AndroidPlatformService");
				return (PlatformService)c.newInstance();
			}else{
				final Class c = Class.forName("hc.server.j2se.J2SEPlatformService");
				return (PlatformService)c.newInstance();
			}
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PlatformService getService(){
		//注意：
		//由于被ResourceUtil.resizeImage所限，不能进行checkAccess
		//如果涉及安全检查，请分散到方法内
		return service;
	}
}
