package hc.core.util;

public abstract class RootBuilder {
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
	public abstract String getAjaxForSimu(String url, boolean isTcp);

}
