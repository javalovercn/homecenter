package hc.server.html5.syn;

import java.util.HashMap;

public class HcCodeMap {
	final HashMap<Integer, Object> hashCodeMap;

	int nextIdx;
	int[] hcCodeArray;
	Object[] objectArray;

	public HcCodeMap(HashMap<Integer, Object> map) {
		this.hashCodeMap = map;
		hcCodeArray = new int[5];
		objectArray = new Object[hcCodeArray.length];
	}

	/**
	 * 没找到返回0
	 * 
	 * @param obj
	 * @return
	 */
	public final int searchObject(final Object obj) {
		for (int i = 0; i < nextIdx; i++) {
			if (objectArray[i] == obj) {
				return hcCodeArray[i];
			}
		}

		return 0;
	}

	public final int buildHcCode(final Object obj, int hashCode) {
		for (int i = 0; i < nextIdx; i++) {
			if (objectArray[i] == obj) {
				return hcCodeArray[i];
			}
		}

		if (nextIdx == hcCodeArray.length - 1) {
			int[] newhcCodeArray = new int[hcCodeArray.length * 2];
			System.arraycopy(hcCodeArray, 0, newhcCodeArray, 0, hcCodeArray.length);
			hcCodeArray = newhcCodeArray;

			Object[] newobjectArray = new Object[hcCodeArray.length];
			System.arraycopy(objectArray, 0, newobjectArray, 0, objectArray.length);
			objectArray = newobjectArray;
		}

		do {
			hashCode++;
		} while (isUsingHcCode(hashCode));

		hcCodeArray[nextIdx] = hashCode;
		objectArray[nextIdx++] = obj;

		return hashCode;
	}

	private final boolean isUsingHcCode(final int hcCode) {
		for (int i = 0; i < nextIdx; i++) {
			if (hcCodeArray[i] == hcCode) {
				return true;
			}
		}

		if (hashCodeMap.containsKey(hcCode)) {
			return true;
		}

		return false;
	}
}
