package hc.core.util;

public class ThreadPool {
	protected static Stack freeThreads = new Stack();
	protected static boolean isShutDown = false;
	
	public static void start(Runnable run){
		synchronized (freeThreads) {
			if(isShutDown){
				return;
			}
			
			RecycleThread rt = (RecycleThread)freeThreads.pop();
			if(rt == null){
				rt = new RecycleThread();
				rt.setRunnable(run);
				rt.start();
//				L.V = L.O ? false : LogManager.log("create new cycleThread");
			}else{
				rt.setRunnable(run);
			}
		}
	}
	
	public static void shutdown(){
		synchronized (freeThreads) {
			isShutDown = true;
			RecycleThread rt;
			while((rt = (RecycleThread)freeThreads.pop()) != null){
				rt.shutdown();
			}
		}
	}
}

class RecycleThread extends Thread{
	boolean isWaiting = true;
	Runnable runnable;
	public void run(){
		while(!ThreadPool.isShutDown){
			if(runnable != null){
				try{
					runnable.run();
				}catch (Throwable e) {
					e.printStackTrace();
				}finally{
					runnable = null;
				}
			}			
			
			final Stack free = ThreadPool.freeThreads;
			synchronized (free) {
				if(ThreadPool.isShutDown){
					return;
				}
				free.push(this);
//				L.V = L.O ? false : LogManager.log("recycle a thread");
			}
			synchronized(this){
				isWaiting = true;
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
		}
//		L.V = L.O ? false : LogManager.log("Recycle Thread finished!");
	}
	
	public void setRunnable(Runnable r){
		runnable = r;
		while(isWaiting == false){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
		synchronized (this) {
			isWaiting = false;
			this.notifyAll();
		}
	}
	
	public void shutdown(){
		while(isWaiting == false){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
		synchronized (this) {
			isWaiting = false;
			this.notifyAll();
		}
	}
}
