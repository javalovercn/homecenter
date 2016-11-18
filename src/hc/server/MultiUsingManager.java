package hc.server;

import hc.core.HCTimer;
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
	
	public final synchronized static void enter(final J2SESession coreSS, final String projectID, final String targetURL){
		Vector<String> list = usingMap.get(coreSS);
		if(list == null){
			list = new Vector<String>(24);
			usingMap.put(coreSS, list);
		}
		
		list.add(buildItem(projectID, targetURL));
		
		new HCTimer("MultiUsingWarning", 3000, true) {
			@Override
			public void doBiz() {
				HCTimer.remove(this);
				warningMutliUsing(coreSS, projectID, targetURL);
			}
		};
	}
	
	private final static synchronized void warningMutliUsing(final J2SESession coreSS, final String projectID, final String targetURL){
		if(usingMap.size() < 2){
			return;
		}
		
		final String item = buildItem(projectID, targetURL);
		
		final Iterator<J2SESession> it = usingMap.keySet().iterator();
		boolean isUsingAlso = false;
		while(it.hasNext()){
			final J2SESession otherCoreSS = it.next();
			if(otherCoreSS != coreSS){
				final Vector<String> list = usingMap.get(otherCoreSS);
				if(list != null && list.contains(item)){
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
		ServerUIAPIAgent.sendMessageViaCoreSS(coreSSS, ResourceUtil.getWarnI18N(), (String)ResourceUtil.get(9239), ProjectContext.MESSAGE_WARN, null, 0);
	}
	
	private final static String buildItem(final String projectID, final String targetURL){
		return projectID + targetURL.toLowerCase();
	}
	
	public final synchronized static void exit(final J2SESession coreSS, final String projectID, final String targetURL){
		final Vector<String> list = usingMap.get(coreSS);
		if(list != null){
			final boolean isRemoved = list.remove(buildItem(projectID, targetURL));
//			if(L.isInWorkshop){
//				if(isRemoved){
//					L.V = L.O ? false : LogManager.log("exit [multiUsingWarning] " + projectID + ", " + targetURL);
//				}
//			}
		}
	}
	
	public final synchronized static void release(final J2SESession coreSS){
		usingMap.remove(coreSS);
	}
}
