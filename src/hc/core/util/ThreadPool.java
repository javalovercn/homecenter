package hc.core.util;

import java.util.Vector;

public abstract class ThreadPool {
	private static final Stack waitStack = new Stack(32);
	final static Vector list = new Vector(64);
	protected final LinkedSet freeThreads = new LinkedSet();
	static protected boolean isShutDown = false;
	protected final Object threadGroup;
	protected String name = "";
	private final boolean isServerAppPool;
	
	private final static String DONE_RETURN = "doneReturn";
	
	/**
	 * 
	 * @param run 如果产生异常，则返回null
	 * @return
	 */
	public final Object runAndWait(final ReturnableRunnable run){
		return this.runAndWait(run, null);
	}
	
	/**
	 * 
	 * @param run 如果产生异常，则返回null
	 * @param threadToken
	 * @return
	 */
	public final Object runAndWait(final ReturnableRunnable run, final Object threadToken){
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
					if(isServerAppPool){
						ExceptionReporter.printStackTrace(e, null, null, ExceptionReporter.INVOKE_NORMAL);
					}else{
						ExceptionReporter.printStackTraceFromThreadPool(e);
					}
				}
				
				synchronized (finalWait) {
					finalWait[1] = DONE_RETURN;
					finalWait.notify();
				}
			}
		}, threadToken);
		
		synchronized (finalWait) {
			if(finalWait[1] == null){
				try{
					finalWait.wait();
				}catch (Exception e) {
//					ExceptionReporter.printStackTrace(e);
				}
			}
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
	
	public final void setName(String n){
		name = n;
	}
	
	/**
	 * 系统级、工程级、会话级ThreadPool都入list
	 * @param threadGroup
	 */
	public ThreadPool(Object threadGroup){
		this(threadGroup, false);
	}
	
	public ThreadPool(Object threadGroup, final boolean isServerAppPool){
		this.isServerAppPool = isServerAppPool;
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
	
	/**
	 * 有可能返回null，比如shutdown时。
	 * @param run
	 * @param token
	 * @return
	 */
	public final RecycleThread run(Runnable run, Object token){
		checkAccessPool(token);
		
		RecycleThread rt;
		
		if(isShutDown){
			return null;
		}
		
		synchronized (freeThreads) {
			while((rt = (RecycleThread)freeThreads.getFirst()) == null){
				try {
					freeThreads.wait();//RecycleThread {addTail() + notify()}
				} catch (InterruptedException e) {
//					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		
		if(isShutDown){
			return null;
		}
		
//			System.out.println("[" + name + "] -> RecycleThead : " + rt.toString());
		if(freeThreads.isEmpty()){
			final boolean[] isDoneNextOne = new boolean[1];
			final Runnable tmpRun = new Runnable() {
				public void run() {
					buildNextOne();
					synchronized (isDoneNextOne) {
						isDoneNextOne[0] = true;
						isDoneNextOne.notify();
					}
				}
			};
			
			rt.setRunnable(tmpRun);//请求另外一个线程生成一个新的，
			
			synchronized (isDoneNextOne) {
				if(isDoneNextOne[0] == false){
					try {
						isDoneNextOne.wait();
					} catch (InterruptedException e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
			}
			
			return this.run(run, token);//必须调用此，以重新找一个，而不能直接rt.setRunnable(run);
		}
		
		rt.setRunnable(run);
		return rt;
	}

	public final void buildNextOne() {
		RecycleThread rt;
		rt = buildRecycleThread(this);
		Thread t = buildThread(rt);
		rt.setThread(t);
//		RootBuilder.getInstance().setDaemonThread(t);
		rt.setRunnable(new Runnable() {
			public void run() {
			}
		});
		t.start();
	}
	
	public RecycleThread buildRecycleThread(ThreadPool pool){
		return new RecycleThread(pool);
	}
	
	public RecycleThread run(Runnable run){
		return run(run, null);
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
		CCoreUtil.checkAccess();
		
		synchronized (list) {
			isShutDown = true;
			final int size = list.size();
			for (int i = 0; i < size; i++) {
				((ThreadPool)list.elementAt(i)).exit();
			}
		}
	}
}