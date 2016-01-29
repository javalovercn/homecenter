package hc.core;

public class ThreadTimer extends Thread {
	final HCTimer timer;
	boolean isShutDown = false;
	public ThreadTimer(final HCTimer timer){
		this.timer = timer;
		setPriority(timer.newThreadPrority);
		start();
	}
	
	private static final int TT_INTERNAL_MS = 1000;
	
	public void notifyShutdown(){
		isShutDown = true;
		synchronized (timer) {
			timer.notify();
		}
	}
	
	public void run(){
		final NestAction nestAction = EventCenter.nestAction;
		long min_wait_mill_second = HCTimer.TEMP_MAX;
		while (!isShutDown) {
			min_wait_mill_second = HCTimer.TEMP_MAX;
			
			if(timer.isEnable){
				final long left = timer.nextExecMS;
				if(min_wait_mill_second > left){
					min_wait_mill_second = left;
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
			
//			L.V = L.O ? false : LogManager.log("HCTimer Main sleep:" + min_wait_mill_second);
			final long nowExecMS = System.currentTimeMillis();
			
			long sleepMS = min_wait_mill_second - nowExecMS;
			if(sleepMS > 0){
				boolean isContinue = false;
				if(min_wait_mill_second > (nowExecMS + TT_INTERNAL_MS)){
					sleepMS = TT_INTERNAL_MS;
					isContinue = true;
				}

				try {
//					L.V = L.O ? false : LogManager.log("ThreadTimer sleep : " + sleepMS);
					Thread.sleep(sleepMS);
                } catch (final Exception e) {
                }
				if(isContinue){
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
