package hc.util;

import java.util.Map;

/**
 * case insensitive
 */
public class I18NStoreableHashMapWithModifyFlag extends StoreableHashMapWithModifyFlag {
	public I18NStoreableHashMapWithModifyFlag(){
		super();
	}
	
	public I18NStoreableHashMapWithModifyFlag(final int size){
		super(size);
	}
	
	@Override
	public final Object put(final Object key, final Object value){
		return super.put(((String)key).toLowerCase(), value);
	}
	
	@Override
	public final Object get(final Object key) {
		return super.get(((String)key).toLowerCase());
	}
	
	@Override
	public final void putAll(final Map map) {
		for(final Object key : map.keySet())   {
			put(((String)key).toLowerCase(), map.get(key));
		}
    }
	
	@Override
	public final Object remove(final Object key) {
		return super.remove(((String)key).toLowerCase());
	}
	
	@Override
	public final boolean containsKey(final Object key) {
		return super.containsKey(((String)key).toLowerCase());
	}
}
