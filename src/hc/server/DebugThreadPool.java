package hc.server;

import hc.core.util.CCoreUtil;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;
import java.util.HashMap;
import java.util.Iterator;

public class DebugThreadPool extends AppThreadPool {
	protected HashMap<RecycleThread, Long> outList;
	protected HashMap<RecycleThread, Thread> outStack;
	Thread t = buildPrintThread();
	
	private Thread buildPrintThread(){
		Thread t = new Thread(){
			public void run(){
				while(true){
					try{
						Thread.sleep(10000);
					}catch (Exception e) {
					}
					
					synchronized (RecycleThread.class) {
						final int size = outList.size();
						if(size > 0){
							final Iterator<RecycleThread> it = outList.keySet().iterator();
							System.out.println("-----------------un-recycle thread (reported by DebugThreadPool.class). to disable this, remove program argument : debugOn-------------------");
							while(it.hasNext()){
								RecycleThread rt = it.next();
								System.out.println(rt.toString() + " out time MS:" + (System.currentTimeMillis() - outList.get(rt)));
								Thread thread = outStack.get(rt);
								StackTraceElement[] ste = thread.getStackTrace();
								final int steSize = ste.length;
								for (int i = 0; i < steSize; i++) {
									System.out.println("  \tat " + ste[i]);
								}
							}
							System.out.println("------------------------------------------------------------");
						}
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
		return t;
	}
	
	@Override
	public final RecycleThread buildRecycleThread(final ThreadPool pool){
		return new RecycleThread(pool){
			@Override
			public final void setRunnable(Runnable r){
				synchronized (RecycleThread.class) {
					if(outList == null){
						outList = new HashMap<RecycleThread, Long>();
						outStack = new HashMap<RecycleThread, Thread>();
					}
					outList.put(this, System.currentTimeMillis());
					outStack.put(this, this.thread);
				}
				super.setRunnable(r);
			}
			
			public void notifyBack(){
//				System.out.println("ThreadPool <- : " + this.toString() + " out time : " + (System.currentTimeMillis() - outList.get(this)));
				synchronized (RecycleThread.class) {
					outList.remove(this);
					outStack.remove(this);
				}
			}
		};
	}
	
	@Override
	protected final void checkAccessPool(Object token){
		CCoreUtil.checkAccess(token);
	}
}
