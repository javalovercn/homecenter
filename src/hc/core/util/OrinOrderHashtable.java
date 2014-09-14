package hc.core.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class OrinOrderHashtable extends Hashtable {
	private Vector list = new Vector();
    public Object put(Object key, Object value){
        final Object out = super.put(key, value);
        if(list.contains(key) == false){
        	list.addElement(key);
        }
        return out;
    }
    
    public void clear()
    {
        super.clear();
        list.removeAllElements();
    }
    public Object remove(Object key)
    {
        final Object out = super.remove(key);
        list.removeElement(key);
        return out;
    }
    public Enumeration keys(){
        return list.elements();
    }
}
