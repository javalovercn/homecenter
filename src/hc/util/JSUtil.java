package hc.util;

public class JSUtil {
	/**
	 * 将单个双引号转为字面码
	 * @param js
	 * @return
	 */
	public static final String replaceShuanYinHao(final String js){
		return js.replaceAll("\"", "\\\\\"");
	}
	
	/**
	 * 换行转成字面码
	 * @param js
	 * @return
	 */
	public static final String replaceNewLine(final String js){
		return js.replaceAll("\n", "\\\\n");
	}
	
	public static final String replaceReturnWithEmtpySpace(final String js){
		return js.replaceAll("\r", "");
	}

	public static final String replaceNewLineWithEmtpySpace(final String js){
		return js.replaceAll("\n", " ");
	}
}
