package hc.server.ui.design.hpj;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.engine.HCJRubyEngine;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RubyExector {
	private static final Pattern linePattern = Pattern.compile("<script>:(\\d+)");
	
	public static final void removeCache(final String script, final HCJRubyEngine hcje){
		hcje.removeCache(script);
	}
	
	public final static void runLater(final String script, final String scriptName, final Map map, final HCJRubyEngine hcje, final ProjectContext context){
		context.run(new Runnable() {
			@Override
			public void run() {
				RubyExector.parse(script, scriptName, hcje, true);
				if(hcje.isError){
					return;
				}
				RubyExector.runNoWait(script, scriptName, map, hcje);
			}
		});
	}
	
	public static final Object run(final String script, final String scriptName, final Map map, final HCJRubyEngine hcje, final ProjectContext context) {
		RubyExector.parse(script, scriptName, hcje, true);
		
		if(hcje.isError){
			return null;
		}
		
		return ServerUIAPIAgent.getThreadPoolFromProjectContext(context).runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				return RubyExector.runNoWait(script, scriptName, map, hcje);
			}
		});
	}

	public static synchronized final void parse(final String script, final String scriptName, final HCJRubyEngine hcje, final boolean isReportException) {
		try {
			if(hcje.isError){
				hcje.errorWriter.reset();
				hcje.isError = false;
			}
			
			hcje.parse(script, scriptName);
		} catch (final Throwable e) {
			if(isReportException){
				ExceptionReporter.printStackTrace(e, script, e.toString(), ExceptionReporter.INVOKE_NORMAL);
			}
			
			final String err = hcje.errorWriter.getMessage();
			hcje.isError = true;
			
//			L.V = L.O ? false : LogManager.log("JRuby Script Error : " + err);
			
			final char[] chars = err.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				if(chars[i] == '\n'){
					hcje.sexception.setMessage(new String(chars, 0, i));
					break;
				}
			}
			
			final Matcher matcher = linePattern.matcher(err);
			if(matcher.find()){
				final String group = matcher.group(1);
				hcje.sexception.setLineNumber(Integer.parseInt(group));
			}
		}finally{
//			System.setProperty(USER_DIR_KEY, userDir);
		}
	}
	
	public static synchronized final Object runNoWait(final String script, final String scriptName, final Map map, final HCJRubyEngine hcje) {
//		final String USER_DIR_KEY = "user.dir";
		
		try {
			if(hcje.isError){
				hcje.errorWriter.reset();
				hcje.isError = false;
			}
			
//			System.out.println("set JRuby path : " + hcje.path);
//			System.setProperty("org.jruby.embed.class.path", hcje.path);
//			if(userDir == null){
//				userDir = System.getProperty(USER_DIR_KEY);
//			}
//			System.setProperty(USER_DIR_KEY, hcje.path);
			
//			System.out.println("Exec Script load path : " + hcje.container.getLoadPaths());
			if(map == null){
//				return hcje.engine.eval(script);
			}else{
				hcje.put("$_hcmap", map);
				
//				container.put("message", "local variable");
//				container.put("@message", "instance variable");
//				container.put("$message", "global variable");
//				container.put("MESSAGE", "constant");
				
//				Bindings bindings = new SimpleBindings();
//				bindings.put("_hcmap", map);
//				return hcje.engine.eval(script, bindings);
			}
			return hcje.runScriptlet(script, scriptName);
			
//			ScriptEngineManager manager = new ScriptEngineManager();  
//			ScriptEngine engine = manager.getEngineByName("jruby");
////			engine.getContext().setErrorWriter(errorWriter);
//			if(map == null){
//				return engine.eval(script);
//			}else{
//				Bindings bindings = new SimpleBindings();
//				bindings.put("_hcmap", map);
//				return engine.eval(script, bindings);
//			}
			
		} catch (final Throwable e) {
			final String err = hcje.errorWriter.getMessage();
			ExceptionReporter.printStackTraceFromRunException(e, script, err);
			System.err.println("------------------error on JRuby script : " + err + "------------------\n" + script + "\n--------------------end error on script---------------------");
			hcje.isError = true;
			
//			L.V = L.O ? false : LogManager.log("JRuby Script Error : " + err);
			
			final char[] chars = err.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				if(chars[i] == '\n'){
					hcje.sexception.setMessage(new String(chars, 0, i));
					break;
				}
			}
			
			final Matcher matcher = linePattern.matcher(err);
			if(matcher.find()){
				final String group = matcher.group(1);
				hcje.sexception.setLineNumber(Integer.parseInt(group));
			}
			return null;
		}finally{
//			System.setProperty(USER_DIR_KEY, userDir);
		}
	}

	public final static Map<String, String> toMap(final HCURL _hcurl) {
		Map<String, String> map = null;
		final int size = _hcurl.getParaSize();
		if(size > 0){
			map = new HashMap<String, String>();
			for (int i = 0; i < size; i++) {
				final String key = _hcurl.getParaAtIdx(i);
				try {
					map.put(key, URLDecoder.decode(_hcurl.getValueofPara(key), IConstant.UTF_8));
				} catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		return map;
	}

	public static void initActive(final HCJRubyEngine hcje) {
		final String script = 
				"require 'java'\n" +
				"str_class = Java::java.lang.String\n" +
				"return str_class::valueOf(\"1\")\n";//初始引擎及调试之用
		final String scriptName = null;
		parse(script, scriptName, hcje, false);
		final Object out = runNoWait(script, scriptName, null, hcje);
	}
	
	
}