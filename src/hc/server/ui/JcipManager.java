package hc.server.ui;

import hc.core.HCTimer;
import hc.core.util.Jcip;
import hc.core.util.LogManager;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;

public class JcipManager {
	
	public static void addFormTimer(String fid, IFormTimer t){
		synchronized (timers) {
			if(size == MAX_SIZE){
				hc.core.L.V=hc.core.L.O?false:LogManager.log("Jcip Form Timer Oversize:" + MAX_SIZE);
				return;
			}
			
			for (int i = 0; i < MAX_SIZE; i++) {
				if(formID[i] == null){
					formID[i] = fid;
					timers[i] = new FormTimer(t);
					size++;
					return;
				}
			}
		}
	}
	
	public static void responseFormSubmit(String jcip_str){
		hc.core.L.V=hc.core.L.O?false:LogManager.log(ScreenCapturer.OP_STR + "mobile request:" + jcip_str);
		final Jcip jcip = new Jcip(jcip_str);
		final String displayID = jcip.getString();
		
		final ICanvas icanvas = ScreenServer.getCurrScreen();
		
		if(icanvas instanceof ServCtrlCanvas){
			final CtrlResponse cr = ((ServCtrlCanvas)icanvas).cr;
			if(displayID.equals(cr.__hide_currentCtrlID)){
				final String keyValue = jcip.getString();
				cr.click(Integer.parseInt(keyValue));
				return;
			}
		}else if(icanvas instanceof MForm){
			
		}
	}
	
	public static void removeAutoResonseTimer(String id){
		for (int i = 0; i < MAX_SIZE; i++) {
			String fid = formID[i];
			if(fid != null && fid.equals(id)){
				clearItem(i);
				return;
			}
		}
	}

	private static void clearItem(int i) {
		formID[i] = null;
		timers[i].setEnable(false);
		HCTimer.remove(timers[i]);
	}
	
	public static void clearAllTimer(){
		int c = 0;
		for (int i = 0; i < MAX_SIZE && c < size; i++) {
			if(formID[i] != null){
				c++;
				clearItem(i);
			}
		}
		size = 0;
	}
	
	private static final int MAX_SIZE = 50;
	private static final String[] formID = new String[MAX_SIZE];
	private static final HCTimer[] timers = new HCTimer[MAX_SIZE];
	private static int size = 0;
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
