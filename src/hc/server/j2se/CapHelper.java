package hc.server.j2se;

import hc.core.util.IMsgNotifier;
import hc.util.ClassUtil;

//import hc.server.ui.video.CapControlFrame;
//import hc.server.ui.video.CapManager;
//import hc.server.ui.video.CapPreviewPane;
//import hc.server.ui.video.CapStream;
//import hc.server.ui.video.CaptureConfig;

public class CapHelper {
	public static final String CapManager_CLASS = "hc.server.ui.video.CapManager";
	public static final String CapNotify_CLASS = "hc.server.ui.video.CapNotify";

	public final static Object capNotify = buildCapNotifyInstance();
	
	static {
		try{
			CapHelper.addListener(CapHelper.capNotify);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static final void addListener(final Object cn){
		try{
			final Class[] paraTypes = {IMsgNotifier.class};
			final Object[] para = {cn};
			ClassUtil.invoke(getCapManagerClass(), getCapManagerClass(), "addListener", paraTypes, para, true);
//		CapManager.addListener(capNotify);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static final Object buildCapNotifyInstance(){
		try{
			final Class c = Class.forName(CapNotify_CLASS);
			return c.newInstance();
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static final Class getCapManagerClass(){
		try {
			return Class.forName(CapManager_CLASS);
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
