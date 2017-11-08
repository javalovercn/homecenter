package hc.core;

import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

public class EventCenterDriver extends HCConditionWatcher {
	private static final Object lock = new Object();
	private static int aliveCount = 0;
	private static boolean notifyShutdown = false;
	
	EventCenterDriver(){
		super("EventCenterDriver", ThreadPriorityManager.GECD_THREADGROUP_PRIORITY);
		synchronized (lock) {
			aliveCount++;
			if(notifyShutdown){
				notifyShutdown();
			}
		}
	}
	
	public void notifyAllDoneAfterShutdown(){
		super.notifyAllDoneAfterShutdown();
		synchronized (lock) {
			aliveCount--;
			if(aliveCount == 0){
				lock.notify();
			}
		}
		L.V = L.WShop ? false : LogManager.log("a EventCenterDriver instance AllDoneAfterShutdown.");
	}
	
	public static void waitForAllDriverDone(){
		synchronized (lock) {
			notifyShutdown = true;
			L.V = L.WShop ? false : LogManager.log("EventCenterDriver aliveCount : " + aliveCount);
			if(aliveCount > 0){
				try {
					lock.wait(ThreadPriorityManager.SEQUENCE_TASK_MAX_WAIT_MS);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
