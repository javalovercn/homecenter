package hc.core.util;

import java.util.Vector;

/**
 * 不同于hashtable是乱序，本对象是按照添加次序排序。
 */
public class VectorMap extends Vector{
	public VectorMap(int size){
		super(size);
	}
	
	public final Object get(final Object key, final Object defaultValue){
		final int size = super.size();
		
		for (int i = 0; i < size; i++) {
			final KeyValue keyValue = (KeyValue)super.elementAt(i);
			if(keyValue.key.equals(key)){
				return keyValue.value;
			}
		}
		
		return defaultValue;
	}
	
	public final void set(final Object key, final Object value){
		final int size = super.size();
		
		for (int i = 0; i < size; i++) {
			final KeyValue keyValue = (KeyValue)super.elementAt(i);
			if(keyValue.key.equals(key)){
				keyValue.value = value;
				return;
			}
		}
		
		final KeyValue keyValue = new KeyValue();
		keyValue.key = key;
		keyValue.value = value;
		super.addElement(keyValue);
	}
}
