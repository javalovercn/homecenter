package hc.server.ui;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.Jcip;
import hc.core.util.LogManager;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;

public class JcipManager {
	
	public static void responseCtrlSubmit(String jcip_str){
		L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "mobile request:" + jcip_str);
		final Jcip jcip = new Jcip(jcip_str);
		final String displayID = jcip.getString();
		
		final ICanvas icanvas = ScreenServer.getCurrScreen();
		
		if(icanvas instanceof ServCtrlCanvas){
			final CtrlResponse cr = ((ServCtrlCanvas)icanvas).cr;
			if(displayID.equals(cr.__hide_currentCtrlID)){
				final String keyValue = jcip.getString();
				cr.getProjectContext().run(new Runnable() {
					@Override
					public void run() {
						cr.click(Integer.parseInt(keyValue));
					}
				});
				return;
			}
		}
	}
	
	public static void appendArray(StringBuilder sb, String[] strs, boolean withDouhao) {
		sb.append('[');
		for (int i = 0; i < strs.length; i++) {
			if(i != 0){
				sb.append(',');
			}
			String tmp = strs[i];
			appendStringItem(sb, tmp);
		}
		sb.append(']');
		
		if(withDouhao){
			sb.append(',');
		}
	}

	public static void appendStringItem(StringBuilder sb, String tmp) {
		sb.append('\'');
		if(tmp.indexOf('\'') >= 0){
			sb.append(tmp.replace("'", "\\'"));
		}else{
			sb.append(tmp);
		}
		sb.append('\'');
	}
}
