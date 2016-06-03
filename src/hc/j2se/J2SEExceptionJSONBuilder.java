package hc.j2se;

import hc.core.util.ExceptionChecker;
import hc.core.util.ExceptionJSON;
import hc.core.util.ExceptionJSONBuilder;
import hc.core.util.HarHelper;
import hc.core.util.HarInfoForJSON;
import hc.core.util.StringUtil;

import java.util.Enumeration;
import java.util.Properties;

public abstract class J2SEExceptionJSONBuilder extends ExceptionJSONBuilder {

	private final String sysPropertiesSnap = getSysProperties();//必须用系统线程权限进行初始化
	
	private final String getSysProperties(){
		final Properties p = System.getProperties();
		final Enumeration en = p.propertyNames();
		final StringBuilder sb = new StringBuilder(2048);
		
		while(en.hasMoreElements()){
			if(sb.length() > 0){
				sb.append(ExceptionJSON.BR);
			}
			final String key = (String)en.nextElement();
			sb.append(key);
			sb.append("=");
			String value = p.getProperty(key);
			value = ExceptionJSON.replaceToWriteCode(value);
			sb.append(value);
		}
		
		return sb.toString();
	}

	@Override
	public final ExceptionJSON buildJSON(final HarHelper helper, final ExceptionChecker checker, final Throwable throwable, final String reportURL, 
			final String script, final String scriptErrMsg) {
		String projectID = "";
		String projectVersion = "";
		if(helper != null){
			final HarInfoForJSON harInfo = helper.getHarInfoForJSON();
			if(harInfo != null){
				projectID = harInfo.projectID;
				projectVersion = harInfo.projectVersion;
			}
		}

		final StringBuilder sb = printStackTraceToStringBuilder(projectID, checker, throwable, null, "");
		if(sb == null){
//			L.V = L.O ? false : LogManager.log("Exception is posted!");
			//isPosted
			return null;
		}
		
		final String blockMessage = "***";

		final ExceptionJSON data = new ExceptionJSON(reportURL);
		final String threadName = Thread.currentThread().getName();
		final String exceptionClassName = throwable.getClass().getName();
		final String exceptionMessage = blockMessage;//throwable.getMessage();

		final String stackTrace = sb.toString();//"this is example stackTrace";
		final String jreVersion = getJREVer();
		final String hcVersion = getHCVersion();
		final String jrubyVersion = getJRubyVer();
		
		data.initData(threadName, exceptionClassName, exceptionMessage, stackTrace, sysPropertiesSnap,
				projectID, projectVersion, jreVersion, hcVersion, jrubyVersion, script, blockMessage);//scriptErrMsg
		
		return data;
	}

	public abstract String getHCVersion();

	public abstract String getJREVer();
	
	public abstract String getJRubyVer();

	final static String red_lt = "<font color='red'>" + ExceptionJSON.LT;
	final static String red_gt = ExceptionJSON.GT + "</font>";
	
	final static String sb_script = ".rb:";
	final static String red_sb_script = ".<font color='red'>rb</font>:";
	
	public static final StringBuilder printStackTraceToStringBuilder(final String harID, final ExceptionChecker checker, final Throwable throwable, 
			StringBuilder sb, final String tab) {
//		sb.append("ThreadName : " + throwable.get)
		final StackTraceElement[] ste = throwable.getStackTrace();
		
		if(checker != null){
			if(checker.isPosted(harID, ste[0].toString())){
				return null;
			}
		}
		
		if(sb == null){
			sb = new StringBuilder(2048);
		}
		
		sb.append("Exception : ");
		sb.append(throwable.getClass().getName());
//		sb.append(": ");
//		sb.append(ExceptionJSON.replaceToWriteCode(cause.getMessage()));
		sb.append(ExceptionJSON.BR);

		final int size = ste.length;
		for (int i = 0; i < size; i++) {
			sb.append(tab);
			sb.append("at : ");
			String writeCode = ExceptionJSON.replaceToWriteCode(ste[i].toString());
			writeCode = StringUtil.replace(writeCode, ExceptionJSON.LT, red_lt);
			writeCode = StringUtil.replace(writeCode, ExceptionJSON.GT, red_gt);
			writeCode = StringUtil.replace(writeCode, sb_script, red_sb_script);
			sb.append(writeCode);
			sb.append(ExceptionJSON.BR);
		}
		
		final String currTab = tab + ExceptionJSON.FOUR_EMPTY_SPACE_HTML;

//		for java 1.7
//		for (final Throwable se : throwable.getSuppressed()){
//			printStackTraceToStringBuilder(se, sb, currTab, "Suppressed by : ");
//		}
		
		final Throwable cause = throwable.getCause();
		if(cause != null){
			sb.append(currTab);
			sb.append("<font color='red'>Caused by: </font>");
			sb.append("<strong>");
			sb.append(cause.getClass().getName());
//			sb.append(": ");
//			sb.append(ExceptionJSON.replaceToWriteCode(cause.getMessage()));
			sb.append("</strong>");
			sb.append(ExceptionJSON.BR);
			printStackTraceToStringBuilder(null, null, cause, sb, currTab);
		}
		
		return sb;
	}

}
