package hc.util;

import hc.core.util.StoreableHashMap;

import java.util.Map;

public class StoreableHashMapWithModifyFlag extends StoreableHashMap {
	private boolean isModify = false;
	
	public StoreableHashMapWithModifyFlag(){
		super();
	}
	
	public StoreableHashMapWithModifyFlag(final int size){
		super(size);
	}
	
	public final void clearModifyTag(){
		isModify = false;
	}
	
	public final boolean isModified(){
		return isModify;
	}
	
	@Override
	public final Object put(final Object key, final Object value){
		final Object oldValue = super.put(key, value);
		if(value != null){
			isModify = isModify || (value.equals(oldValue));
		}else{
			isModify = isModify || (oldValue == null);
		}
		return oldValue;
	}
	
	@Override
	public final void clear() {
		final int oldSize = size();
		
		if(oldSize != 0){
			super.clear();
			isModify = true;
		}
	}
	
	@Override
	public final Object remove(final Object key) {
		final boolean isExists = super.contains(key);
		if(isExists){
			isModify = true;
			return super.remove(key);
		}else{
			return null;
		}
	}
	
	@Override
	public final void putAll(final Map t) {
		isModify = true;
		super.putAll(t);
	}
}
