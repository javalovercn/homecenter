package hc.util;

import hc.core.util.CCoreUtil;

import java.util.HashMap;

public class ThreadConfig {
	public static final Integer AUTO_PUSH_EXCEPTION = 1;
	
	public static final Integer SYS_CONFIG = 100000;//该值以下，不作系统级权限要求
	
	private static final HashMap<Long, HashMap<Integer, Object>> threadTable = new HashMap<Long, HashMap<Integer,Object>>(16);
	
	public static synchronized void putValue(final Integer tag, final Object value){
		if(tag >= SYS_CONFIG){
			CCoreUtil.checkAccess();
		}
		
		final long currID = Thread.currentThread().getId();
		HashMap<Integer, Object> table = threadTable.get(currID);
		if(table == null){
			table = new HashMap<Integer, Object>(8);
			threadTable.put(currID, table);
		}
		
		table.put(tag, value);
	}
	
	public static synchronized Object getValue(final Integer tag){
		if(tag >= SYS_CONFIG){
			CCoreUtil.checkAccess();
		}
		
		final long currID = Thread.currentThread().getId();
		final HashMap<Integer, Object> table = threadTable.get(currID);
		
		if(table == null){
			return null;
		}else{
			return table.get(tag);
		}
	}
	
	public static void putValue(final Integer tag, final boolean value){
		putValue(tag, value?Boolean.TRUE:Boolean.FALSE);
	}
	
	public static boolean isTrue(final Integer tag, final boolean valueWhenNull){
		final Object obj = getValue(tag);
		if(obj == null){
			return valueWhenNull;
		}
		
		if((obj instanceof Boolean) == false){
			return false;
		}
		
		return ((Boolean)obj);
	}
}
