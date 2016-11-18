package hc.core;

import hc.core.util.CCoreUtil;

/**
 * 依赖于HCTimer
 */
public class GlobalConditionWatcher {
	private static final HCConditionWatcher eventConditionWatcher = new HCConditionWatcher("GlobalCondWatcher");
	
	public static void cancelAllWatch(){
		eventConditionWatcher.cancelAllWatch();
	}

	public static void removeWatch(final IWatcher watcher){
		eventConditionWatcher.removeWatch(watcher);
	}
	
	public static boolean isEmpty(){
		return eventConditionWatcher.isEmpty();
	}
	
	/**
	 * 如果服务器断线，则可能删除全部watcher，除非IWatcher声明isCancelable
	 * @param watcher
	 */
	public static void addWatcher(final IWatcher watcher){
		CCoreUtil.checkAccess();
		
		eventConditionWatcher.addWatcher(watcher);
	}

}
