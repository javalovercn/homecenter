package hc.core.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * 先入后出 应用须确保并发安全
 */
public class Stack {
	short elementCount;
	Object[] elementData;

	public final Enumeration elements() {
		return new Enumeration() {
			int count = 0;

			public boolean hasMoreElements() {
				return count < elementCount;
			}

			public Object nextElement() {
				synchronized (this) {
					if (count < elementCount) {
						return elementData[count++];
					}
				}
				throw new NoSuchElementException("Stack Enumeration");
			}
		};
	}

	public Stack() {
		elementData = new Object[10];
	}

	public Stack(int initSize) {
		elementData = new Object[initSize];
	}

	/**
	 * 没有找到，返回-1
	 * 
	 * @param obj
	 * @return
	 */
	public final int search(Object obj) {
		for (int i = 0; i < elementCount; i++) {
			if (elementData[i].equals(obj)) {
				return i;
			}
		}
		return -1;
	}

	public final Object elementAt(int idx) {
		if (idx > elementCount) {
			return null;
		} else {
			return elementData[idx];
		}
	}

	public final void removeAt(int idx) {
		if (idx > elementCount) {
		} else {
			for (int i = idx + 1; i < elementCount; i++) {
				elementData[i - 1] = elementData[i];
			}
			--elementCount;
		}

	}

	public final void push(Object item) {
		if (elementCount == elementData.length) {
			Object[] newElementData = new Object[elementCount * 2];
			System.arraycopy(elementData, 0, newElementData, 0, elementCount);
			elementData = newElementData;
		}
		elementData[elementCount++] = item;
	}

	public final void removeAllElements() {
		for (int i = 0; i < elementCount; i++) {
			elementData[i] = null;
		}
		elementCount = 0;
	}

	public Object pop() {
		if (elementCount == 0) {
			return null;
		} else {
			return elementData[--elementCount];
		}
	}

	public final int size() {
		return elementCount;
	}

	public final boolean isEmpty() {
		return elementCount == 0;
	}
}
