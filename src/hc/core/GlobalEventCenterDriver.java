package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.ThreadPriorityManager;

public class GlobalEventCenterDriver extends HCConditionWatcher {
	GlobalEventCenterDriver(){
		super("GlobalEventCenterDriver", ThreadPriorityManager.GECD_THREADGROUP_PRIORITY);
	}
	
	static GlobalEventCenterDriver gecd = new GlobalEventCenterDriver();
	
	public static GlobalEventCenterDriver getGECD(){
		CCoreUtil.checkAccess();
		return gecd;
	}
}
