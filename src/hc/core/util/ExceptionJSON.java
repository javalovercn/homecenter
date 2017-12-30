package hc.core.util;

import hc.core.IConstant;
import hc.core.RootServerConnector;

import java.io.UnsupportedEncodingException;

public class ExceptionJSON {
	public static final String GT = "&gt;";
	public static final String LT = "&lt;";
	private static final String __T = "\\t";
	private static final String __F = "\\f";
	private static final String __B = "\\b";
	private static final String CARRIAGE_RETURN_AND_NEWLINE = "\r\n";
	private static final String NEWLINE = "\n";
	private static final String CARRIAGE_RETURN = "\r";
	private static final String YINHAO_HTML_REPLACE_TO = "&quot;";
	private static final char[] CS_YINHAO_HTML_REPLACE_TO = YINHAO_HTML_REPLACE_TO.toCharArray();
	public static final String BR = "<BR/>";
	public static final char[] CS_BR = BR.toCharArray();
	public static final String FOUR_EMPTY_SPACE_HTML = "&nbsp;&nbsp;&nbsp;&nbsp;";


	private static final String doubleHao = "\"";
	private static final String maoHAO = ":";
	final VectorMap table;
	
	private String toURL;
	public boolean isForTest;
	
	public static final String HC_EXCEPTION_URL = RootServerConnector.AJAX_HTTPS_44X_URL_PREFIX + "exception.php";
	public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
	
	public final String getToURL(){
		return toURL;
	}
	
	public ExceptionJSON(final String url){
		this.toURL = url;
		this.table = new VectorMap(16);
	}
	
	public final void setToURL(final String url){
		toURL = url;
	}
	
	private static final String tag_structVersion = "tag_structureVersion";
	private static final String HAR_ID = "harId";
	private static final String HAR_VERSION = "harVersion";
	private static final String JRE_VERSION = "jreVersion";
	private static final String JRUBY_VERSION = "jrubyVersion";
	private static final String HC_VERSION = "hcVersion";
	private static final String THREAD_NAME = "threadName";
	private static final String CLASS_NAME = "className";
	private static final String MESSAGE = "message";
	private static final String SYS_PROPERTIES = "sysProperties";
	private static final String SCRIPT_ERR_MESSAGE = "scriptErrMessage";
	private static final String STACKTRACE = "stackTrace";
	private static final String SCRIPT = "script";
	private static final String tag_attachEmail = "tag_attachEmail";
	private static final String tag_isReceiveException = "tag_isReceiveException";
	
	/**
	 * if HomeCenter will NOT receive exception, only send email, this tag is used.
	 * @param receiveHar
	 */
	public final void setReceiveExceptionForHC(final boolean receiveHar){
		set(ExceptionJSON.tag_isReceiveException, receiveHar?IConstant.TRUE:IConstant.FALSE);
	}

	public final void setAttToEmail(final String email){
		set(ExceptionJSON.tag_attachEmail, email);
	}
	
	public final String getAttToEmail(){
		return (String)table.get(tag_attachEmail, null);
	}
	
	static final char[] searchBytes = {'"', '<', '>', 
		'\\', '\n', '\t', '\b', '\f'};
	static final char[][] replaceToBytes = {CS_YINHAO_HTML_REPLACE_TO, LT.toCharArray(), GT.toCharArray(),
		"&#92;".toCharArray(), CS_BR, FOUR_EMPTY_SPACE_HTML.toCharArray(), __B.toCharArray(), __F.toCharArray()};

	public final String replaceScript(final String script){
		if(script == null){
			return null;
		}
		
		final char[] srcBS = script.toCharArray();
		StringBuffer sb = null;
		
		final int srcLength = srcBS.length;
		int toAppendIdx = 0;
		for (int i = 0; i < srcLength; i++) {
			final char oneByte = srcBS[i];
			for (int j = 0; j < searchBytes.length; j++) {
				if(oneByte == searchBytes[j]){
					if(sb == null){
						sb = new StringBuffer(srcLength + 1024);
					}
					sb.append(srcBS, toAppendIdx, i - toAppendIdx);
					toAppendIdx = i + 1;
					sb.append(replaceToBytes[j]);
					break;
				}
			}
		}
		
		if(sb == null){
			return script;
		}else{
			sb.append(srcBS, toAppendIdx, srcLength - toAppendIdx);
			return sb.toString();
		}
	}
	
	public final void initData(final String threadName, final String className, final String message, final String stackTrace,
			final String sysProperties, final String harID, final String harVersion, final String jreVersion, final String hcVersion, 
			final String jrubyVersion, final String script, final String scriptErrMessage){
		set(tag_structVersion, "1");
		set(CLASS_NAME, className);
		set(MESSAGE, replaceForTail(message));
		set(STACKTRACE, stackTrace);
		set(SYS_PROPERTIES, sysProperties);//已执行replaceToWriteCode
		set(SCRIPT, replaceScript(script));//前置特殊处理
		set(SCRIPT_ERR_MESSAGE, replaceToWriteCode(scriptErrMessage));
		set(HAR_ID, harID);
		set(HAR_VERSION, harVersion);
		set(JRE_VERSION, jreVersion);
		set(HC_VERSION, hcVersion);
		set(JRUBY_VERSION, jrubyVersion);
		set(THREAD_NAME, replaceToWriteCode(threadName));
	}
	
