package hc.core;

import hc.core.util.ExceptionReporter;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

public class HCConditionWatcher {
	private boolean isNotifyShutdown;
	
	public final void notifyShutdown(){
		isNotifyShutdown = true;
	}
	
	private final String timeName;
	//注意:
	//由于本对象属于HCTimer，所以AckBatchHCTimer去掉锁，未来变动时，请开启AckBatchHCTimer的锁机制。
	private final HCTimer watcherTimer;
	private final boolean isInWorkshop;
	
	public HCConditionWatcher(final String timeName){
		this(timeName, ThreadPriorityManager.LOWEST_PRIORITY);
	}
	
	public HCConditionWatcher(final String timeName, final int newThreadPrority){
		this(timeName, true, newThreadPrority);
	}
	
	private HCConditionWatcher(final String timeName, final boolean isNewThread, final int newThreadPrority){
		this.timeName = timeName;
		isInWorkshop = L.isInWorkshop;
		
		watcherTimer = new HCTimer(timeName, HCTimer.HC_INTERNAL_MS, false, isNewThread, newThreadPrority){
//			public final void setEnable(final boolean enable){
//				if(isInWorkshop){
//					L.V = L.O ? false : LogManager.log("[" + timeName + "] setEnable : " + enable);
//				}
//				super.setEnable(enable);
//			}
			
			public final void doBiz() {
				IWatcher temp;
				boolean isAddUnUsed = false;
				do{
					synchronized (watchers) {
						temp = (IWatcher)watchers.getFirst();//注意：必须先入先处理
					}
					
					if(temp == null){
						if(isNotifyShutdown){//直到所有任务完成后，移除自己
							if(L.isInWorkshop){
								L.V = L.O ? false : LogManager.log("shutdown HCConditionWatcher [" + watcherTimer.getName() + "].");
							}
							HCTimer.remove(watcherTimer);
						}
						break;
					}
					
//					if(isInWorkshop){
//						L.V = L.O ? false : LogManager.log("[" + timeName + "] processing a watcher : " + temp.hashCode());
//					}
					
					//因为在执行本实例时，有可能遇到同时请求移出，所以加实例锁，并在内部加集合锁
					synchronized (temp) {
						try{
							if(temp.watch() == false){
								synchronized (watchers) {
									usedRewatchers.addTail(temp);
									isAddUnUsed = true;
								}
							}
						}catch (Throwable e) {//异常不会返回true，导致永久执行
							ExceptionReporter.printStackTrace(e);
						}
					}
//					if(isInWorkshop){
//						L.V = L.O ? false : LogManager.log("[" + timeName + "] processed a watcher : " + temp.hashCode());
//					}
				}while(true);
				
				if(isAddUnUsed){
					if(isInWorkshop){
						L.V = L.O ? false : LogManager.log("[" + timeName + "] processing unUsed watcher.");
					}
					
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
	}
	
	public final void cancelAllWatch(){
		synchronized (watchers){
			cancelOp(watchers);
			
			cancelOp(usedRewatchers);
		}
	}

	private final void cancelOp(final LinkedSet ls) {
		IWatcher watcher;
		LinkedSet unCancelSet = null;
		
		while((watcher = (IWatcher)ls.getFirst()) != null){
			if(watcher.isCancelable()){//专为EventBack之用
				if(isInWorkshop){
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

	public final void removeWatch(final IWatcher watcher){
		synchronized (watcher) {
			synchronized (watchers) {
				watchers.removeData(watcher);
				usedRewatchers.removeData(watcher);
				
				if(watchers.isEmpty() && usedRewatchers.isEmpty()){
					watcherTimer.setEnable(false);
				}
			}		
		}
	}
	
	public final boolean isEmpty(){
		synchronized (watchers) {
			return watchers.isEmpty() && usedRewatchers.isEmpty();
		}
	}
	
	private final LinkedSet watchers = new LinkedSet();
	private final LinkedSet usedRewatchers = new LinkedSet();
	
	/**
	 * 如果服务器断线，则可能删除全部watcher，除非IWatcher声明isCancelable
	 * @param watcher
	 */
	public final void addWatcher(final IWatcher watcher){
//		if(isNotifyShutdown){//允许继续添加
//			return;
//		}
		
		if(isInWorkshop){
			L.V = L.O ? false : LogManager.log("[" + timeName + "] add watcher : " + watcher.hashCode());
		}
		
		synchronized (watchers) {
			if(watchers.addTail(watcher)){
				watcherTimer.setEnable(true);
			}
		}
	}

}
