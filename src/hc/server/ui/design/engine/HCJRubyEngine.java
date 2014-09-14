package hc.server.ui.design.engine;

import hc.core.util.LogManager;
import hc.server.J2SEContext;
import hc.server.ui.design.hpj.HCScriptException;
import hc.server.ui.design.hpj.RubyWriter;
import hc.util.ResourceUtil;

import java.io.File;
import java.util.ArrayList;

import javax.script.ScriptException;
import javax.swing.JOptionPane;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

public class HCJRubyEngine {
	public boolean isError = false;
	public final RubyWriter errorWriter = new RubyWriter();
	public final HCScriptException sexception = new HCScriptException("");
	private static boolean isLoadedJrubyJar = false;
	private final ScriptingContainer container;	
	
	public void put(String key, Object value){
		container.put(key, value);
	}
	
	public Object runScriptlet(String script){
		return container.runScriptlet(script);
	}

	/**
	 * 没有错误，返回null
	 * @return
	 */
	public ScriptException getEvalException(){
		return isError?sexception:null;
	}
	
	public void terminate(){
		try{
			container.terminate();
		}catch (Throwable e) {
		}
		
		try{
			container.getVarMap().clear();
		}catch (Throwable e) {
		}
		
		try{
			container.clear();
		}catch (Throwable e) {
		}
	}

	public HCJRubyEngine(String paths){
		if(HCJRubyEngine.isLoadedJrubyJar == false){
			try {
				ResourceUtil.loadJar(new File(J2SEContext.jrubyjarname));
			} catch (Throwable e) {
				e.printStackTrace();
				LogManager.err( "Error load lib " + J2SEContext.jrubyjarname);
				
				new Thread(){
					public void run(){
						JOptionPane.showMessageDialog(null, "Load JRuby lib error!", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}.start();
			}
			HCJRubyEngine.isLoadedJrubyJar = true;
		}
	
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
		container = new ScriptingContainer(LocalContextScope.THREADSAFE, 
				LocalVariableBehavior.PERSISTENT, true);
		
		if(paths != null && paths.length() > 0){
			ArrayList<String> loadPaths = new ArrayList<String>(1);
			loadPaths.add(paths);
			container.setLoadPaths(loadPaths);
		}
		container.setCurrentDirectory(paths);
		container.setError(errorWriter);
	}
}
