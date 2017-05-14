package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

import java.util.Vector;

public abstract class HCTimer {
	//默认为１０分
	long interval;
	private Object bizObj;
	final String name;
	public static final long ONE_HOUR = 1000 * 60 * 60;
	public static final long ONE_DAY = 86400000;
	long nextExecMS;
	final boolean isNewThread;
	final int newThreadPrority;
	
	public static void doNothing(){
	}
	
	/**
	 * 默认为１０分
	 */
	public HCTimer(final String name, final boolean enable) {
		this(name, 1000 * 60 * 10, enable, false, ThreadPriorityManager.LOWEST_PRIORITY);
	}
	
	public final String getName(){
		return this.name;
	}

	public HCTimer(final String name, final long ms, final boolean enable) {
		this(name, ms, enable, false, ThreadPriorityManager.LOWEST_PRIORITY);
	}
	
	/**
	 * 
	 * @param name
	 * @param ms
	 * @param enable
	 * @param isNewThread true:表示另起独立线程来驱动本逻辑；false:共享线程来驱动本逻辑
	 * @param newThreadPrority
	 */
	public HCTimer(final String name, final long ms, final boolean enable, final boolean isNewThread, final int newThreadPrority) {
		interval = ms;
		isEnable = enable;
		this.name = name;
		this.isNewThread = isNewThread;
		this.newThreadPrority = newThreadPrority;
		init();
	}
	
	public final void setBizObject(final Object obj){
		bizObj = obj;
	}
	
	public final Object getBizObject(){
		return bizObj;
	}

	private final void init() {
		CCoreUtil.checkAccess();
		
		if(L.isInWorkshop){
			LogManager.log("create HCTimer [" + name + "].");
		}
		
		if(isNewThread == false){
			notifyToGenerailManager(this);
		}else{
			notifyToNewThread(this);
		}
	}
	
	protected boolean isEnable = true;
	
	public final boolean isEnable(){
		return isEnable;
	}

	public void setEnable(final boolean enable){
		if(this.isEnable != enable){
			if(L.isInWorkshop){
				LogManager.log("HCTimer [" + name + "] setEnable : " + enable);
			}
			this.isEnable = enable;
			if(enable){
				final long nowMS = System.currentTimeMillis();
				if(this.nextExecMS < nowMS){
					this.nextExecMS = getNextMS(nowMS, interval);
				}
				wakeUp();
			}
		}
	}

	private final void wakeUp() {
		if(isNewThread){
			synchronized (this) {
				this.notify();
			}
		}else{
			if(isMinInternalWait){
//				LogManager.log("skip notify for min internal ms wait");
				return;
			}
			synchronized (LOCK) {
				LOCK.notify();
			}
		}
	}
	
	/**
	 * 计时器清零计算
	 */
	public final void resetTimerCount(){
		resetToMS(interval);
	}

	private final void resetToMS(final long ms) {
		nextExecMS = getNextMS(0, ms);
	}

	/**
	 * 将下次执行时间设为最小，以获得最快执行，
	 * 本方法内不调用doBiz
	 */
	public final void doNowAsynchronous(){
		resetToMS(0);
		wakeUp();
	}
	
	/**
	 * 设定替换旧成员的时间间隔，单位：MS
	 * @param secondMS
	 */
	public final void setIntervalMS(final long secondMS) {
		interval = secondMS;
		nextExecMS = getNextMS(0, interval);
		
		if(isEnable){
			wakeUp();
		}
	}

	public final long getIntervalMS() {
		return interval;
	}


	public abstract void doBiz();

	//数据
	final private static Object LOCK = new Object();
	private static int CURR_SIZE = 0;
	private static final int HCTIME_MAX_SIZE = (IConstant.serverSide?1000:100);
	final private static HCTimer[] HC_TIMERS = new HCTimer[HCTIME_MAX_SIZE];
	private static boolean isShutDown = false;
	public static final long TEMP_MAX = 99999999999999999L;
	public static final int HC_INTERNAL_MS = 100;
	public static final int HC_HALF_INTERNAL_MS = HC_INTERNAL_MS / 2;
	
//	private static boolean isPause = false;

	private static boolean isMinInternalWait = false;
	
