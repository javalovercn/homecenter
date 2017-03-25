package hc.server.ui;

import hc.core.ContextManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ReturnableRunnable;
import hc.res.ImageSrc;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

import java.util.Hashtable;

public class ClientSessionForSys {
	public final static String CLIENT_SESSION_ATTRIBUTE_IS_TRANSED_MLET_BODY = CCoreUtil.SYS_RESERVED_KEYS_START + "mlet_html_body";
	public final static String CLIENT_SESSION_ATTRIBUTE_OK_ICON = CCoreUtil.SYS_RESERVED_KEYS_START + "okIcon";
	public final static String CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON = CCoreUtil.SYS_RESERVED_KEYS_START + "cancelIcon";

	private final ThreadGroup token;
	private final J2SESession coreSS;
	
	private final Hashtable<String, Object> attribute_map = new Hashtable<String, Object>();
	
	public ClientSessionForSys(final J2SESession coreSS, final ThreadGroup token){
		this.coreSS = coreSS;
		this.token = token;
	}
	
	public final Object getAttribute(final String name) {
		Object out = attribute_map.get(name);
		if(out == null){
			if(CLIENT_SESSION_ATTRIBUTE_OK_ICON == name){
				out = ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
					@Override
					public Object run() {
						return getOK(name);
					}
				}, token);
			}else if(CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON == name){
				out = ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
					@Override
					public Object run() {
						return getCancel(name);
					}
				}, token);
			}
		}
		return out;
	}

	private Object getCancel(final String name) {
		Object out;
		final int dpi = UserThreadResourceUtil.getMobileDPIFrom(coreSS);
		if(dpi < 300){
			out = ImageSrc.loadImageFromPath(ImageSrc.HC_RES_CANCEL_22_PNG_PATH);
		}else{
			out = ImageSrc.loadImageFromPath(ImageSrc.HC_RES_CANCEL_44_PNG_PATH);
		}
		attribute_map.put(name, out);
		return out;
	}

	private Object getOK(final String name) {
		Object out;
		final int dpi = UserThreadResourceUtil.getMobileDPIFrom(coreSS);
		if(dpi < 300){
			out = ImageSrc.loadImageFromPath(ImageSrc.HC_RES_OK_22_PNG_PATH);
		}else{
			out = ImageSrc.loadImageFromPath(ImageSrc.HC_RES_OK_44_PNG_PATH);
		}
		attribute_map.put(name, out);
		return out;
	}
	
	public final Object getAttribute(final String name, final Object defaultValue) {
		final Object value = getAttribute(name);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}
	
	public final Object setAttribute(final String name, final Object obj) {
		return attribute_map.put(name, obj);
	}
	
	public final Object removeAttribute(final String name) {
		return attribute_map.remove(name);
	}
	
	public final Object clearAttribute(final String name) {
		return removeAttribute(name);
	}
	
	public final int getAttributeSize() {
		return attribute_map.size();
	}
	
    public final boolean containsAttributeName(final Object name) {
        return attribute_map.containsKey(name);
    }
    
    public final boolean containsAttributeObject(final Object object) {
        return attribute_map.contains(object);
    }
    
    public final boolean isAttributeEmpty(){
    	return attribute_map.isEmpty();
    }
}
