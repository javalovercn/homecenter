package hc.core.util;

public interface IHCURLAction {
	
	/**
	 * 方法内不处理HCURL的回收
	 * @param url
	 * @return
	 */
	public boolean doBiz(HCURL url);
}