	//线程控制
	private final static Thread shareThread = new Thread() {//"HCTimer Thread"
		final NestAction nestAction = (NestAction)ConfigManager.get(ConfigManager.BUILD_NESTACTION, null);//EventCenter.nestAction;
		
		public void run() {
			final long maxMS = TEMP_MAX;
			long min_next_exec_mill_second;
			while ((!isShutDown)) {
				min_next_exec_mill_second = maxMS;
				
//				LogManager.log("HCTimer Main sleep:" + min_wait_mill_second);
				final long nowMS = System.currentTimeMillis();

				synchronized(LOCK){
					for (int i = 0; i < CURR_SIZE; i++) {
						final HCTimer hcTimer = HC_TIMERS[i];
						if(hcTimer.isEnable){
							final long next = hcTimer.nextExecMS;
							if(min_next_exec_mill_second > next){
								min_next_exec_mill_second = next;
							}
						}
	                }
					final long sleepMS = min_next_exec_mill_second - nowMS;
					if(sleepMS > 0){
						final boolean isMinMS = sleepMS <= HC_INTERNAL_MS;
						if(isMinMS){
							isMinInternalWait = true;
						}
						try {
//							LogManager.log("HCTimer wait ms : " + sleepMS);
							LOCK.wait(sleepMS);
//							LogManager.log("HCTimer break wait.");
						} catch (InterruptedException e) {
						}
						if(isMinMS){
							isMinInternalWait = false;
						}
						continue;
					}				
				}
				
            	int i = 0;
            	HCTimer timer = null;
            	final long min_next_exec_with_half = min_next_exec_mill_second + HC_HALF_INTERNAL_MS;//增加half量，以执行相近
            	while(true){
                	boolean isFound = false;
                	
					synchronized(LOCK){
						for (; i < CURR_SIZE; i++) {
							timer = HC_TIMERS[i];
							if(timer.isEnable){
								if (timer.nextExecMS <= min_next_exec_with_half) {
									isFound = true;
									break;
								}
							}
						}
					}
					
					if(isFound){
						//注意：
						//调整nextExecMS要优先执行，如果doBize可能需要调整下次时间，由doBiz内部处理，如setInterval()
						//本行只负责通用情形
						timer.nextExecMS += timer.interval;		
						
						//严重滞时的情形，补正为下一段正确时间
						if(timer.nextExecMS < nowMS){
							timer.nextExecMS = getNextMS(nowMS, timer.interval);
						}
//							long curr = System.currentTimeMillis();
		                try{
							if(nestAction == null){
								timer.doBiz();
							}else{
								nestAction.action(NestAction.HCTIMER, timer);
							}
//							LogManager.log("HCTimer[" + timer.name + "] exe cost: " + (System.currentTimeMillis() - curr));
		                }catch (final Throwable e) {
		                	ExceptionReporter.printStackTrace(e);
						}
					}else{
						break;
					}
            	}//while find executable timer
			}//while isShutDown
			LogManager.log("HCTimer shutdown");
		}
	};

	public static void shutDown() {
		synchronized (LOCK) {
			isShutDown = true;
			LOCK.notify();
		}
		
		synchronized (newThreadTimer) {
			final int size = newThreadTimer.size();
			for (int i = 0; i < size; i++) {
				final ThreadTimer tt = (ThreadTimer)newThreadTimer.elementAt(i);
				tt.notifyShutdown();
			}
		}
	}

	/**
	 * 删除时，会将enable置于false。
	 * @param t
	 */
	public static void remove(final HCTimer t){
		if(t == null){
			return ;
		}
		
		t.setEnable(false);
		boolean isFound = false;
		
		if(t.isNewThread == false){
			//共享线程
			synchronized(LOCK){
				for (int i = 0; i < CURR_SIZE; i++) {
					if(isFound == false && HC_TIMERS[i] == t){
						isFound = true;
					}
					if(isFound){
						if(i < (CURR_SIZE - 1)){
							HC_TIMERS[i] = HC_TIMERS[i+1];
						}					
					}
				}
	
				if(isFound){
					--CURR_SIZE;
				}
			}
		}else{
			//自带线程
			synchronized (newThreadTimer) {
				final int size = newThreadTimer.size();
				for (int i = 0; i < size; i++) {
					final ThreadTimer threadTimer = (ThreadTimer)newThreadTimer.elementAt(i);
					if(threadTimer.timer == t){
						isFound = true;
						threadTimer.notifyShutdown();
						newThreadTimer.removeElementAt(i);
						break;
					}
				}
			}
		}
		
		if(isFound){
			if(L.isInWorkshop){
				LogManager.log("remove HCTimer [" + t.name + "]");
			}
		}
	}
	
	private static final Vector newThreadTimer = new Vector();
	protected static void notifyToNewThread(final HCTimer byer) {
		final ThreadTimer threadTimer = new ThreadTimer(byer);
		synchronized (newThreadTimer) {
			newThreadTimer.addElement(threadTimer);
		}
	}
	
	protected static void notifyToGenerailManager(final HCTimer byer) {
		byer.nextExecMS = getNextMS(0, byer.interval);
		synchronized (LOCK) {
			if (CURR_SIZE == HCTIME_MAX_SIZE) {
				LogManager.err("HCTimer overflow:" + String.valueOf(CURR_SIZE));
				return;
			}

			HC_TIMERS[CURR_SIZE++] = byer;
			LOCK.notify();
		}
	}

	public static long getNextMS(final long nowMS, final long interv) {
		final long currentTimeMillis = (nowMS==0?System.currentTimeMillis():nowMS);
		return currentTimeMillis - (currentTimeMillis % HC_INTERNAL_MS) + interv;
	}

	static {
		isShutDown = false;
		//没有 必要 定为最高级
		shareThread.setPriority(ThreadPriorityManager.HCTIMER_PRIORITY);
		shareThread.start();
	}

}