	public final String get(final String key){
		return (String)table.get(key, "");
	}

	public final void set(String key, String value){
		if(value == null){
			value = "";
		}
		
		if(key.indexOf(doubleHao) >= 0){
			key = StringUtil.replace(key, doubleHao, YINHAO_HTML_REPLACE_TO);
		}
		
//		value = replaceForTail(value);
		
		table.set(key, value);
	}

	public static String replaceForTail(String value) {
		if(value == null){
			value = "";
		}
		
		value = StringUtil.replace(value, "<", LT);
		value = StringUtil.replace(value, ">", GT);
		
		if(value.indexOf(doubleHao) >= 0){
			value = StringUtil.replace(value, doubleHao, YINHAO_HTML_REPLACE_TO);
		}

		if(value.indexOf(CARRIAGE_RETURN_AND_NEWLINE) >= 0){
			value = StringUtil.replace(value, CARRIAGE_RETURN_AND_NEWLINE, BR);//针对脚本中的换行、回车
		}
		
		if(value.indexOf(NEWLINE) >= 0){
			value = StringUtil.replace(value, NEWLINE, BR);
		}
		
		if(value.indexOf(CARRIAGE_RETURN) >= 0){
			value = StringUtil.replace(value, CARRIAGE_RETURN, BR);
		}
		
//		if(value.indexOf("\\") >= 0){
//			value = StringUtil.replace(value, "\\", "&#92;");
//		}
//		
//		if(value.indexOf("/") >= 0){
//			value = StringUtil.replace(value, "/", "&#47;");
//		}
		
		if(value.indexOf("\b") >= 0){
			value = StringUtil.replace(value, "\b", __B);
		}
		
		if(value.indexOf("\f") >= 0){
			value = StringUtil.replace(value, "\f", __F);
		}
		
		if(value.indexOf("\t") >= 0){
			value = StringUtil.replace(value, "\t", __T);
		}
		return value;
	}
	
	byte[] jsonbs;
	public final byte[] getJSONBytesCache(){
		if(jsonbs == null){
			final String jsonStr = toJSON();
			try {
				jsonbs = jsonStr.getBytes(IConstant.UTF_8);
			} catch (UnsupportedEncodingException e) {
				jsonbs = jsonStr.getBytes();
			}
		}
		return jsonbs;
	}
	
	public final String toEmailHtml(){
		final StringBuffer sb = new StringBuffer(5120);
		sb.append("<h3>Exception Report</h3>");
		sb.append("<table border=\"1\">");
		//表头
		sb.append("<tr style=\"white-space:nowrap; background: #e6e6e6;\"><td><p style=\"text-align: center\" align=\"center\">item</p></td><td><p style=\"text-align: center\" align=\"center\">value</p></td></tr>");
		//表行
		final int size = table.size();
		for (int i = 0; i < size; i++) {
			final KeyValue kv = (KeyValue)table.elementAt(i);
			sb.append("<tr style=\"white-space:nowrap;\"><td><p style=\"text-align: center\" align=\"center\">");
			sb.append(kv.key);
			sb.append("</p></td><td>");
			sb.append(kv.value);
			sb.append("</td></tr>");
		}
		sb.append("</table>");
		
		return sb.toString();
	}
	
	public final String toJSON(){
//		{"public_ex":"this is public"}
//		{"projCode":"aa\"\""} 
		final StringBuffer sb = new StringBuffer(1024);
		final int size = table.size();
		
		sb.append("{");
		
		for (int i = 0; i < size; i++) {
			if(sb.length() > 1){
				sb.append(",");
			}
			
			KeyValue keyValue = (KeyValue)table.elementAt(i);
			sb.append(doubleHao);
			sb.append(keyValue.key);
			sb.append(doubleHao);
			
			sb.append(maoHAO);
			
			sb.append(doubleHao);
			sb.append(keyValue.value);
			sb.append(doubleHao);
		}
		
		sb.append("}");
		
		return sb.toString();
	}

	public static String replaceToWriteCode(String value) {
		if(value == null){
			value = "";
		}

		value = StringUtil.replace(value, "<", LT);
		value = StringUtil.replace(value, ">", GT);
		value = StringUtil.replace(value, "\"", YINHAO_HTML_REPLACE_TO);
		// value = StringUtil.replace(value, "\\", "\\\\");
		// value = StringUtil.replace(value, "/", "\\/");
		value = StringUtil.replace(value, "\b", "\\b");
		value = StringUtil.replace(value, "\f", "\\f");
		value = StringUtil.replace(value, "\t", "\\t");
		value = StringUtil.replace(value, "\n", "\\n");
		value = StringUtil.replace(value, "\r", "\\r");
		return value;
	}
}
