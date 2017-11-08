package hc.core.util;

public class ThreadPriorityManager {
	public static final int DIRECT_SERVER_RECEIVE_PRIORITY = Thread.MAX_PRIORITY;//以减少直接连接时，
	public static final int KEEP_ALIVE_PRIORITY = Thread.MAX_PRIORITY;
	
	public static final int FILE_SAVE_PRIORITY = Thread.MAX_PRIORITY;
	
	public static final int SEQUENCE_SCRIPT_PRIORITY = Thread.MAX_PRIORITY;
	
	public static final int DATA_TRANS_PRIORITY = Thread.MAX_PRIORITY - 1;
	
	public static final int HCTIMER_PRIORITY = DATA_TRANS_PRIORITY - 1;

	public static final int SERVER_THREADPOOL_PRIORITY = Thread.NORM_PRIORITY;
	
	public static final int HC_LIMIT_THREADGROUP_PRIORITY = DATA_TRANS_PRIORITY - 1;//高1：快速接收签收，以减少重发。以增加用户响应速度
	//与HC_LIMIT_THREADGROUP_PRIORITY的相同考虑：
	//1. 同一会话的多个事件，可以批处理。
	//2. 不同会话的事件，平行处理，以减少并发
	public static final int GECD_THREADGROUP_PRIORITY = HC_LIMIT_THREADGROUP_PRIORITY;
	public static final int PROJ_CONTEXT_THREADPOOL_PRIORITY = HC_LIMIT_THREADGROUP_PRIORITY;//应高于AI_BACK
	
	public static final int AI_BACKGROUND = Thread.MIN_PRIORITY;//应低于PROJ_CONTEXT_THREADPOOL_PRIORITY
	
	public static final int CAPTURER_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int WATCH_ON_LINE_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int LOWEST_PRIORITY = Thread.MIN_PRIORITY;
	
	public static final int UI_WAIT_MS = 100;

	public static final int RELAY_FIRST_MS = 100;
	
	public static final int UI_WAIT_OTHER_THREAD_EXEC_MS = 20;
	
	public static final int UI_CODE_HELPER_DELAY_MS = 2000;
	
	public static final int NET_MAX_RENEWAL_CONN_MS = 3000;
	
	public static final int UI_DELAY_MOMENT = 500;
	
	public static final int NET_FLUSH_DELAY = 500;
	
	public static final int UI_FLUSH_GRAPHICS = 10;
	
	public static final int UI_WAIT_FOR_EVENTQUEUE = 5;
	
	public static final int SEQUENCE_TASK_MAX_WAIT_MS = 1000 * 15;
	
	public static final int REBUILD_SWAP_SOCK_MIN_MS = 10000;//由原5000改为，主要考虑是【未来】relay下send饱和，导致不停重连。
}
