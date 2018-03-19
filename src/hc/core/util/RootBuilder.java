package hc.core.util;

public abstract class RootBuilder {
	public static final short ROOT_BIZ_AJAX_X509_PATH = 1;
	public static final short ROOT_BIZ_IS_SIMU = 2;
	public static final short ROOT_BIZ_CHECK_STACK_TRACE = 3;
	public static final short ROOT_RELEASE_EXT_J2SE = 4;
	public static final short ROOT_GET_CLASS_FROM_3RD_AND_SERV_LIBS = 5;
	public static final short ROOT_BUILD_NEW_CONNECTION = 6;
	public static final short ROOT_CHECK_CHECKPERMISSION = 7;
	public static final short ROOT_GET_RESOURCE = 8;
	public static final short ROOT_SET_PUBLISH_LOCATION_INTER_MS = 9;
	public static final short ROOT_SET_THREAD_PARA = 10;// 引用parameter :
														// KeyValue [Integer,
														// Object]
	public static final short ROOT_IS_IN_LOGIN_PROCESS = 11;
	public static final short ROOT_PRINT_STACK_WITH_FULL_CAUSE = 12;
	public static final short ROOT_GET_LAST_ROOT_CFG = 13;// 没有，则返回null
	public static final short ROOT_SET_LAST_ROOT_CFG = 14;
	public static final short ROOT_GET_LAST_ALIVE_IP_INFO = 15;
	public static final short ROOT_SET_LAST_ALIVE_IP_INFO = 16;
	public static final short ROOT_QUERY_DIRECT_SERVER_IP = 17;
	public static final short ROOT_SPEAK_VOICE = 18;
	public static final short ROOT_GET_MB_PROP = 19;
	public static final short ROOT_THROW_CAUSE_ERROR = 20;
	public static final short ROOT_GET_CAUSE_ERROR = 21;
	public static final short ROOT_IS_CURR_THREAD_IN_SESSION_OR_PROJ_POOL = 22;
	public static final short ROOT_START_NEW_IDEL_SESSION = 23;

	private static RootBuilder instance;

	public static void setInstance(RootBuilder builder) {
		if (instance != null) {
			instance.doBiz(RootBuilder.ROOT_BIZ_CHECK_STACK_TRACE, null);
		}

		instance = builder;
	}

	public static final RootBuilder getInstance() {
		return instance;
	}

	public abstract ExceptionJSONBuilder getExceptionJSONBuilder();

	public abstract void reportException(final ExceptionJSON json);

	public abstract String getAjax(String url);

	public abstract void setDaemonThread(final Thread thread);

	public abstract Object doBiz(final int rootBizNo, final Object para);

	/**
	 * 适合服务器和客户端
	 * 
	 * @return
	 */
	public static final boolean isSimu() {
		final Object out = instance.doBiz(RootBuilder.ROOT_BIZ_IS_SIMU, null);
		if (out != null && out instanceof Boolean) {
			return ((Boolean) out).booleanValue();
		}
		return false;
	}
}
