package hc.server.ui.design.engine;

import hc.core.util.LRUCache;
import hc.server.ui.design.hpj.HCScriptException;
import hc.server.ui.design.hpj.RubyWriter;
import hc.util.ClassUtil;

import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

//import org.jruby.runtime.builtin.IRubyObject;
//import org.jruby.embed.LocalContextScope;
//import org.jruby.embed.LocalVariableBehavior;
//import org.jruby.embed.ScriptingContainer;
//import org.jruby.javasupport.JavaEmbedUtils;
//import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;

public class HCJRubyEngine {
	public boolean isError = false;
	public final RubyWriter errorWriter = new RubyWriter();
	public final HCScriptException sexception = new HCScriptException("");
//	private final ScriptingContainer container;	
	private Class classScriptingContainer;
	private Object container;	
	private final LRUCache lruCache = new LRUCache(64);
	
	final Class[] putparaTypes = {String.class, Object.class};
	public final void put(final String key, final Object value){
//		container.put(key, value);
		final Object[] paras = {key, value};
		synchronized (this) {
			ClassUtil.invoke(classScriptingContainer, container, "put", putparaTypes, paras, false);
		}
	}
	
	public final void removeCache(final String script){
		synchronized (this) {
			lruCache.remove(script);
		}
	}
	
	final Class[] parseParaTypes = {String.class, int[].class};
	final int[] zero = {0};
	public final synchronized Object parse(final String script) throws Exception{
		Object unit;
		unit = lruCache.get(script);
		if(unit == null){
//			EvalUnit unit = container.parse(script);//后面的int是可选参数
			final Object[] para = {script, zero};
			unit = ClassUtil.invokeWithExceptionOut(classScriptingContainer, container, "parse", parseParaTypes, para, false);
			lruCache.put(script, unit);
		}
		return unit;
	}
	
	Class classEvalUnit;
	final Class[] emptyParaTypes = {};
	final Object[] emptyPara = {};
	Class classJavaEmbedUtils;
	Class classIRubyObject;
	Class[] rubyToJavaParaTypes;// = {classIRubyObject};
	public final synchronized Object runScriptlet(final String script) throws Exception{
//        return JavaEmbedUtils.rubyToJava(evalUnitMap.get(script).run());
		Object evalUnit;
		evalUnit = lruCache.get(script);
		if(evalUnit == null){
			final Object[] para = {script, zero};
			evalUnit = ClassUtil.invokeWithExceptionOut(classScriptingContainer, container, "parse", parseParaTypes, para, false);
			lruCache.put(script, evalUnit);
		}
		try{
			final Object runOut = ClassUtil.invokeWithExceptionOut(classEvalUnit, evalUnit, "run", emptyParaTypes, emptyPara, false);
			final Object[] para = {runOut};
			return ClassUtil.invokeWithExceptionOut(classJavaEmbedUtils, classJavaEmbedUtils, "rubyToJava", rubyToJavaParaTypes, para, false);
		}catch (final Exception e) {
			lruCache.remove(script);//执行错误后，需重新编译
			throw e;
		}
	}

	/**
	 * 没有错误，返回null
	 * @return
	 */
	public final ScriptException getEvalException(){
		return isError?sexception:null;
	}
	
	public final void terminate(){
		ClassUtil.invoke(classScriptingContainer, container, "clear", emptyParaTypes, emptyPara, false);
//		try{
//			container.clear();
//		}catch (Throwable e) {
//		}

		ClassUtil.invoke(classScriptingContainer, container, "terminate", emptyParaTypes, emptyPara, false);
//		try{
//			container.terminate();
//		}catch (Throwable e) {
//		}
		
//		try{
//			container.getVarMap().clear();
//		}catch (Throwable e) {
//		}
		
	}
	
	private ClassLoader projClassLoader;
	
	public ClassLoader getProjClassLoader(){
		return projClassLoader;
	}

	/**
	 * 
	 * @param absPath 可以为null
	 * @param projClassLoader
	 */
	public HCJRubyEngine(final String absPath, final ClassLoader projClassLoader){
		try{
		this.projClassLoader = projClassLoader;
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
		
		classScriptingContainer = projClassLoader.loadClass("org.jruby.embed.ScriptingContainer");
		final Class[] construParaTypes = {classLocalContextScope, classLocalVariableBehavior, boolean.class};
		final Constructor constr = classScriptingContainer.getConstructor(construParaTypes);
		final Object[] constrPara = {
				ClassUtil.getField(classLocalContextScope, classLocalContextScope, "SINGLETHREAD", false),//SINGLETHREAD : ScriptingContainer will not do any multiplexing at all.
				ClassUtil.getField(classLocalVariableBehavior, classLocalVariableBehavior, "TRANSIENT", false),//TRANSIENT
				Boolean.TRUE};
		container = constr.newInstance(constrPara);
		
		if(absPath != null && absPath.length() > 0){
			final ArrayList<String> loadPaths = new ArrayList<String>(1);
			loadPaths.add(absPath);
//			container.setLoadPaths(loadPaths);//Changes a list of load paths Ruby scripts/libraries
			final Class[] paraTypes = {List.class};
			final Object[] para = {loadPaths};
			ClassUtil.invoke(classScriptingContainer, container, "setLoadPaths", paraTypes, para, false);
		}
		
//		container.setCurrentDirectory(absPath);
		{
			final Class[] paraType = {String.class};
			final Object[] para = {absPath};
			ClassUtil.invoke(classScriptingContainer, container, "setCurrentDirectory", paraType, para, false);
		}
		
//		container.setError(errorWriter);
		{
			final Class[] paraType = {Writer.class};
			final Object[] para = {errorWriter};
			ClassUtil.invoke(classScriptingContainer, container, "setError", paraType, para, false);
		}
		
//		container.setClassLoader(projClassLoader);
		final Class[] paraType = {ClassLoader.class};
		final Object[] para = {projClassLoader};
		ClassUtil.invoke(classScriptingContainer, container, "setClassLoader", paraType, para, false);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
}
