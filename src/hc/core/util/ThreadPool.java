package hc.core.util;

import hc.core.IConstant;
import java.util.Vector;

public abstract class ThreadPool {
	private static final Stack waitStack = new Stack(32);
	final static Vector list = new Vector(64);
	protected final LinkedSet freeThreads = new LinkedSet();
	static protected boolean isShutDown = false;
	protected final Object threadGroup;
	protected String name = "";
	
	private final static String DONE_RETURN = "doneReturn";
	
	/**
	 * 
	 * @param run 如果产生异常，则返回null
	 * @return
	 */
	public Object runAndWait(final ReturnableRunnable run){
		return this.runAndWait(run, null);
	}
	
	/**
	 * 
	 * @param pool
	 * @param run 如果产生异常，则返回null
	 * @param threadToken
	 * @return
	 */
	public Object runAndWait(final ReturnableRunnable run, final Object threadToken){
		Object[] wait;
		synchronized (waitStack) {
			wait = (Object[])waitStack.pop();
		}
		if(wait == null){
			wait = new Object[2];
		}
		
		final Object[] finalWait = wait;
		
		this.run(new Runnable() {
			public void run() {
				try{
					finalWait[0] = run.run();
				}catch (Throwable e) {
					finalWait[0] = e;
					e.printStackTrace();
				}
				
				synchronized (finalWait) {
					finalWait[1] = DONE_RETURN;
					finalWait.notify();
				}
			}
		}, threadToken);
		
		try{
			synchronized (finalWait) {
				if(finalWait[1] == null){
					finalWait.wait();
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		Object out = finalWait[0];
		if(out != null && out instanceof Throwable){
			this.printStack();
			out = null;
		}
		finalWait[0] = null;
		finalWait[1] = null;
		synchronized (waitStack) {
			waitStack.push(finalWait);
		}
		
		return out;
	}
	
	public void printStack(){
		
	}
	
	public void setName(String n){
		name = n;
	}
	
	public ThreadPool(Object threadGroup){
		this.threadGroup = threadGroup;
		
		synchronized (list) {
			if(isShutDown){
				throw new Error("system is shutdown!");
			}
			list.addElement(this);
		}
		
		buildNextOne();
	}
	
	public final Object getThreadGroup(){
		return threadGroup;
	}
	
	protected abstract void checkAccessPool(Object token);
	
	public final synchronized void run(Runnable run, Object token){
		checkAccessPool(token);
		
		RecycleThread rt;
		
		boolean buildOne = false;

		while(freeThreads.isEmpty()){
			try{
				Thread.sleep(IConstant.THREAD_WAIT_INNER_MS);
			}catch (Exception e) {
			}
		}
		
		if(isShutDown){
			return;
		}
		synchronized (freeThreads) {
			rt = (RecycleThread)freeThreads.getFirst();
		}
//			System.out.println("[" + name + "] -> RecycleThead : " + rt.toString());
		if(freeThreads.isEmpty()){
			rt.setRunnable(new Runnable() {
				public void run() {
					buildNextOne();
				}
			});
			buildOne = true;
		}
		
		if(buildOne){
			do{
				try{
					Thread.sleep(IConstant.THREAD_WAIT_INNER_MS);
				}catch (Exception e) {
				}
			}while(freeThreads.isEmpty());
				
			this.run(run, token);
			
			return;
		}

		rt.setRunnable(run);
	}

	public final void buildNextOne() {
		RecycleThread rt;
		rt = buildRecycleThread(this);
		Thread t = buildThread(rt);
		rt.setThread(t);
		rt.setRunnable(new Runnable() {
			public void run() {
			}
		});
		t.start();
	}
	
	public RecycleThread buildRecycleThread(ThreadPool pool){
		return new RecycleThread(pool);
	}
	
	public void run(Runnable run){
		run(run, null);
	}

	protected abstract Thread buildThread(RecycleThread rt);
	
	private final synchronized void exit(){
		RecycleThread rt;
		synchronized (freeThreads) {
			while((rt = (RecycleThread)freeThreads.getFirst()) != null){
				rt.wakeUp();
			}
		}
	}
	
	public static void shutdown(){
		synchronized (list) {
			isShutDown = true;
			final int size = list.size();
			for (int i = 0; i < size; i++) {
				((ThreadPool)list.elementAt(i)).exit();
			}
		}
	}
}