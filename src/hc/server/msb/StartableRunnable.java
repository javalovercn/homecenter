package hc.server.msb;

public abstract class StartableRunnable implements Runnable {
	boolean isStarted = false;
	
	public void start(){
		synchronized (this) {
			if(isStarted == false){
				isStarted = true;
				notify();
			}
		}
	}
	
	@Override
	public void run() {
		synchronized (this) {
			if(isStarted == false){
				try{
					wait();
				}catch (Exception e) {
				}
			}
		}
		
		try{
			runAfterStart();
		}catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public abstract void runAfterStart();

}
