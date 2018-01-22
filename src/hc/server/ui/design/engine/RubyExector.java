package hc.server.ui.design.engine;

import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.core.util.StringValue;
import hc.server.CallContext;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.SessionContext;
import hc.util.ResourceUtil;
import hc.util.ThreadConfig;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class RubyExector {
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
//		RubyExector.parse(callCtx, script, scriptName, hcje, true);
		
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
			final SessionContext mobileSession = ServerUIAPIAgent.getProjResponserMaybeNull(context).getMobileSession(coreSS);
			if(mobileSession != null){
				return mobileSession.recycleRes.threadPool.runAndWait(run);
			}else{
				return null;
			}
		}else{
			if(L.isInWorkshop){
				LogManager.log("[workshop] this script runs in project level.");
			}
			return ServerUIAPIAgent.runAndWaitInProjContext(context, run);
		}
	}

	public static synchronized final void parse(final CallContext callCtx, final String sc, final String scriptName, final HCJRubyEngine hcje, final boolean isReportException) {
		final StringValue sv = new StringValue();
		sv.value = sc;
		try {
			hcje.parse(sv, scriptName);
		} catch (final Throwable e) {
			if(isReportException){
				final String errorMsg = "project [" + hcje.projectIDMaybeBeginWithIDE + "] script error : " + e.toString();
				LogManager.errToLog(errorMsg);
				ExceptionReporter.printStackTrace(e, sv.value, errorMsg, ExceptionReporter.INVOKE_NORMAL);
			}
			
			final String err = hcje.errorWriter.getMessage();
			if(callCtx != null){
				callCtx.setError(err, sv.value, e);
			}
			hcje.resetError();
//			LogManager.log("JRuby Script Error : " + err);
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
	public static final Object runAndWaitOnEngine(final CallContext callCtx, final String sc, final String scriptName, final Map map, final HCJRubyEngine hcje) {
		final StringValue sv = new StringValue();
		sv.value = sc;
		try {
			return runAndWaitOnEngine(sv, scriptName, map, hcje);
			
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
			String err = hcje.errorWriter.getMessage();
			ExceptionReporter.printStackTraceFromHAR(e, sv.value, err);
			err = StringUtil.replace(err, "\n", "");//去掉换行
			err = StringUtil.replace(err, "\t", "");//去掉缩进
			System.err.println("------------------error on JRuby script : [" + err + "] ------------------\n" + sv.value + "\n--------------------end error on script---------------------");
			if(callCtx != null){
				callCtx.setError(err, sv.value, e);
			}
			hcje.resetError();
//			LogManager.log("JRuby Script Error : " + err);
			return null;
		}finally{
//			System.setProperty(USER_DIR_KEY, userDir);
		}
	}

	public static Object runAndWaitOnEngine(final StringValue sv, final String scriptName,
			final Map map, final HCJRubyEngine hcje) throws Throwable {
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
		//				LogManager.log("====>Thread [" + Thread.currentThread().getId() + "] before runScriptlet.");
		//			}
					final Object out = hcje.runScriptlet(sv, scriptName);
		//			if(L.isInWorkshop){
		//				LogManager.log("====>Thread [" + Thread.currentThread().getId() + "] after runScriptlet.");
		//			}
					return out;
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
				"#init TestEngine\n" +
				"str_class = java.lang.String\n";//初始引擎及调试之用
		final String scriptName = null;
//		parse(null, script, scriptName, hcje, false);
		runAndWaitOnEngine(null, script, scriptName, null, hcje);
	}

	private static final void notifyMobileErrorScript(final J2SESession coreSS, final ProjectContext ctx, final String title){
		if(coreSS == null){
			return;
		}
		
		String msg = ResourceUtil.get(coreSS, 9163);
		msg = StringUtil.replace(msg, "{title}", title);
		
		final J2SESession[] coreSSS = {coreSS};
		ServerUIAPIAgent.sendMessageViaCoreSSInUserOrSys(coreSSS, ResourceUtil.get(coreSS, IContext.ERROR), msg, ProjectContext.MESSAGE_ERROR, 
				null, 0);
	}
	
}