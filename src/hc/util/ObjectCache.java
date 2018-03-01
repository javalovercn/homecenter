package hc.util;

import hc.core.util.Stack;

public class ObjectCache<T> {
	final private Stack free = new Stack();

	private int freeSize = 0;
	private final Class<T> typeArgumentClass;

	public ObjectCache(final Class<T> typeArgumentClass) {
		this.typeArgumentClass = typeArgumentClass;
	}

	public final T getFree() {
		synchronized (free) {
			if (freeSize == 0) {
				try {
					return typeArgumentClass.newInstance();
				} catch (final Throwable e) {
					return null;
				}
			} else {
				freeSize--;
				return (T) free.pop();
			}
		}
	}

	public final void cycle(final T dp) {
		synchronized (free) {
			free.push(dp);
			freeSize++;
		}
	}
}
