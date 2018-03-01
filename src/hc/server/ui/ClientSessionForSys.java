package hc.server.ui;

import hc.core.ContextManager;
import hc.core.util.ReturnableRunnable;
import hc.res.ImageSrc;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

import java.util.Hashtable;

public class ClientSessionForSys {

	private final static int CLIENT_SESSION_ATTRIBUTE_OK_ICON = 0;
	private final static int CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON = 1;
	private final static int CLIENT_SESSION_ATTRIBUTE_MOV_UP_ICON = 2;
	private final static int CLIENT_SESSION_ATTRIBUTE_MOV_DOWN_ICON = 3;
	private final static int CLIENT_SESSION_ATTRIBUTE_REMOVE_ICON = 4;

	public final static String STR_CLIENT_SESSION_ATTRIBUTE_OK_ICON = String
			.valueOf(CLIENT_SESSION_ATTRIBUTE_OK_ICON);
	public final static String STR_CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON = String
			.valueOf(CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON);
	public final static String STR_CLIENT_SESSION_ATTRIBUTE_MOV_UP_ICON = String
			.valueOf(CLIENT_SESSION_ATTRIBUTE_MOV_UP_ICON);
	public final static String STR_CLIENT_SESSION_ATTRIBUTE_MOV_DOWN_ICON = String
			.valueOf(CLIENT_SESSION_ATTRIBUTE_MOV_DOWN_ICON);
	public final static String STR_CLIENT_SESSION_ATTRIBUTE_REMOVE_ICON = String
			.valueOf(CLIENT_SESSION_ATTRIBUTE_REMOVE_ICON);

	private final static String[] ICON_KEYS = { STR_CLIENT_SESSION_ATTRIBUTE_OK_ICON,
			STR_CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON, STR_CLIENT_SESSION_ATTRIBUTE_MOV_UP_ICON,
			STR_CLIENT_SESSION_ATTRIBUTE_MOV_DOWN_ICON, STR_CLIENT_SESSION_ATTRIBUTE_REMOVE_ICON };

	private static final String[] SMALL_PATHS = { ImageSrc.HC_RES_OK_22_PNG_PATH,
			ImageSrc.HC_RES_CANCEL_22_PNG_PATH, ImageSrc.HC_RES_UP_22_PNG_PATH,
			ImageSrc.HC_RES_DOWN_22_PNG_PATH, ImageSrc.HC_RES_REMOVE_22_PNG_PATH };
	private static final String[] NORM_PATHS = { ImageSrc.HC_RES_OK_44_PNG_PATH,
			ImageSrc.HC_RES_CANCEL_44_PNG_PATH, ImageSrc.HC_RES_UP_44_PNG_PATH,
			ImageSrc.HC_RES_DOWN_44_PNG_PATH, ImageSrc.HC_RES_REMOVE_44_PNG_PATH };

	private final ThreadGroup token;
	private final J2SESession coreSS;

	private final Hashtable<String, Object> attribute_map = new Hashtable<String, Object>();

	public ClientSessionForSys(final J2SESession coreSS, final ThreadGroup token) {
		this.coreSS = coreSS;
		this.token = token;
	}

	public final Object getAttribute(final String name) {
		Object out = attribute_map.get(name);
		if (out == null) {
			for (int i = 0; i < ICON_KEYS.length; i++) {
				if (ICON_KEYS[i] == name) {
					final int nameIdx = i;
					out = ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
						@Override
						public Object run() throws Throwable {
							return getIcon(nameIdx, name);
						}
					}, token);
					break;
				}
			}

		}
		return out;
	}

	private final Object getIcon(final int nameIdx, final String key) {
		Object out;
		final int dpi = UserThreadResourceUtil.getMletDPIFrom(coreSS);
		if (dpi < 300) {
			out = ImageSrc.loadImageFromPath(SMALL_PATHS[nameIdx]);
		} else {
			out = ImageSrc.loadImageFromPath(NORM_PATHS[nameIdx]);
		}
		attribute_map.put(key, out);
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

	public final boolean isAttributeEmpty() {
		return attribute_map.isEmpty();
	}
}
