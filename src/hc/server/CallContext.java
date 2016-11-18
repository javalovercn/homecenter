package hc.server;

import hc.core.util.Stack;
import hc.server.ui.design.hpj.HCScriptException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallContext {
	private static final Stack free = new Stack(8);
	private static final Pattern linePattern = Pattern.compile("<script>:(\\d+)");
	public final static String UN_KNOWN_TARGET = "unknown";

	public static CallContext getFree(){
		synchronized (free) {
			if(free.size() == 0){
				return new CallContext();
			}else{
				return (CallContext)free.pop();
			}
        }
	}
	
	public static void cycle(final CallContext callCtx){
		if(callCtx == null){
			return;
		}
		
		callCtx.reset();
		
		synchronized (free) {
			free.push(callCtx);
        }		
	}
	
	public final void setError(final String errMsg, final String script, final Throwable t){
		isError = true;
		jrubyErrMessage = errMsg;
		rubyScripts = script;
		rubyThrowable = t;
	}
	
	public final String getMessage(){
		proc();
		return sexception.getMessage();
	}
	
	public final int getLineNumber(){
		proc();
		return sexception.getLineNumber();
	}
	
	private final void proc(){
		if(isProccessed){
			return;
		}
		isProccessed = true;
		
		final char[] chars = jrubyErrMessage.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if(chars[i] == '\n'){
				sexception.setMessage(new String(chars, 0, i));
				break;
			}
		}
		
		final Matcher matcher = linePattern.matcher(jrubyErrMessage);
		if(matcher.find()){
			final String group = matcher.group(1);
			sexception.setLineNumber(Integer.parseInt(group));
		}
	}
	
	public final void reset(){
		isError = false;
		isProccessed = false;
		targetURL = null;
		jrubyErrMessage = null;
		rubyThrowable = null;
		rubyScripts = null;
		sexception.reset();
	}
	
	private boolean isProccessed;
	public boolean isError;
	public String targetURL;
	private String jrubyErrMessage;
	public Throwable rubyThrowable;
	public String rubyScripts;
	private final HCScriptException sexception = new HCScriptException("");

}
