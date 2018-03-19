package hc.core.cache;

import java.util.Vector;
import hc.core.util.ByteUtil;

public class CacheDataItem {
	// 记录已存在，可能为空记录
	public static final int STATUS_EXISTS = 1;

	// 将要增加到库中，更新到库后，将转为STATUS_EXISTS
	public static final int STATUS_TO_ADD = 2;

	// 将从库中删除，删除后，将转为STATUS_EXISTS，并转为空记录
	public static final int STATUS_TO_DEL = 3;

	// 将要更新到库中，更新到库后，将转为STATUS_EXISTS
	public static final int STATUS_TO_UPDATE = 4;

	public boolean isEmpty = false;
	public byte[] bs;
	public int status;
	public String stringValue;
	int emptyVectorIdx;

	public final String toStringValue() {
		if (isEmpty) {
			return "";
		}

		if (stringValue == null) {
			stringValue = ByteUtil.toString(bs, 0, bs.length);
		}
		return stringValue;
	}

	public final void setEmpty() {
		bs = CacheStoreManager.EMPTY_BS;
		isEmpty = true;
		stringValue = null;
	}

	public CacheDataItem(byte[] bs, int status) {
		this.bs = bs;
		if (CacheStoreManager.isEmptyBS(bs)) {
			isEmpty = true;
		}
		this.status = status;
	}

	public final boolean equalBS(final byte[] value, final int offset,
			final int len) {
		if (len == bs.length) {
			final int endIdx = offset + len;
			for (int i = offset, j = 0; i < endIdx; i++, j++) {
				if (bs[j] != value[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public final static int addRecrodAndSave(final String rmsName,
			final Vector vector, byte[] value, int offset, int len,
			boolean valueIsCopyed, boolean isEmptyFirst) {
		byte[] copybs;
		if (valueIsCopyed) {
			copybs = value;
		} else {
			copybs = new byte[len];
			System.arraycopy(value, offset, copybs, 0, len);// 必须是拷贝方式，因为数据源是EventCenter和压缩可复用对象
		}

		// synchronized (vector) {
		CacheDataItem item;
		if (isEmptyFirst && (item = getEmptyRecord(vector)) != null) {
			item.setUpdate(copybs);

			final int lastStoreIdx = item.emptyVectorIdx;
			CacheStoreManager.storeData(rmsName, vector);
			return lastStoreIdx;
		} else {
			item = new CacheDataItem(copybs, STATUS_TO_ADD);
			vector.addElement(item);

			CacheStoreManager.storeData(rmsName, vector);
			return vector.size() - 1;
		}
		// }
	}

	public final static boolean isEmptyFirst = true;

	/**
	 * 
	 * @param vector
	 * @param value
	 * @return > 0: exits/add vector index
	 */
	public final static int addRecrodIfNotExists(final String rmsName,
			final Vector vector, final byte[] value, final int offset,
			final int len) {
		final int idx = searchMatchVectorIdx(vector, value, offset, len);
		if (idx == -1) {
			return addRecrodAndSave(rmsName, vector, value, offset, len, false,
					isEmptyFirst);
		} else {
			return idx;
		}
	}

	/**
	 * 没找到，返回-1。找到，返回vector index
	 * 
	 * @param vector
	 * @param value
	 * @param offset
	 * @param len
	 * @return
	 */
	public static int searchMatchVectorIdx(final Vector vector,
			final byte[] value, final int offset, final int len) {
		final int endIdx = vector.size();
		for (int i = endIdx - 1; i >= 1; i--) {// 因为尾部可能存在存储不一致产生的空位。
			CacheDataItem item = (CacheDataItem) vector.elementAt(i);
			if (item.isEmpty) {
				continue;
			}

			if (item.equalBS(value, offset, len)) {
				return i;
			}
		}
		return -1;
	}

	public final void setUpdate(final byte[] newBS) {
		bs = newBS;
		status = STATUS_TO_UPDATE;
		stringValue = null;
		isEmpty = false;
	}

	public final void setDel() {
		status = STATUS_TO_DEL;
	}

	/**
	 * 如果没有空记录，则返回null
	 * 
	 * @param vector
	 * @return
	 */
	private static CacheDataItem getEmptyRecord(final Vector vector) {
		final int endIdx = vector.size();
		for (int i = 1; i < endIdx; i++) {
			CacheDataItem item = (CacheDataItem) vector.elementAt(i);
			if (item.isEmpty) {
				item.emptyVectorIdx = i;
				return item;
			}
		}
		return null;
	}
}
