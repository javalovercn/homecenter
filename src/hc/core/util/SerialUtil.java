package hc.core.util;

import hc.core.IConstant;

public class SerialUtil {
	private static final String STRING_ARRAY_SPLIT = "_";

	/**
	 * 与unserialToStringArray相反
	 * @param strings
	 * @return
	 */
	public static String serial(final String[] strings){
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strings.length; i++) {
			if(sb.length() > 0){
				sb.append(STRING_ARRAY_SPLIT);
			}
			sb.append(ByteUtil.toHex(ByteUtil.getBytes(strings[i], IConstant.UTF_8)));
		}
		return sb.toString();
	}
	
	/**
	 * 与serial(final String[] strings)相反
	 * @param str
	 * @return
	 */
	public static String[] unserialToStringArray(final String str){
		final String[] arr = StringUtil.splitToArray(str, STRING_ARRAY_SPLIT);
		for (int i = 0; i < arr.length; i++) {
			final byte[] bs = ByteUtil.toBytesFromHexStr(arr[i]);
			arr[i] = ByteUtil.buildString(bs, 0, bs.length, IConstant.UTF_8);
		}
		return arr;
	}
}
