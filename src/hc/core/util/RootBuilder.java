package hc.core.util;

public abstract class RootBuilder {
	public static final short ROOT_BIZ_AJAX_X509_PATH = 1;
	public static final short ROOT_BIZ_IS_SIMU = 2;
	public static final short ROOT_BIZ_CHECK_STACK_TRACE = 3;
	
	private static RootBuilder instance;
	
	public static void setInstance(RootBuilder builder){
		CCoreUtil.checkAccess();
		instance = builder;
	}
	
	public static final RootBuilder getInstance(){
		return instance;
	}
	
	public abstract ExceptionJSONBuilder getExceptionJSONBuilder();
	
	public abstract String getAjax(String url);
	public abstract String getAjaxForSimu(String url);

	public abstract Object doBiz(final int rootBizNo, final Object para);
}
