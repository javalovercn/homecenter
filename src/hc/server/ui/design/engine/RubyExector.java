package hc.server.ui.design.engine;

import hc.core.BaseWatcher;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.server.CallContext;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.util.ResourceUtil;
import hc.util.ThreadConfig;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class RubyExector {
	public static final void removeCache(final String script, final HCJRubyEngine hcje){
		hcje.removeCache(script);
	}
	
	public static final Object runAndWaitInProjectOrSessionPoolWithRepErr(final J2SESession coreSS, final CallContext runCtx, final String script, final String scriptName, final Map map, final HCJRubyEngine hcje, final ProjectContext context, final Class requireReturnClass) {
		Object out = null;
		try{
			out = requireReturnClass.cast(RubyExector.runAndWaitInProjectOrSessionPool(coreSS, runCtx, script, scriptName, map, hcje, context));
		}catch (final Throwable e) {
			e.printStackTrace();
			//不return，因为需要后续报告错误。
		}
		if(out == null){
			String message;
			if(runCtx.isError){
				message = runCtx.getMessage();
			}else{
				message = "expected return Class : " + requireReturnClass.getName() + ", but return null";
			}
			LogManager.errToLog("parse script error : [" + message + "], for script : \n" + script);
			LogManager.err("Error instance " + requireReturnClass.getSimpleName() + " in project [" + context.getProjectID() +"].");
			//Fail to add HAR message
			notifyMobileErrorScript(coreSS, context, scriptName);
		}
		return out;
	}
	
	public static final Object runAndWaitInProjectOrSessionPool(final J2SESession coreSS, final CallContext callCtx, final String script, final String scriptName, final Map map, final HCJRubyEngine hcje, final ProjectContext context) {
		RubyExector.parse(callCtx, script, scriptName, hcje, true);
		
		if(callCtx.isError){
			return null;
		}
		
		final ReturnableRunnable run = new ReturnableRunnable() {
			@Override
			public Object run() {
				ThreadConfig.setThreadTargetURL(callCtx);
				return RubyExector.runAndWaitOnEngine(callCtx, script, scriptName, map, hcje);
			}
		};
		
		if(coreSS != null){
			return ServerUIAPIAgent.getProjResponserMaybeNull(context).getMobileSession(coreSS).recycleRes.threadPool.runAndWait(run);
		}else{
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("[workshop] this script runs in project level.");
			}
			return ServerUIAPIAgent.runAndWaitInProjContext(context, run);
		}
	}

	public static synchronized final void parse(final CallContext callCtx, final String script, final String scriptName, final HCJRubyEngine hcje, final boolean isReportException) {
		try {
			hcje.parse(script, scriptName);
		} catch (final Throwable e) {
			if(isReportException){
				ExceptionReporter.printStackTrace(e, script, e.toString(), ExceptionReporter.INVOKE_NORMAL);
			}
			
			final String err = hcje.errorWriter.getMessage();
			if(callCtx != null){
				callCtx.setError(err, script, e);
			}
			hcje.resetError();
//			L.V = L.O ? false : LogManager.log("JRuby Script Error : " + err);
		}finally{
//			System.setProperty(USER_DIR_KEY, userDir);
		}
	}
	
	/**
	 * 注意：用户级runAndWait会导致死锁，故关闭synchronized
	 * @param callCtx
	 * @param script
	 * @param scriptName
	 * @param map
	 * @param hcje
	 * @return
	 */
	public static final Object runAndWaitOnEngine(final CallContext callCtx, final String script, final String scriptName, final Map map, final HCJRubyEngine hcje) {
		try {
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
				LogManager.errToLog("$_hcmap is deprecated, there are serious concurrent risks in it. " +
						"\nplease remove all parameters in target URL, and set them to attributes of ProjectContext or ClientSession.");
				hcje.put("$_hcmap", map);
				
//				container.put("message", "local variable");
//				container.put("@message", "instance variable");
//				container.put("$message", "global variable");
//				container.put("MESSAGE", "constant");
				
//				Bindings bindings = new SimpleBindings();
//				bindings.put("_hcmap", map);
//				return hcje.engine.eval(script, bindings);
			}
			
//			if(L.isInWorkshop){
//				L.V = L.O ? false : LogManager.log("====>Thread [" + Thread.currentThread().getId() + "] before runScriptlet.");
//			}
			final Object out = hcje.runScriptlet(script, scriptName);
//			if(L.isInWorkshop){
//				L.V = L.O ? false : LogManager.log("====>Thread [" + Thread.currentThread().getId() + "] after runScriptlet.");
//			}
			return out;
			
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
			ExceptionReporter.printStackTraceFromHAR(e, script, err);
			System.err.println("------------------error on JRuby script : " + err + "------------------\n" + script + "\n--------------------end error on script---------------------");
			if(callCtx != null){
				callCtx.setError(err, script, e);
			}
			hcje.resetError();
//			L.V = L.O ? false : LogManager.log("JRuby Script Error : " + err);
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
				"str_class = Java::java.lang.String\n" +
				"return str_class::valueOf(\"1\")\n";//初始引擎及调试之用
		final String scriptName = null;
		parse(null, script, scriptName, hcje, false);
		runAndWaitOnEngine(null, script, scriptName, null, hcje);
	}

	private static final void notifyMobileErrorScript(final J2SESession coreSS, final ProjectContext ctx, final String title){
		if(coreSS == null){
			return;
		}
		
		String msg = (String)ResourceUtil.get(9163);
		msg = StringUtil.replace(msg, "{title}", title);
		
		final J2SESession[] coreSSS = {coreSS};
		ServerUIAPIAgent.sendMessageViaCoreSS(coreSSS, (String)ResourceUtil.get(IContext.ERROR), msg, ProjectContext.MESSAGE_ERROR, 
				null, 0);
	}

	public static void execInSequenceForSession(final J2SESession coreSS,
			final ProjResponser resp, final ReturnableRunnable runnable) {
		resp.getMobileSession(coreSS).recycleRes.sequenceWatcher.addWatcher(new BaseWatcher() {
			@Override
			public boolean watch() {
				ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, resp, runnable);
				return true;
			}
		});
	}
	
	
}