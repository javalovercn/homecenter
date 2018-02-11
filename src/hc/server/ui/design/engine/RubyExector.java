package hc.server.ui.design.engine;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringBufferCacher;
import hc.core.util.StringUtil;
import hc.core.util.StringValue;
import hc.server.CallContext;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.SessionContext;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;
import hc.util.ThreadConfig;

public class RubyExector {
	public static final String JAVA_MAO_MAO = "Java::";
	private static final String IMPORT = "import ";
	private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*(import(\\s+))([a-zA-Z0-9:_\\.]+)\\s*(#.*?)?$", Pattern.MULTILINE);

	/**
	 * 将java.lang.String转为JavaLangString
	 * @param packageName
	 * @return
	 */
	private static String toJavaConstant(final ScriptPosition sp) {
		String packageName = sp.item;
		if(packageName.startsWith(JAVA_MAO_MAO)) {
			packageName = packageName.substring(JAVA_MAO_MAO.length());
		}
		
		final Vector parts = StringUtil.split(packageName, ".");
		final StringBuffer sb = StringBufferCacher.getFree();
		final int size = parts.size();
		final int lastItemIdx = size - 1;
		sp.extItem1 = (String)parts.elementAt(lastItemIdx);
		
		for (int i = 0; i < size; i++) {
			final String item = (String)parts.elementAt(i);
			if(i == lastItemIdx) {
				sb.append(item);
			}else {
				final char firstChar = item.charAt(0);
				if(firstChar >= 'a' && firstChar <= 'z') {
					//97(s) - 65(A)
					sb.append((char)(firstChar - 32));
				}else {
					sb.append(firstChar);
				}
				if(item.length() > 1) {
					sb.append(item.substring(1));
				}
			}
		}
		final String out = sb.toString();
		StringBufferCacher.cycle(sb);
		return out;
	}
	
	public static String replaceImport(final String scripts){
		final int firstIdx = scripts.indexOf(IMPORT, 0);
		if(firstIdx < 0){
			return scripts;
		}
	
		ScriptPositionList classRefPositions = null;
		ScriptPositionList imports = null;
		ScriptPositionList rememOrString = null;
		
		try {
			final Matcher matcher = IMPORT_PATTERN.matcher(scripts);
			while(matcher.find()) {
				if(imports == null) {
					imports = new ScriptPositionList();
				}
				final ScriptPosition p = imports.addPosition(false, matcher.start(1), matcher.end(3), matcher.group(3), true);
				final String constantName = toJavaConstant(p);
				p.extItem2 = constantName;
			}
			
			if(imports == null) {
				return scripts;
			}
			
			final char[] chars = scripts.toCharArray();
			final int charsLen = chars.length;
			rememOrString = new ScriptPositionList(64);
			{
				//add #
				int remStartIdx = 0;
				boolean isFindRem = false;
				for (int i = 0; i < charsLen; i++) {
					final char c = chars[i];
					if(isFindRem == false) {
						if(c == '#') {
							isFindRem = true;
							remStartIdx = i;
						}
					}else {
						if(c == '\n') {
							rememOrString.addPosition(false, remStartIdx, i, true);
							isFindRem = false;
						}
					}
				}
				if(isFindRem) {
					rememOrString.addPosition(false, remStartIdx, charsLen, true);
				}
			}
			
			{
				//add '或“”
				int stringStartIdx = 0;
				boolean isFindStr = false;
				char charType = 0;
				for (int i = 0; i < charsLen; i++) {
					final char c = chars[i];
					if(c == '\'' || c == '\"') {
						if(chars[i - 1] == '\\') {
							continue;
						}
						if(isFindStr == false) {
							charType = c;
							isFindStr = true;
							stringStartIdx = i;
						}else{
							if(charType == c) {
								rememOrString.addPosition(true, stringStartIdx, i + 1, false);
								isFindStr = false;
							}
						}
					}
				}
			}
			
			classRefPositions = new ScriptPositionList(128);
			final Vector<String> usedConstant = new Vector<String>(64);
			for (int i = 0; i < imports.count; i++) {
				final ScriptPosition sp = imports.positions[i];
				final String constantName = sp.extItem2;

				if(usedConstant.contains(constantName)) {//出现重复定义
					continue;
				}
				usedConstant.add(constantName);
				
				final String shortClassName = sp.extItem1;
				final int classNameLength = shortClassName.length();
				
				{
					//search Class Ref Idx
					int startIdx = sp.endIdx;
					int idx = 0;
					while((idx = scripts.indexOf(shortClassName, startIdx)) >= 0) {
						final int afterIdx = idx + classNameLength;
						startIdx = afterIdx;
						
						if(rememOrString.isInclude(idx)) {
							continue;
						}
						{
							final char beforeByte = chars[idx - 1];
							if(beforeByte >= 'a' && beforeByte <= 'z' 
									|| beforeByte >= 'A' && beforeByte <= 'Z' 
									|| beforeByte >= '0' && beforeByte <= '9' 
									|| beforeByte == '_'
									|| beforeByte == '.'
									|| beforeByte == ':') {
								continue;
							}
						}
						if(afterIdx < charsLen) {//文尾
							final char afterByte = chars[afterIdx];
							if(afterByte >= 'a' && afterByte <= 'z' 
									|| afterByte >= 'A' && afterByte <= 'Z' 
									|| afterByte >= '0' && afterByte <= '9' 
									|| afterByte == '_') {
								continue;
							}
						}
						classRefPositions.addPosition(false, idx, startIdx, shortClassName, constantName, false);
					}
				}
			}
			
			{
				int imIdx = 0, consIdx = 0;
				int jointIdx = 0;
				StringBuilder sb = null;
				ScriptPosition spIm = (imports.count==imIdx?null:imports.positions[imIdx++]);
				ScriptPosition spCons = (classRefPositions.count==consIdx?null:classRefPositions.positions[consIdx++]);
				while(true) {
					if(spIm == null && spCons == null) {
						if(sb == null) {
							return scripts;
						}else {
							sb.append(chars, jointIdx, chars.length - jointIdx);
							
							final String out = sb.toString();
							StringBuilderCacher.cycle(sb);
							return out;
						}
					}
					
					if(sb == null) {
						sb = StringBuilderCacher.getFree();
					}
					
					if(spIm != null && (spCons == null || spIm.startIdx < spCons.startIdx)) {
						sb.append(chars, jointIdx, spIm.startIdx - jointIdx);
						jointIdx = spIm.endIdx;
						
						sb.append(spIm.extItem2);
						sb.append('=');
						sb.append(spIm.item);
						
						spIm = (imports.count==imIdx?null:imports.positions[imIdx++]);
					}else {
						sb.append(chars, jointIdx, spCons.startIdx - jointIdx);
						jointIdx = spCons.endIdx;
						
						sb.append(spCons.extItem1);
						
						spCons = (classRefPositions.count==consIdx?null:classRefPositions.positions[consIdx++]);
					}
				}
			}
		}finally {
			if(imports != null) {
				imports.release();
			}
			if(rememOrString != null) {
				rememOrString.release();
			}
			if(classRefPositions != null) {
				classRefPositions.release();
			}
		}
	}
	
	public static final Object runAndWaitInProjectOrSessionPoolWithRepErr(final J2SESession coreSS, final CallContext runCtx, final StringValue script, final String scriptName, final Map map, final HCJRubyEngine hcje, final ProjectContext context, final Class requireReturnClass) {
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
	
	public static final Object runAndWaitInProjectOrSessionPool(final J2SESession coreSS, final CallContext callCtx, final StringValue script, final String scriptName, final Map map, final HCJRubyEngine hcje, final ProjectContext context) {
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

	public static synchronized final void parse(final CallContext callCtx, final StringValue sv, final String scriptName, final HCJRubyEngine hcje, final boolean isReportException) {
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
	public static final Object runAndWaitOnEngine(final CallContext callCtx, final StringValue sv, final String scriptName, final Map map, final HCJRubyEngine hcje) {
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
		if(map == null){
		}else{
//						LogManager.errToLog("$_hcmap is deprecated, there are serious concurrent risks in it. " +
//								"\nplease remove all parameters in target URL, and set them to attributes of ProjectContext or ClientSession.");
//						hcje.put("$_hcmap", map);
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
		runAndWaitOnEngine(null, new StringValue(script), scriptName, null, hcje);
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