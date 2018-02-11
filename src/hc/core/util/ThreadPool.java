package hc.core.util;

import java.util.Vector;

import hc.core.IConstant;

public abstract class ThreadPool {
	private static final Stack waitStack = new Stack(32);
	final static Vector list = new Vector(64);
	protected final LinkedSet freeThreads = new LinkedSet();
	static protected boolean isShutDown = false;
	protected final Object threadGroup;
	protected String name = "";
	private final boolean isServerLevelPool;
	public final int poolType;
	
	public final static int TYPE_DEFAULT = 0;
	public final static int TYPE_PROJECT = 1;
	public final static int TYPE_SESSION = 2;
	
	public final static Vector getPoolVector() {
		RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_CHECK_STACK_TRACE, null);
		return list;
	}
	
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
		return runAndWait(run, threadToken, false);
	}
	
	public final Object runAndWait(final ReturnableRunnable run, final Object threadToken, final boolean isThrowError){
		ThreadPoolResult wait;
		synchronized (waitStack) {
			wait = (ThreadPoolResult)waitStack.pop();
		}
		if(wait == null){
			wait = new ThreadPoolResult();
		}
		
		final ThreadPoolResult finalWait = wait;
		
		this.run(new Runnable() {
			public void run() {
				try{
					finalWait.result = run.run();
				}catch (Throwable e) {
					finalWait.nestThrowable = e;
					if(isServerLevelPool){
						ExceptionReporter.printStackTrace(e, null, null, ExceptionReporter.INVOKE_NORMAL);
					}else{
						ExceptionReporter.printStackTraceFromThreadPool(e);
					}
				}
				
				synchronized (finalWait) {
					finalWait.isDone = true;
					finalWait.notify();
				}
			}
		}, threadToken);
		
		synchronized (finalWait) {
			if(finalWait.isDone == false){
				try{
					finalWait.wait();
				}catch (Exception e) {
//					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		
		Object out = finalWait.result;
		final Throwable nestThrowable = finalWait.nestThrowable;
		
		finalWait.result = null;
		finalWait.nestThrowable = null;
		finalWait.isDone = false;
		
		synchronized (waitStack) {
			waitStack.push(finalWait);
		}
		
		if(nestThrowable != null) {
			if(isThrowError  || ExceptionReporter.isCauseByLineOffSession(nestThrowable)) {
				final RootBuilder instance = RootBuilder.getInstance();
				final Boolean throwForProjAlso = isThrowError?IConstant.BOOL_TRUE:IConstant.BOOL_FALSE;
				if(((Boolean)instance.doBiz(RootBuilder.ROOT_IS_CURR_THREAD_IN_SESSION_OR_PROJ_POOL, throwForProjAlso)).booleanValue()) {
					instance.doBiz(RootBuilder.ROOT_THROW_CAUSE_ERROR, nestThrowable);
				}
			}
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
		this(threadGroup, isServerAppPool, TYPE_DEFAULT);
	}
	
	public ThreadPool(Object threadGroup, final boolean isServerLevelPool, final int poolType){
		this.isServerLevelPool = isServerLevelPool;
		this.threadGroup = threadGroup;
		this.poolType = poolType;
		
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