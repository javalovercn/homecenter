package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.ExceptionJSONBuilder;
import hc.core.util.ReturnableRunnable;
import hc.core.util.RootBuilder;
import hc.j2se.J2SEExceptionJSONBuilder;
import hc.server.StarterManager;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;

public class J2SERootBuilder extends RootBuilder {
	final ThreadGroup token;
	
	public J2SERootBuilder(final ThreadGroup token) {
		this.token = token;
	}
	
	@Override
	public ExceptionJSONBuilder getExceptionJSONBuilder() {
		return new J2SEExceptionJSONBuilder(){
			String jrubyVersion;

			@Override
			public final String getHCVersion() {
				return StarterManager.getHCVersion();
			}

			@Override
			public final String getJREVer() {
				return String.valueOf(App.getJREVer());
			}
			
			@Override
			public final String getJRubyVer() {
				if(jrubyVersion == null){
					jrubyVersion = (String)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
						@Override
						public Object run() {
							return PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);//有可能为null
						}
					}, token);
				}
				return jrubyVersion;
			}
		};
	}

	@Override
	public String getAjax(final String url) {
		return HttpUtil.getAjax(url);
	}

	@Override
	public String getAjaxForSimu(final String url, final boolean isTCP) {
		return HttpUtil.getAjaxForSimu(url, false);
	}
}
