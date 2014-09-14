package hc.core;

import hc.core.util.LinkedSet;

/**
 * 依赖于HCTimer
 * @author homecenter
 *
 */
public class ConditionWatcher {
	//注意:
	//由于本对象属于HCTimer，所以AckBatchHCTimer去掉锁，未来变动时，请开启AckBatchHCTimer的锁机制。
	private static HCTimer instance = new HCTimer("CondWat", HCTimer.HC_INTERNAL_MS, false){
		public void doBiz() {
			IWatcher temp;
			do{
				synchronized (watchers) {
					temp = (IWatcher)watchers.getFirst();
				}
				
				if(temp == null){
					break;
				}
				
				//因为在执行本实例时，有可能遇到同时请求移出，所以加实例锁，并在内部加集合锁
				synchronized (temp) {
					if(temp.watch() == false){
						synchronized (watchers) {
							usedRewatchers.addTail(temp);
						}
					}
				}
			}while(true);

			
			synchronized (watchers) {
				Object rewatcher;
				while((rewatcher = usedRewatchers.getFirst()) != null){
					watchers.addTail(rewatcher);
				}
				
				if(watchers.isEmpty()){
					setEnable(false);
					return;
				}
			}
		}
	};
	
	public static void cancelAllWatch(){
		synchronized (watchers){
			cancelOp(watchers);
			
			cancelOp(usedRewatchers);
		}
	}

	private static void cancelOp(LinkedSet ls) {
		IWatcher watcher;
		LinkedSet unCancelSet = null;
		
		while((watcher = (IWatcher)ls.getFirst()) != null){
			if(watcher.isNotCancelable()){
				//不可cancel
				if(unCancelSet == null){
					unCancelSet = new LinkedSet();
				}
				unCancelSet.addTail(watcher);
			}else{
				watcher.cancel();
			}
		}
		
		while(unCancelSet != null && (watcher = (IWatcher)unCancelSet.getFirst()) != null){
			ls.addTail(watcher);
		}
	}

	public static void removeWatch(IWatcher watcher){
		synchronized (watcher) {
			synchronized (watchers) {
				watchers.removeData(watcher);
				usedRewatchers.removeData(watcher);
				
				if(watchers.isEmpty() && usedRewatchers.isEmpty()){
					instance.setEnable(false);
				}
			}		
		}
	}
	
	public static boolean isEmpty(){
		synchronized (watchers) {
			return watchers.isEmpty() && usedRewatchers.isEmpty();
		}
	}
	
	private final static LinkedSet watchers = new LinkedSet();
	private final static LinkedSet usedRewatchers = new LinkedSet();
	
	/**
	 * 如果服务器断线，则可能删除全部watcher，除非IWatcher声明isNotCancelable
	 * @param watcher
	 */
	public static void addWatcher(IWatcher watcher){
		boolean isEmpty = false;
		synchronized (watchers) {
			isEmpty = watchers.isEmpty();
			
			watchers.addTail(watcher);
		}
		if(isEmpty){
			instance.setEnable(true);
		}
	}

}
