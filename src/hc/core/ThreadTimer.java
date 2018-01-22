package hc.core;

import hc.core.util.ExceptionReporter;


public class ThreadTimer extends Thread {
	final HCTimer timer;
	boolean isShutDown = false;
	public ThreadTimer(final HCTimer timer){
		this.timer = timer;
		setPriority(timer.newThreadPrority);
		start();
	}
	
	public final void notifyShutdown(){
		isShutDown = true;
		synchronized (timer) {
			timer.notify();
		}
	}
	
	public void run(){
		final NestAction nestAction = (NestAction)ConfigManager.get(ConfigManager.BUILD_NESTACTION, null);
		
		while (!isShutDown) {
			if(timer.isEnable){
				final long nowMS = System.currentTimeMillis();
				final long sleepMS = timer.nextExecMS - nowMS;
				if(sleepMS > 0){
					synchronized (timer) {
						if(isShutDown){
							break;
						}
						try {
//							L.V = L.WShop ? false : LogManager.log("[HCTimer] time : " + timer.name + ", will sleep : " + sleepMS);
							timer.wait(sleepMS);
						} catch (InterruptedException e) {
						}
						continue;
					}		
				}
			}else{
				synchronized (timer) {
					if(isShutDown){
						break;
					}
					try {
						timer.wait();
					} catch (final InterruptedException e) {
					}
					continue;
				}
			}
			
            try{
				if(timer.isEnable){
					timer.nextExecMS += timer.interval;		
						
//					LogManager.log("ThreadTimer do Biz...");
					if(nestAction == null){
						timer.doBiz();
					}else{
						nestAction.action(NestAction.HCTIMER, timer);
					}
				}
            }catch (final Throwable e) {
            	ExceptionReporter.printStackTrace(e);
			}
		}
//		LogManager.log("shutdown ThreadTimer:" + timer.name);
	}
}
