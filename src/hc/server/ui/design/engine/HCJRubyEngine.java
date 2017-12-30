package hc.server.ui.design.engine;

import hc.core.DelayWatcher;
import hc.core.GlobalConditionWatcher;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.data.StoreDirManager;
import hc.server.ui.design.hpj.ConsoleWriter;
import hc.server.ui.design.hpj.RubyWriter;
import hc.util.ClassUtil;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

//import org.jruby.runtime.builtin.IRubyObject;
//import org.jruby.embed.LocalContextScope;
//import org.jruby.embed.LocalVariableBehavior;
//import org.jruby.embed.ScriptingContainer;
//import org.jruby.javasupport.JavaEmbedUtils;
//import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;

public class HCJRubyEngine {
	public static final String IDE_LEVEL_ENGINE = "_IDE_";
	public static final String ORG_JRUBY_EMBED_SCRIPTING_CONTAINER = "org.jruby.embed.ScriptingContainer";
	private static final boolean isAndroidServerPlatform = ResourceUtil.isAndroidServerPlatform();
	public static final String JRUBY_VERSION = "RUBY1_8";//for container and parser
	public static final String JRUBY_PARSE_VERSION = "RUBY1_8";//for container and parser
	
	public final RubyWriter errorWriter;
//	private final HCJRubyEngine engineLock;
//	private final ScriptingContainer container;	
	private Class classScriptingContainer;
	private Object container;	
//	private final HashMap<String, Object> lruCache = new LinkedHashMap<String, Object>(90, 0.75f, true) {
//	    @Override
//	    protected boolean removeEldestEntry(final Map.Entry<String, Object> eldest) {
//	    	return size() > 256;
//	    }
//	};
	
	public final void resetError(){
		errorWriter.reset();
	}
	
	static final Class[] putparaTypes = {String.class, Object.class};
	static final Class[] parseParaTypes = {String.class, int[].class};
	static final Class[] parseStreamParaTypes = {InputStream.class, String.class, int[].class};
	static final int[] zero = {0};
	
	public final void put(final String key, final Object value){
//		container.put(key, value);
		final Object[] paras = {key, value};
//		synchronized (engineLock) {
//			ClassUtil.invoke(classScriptingContainer, container, "put", putparaTypes, paras, false);
//		}
	}
	
	public final void removeCache(final String script){
//		synchronized (engineLock) {
//			lruCache.remove(script);
//		}
	}
	
	final Object parse(final String script, final String scriptName) throws Exception{
		Object unit = null;
//		synchronized (engineLock) {
//			unit = lruCache.remove(script);
//		}
		if(unit == null){
//			EvalUnit unit = container.parse(script);//后面的int是可选参数
			unit = putCompileUnit(script, scriptName);
		}
		return unit;
	}

	private final Object putCompileUnit(final String script, final String scriptName) throws Exception {
		if(isShutdown){
			return null;
		}
		
		final Object unit;
//		final Object currentDirectory = getCurrentDirectory();
//		LogManager.log("JRubyEngine getCurrentDirectory : " + currentDirectory);
//		if(currentDirectory == null){
//			setCurrentDirectory();
//			LogManager.log("JRubyEngine getCurrentDirectory : " + currentDirectory);
//		}
//		LogManager.log("JRubyEngine getHomeDirectory : " + getHomeDirectory());
//		if(isReportException){
////			InputStream istream, String filename, int... lines
//			final InputStream in = new ByteArrayInputStream(StringUtil.getBytes(script));//支持中文
//			scriptName = (scriptName==null||scriptName.length()==0)?"<script>":("<" + scriptName + ">");
////			LogManager.log("compile name : " + scriptName + " for src : " + script);
//			final Object[] para = {in, scriptName, zero};
//			unit = ClassUtil.invokeWithExceptionOut(classScriptingContainer, container, "parse", parseStreamParaTypes, para, false);
//		}else{
			final Object[] para = {script, zero};
			unit = ClassUtil.invokeWithExceptionOut(classScriptingContainer, container, "parse", parseParaTypes, para, false);
//		}
		
		return unit;
	}

