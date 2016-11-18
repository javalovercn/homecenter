package hc.core.util;

import hc.core.CoreSession;

public interface IHCURLAction {
	
	/**
	 * 方法内不处理HCURL的回收
	 * @param coreSS 
	 * @param url
	 * @return
	 */
	public boolean doBiz(CoreSession coreSS, HCURL url);
}
