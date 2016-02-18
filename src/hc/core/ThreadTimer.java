package hc.core;


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
		final NestAction nestAction = EventCenter.nestAction;
		
		while (!isShutDown) {
			if(timer.isEnable){
				final long nowMS = System.currentTimeMillis();
				final long sleepMS = timer.nextExecMS - nowMS;
				if(sleepMS > 0){
					synchronized (timer) {
						try {
							timer.wait(sleepMS);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}		
				}
			}else{
				synchronized (timer) {
					try {
						timer.wait();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
			}
			
            try{
				if(timer.isEnable){
					timer.nextExecMS += timer.interval;		
						
//					L.V = L.O ? false : LogManager.log("ThreadTimer do Biz...");
					if(nestAction == null){
						timer.doBiz();
					}else{
						nestAction.action(NestAction.HCTIMER, timer);
					}
				}
            }catch (final Throwable e) {
            	e.printStackTrace();
			}
		}
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("shutdown ThreadTimer:" + timer.name);
	}
}
