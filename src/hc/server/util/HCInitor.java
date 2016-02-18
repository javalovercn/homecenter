package hc.server.util;

import hc.util.ResourceUtil;

public class HCInitor {
	public static void init(){
		//初始时，要删除上次可能因停电产生的需要待删除的资源。
		ResourceUtil.notifyCancel();
	}
}
