package hc.util;

import hc.core.util.StoreableHashMap;

import java.util.Map;

public class StoreableHashMapWithModifyFlag extends StoreableHashMap {
	private boolean isModify = false;

	public StoreableHashMapWithModifyFlag() {
		super();
	}

	public StoreableHashMapWithModifyFlag(final int size) {
		super(size);
	}

	public final void clearModifyTag() {
		isModify = false;
	}

	public final boolean isModified() {
		return isModify;
	}

	@Override
	public Object put(final Object key, final Object value) {
		final Object oldValue = super.put(key, value);
		if (value != null) {
			isModify = isModify || (value.equals(oldValue));
		} else {
			isModify = isModify || (oldValue == null);
		}
		return oldValue;
	}

	@Override
	public final void clear() {
		final int oldSize = size();

		if (oldSize != 0) {
			super.clear();
			isModify = true;
		}
	}

	@Override
	public Object remove(final Object key) {
		final Object result = super.remove(key);
		if (result != null) {
			isModify = true;
		}
		return result;
	}

	@Override
	public void putAll(final Map t) {
		isModify = true;
		super.putAll(t);
	}
}
