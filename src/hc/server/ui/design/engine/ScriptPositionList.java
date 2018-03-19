package hc.server.ui.design.engine;

import hc.core.util.Stack;

public class ScriptPositionList {
	private static final Stack free = new Stack(1024);

	static ScriptPosition getFree() {
		synchronized (free) {
			if (free.size() == 0) {
				return new ScriptPosition();
			} else {
				return (ScriptPosition) free.pop();
			}
		}
	}

	static void cycle(final ScriptPosition sb) {
		if (sb == null) {
			return;
		}

		synchronized (free) {
			free.push(sb);
		}
	}

	ScriptPosition[] positions;
	int count;

	ScriptPositionList() {
		this(8);
	}

	ScriptPositionList(final int size) {
		positions = new ScriptPosition[size];
	}

	/**
	 * 
	 * @param startIdx
	 * @param endIdx
	 *            excludes
	 * @param isAddedBySerial
	 */
	public final ScriptPosition addPosition(final boolean isAddStringExcludesPosi, final int startIdx, final int endIdx,
			final boolean isAddedBySerial) {
		return addPosition(isAddStringExcludesPosi, startIdx, endIdx, null, isAddedBySerial);
	}

	public final ScriptPosition addPosition(final boolean isAddStringExcludesPosi, final int startIdx, final int endIdx, final String item,
			final boolean isAddedBySerial) {
		return addPosition(isAddStringExcludesPosi, startIdx, endIdx, item, null, isAddedBySerial);
	}

	/**
	 * 
	 * @param startIdx
	 * @param endIdx
	 *            excludes
	 * @param item
	 * @param isAddedBySerial
	 */
	public final ScriptPosition addPosition(final boolean isAddStringExcludesPosi, final int startIdx, final int endIdx, final String item,
			final String extItem, final boolean isAddedBySerial) {
		final ScriptPosition p = getFree();
		p.startIdx = startIdx;
		p.endIdx = endIdx;// exclude
		p.item = item;
		p.extItem1 = extItem;

		if (count == positions.length) {
			final ScriptPosition[] newP = new ScriptPosition[count * 2];
			for (int i = 0; i < count; i++) {
				newP[i] = positions[i];
			}
			positions = newP;
		}

		if (isAddedBySerial || count == 0) {
			positions[count++] = p;
		} else {
			ScriptPosition tailOne = null;
			boolean isMov = false;
			for (int i = 0; i < count; i++) {
				if (isMov == false) {
					final int pStartIdx = positions[i].startIdx;
					if (pStartIdx > startIdx) {
						if (isAddStringExcludesPosi && endIdx > pStartIdx) {// "hello#world"
																			// +
																			// Thread.sleep()
							positions[i] = p;// 替换
							return p;
						}
						tailOne = positions[i];
						positions[i] = p;
						isMov = true;
					}
				} else {
					final ScriptPosition next = positions[i];
					positions[i] = tailOne;
					tailOne = next;
				}
			}
			positions[count++] = isMov ? tailOne : p;
		}

		return p;
	}

	public final void release() {
		for (int i = 0; i < count; i++) {
			cycle(positions[i]);
		}
	}

	public final boolean isInclude(final int startIdx) {
		for (int i = 0; i < count; i++) {
			final ScriptPosition p = positions[i];
			final boolean isBiggerThanStart = p.startIdx <= startIdx;
			final boolean isSmallThanEnd = startIdx <= p.endIdx;
			if (isBiggerThanStart && isSmallThanEnd) {
				return true;
			}
			if (isBiggerThanStart == false) {
				return false;
			}
		}
		return false;
	}
}

class ScriptPosition {
	int startIdx, endIdx;
	String item;
	String extItem1;
	String extItem2;
}
