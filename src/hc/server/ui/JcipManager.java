package hc.server.ui;

import hc.core.L;
import hc.core.util.Jcip;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.engine.RubyExector;

public class JcipManager {
	
	public static void responseCtrlSubmit(final J2SESession coreSS, final String jcip_str){
		L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "mobile request:" + jcip_str);
		final Jcip jcip = new Jcip(coreSS, jcip_str);
		final String displayID = jcip.getString();
		
		final ICanvas icanvas = ScreenServer.getCurrScreen(coreSS);
		
		if(icanvas instanceof ServCtrlCanvas){
			final CtrlResponse cr = ((ServCtrlCanvas)icanvas).cr;
			if(displayID.equals(cr.target)){
				final String keyValue = jcip.getString();
				RubyExector.execInSequenceForSession(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(cr.getProjectContext()), new ReturnableRunnable() {
					@Override
					public Object run() {
						cr.click(Integer.parseInt(keyValue));
						return null;
					}
				});
				return;
			}
		}
	}
	
	public static void appendArray(final StringBuilder sb, final String[] strs, final boolean withDouhao) {
		sb.append('[');
		for (int i = 0; i < strs.length; i++) {
			if(i != 0){
				sb.append(',');
			}
			final String tmp = strs[i];
			appendStringItem(sb, tmp);
		}
		sb.append(']');
		
		if(withDouhao){
			sb.append(',');
		}
	}

	public static void appendStringItem(final StringBuilder sb, final String tmp) {
		sb.append('\'');
		if(tmp.indexOf('\'') >= 0){
			sb.append(tmp.replace("'", "\\'"));
		}else{
			sb.append(tmp);
		}
		sb.append('\'');
	}
}
