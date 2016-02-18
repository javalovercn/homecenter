package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;

/**
 * 依赖于HCTimer
 */
public class ConditionWatcher {
	//注意:
	//由于本对象属于HCTimer，所以AckBatchHCTimer去掉锁，未来变动时，请开启AckBatchHCTimer的锁机制。
	private static HCTimer instance = new HCTimer("CondWat", HCTimer.HC_INTERNAL_MS, false){
		public final void doBiz() {
			IWatcher temp;
			boolean isAddUnUsed = false;
			do{
				synchronized (watchers) {
					temp = (IWatcher)watchers.getFirst();//注意：必须先入先处理
				}
				
				if(temp == null){
					break;
				}
				
				//因为在执行本实例时，有可能遇到同时请求移出，所以加实例锁，并在内部加集合锁
				synchronized (temp) {
					if(temp.watch() == false){
						synchronized (watchers) {
							usedRewatchers.addTail(temp);
							isAddUnUsed = true;
						}
					}
				}
			}while(true);
			
			if(isAddUnUsed){
				synchronized (watchers) {
					Object rewatcher;
					while((rewatcher = usedRewatchers.getFirst()) != null){
						watchers.addTail(rewatcher);
					}
				}
			}else{
				synchronized (watchers) {
					if(watchers.isEmpty()){
						setEnable(false);
						return;
					}
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

	private static void cancelOp(final LinkedSet ls) {
		IWatcher watcher;
		LinkedSet unCancelSet = null;
		
		while((watcher = (IWatcher)ls.getFirst()) != null){
			if(watcher.isCancelable()){//专为EventBack之用
				if(L.isInWorkshop){
					System.out.println("----------cancle EventBack : " + watcher.getClass().getName());
				}
				watcher.cancel();
			}else{
				//不可cancel
				if(unCancelSet == null){
					unCancelSet = new LinkedSet();
				}
				unCancelSet.addTail(watcher);
			}
		}
		
		while(unCancelSet != null && (watcher = (IWatcher)unCancelSet.getFirst()) != null){
			ls.addTail(watcher);
		}
	}

	public static void removeWatch(final IWatcher watcher){
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
	 * 如果服务器断线，则可能删除全部watcher，除非IWatcher声明isCancelable
	 * @param watcher
	 */
	public static void addWatcher(final IWatcher watcher){
		CCoreUtil.checkAccess();
		
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
