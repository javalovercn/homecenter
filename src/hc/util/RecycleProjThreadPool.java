package hc.util;

import hc.core.util.CCoreUtil;
import hc.core.util.ThreadPool;
import java.util.HashMap;
import java.util.Iterator;

public class RecycleProjThreadPool {
	private final static HashMap<String, ThreadPool> stack = new HashMap<String, ThreadPool>(16);
	
	public final static void recycle(String projID, ThreadPool pool){
		if(pool == null){
			return;
		}
		
		CCoreUtil.checkAccess();
		
		synchronized (stack) {
			stack.put(projID, pool);
		}
	}
	
	public final static boolean containsProjID(String projID){
		return stack.containsKey(projID);
	}
	
	public final static ThreadPool getFree(String projID){
		CCoreUtil.checkAccess();
		
		synchronized (stack) {
			ThreadPool out = stack.remove(projID);
			if(out == null){
				if(stack.isEmpty()){
					return null;
				}
				Iterator<String> it = stack.keySet().iterator();
				it.hasNext();
				out = stack.remove(it.next());
			}
			return out;
		}
	}
	
}