	Class classEvalUnit;
	final Class[] emptyParaTypes = {};
	final Object[] emptyPara = {};
	Class classJavaEmbedUtils;
	Class classIRubyObject;
	Class[] rubyToJavaParaTypes;// = {classIRubyObject};
	public final Object runScriptlet(final String script, final String scriptName) throws Throwable{
//        return JavaEmbedUtils.rubyToJava(evalUnitMap.get(script).run());
		final Object evalUnit = parse(script, scriptName);
		if(isShutdown){
			LogManager.log("JRuby Engine is shutdown, skip runing scripts : \n" + script);
			return null;
		}
		L.V = L.WShop ? false : LogManager.log("run in [" + Thread.currentThread().getName() + "], scripts : \n" + script);
		try{
			final Object runOut = ClassUtil.invokeWithExceptionOut(classEvalUnit, evalUnit, "run", emptyParaTypes, emptyPara, false);
			final Object[] para = {runOut};
			final Object result = ClassUtil.invokeWithExceptionOut(classJavaEmbedUtils, classJavaEmbedUtils, "rubyToJava", rubyToJavaParaTypes, para, false);
//			synchronized (engineLock) {
//				lruCache.put(script, evalUnit);
//			}
			return result;
		}catch (final Throwable e) {
			throw e;
		}finally{
			L.V = L.WShop ? false : LogManager.log("finish run in [" + Thread.currentThread().getName() + "]!");
		}
	}
	
	public final Object rubyToJava(final Object obj) throws Exception{
		final Object[] para = {obj};
		return ClassUtil.invokeWithExceptionOut(classJavaEmbedUtils, classJavaEmbedUtils, "rubyToJava", rubyToJavaParaTypes, para, false);
	}
	
	boolean isShutdown;

	public final void terminate(){
		isShutdown = true;
		
		GlobalConditionWatcher.addWatcher(new DelayWatcher(1000) {//延时，不影响当前可能正在的任务
			@Override
			public void doBiz() {
				ClassUtil.invoke(classScriptingContainer, container, "clear", emptyParaTypes, emptyPara, false);
//				try{
//					container.clear();
//				}catch (Throwable e) {
//				}

				ClassUtil.invoke(classScriptingContainer, container, "terminate", emptyParaTypes, emptyPara, false);
//				try{
//					container.terminate();
//				}catch (Throwable e) {
//				}
				
//				PlatformManager.getService().closeLoader(projClassLoader);
			}
		});

	}
	
	private final ClassLoader projClassLoader;
	public final String projectIDMaybeBeginWithIDE;
	
	public ClassLoader getProjClassLoader(){
		return projClassLoader;
	}
	
	private static boolean isInit = false;

