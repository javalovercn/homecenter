package hc.util;

import hc.core.util.CCoreUtil;
import hc.core.util.RecycleRes;

import java.util.HashMap;
import java.util.Iterator;

public class RecycleProjThreadPool {
	private final static HashMap<String, RecycleRes> stack = new HashMap<String, RecycleRes>(16);
	
	public final static void recycle(final String projID, final RecycleRes recycleRes){
		if(recycleRes == null){
			return;
		}
		
		CCoreUtil.checkAccess();
		
		synchronized (stack) {
			stack.put(projID, recycleRes);
		}
	}
	
	public final static boolean containsProjID(final String projID){
		return stack.containsKey(projID);
	}
	
	public final static RecycleRes getFree(final String projID){
		CCoreUtil.checkAccess();
		
		synchronized (stack) {
			RecycleRes out = stack.remove(projID);
			if(out == null){
				if(stack.isEmpty()){
					return null;
				}
				final Iterator<String> it = stack.keySet().iterator();
				it.hasNext();
				out = stack.remove(it.next());
			}
			return out;
		}
	}
	
}
