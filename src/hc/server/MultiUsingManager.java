package hc.server;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class MultiUsingManager {
	public final static String NULL_PROJECT_ID = "NullProjID";
	
	private final static HashMap<J2SESession, Vector<String>> usingMap = new HashMap<J2SESession, Vector<String>>(2);
	
	public final synchronized static boolean enter(final J2SESession coreSS, final String screenID, final String targetURL){
		if(targetURL.startsWith(HCURL.URL_DIALOG_PRE, 0)){
			return false;
		}
		
		if(L.isInWorkshop){
			LogManager.log("MultiUsingManager enter : " + screenID);
		}
		
		Vector<String> list = usingMap.get(coreSS);
		if(list == null){
			list = new Vector<String>(24);
			usingMap.put(coreSS, list);
		}
		
		list.add(screenID);
		
		new HCTimer("MultiUsingWarning", 3000, true) {
			@Override
			public void doBiz() {
				HCTimer.remove(this);
				warningMutliUsing(coreSS, screenID);
			}
		};
		
		return true;
	}
	
	private final static synchronized void warningMutliUsing(final J2SESession coreSS, final String screenID){
		if(usingMap.size() < 2){
			return;
		}
		
		final Iterator<J2SESession> it = usingMap.keySet().iterator();
		boolean isUsingAlso = false;
		while(it.hasNext()){
			final J2SESession otherCoreSS = it.next();
			if(otherCoreSS != coreSS){
				final Vector<String> list = usingMap.get(otherCoreSS);
				if(list != null && list.contains(screenID)){
					isUsingAlso = true;
					sendWarning(otherCoreSS);
				}
			}
		}
		
		if(isUsingAlso){
			sendWarning(coreSS);
		}
	}

	private static void sendWarning(final J2SESession coreSS) {
		final J2SESession[] coreSSS = {coreSS};
		ServerUIAPIAgent.sendMessageViaCoreSS(coreSSS, ResourceUtil.getWarnI18N(coreSS), (String)ResourceUtil.get(coreSS, 9239), ProjectContext.MESSAGE_WARN, null, 0);
	}
	
	public final synchronized static void exit(final J2SESession coreSS, final String screenID){
		final Vector<String> list = usingMap.get(coreSS);
		if(list != null){
			final boolean isRemoved = list.remove(screenID);
			if(L.isInWorkshop){
				if(isRemoved){
					LogManager.log("exit [multiUsingWarning] " + screenID);
				}
			}
		}
	}
	
	public final synchronized static void release(final J2SESession coreSS){
		usingMap.remove(coreSS);
	}
}
