package hc.server.ui.design;

import java.util.HashMap;

/**
 * 进行session进行Event传递和触发
 */
public class SessionEventManager {
	public static final int EVENT_ADD_HAR_BUSY = 1;
	
	private final HashMap<Integer, Runnable> listener = new HashMap<Integer, Runnable>(1);
	
	public final void addListener(final int listenerID, final Runnable run){
		listener.put(listenerID, run);
	}
	
	public final void actionListener(final int listenerID){
		final Runnable run = listener.remove(listenerID);
		if(run != null){
			run.run();
		}
	}
}
