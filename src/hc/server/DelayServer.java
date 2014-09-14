package hc.server;

import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.util.LogManager;

import java.util.LinkedList;

/**
 * 拥有自己独立线程
 * @author homecenter
 *
 */
public class DelayServer extends Thread{
	private static DelayServer server = null;
	
	public static DelayServer getInstance(){
		if(server == null){
			server = new DelayServer();
			server.setPriority(Thread.MIN_PRIORITY);
			server.start();			
		}
		
		return server;
	}
	
    
    public DelayServer(){
    	super("DelayServer");
    }
    
	public void run(){
		AbstractDelayBiz obj = null;
		while (true) {
			synchronized (queue) {
				if (size == 0) {
					if(!isShutdown){
    					try {
    						queue.wait();
    					} catch (InterruptedException ignored) {
    						ignored.printStackTrace();
    					}
						continue;
					}else if(isShutdown){
						break;
					}
				}

				if (size > 0) {
					obj = queue.removeFirst();
					size--;
				}else{
					obj = null;
					//System.err.println("*** UCRunner Error: queue.isEmpty!!" + thread.getName());
				}
				queue.notify();
			}

			
			try{
				obj.doBiz();
			}catch (Throwable e) {
				e.printStackTrace();
			}
			
		}//while
		hc.core.L.V=hc.core.L.O?false:LogManager.log("DelayServer shutdown");
//		if(isShutdown){
//			taskStatusReporter.notifyTaskNumber(taskNumIndex, 0);
//			doShutDownExtralTask();			
//		}
		int time = 15000;//原为30，因退出时，时间过长，所以改为15
		while(time > 0){
			if(ConditionWatcher.isEmpty()){
				break;
			}else{
				time -= 1000;
				try{
					Thread.sleep(1000);
				}catch (Exception e) {
					
				}
			}
		}
		ContextManager.shutDown();

	}	
	
	private LinkedList<AbstractDelayBiz> queue = new LinkedList<AbstractDelayBiz>();
	
	private boolean isShutdown = false;
	
	int size = 0;
	
	public void addDelayBiz(AbstractDelayBiz obj) {
    	synchronized (queue) {
    		queue.addLast(obj);
    		size++;
    		queue.notify();
    	}
    }

	public void shutDown() {
    	isShutdown = true;

    	synchronized (queue) {
    		queue.notifyAll();
    	}
    }
}
