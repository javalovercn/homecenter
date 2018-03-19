package hc.server.msb;

import hc.server.ui.design.LinkProjectStore;

import java.util.Vector;

public class DeviceMatchManager {
	private static final char[] splitChars = { ',', ';', '.', '，', '；', '。', '\n', '\t' };
	private static final int splitCharLen = splitChars.length;
	private static final String[] emptyStringArray = {};

	/**
	 * 支持各种分割符，如果是双引号，则可含任何内容
	 * 
	 * @param desc
	 * @return
	 */
	public static String[] getMatchItemFromDesc(final String desc) {
		if (desc == null) {
			return emptyStringArray;
		}

		final char[] chars = desc.toCharArray();
		final int size = chars.length;
		final Vector<String> vector = new Vector<String>();
		int lastAvaiBeginCharIdx = -1;
		for (int i = 0; i < size; i++) {
			final char oneChar = chars[i];

			final boolean isYinHao = oneChar == '\"';
			if (isYinHao) {
				final int startJ = i + 1;
				boolean isAdded = false;
				int j = startJ;
				for (; j < size; j++) {
					final char endChar = chars[j];
					if (endChar == '\"') {
						addToVector(vector, new String(chars, startJ, j - startJ));
						isAdded = true;
						break;
					}
				}
				if (isAdded) {
					i = j;
					lastAvaiBeginCharIdx = -1;
					continue;
				}
			}

			boolean isSplitChar = false;
			if (isYinHao) {
				isSplitChar = true;
			} else {
				for (int j = 0; j < splitCharLen; j++) {
					if (oneChar == splitChars[j]) {
						isSplitChar = true;
						break;
					}
				}
			}

			if (isSplitChar) {
				if (lastAvaiBeginCharIdx >= 0) {
					addToVector(vector, new String(chars, lastAvaiBeginCharIdx, i - lastAvaiBeginCharIdx));
					lastAvaiBeginCharIdx = -1;
				}
			} else {
				if (oneChar == '-' || oneChar == '_') {
					chars[i] = ' ';
				}
				if (lastAvaiBeginCharIdx < 0) {
					lastAvaiBeginCharIdx = i;
				}
			}
		}
		if (lastAvaiBeginCharIdx >= 0) {
			addToVector(vector, new String(chars, lastAvaiBeginCharIdx, size - lastAvaiBeginCharIdx));
		}

		if (vector.size() > 0) {
			return vector.toArray(emptyStringArray);
		} else {
			return emptyStringArray;
		}
	}

	private static final void addToVector(final Vector<String> vector, final String item) {
		final String trimStr = item.trim();
		if (trimStr.length() > 0) {
			vector.add(trimStr.toLowerCase());// 大小写不敏感
		}
	}

	/**
	 * 如果从target中发现有与src相同且匹配，则返回true
	 * 
	 * @param src
	 * @param target
	 * @return
	 */
	public static boolean match(final String[] src, final String[] target) {
		final int srcSize = src.length;
		final int targetSize = target.length;

		for (int i = 0; i < srcSize; i++) {
			final String oneSrc = src[i];
			for (int j = 0; j < targetSize; j++) {
				if (oneSrc.equals(target[j])) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * @param lpss
	 *            the projects added to system.
	 * @return true:auto match all; false : at least one is NOT matched.
	 */
	public static boolean doAutoMatch(final LinkProjectStore[] lpss) {
		// mobiResp.getProjResponser(bdn.projID).threadPool;

		return false;
	}
}
