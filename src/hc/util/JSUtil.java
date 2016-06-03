package hc.util;

import java.util.regex.Pattern;

public class JSUtil {
	/**
	 * 将单个双引号转为字面码
	 * @param js
	 * @return
	 */
	public static final String replaceShuanYinHao(final String js){
//		return js.replaceAll("\"", "\\\\\"");
		return SHUAN_YIN_HAO.matcher(js).replaceAll("\\\\\"");
	}
	
	private static final Pattern SHUAN_YIN_HAO = Pattern.compile("\"");
	
	private static final Pattern NEW_LINE = Pattern.compile("\n");

	/**
	 * 换行转成字面码
	 * @param js
	 * @return
	 */
	public static final String replaceNewLine(final String js){
//		return js.replaceAll("\n", "\\\\n");
		return NEW_LINE.matcher(js).replaceAll("\\\\n");
	}
	
	private static final Pattern RETURN = Pattern.compile("\r");

	public static final String replaceReturnWithEmtpySpace(final String js){
//		return js.replaceAll("\r", "");
		return RETURN.matcher(js).replaceAll("");
	}

}
