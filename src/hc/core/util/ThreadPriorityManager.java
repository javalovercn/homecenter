package hc.core.util;

public class ThreadPriorityManager {
	public static final int KEEP_ALIVE_PRIORITY = Thread.MAX_PRIORITY;
	
	public static final int FILE_SAVE_PRIORITY = Thread.MAX_PRIORITY;
	
	public static final int SEQUENCE_SCRIPT_PRIORITY = Thread.MAX_PRIORITY;
	
	public static final int DATA_TRANS_PRIORITY = Thread.MAX_PRIORITY - 1;
	
	public static final int HCTIMER_PRIORITY = DATA_TRANS_PRIORITY - 1;

	public static final int SERVER_THREADPOOL_PRIORITY = Thread.NORM_PRIORITY;
	
	public static final int HC_LIMIT_THREADGROUP_PRIORITY = 2;
	
	public static final int PROJ_CONTEXT_THREADPOOL_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int CAPTURER_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int WATCH_ON_LINE_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int LOWEST_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int UI_WAIT_MS = 100;
	
	public static final int UI_WAIT_OTHER_THREAD_EXEC_MS = 20;
	
	public static final int UI_CODE_HELPER_DELAY_MS = 2000;
	
	public static final int UI_DELAY_MOMENT = 500;
	
	public static final int UI_FLUSH_GRAPHICS = 10;
	
	public static final int UI_WAIT_FOR_EVENTQUEUE = 5;
}
