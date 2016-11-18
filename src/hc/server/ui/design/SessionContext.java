package hc.server.ui.design;

import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;
import hc.server.ui.ClientSession;

import java.util.HashMap;
import java.util.Stack;

public class SessionContext {
	final static HashMap<String, Stack<SessionContext>> map = new HashMap<String, Stack<SessionContext>>(12);
	
	public static synchronized SessionContext getFreeMobileContext(final String projID, final ThreadGroup projectGroup){
		final Stack<SessionContext> stack = map.get(projID);
		if(stack == null || stack.size() == 0){
			if(L.isInWorkshop){
				LogManager.log("build new instance SessionContext for project [" + projID + "].");
			}
			return new SessionContext(projectGroup);
		}else{
			if(L.isInWorkshop){
				LogManager.log("re-use a SessionContext for project [" + projID + "].");
			}
			return stack.pop();
		}
	}
	
	public static synchronized void cycle(final String projID, final SessionContext mc){
		if(mc == null){
			return;
		}
		
		Stack<SessionContext> stack = map.get(projID);
		if(stack == null){
			stack = new Stack<SessionContext>();
			map.put(projID, stack);
			if(L.isInWorkshop){
				LogManager.log("successful cycle SessionContext for project [" + projID + "]!");
			}
		}
		
		stack.push(mc);
	}
	
	static int groupIdx = 1;
	
	final ThreadGroup mtg;
	public final ThreadPool sessionPool;
	private ClientSession clientSession;
	public J2SESession j2seSocketSession;
	
	/**
	 * 由于重用，需后期更新对应值
	 * @param ss
	 * @param cs
	 */
	public final void setClientSession(final J2SESession ss, final ClientSession cs){
		this.j2seSocketSession = ss;
		this.clientSession = cs;
	}
	
	public final ClientSession getClientSession(){
		return this.clientSession;
	}
	
	public SessionContext(final ThreadGroup projectGroup){
		synchronized (SessionContext.class) {
			mtg = new ThreadGroup(projectGroup, "SessionThreadPoolGroup" + (groupIdx++));
		}
		
		sessionPool = new ThreadPool(mtg) {
			@Override
			protected void checkAccessPool(final Object token) {
			}
			
			@Override
			protected Thread buildThread(final RecycleThread rt) {
				return new Thread(mtg, rt);
			}
		};
	}
}
