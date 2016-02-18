package hc.core.util;

import hc.core.IConstant;


public class RecycleThread implements Runnable{
	//该变量能确保先进入waiting，而后被应用。否则极端下，有可能被应用，而后自己将自己waiting
	boolean isWaiting = true;
	Runnable runnable;
	final ThreadPool pool;
	protected Thread thread;
	
	public RecycleThread(ThreadPool p){
		pool = p;
	}
	
	public final void setThread(Thread thread){
		this.thread = thread;
	}
	
	public final void run(){
		final LinkedSet free = pool.freeThreads;

		while(!ThreadPool.isShutDown){
			if(runnable != null){
				try{
					runnable.run();
				}catch (Throwable e) {
					e.printStackTrace();
				}
			}			
			
			notifyBack();//本行要置于push之前，以关闭完成前项逻辑。
			
			synchronized(this){
				synchronized (free) {
					if(ThreadPool.isShutDown){
						return;
					}
					free.addTail(this);
					free.notifyAll();//注意：不能notify()
	//				System.out.println("[" + pool.name + "] <- RecycleThead : " + toString());
				}
			
				isWaiting = true;
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
		}
//		L.V = L.O ? false : LogManager.log("Recycle Thread finished!");
	}
	
	public void notifyBack(){
		
	}
	
	public void setRunnable(Runnable r){
		runnable = r;
		synchronized (this) {
			isWaiting = false;
			this.notify();
		}
	}

	final void wakeUp() {
		while(isWaiting == false){
			try {
				Thread.sleep(IConstant.THREAD_WAIT_INNER_MS);
			} catch (InterruptedException e) {
			}
		}
		synchronized (this) {
			isWaiting = false;
			this.notify();
		}
	}
	
}