	/**
	 * 
	 * @param absPath 可以为null
	 * @param projClassLoader
	 */
	public HCJRubyEngine(final ConsoleWriter displayWriterMaybeNull, final String absPath, final ClassLoader projClassLoader, final boolean isReportException, final String projectID){
		this.projectIDMaybeBeginWithIDE = projectID;
//		engineLock = this;
		errorWriter = new RubyWriter(displayWriterMaybeNull);
		
		if(isInit == false){
			isInit = true;
			
			if(isAndroidServerPlatform){
				final File optimizBaseDir = PlatformManager.getService().getJRubyAndroidOptimizBaseDir();

				//System.setProperty("jruby.compile.mode", "OFF"); // OFF OFFIR JITIR? FORCE FORCEIR
		        //System.setProperty("jruby.compile.backend", "DALVIK");
		        System.setProperty("jruby.bytecode.version", "1.6");
//		        System.setProperty("jruby.interfaces.useProxy", "true");
//		        System.setProperty("jruby.management.enabled", "false");
//		        System.setProperty("jruby.objectspace.enabled", "false");
//		        System.setProperty("jruby.thread.pooling", "true");
//		        System.setProperty("jruby.native.enabled", "true");//不能开启，ruboto-core-1.0.5.apk，会出现jnr-ffi
//		        System.setProperty("jruby.native.verbose", "true");
//		         System.setProperty("jruby.compat.version", "RUBY1_8");
//		        System.setProperty("jruby.ir.passes", "LocalOptimizationPass,DeadCodeElimination");
//		        System.setProperty("jruby.backtrace.style", "normal"); // normal raw full mri

		        // Uncomment these to debug/profile Ruby source loading
		        // System.setProperty("jruby.debug.loadService", "true");
		        // System.setProperty("jruby.debug.loadService.timing", "true");

	            // Used to enable JRuby to generate proxy classes
	            System.setProperty("jruby.ji.proxyClassFactory", "org.ruboto.DalvikProxyClassFactory");
	            System.setProperty("jruby.ji.upper.case.package.name.allowed", "true");
	    		//the following property is for System.setProperty("jruby.ji.proxyClassFactory", "org.ruboto.DalvikProxyClassFactory");
	   		 
		        {
		        	final File proxyCache = new File(optimizBaseDir, "ruboto_cache");
		        	proxyCache.mkdirs();
		        	System.out.println("creaet dir : " + proxyCache.getAbsolutePath());
		        	System.setProperty("jruby.class.cache.path", proxyCache.getAbsolutePath());
		        }
		        
	            System.setProperty("jruby.backtrace.style", "normal"); // normal raw full mri
	            System.setProperty("jruby.bytecode.version", "1.6");
	            // BEGIN Ruboto RubyVersion
	            // System.setProperty("jruby.compat.version", "RUBY2_0"); 
	            // END Ruboto RubyVersion
	            // System.setProperty("jruby.compile.backend", "DALVIK");
//	            System.setProperty("jruby.compile.mode", "OFF"); // OFF OFFIR JITIR? FORCE FORCEIR
	            System.setProperty("jruby.interfaces.useProxy", "true");
	            System.setProperty("jruby.ir.passes", "LocalOptimizationPass,DeadCodeElimination");
	            System.setProperty("jruby.management.enabled", "false");
	            System.setProperty("jruby.native.enabled", "false");
	            System.setProperty("jruby.objectspace.enabled", "false");
	            System.setProperty("jruby.rewrite.java.trace", "true");
	            System.setProperty("jruby.thread.pooling", "true");

	            // Uncomment these to debug/profile Ruby source loading
	            // Analyse the output: grep "LoadService:   <-" | cut -f5 -d- | cut -c2- | cut -f1 -dm | awk '{total = total + $1}END{print total}'
	            // System.setProperty("jruby.debug.loadService", "true");
	            // System.setProperty("jruby.debug.loadService.timing", "true");
			}
		}
		
		this.projClassLoader = projClassLoader;
		
		try{
		classJavaEmbedUtils = projClassLoader.loadClass("org.jruby.javasupport.JavaEmbedUtils");
		classEvalUnit = projClassLoader.loadClass("org.jruby.javasupport.JavaEmbedUtils$EvalUnit");
		classIRubyObject = projClassLoader.loadClass("org.jruby.runtime.builtin.IRubyObject");
		final Class[] array = {classIRubyObject};
		rubyToJavaParaTypes = array;

//        ScriptingContainer container = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
//        container.runScriptlet("p=9.0");
//        container.runScriptlet("q = Math.sqrt p");
//        container.runScriptlet("puts \"square root of #{p} is #{q}\"");
//        System.out.println("Ruby used values: p = " + container.get("p") +
//              ", q = " + container.get("q"));
//		 Produces:
//			 square root of 9.0 is 3.0
//			 Ruby used values: p = 9.0, q = 3.0
		
		//不能使用LocalVariableBehavior.TRANSIENT，会导致多次调用不能共享变量，参见上段演示代码
//		(String absPath, ClassLoader projClassLoader, final ScriptingContainer c){
//		new ScriptingContainer(LocalContextScope.SINGLETHREAD, 
//				LocalVariableBehavior.TRANSIENT, true);//keep local variables across multiple evaluations
//		LocalVariableBehavior.PERSISTENT;

		final Class classLocalContextScope = projClassLoader.loadClass("org.jruby.embed.LocalContextScope");
		final Class classLocalVariableBehavior = projClassLoader.loadClass("org.jruby.embed.LocalVariableBehavior");
		
		classScriptingContainer = projClassLoader.loadClass(ORG_JRUBY_EMBED_SCRIPTING_CONTAINER);
		final Class[] construParaTypes = {classLocalContextScope, classLocalVariableBehavior, boolean.class};
		final Constructor constr = classScriptingContainer.getConstructor(construParaTypes);
		final Object[] constrPara = {
				ClassUtil.getField(classLocalContextScope, classLocalContextScope, "SINGLETHREAD", false, true),//SINGLETHREAD : SingleThreadLocalContextProvider.getRuntime().
				ClassUtil.getField(classLocalVariableBehavior, classLocalVariableBehavior, "TRANSIENT", false, true),//TRANSIENT
				Boolean.TRUE};
		container = constr.newInstance(constrPara);
		
		//container.setAttribute(AttributeName.SHARING_VARIABLES, Boolean.FALSE);
		{
			final Class AttributeName = projClassLoader.loadClass("org.jruby.embed.AttributeName");
			final Class[] setAttributeTypes = {Object.class, Object.class};
			final Object[] setAttributePara = {
					ClassUtil.getField(AttributeName, AttributeName, "SHARING_VARIABLES", false, true),
					Boolean.FALSE};
			ClassUtil.invoke(classScriptingContainer, container, "setAttribute", setAttributeTypes, setAttributePara, false);
		}
		
//		System.out.println("getHomeDirectory : " + getHomeDirectory());
//		System.out.println("getCurrentDirectory : " + getCurrentDirectory());
		
//		container.setProfile(org.jruby.Profile.DEBUG_ALLOW);
//		{
//			final Class profile = projClassLoader.loadClass("org.jruby.Profile");
//			final Object debugAllow = ClassUtil.getField(profile, profile, "DEBUG_ALLOW", false);
//			final Class[] paraTypes = {profile};
//			final Object[] para = {debugAllow};
//			ClassUtil.invoke(classScriptingContainer, container, "setProfile", paraTypes, para, false);
//		}
		
//		container.setCompatVersion(org.jruby.CompatVersion.RUBY2_0);
//		if(isAndroidServerPlatform)
		{
			final Class compatVersionClass = projClassLoader.loadClass("org.jruby.CompatVersion");
			final Object rubyVersion = ClassUtil.getField(compatVersionClass, compatVersionClass, JRUBY_VERSION, false, true);
			final Class[] paraTypes = {compatVersionClass};
			final Object[] para = {rubyVersion};
			
			ClassUtil.invoke(classScriptingContainer, container, "setCompatVersion", paraTypes, para, false);
			if(ResourceUtil.isLoggerOn() == false){
				LogManager.warning("org.jruby.CompatVersion : " + JRUBY_VERSION);
			}
		}

//		container.setCompileMode(org.jruby.RubyInstanceConfig$CompileMode.FORCE);
//		if(isReportException){
//			Object compileMode;
//			final Class compileModeClass = projClassLoader.loadClass("org.jruby.RubyInstanceConfig$CompileMode");
//////			TRUFFLE
////			compileMode = ClassUtil.getField(compileModeClass, compileModeClass, "TRUFFLE", false, false);
////			if(compileMode != null){
////				LogManager.log("JRubyEngine compileMode : TRUFFLE");
////			}else{
//				if(isAndroidServerPlatform){
//					compileMode = ClassUtil.getField(compileModeClass, compileModeClass, "JIT", false, true);
////					LogManager.log("JRubyEngine compileMode : JIT");//多实例，无意义
//				}else{
//					compileMode = ClassUtil.getField(compileModeClass, compileModeClass, "FORCE", false, true);//in Android, 1.8，2.0均不能parse(AddHAR)
////					LogManager.log("JRubyEngine compileMode : FORCE");//多实例，无意义
//				}
////			}
//			final Class[] paraTypes = {compileModeClass};
//			final Object[] para = {compileMode};
//			ClassUtil.invoke(classScriptingContainer, container, "setCompileMode", paraTypes, para, false);
//		}
		
		if(absPath != null && absPath.length() > 0){
			final ArrayList<String> loadPaths = new ArrayList<String>(1);
			loadPaths.add(absPath);
//			container.setLoadPaths(loadPaths);//Changes a list of load paths Ruby scripts/libraries
			final Class[] paraTypes = {List.class};
			final Object[] para = {loadPaths};
			ClassUtil.invoke(classScriptingContainer, container, "setLoadPaths", paraTypes, para, false);
		}
		
//		container.setCurrentDirectory(absPath);
		if(absPath != null){//脚本工作目录，比如JRuby对象File, Dir生成文件
			final String currDir;
			if(absPath == StoreDirManager.RUN_TEST_ABS_PATH){
				currDir = StoreDirManager.RUN_TEST_ABS_PATH;
			}else{
				currDir = StoreDirManager.getUserDataBaseDir(projectID);
			}
			
			final Class[] paraType = {String.class};
			final Object[] para = {currDir};
			ClassUtil.invoke(classScriptingContainer, container, "setCurrentDirectory", paraType, para, false);
		}
		
//		container.setError(errorWriter);
		{
			final Class[] paraType = {Writer.class};
			final Object[] para = {errorWriter};
			ClassUtil.invoke(classScriptingContainer, container, "setError", paraType, para, false);
		}
//		container.setOutput(errorWriter);
		if(displayWriterMaybeNull != null){
			final Class[] paraType = {Writer.class};
			final Object[] para = {displayWriterMaybeNull};
			ClassUtil.invoke(classScriptingContainer, container, "setOutput", paraType, para, false);
		}
		
//		container.setClassLoader(projClassLoader);
		final Class[] paraType = {ClassLoader.class};
		final Object[] para = {projClassLoader};
		ClassUtil.invoke(classScriptingContainer, container, "setClassLoader", paraType, para, false);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		
//		container.getRuntime();
		try{
			//强制初始化，因为在用这线程会导致block PropertyPermission : (java.util.PropertyPermission java.net.preferIPv4Stack write) in HAR Project.
			classScriptingContainer.getDeclaredMethod("getRuntime", null).invoke(container, null);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		
		if(isAndroidServerPlatform){
			PlatformManager.getService().doExtBiz(PlatformService.BIZ_INIT_RUBOTO_ENVIROMENT, container);
		}
	}

	private Object getCurrentDirectory() {
		return ClassUtil.invoke(classScriptingContainer, container, "getCurrentDirectory", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS, false);
	}

	private Object getHomeDirectory() {
		return ClassUtil.invoke(classScriptingContainer, container, "getHomeDirectory", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS, false);
	}
}
