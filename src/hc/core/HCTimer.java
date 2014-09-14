package hc.core;

import java.util.Vector;

import hc.core.util.LogManager;

public abstract class HCTimer {
	//默认为１０分
	int interval;
	private Object bizObj;
	String name;
	public static final int ONE_HOUR = 1000 * 60 * 60;
	public static final int ONE_DAY = 86400000;
	long nextExecMS;
	final boolean isNewThread;
	
	/**
	 * 默认为１０分
	 */
	public HCTimer(String name, boolean enable) {
		this(name, 1000 * 60 * 10, enable, false);
	}
	
	public String getName(){
		return this.name;
	}

	public HCTimer(String name, int ms, boolean enable) {
		this(name, ms, enable, false);
	}
	
	/**
	 * 
	 * @param name
	 * @param ms
	 * @param enable
	 * @param isNewThread true:表示另起线程来驱动本逻辑；false:共享线程来驱动本逻辑
	 */
	public HCTimer(String name, int ms, boolean enable, boolean isNewThread) {
		interval = ms;
		isEnable = enable;
		this.name = name;
		this.isNewThread = isNewThread;
		init();
	}
	
	public void setBizObject(Object obj){
		bizObj = obj;
	}
	
	public Object getBizObject(){
		return bizObj;
	}

	private void init() {
		if(isNewThread == false){
			notifyToGenerailManager(this);
		}else{
			notifyToNewThread(this);
		}
	}
	
	boolean isEnable = true;
	
	public boolean isEnable(){
		return isEnable;
	}

	public void setEnable(final boolean enable){
		if(this.isEnable != enable){
			this.isEnable = enable;
			if(enable){
				if(this.nextExecMS < System.currentTimeMillis()){
					this.nextExecMS = getNextMS(interval);
				}
			}
		}
	}
	
	/**
	 * 计时器清零计算
	 */
	public void resetTimerCount(){
		resetToMS(interval);
	}

	private void resetToMS(final int ms) {
		nextExecMS = getNextMS(ms);
	}

	/**
	 * 将下次执行时间设为最小，以获得最快执行，
	 * 本方法内不调用doBiz
	 */
	public void doNowAsynchronous(){
		resetToMS(0);
	}
	
	/**
	 * 设定替换旧成员的时间间隔，单位：MS
	 * @param secondMS
	 */
	public void setIntervalMS(int secondMS) {
		interval = secondMS;
		nextExecMS = getNextMS(interval);
	}

	public int getIntervalMS() {
		return interval;
	}


	public abstract void doBiz();

	//数据
	final private static Boolean LOCK = new Boolean(false);
	private static int CURR_SIZE = 0;
	private static final int HCTIME_MAX_SIZE = (IConstant.serverSide?3000:100);
	final private static HCTimer[] HC_TIMERS = new HCTimer[HCTIME_MAX_SIZE];;
	private static boolean isShutDown = false;
	public static final long TEMP_MAX = 99999999999999999L;
	public static final int HC_INTERNAL_MS = 100;
//	private static boolean isPause = false;

	//线程控制
	private static Thread thread = new Thread() {//"HCTimer Thread"
		public void run() {
			long min_wait_mill_second = TEMP_MAX;
			while ((!isShutDown)) {
				min_wait_mill_second = TEMP_MAX;
				
				synchronized(LOCK){
					for (int i = 0; i < CURR_SIZE; i++) {
						final HCTimer hcTimer = HC_TIMERS[i];
						if(hcTimer.isEnable){
							final long left = hcTimer.nextExecMS;
							if(min_wait_mill_second > left){
								min_wait_mill_second = left;
							}
						}
	                }
				}
				
//				L.V = L.O ? false : LogManager.log("HCTimer Main sleep:" + min_wait_mill_second);
				final long nowExecMS = System.currentTimeMillis();
				
				long sleepMS = min_wait_mill_second - nowExecMS;
				if(sleepMS > 0){
					boolean isContinue = false;
					if(min_wait_mill_second > (nowExecMS + HC_INTERNAL_MS)){
						sleepMS = HC_INTERNAL_MS;
						isContinue = true;
					}

					try {
						Thread.sleep(sleepMS);
	                } catch (Exception e) {
	                }
					if(isContinue){
						continue;
					}
				}
				
                try{
					synchronized(LOCK){
						for (int i = 0; i < CURR_SIZE; i++) {
							final HCTimer timer = HC_TIMERS[i];
							if(timer.isEnable){
								if (timer.nextExecMS <= min_wait_mill_second) {
									//注意：
									//调整nextExecMS要优先执行，如果doBize可能需要调整下次时间，由doBiz内部处理，如setInterval()
									//本行只负责通用情形
									timer.nextExecMS += timer.interval;		
									
									//严重滞时的情形，补正为下一段正确时间
									if(timer.nextExecMS < nowExecMS){
										timer.nextExecMS = getNextMS(timer.interval);
									}
//									long curr = System.currentTimeMillis();
									timer.doBiz();
//									hc.core.L.V=hc.core.L.O?false:LogManager.log("HCTimer[" + timer.name + "] exe cost: " + (System.currentTimeMillis() - curr));
								}
							}
						}
					}
                }catch (Exception e) {
                	e.printStackTrace();
				}
			}
			hc.core.L.V=hc.core.L.O?false:LogManager.log("HCTimer shutdown");
		}
	};

	public static void shutDown() {
		isShutDown = true;
		
		final int size = newThreadTimer.size();
		for (int i = 0; i < size; i++) {
			ThreadTimer tt = (ThreadTimer)newThreadTimer.elementAt(i);
			tt.notifyShutdown();
		}
		
		try{
			ContextManager.getContextInstance().interrupt(thread);
		}catch (Exception e) {
			//可能NullPointerException
		}
		//非j2me应用环境
//		if(IConstant.getInstance().getInt(IConstant.IS_J2ME) == 0){
//			thread.interrupt();
//		}
	}

	public static void remove(HCTimer t){
		if(t == null){
			return ;
		}
		
		t.setEnable(false);
		boolean isFound = false;
		
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
	}
	
	private static final Vector newThreadTimer = new Vector();
	protected static void notifyToNewThread(HCTimer byer) {
		newThreadTimer.addElement(new ThreadTimer(byer));
	}
	
	protected static void notifyToGenerailManager(HCTimer byer) {
		byer.nextExecMS = getNextMS(byer.interval);
		synchronized (LOCK) {
			if (CURR_SIZE == HCTIME_MAX_SIZE) {
				LogManager.err("HCTimer overflow:" + String.valueOf(CURR_SIZE));
				return;
			}

			HC_TIMERS[CURR_SIZE++] = byer;

			LOCK.notify();
		}
	}

	public static long getNextMS(int interv) {
		final long currentTimeMillis = System.currentTimeMillis();
		return currentTimeMillis - (currentTimeMillis % HC_INTERNAL_MS) + HC_INTERNAL_MS
				+ interv;
	}

	static {
		isShutDown = false;
		//没有 必要 定为最高级
		//thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

}
