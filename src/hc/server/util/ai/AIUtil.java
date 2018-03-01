package hc.server.util.ai;

import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.core.util.StringBufferCacher;
import hc.core.util.StringUtil;

import java.util.List;
import java.util.regex.Pattern;

public class AIUtil {
	public static final int MAX_LABLE_NUM_IN_SAME_LOC = 99;

	static final Pattern floatPattern = Pattern.compile("[+-]?([0-9]*)\\.([0-9]+)");
	static final Pattern intPattern = Pattern.compile("[+-]?([0-9]+)");

	public static final String floatReplace = "%!%";
	public static final String intReplace = "%@%";

	public static final String UNReplace = "%?%";

	public final static String findPattern(String text) {
		text = findFloatPattern(text);
		return findIntPattern(text);
	}

	public final static String findAnyPattern(final String text) {
		return null;
	}

	public final static String findFloatPattern(final String text) {
		final java.util.regex.Matcher m = floatPattern.matcher(text);
		boolean result = m.find();
		if (result) {
			final StringBuffer sb = StringBufferCacher.getFree();
			do {
				m.appendReplacement(sb, floatReplace);
				result = m.find();
			} while (result);
			m.appendTail(sb);
			final String out = sb.toString();
			StringBufferCacher.cycle(sb);
			return out;
		}

		return text;
	}

	public final static String findIntPattern(final String text) {
		final java.util.regex.Matcher m = intPattern.matcher(text);
		boolean result = m.find();
		if (result) {
			final StringBuffer sb = StringBufferCacher.getFree();
			do {
				m.appendReplacement(sb, intReplace);
				result = m.find();
			} while (result);
			m.appendTail(sb);
			final String out = sb.toString();
			StringBufferCacher.cycle(sb);
			return out;
		}

		return text;
	}

	public final static byte[] toRecord(final List<String> list) {
		final byte[] bs = AIUtil.cache.getFree(AIUtil.maxByteLen);
		int len = 0;

		final int itemSize = list.size();
		for (int i = 0; i < itemSize; i++) {
			final String str = list.get(i);
			final byte[] itembs = StringUtil.getBytes(str);

			final int itemBsLen = itembs.length;
			if (itemBsLen + 1 + len > AIUtil.maxByteLen) {
				LogManager.warning("trim too long data on : " + str);
				break;
			}

			bs[len++] = (byte) itemBsLen;
			for (int j = 0; j < itemBsLen;) {
				bs[len++] = itembs[j++];
			}
		}

		final byte[] storeBS = new byte[len];
		for (int i = 0; i < len; i++) {
			storeBS[i] = bs[i];
		}
		AIUtil.cache.cycle(bs);

		return storeBS;
	}

	static final ByteArrayCacher cache = ByteUtil.byteArrayCacher;
	protected static final int maxByteLen = 500;

}
