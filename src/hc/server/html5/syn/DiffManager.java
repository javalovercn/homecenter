package hc.server.html5.syn;

import java.util.HashMap;

public class DiffManager {
	private static final HashMap<Class, JComponentDiff> map = new HashMap<Class, JComponentDiff>();
	
	public static final synchronized JComponentDiff getDiff(Class c){
		JComponentDiff obj = map.get(c);
		if(obj == null){
			try {
				obj = (JComponentDiff)c.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
			map.put(c, obj);
		}
		
		return obj;
	}
}
