package hc.server.ui.design.hpj;

import hc.core.IConstant;
import hc.core.util.HCURL;
import hc.server.AbstractDelayBiz;
import hc.server.DelayServer;
import hc.server.ui.ProjectContext;
import hc.server.ui.ProjectContextManager;
import hc.server.ui.design.engine.HCJRubyEngine;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RubyExector {
	private static Pattern linePattern = Pattern.compile("<script>:(\\d+)");
	
	public static void runLater(final String script, final Map map, final HCJRubyEngine hcje, final ProjectContext context){
		DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
			@Override
			public void doBiz() {
				RubyExector.run(script, map, hcje, context);
			}
		});
	}
	public static final Object run(final String script, final Map map, final HCJRubyEngine hcje, final ProjectContext context) {
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
			ProjectContextManager.setThreadObject(context);
			return hcje.runScriptlet(script);
			
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
			
		} catch (Throwable e) {
			
			String err = hcje.errorWriter.getMessage();
			hcje.isError = true;
			
//			L.V = L.O ? false : LogManager.log("JRuby Script Error : " + err);
			
			char[] chars = err.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				if(chars[i] == '\n'){
					hcje.sexception.setMessage(new String(chars, 0, i));
					break;
				}
			}
			
			Matcher matcher = linePattern.matcher(err);
			if(matcher.find()){
				final String group = matcher.group(1);
				hcje.sexception.setLineNumber(Integer.parseInt(group));
			}
			return null;
		}finally{
//			System.setProperty(USER_DIR_KEY, userDir);
		}
	}

	public static Map<String, String> toMap(final HCURL _hcurl) {
		Map<String, String> map = null;
		final int size = _hcurl.getParaSize();
		if(size > 0){
			map = new HashMap<String, String>();
			for (int i = 0; i < size; i++) {
				final String key = _hcurl.getParaAtIdx(i);
				try {
					map.put(key, URLDecoder.decode(_hcurl.getValueofPara(key), IConstant.UTF_8));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}
	
	
}