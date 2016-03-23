package hc.server.html5.syn;

import hc.core.util.ExceptionReporter;

import java.util.HashMap;

public class DiffManager {
	private static final HashMap<Class, JComponentDiff> map = new HashMap<Class, JComponentDiff>();
	
	public static final synchronized JComponentDiff getDiff(final Class c){
		JComponentDiff obj = map.get(c);
		if(obj == null){
			try {
				obj = (JComponentDiff)c.newInstance();
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
			map.put(c, obj);
		}
		
		return obj;
	}
}
