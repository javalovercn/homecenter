package hc.util;

import java.util.regex.Pattern;

public class JSUtil {
	/**
	 * 将单个双引号转为字面码
	 * 
	 * @param js
	 * @return
	 */
	public static final String replaceShuanYinHao(final String js) {
		// return js.replaceAll("\"", "\\\\\"");
		return SHUAN_YIN_HAO.matcher(js).replaceAll("\\\\\"");
	}

	private static final Pattern SHUAN_YIN_HAO = Pattern.compile("\"");

	private static final Pattern NEW_LINE = Pattern.compile("\n");
	
	private static final Pattern JS_FUNC = Pattern.compile("\\bfunction(\\s*\\w+)?\\s*\\((\\s*\\w+\\s*(\\,\\s*\\w+\\s*)*)?\\)\\s*\\{");
	
	public static final boolean isMatchJSFunction(final String js) {
		return JS_FUNC.matcher(js).find();
	}

	/**
	 * 换行转成字面码
	 * 
	 * @param js
	 * @return
	 */
	public static final String replaceNewLine(final String js) {
		// return js.replaceAll("\n", "\\\\n");
		return NEW_LINE.matcher(js).replaceAll("\\\\n");
	}

	private static final Pattern RETURN = Pattern.compile("\r");

	public static final String replaceReturnWithEmtpySpace(final String js) {
		// return js.replaceAll("\r", "");
		return RETURN.matcher(js).replaceAll("");
	}

	final static String[] JAVA_SCRIPT_DOC_METHODS = {
		"document.getElementById(",
		"document.createElement(",
		"document.getElementsByTagName(",
		"document.getElementsByClassName("};

	public static final boolean isJavaScriptPaste(final String paste) {
		if(isMatchJSFunction(paste)) {
			return true;
		}
		
		char[] chars = null;
		for (int i = 0; i < JAVA_SCRIPT_DOC_METHODS.length; i++) {
			int idx = 0;
			final String m = JAVA_SCRIPT_DOC_METHODS[i];
			if((idx = paste.indexOf(m, idx)) > 0) {
				if(chars == null) {
					chars = paste.toCharArray();
				}
				if(ResourceUtil.isWordSpliter(chars, idx - 1)) {
					if(ResourceUtil.isInString(chars, idx) == false) {//有可能粘入为带#的一行Ruby代码
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public static final String formatInsertJS(String src) {
		src = src.replace("\r", "");
		
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("js = \"");
		
		final char[] chars = src.toCharArray();
		final int len = chars.length;
		
		for (int i = 0; i < len; i++) {
			final char c = chars[i];
			if(c == '\"') {
				sb.append('\\');
				sb.append(c);
			}else if(c == '\n') {
				sb.append("\" + ");
				sb.append(c);
				sb.append('\"');
			}else {
				sb.append(c);
			}
		}
		sb.append('\"');
		
		final String result = sb.toString();
		StringBuilderCacher.cycle(sb);
		return result;
	}

}
