package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.ExceptionJSON;
import hc.core.util.ExceptionJSONBuilder;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.RootBuilder;
import hc.j2se.HCAjaxX509TrustManager;
import hc.j2se.J2SEExceptionJSONBuilder;
import hc.server.MultiUsingManager;
import hc.server.StarterManager;
import hc.server.rms.RMSLastAccessTimeManager;
import hc.server.ui.design.J2SESession;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class J2SERootBuilder extends RootBuilder {
	final ThreadGroup token;
	
	public J2SERootBuilder(final ThreadGroup token) {
		this.token = token;
	}
	
	@Override
	public final void reportException(final ExceptionJSON json){
		RMSLastAccessTimeManager.save();//故障可能导致数据丢失前，进行一次保存。
		
		final boolean forTest = json.isForTest;
		HttpURLConnection connection = null;
		DataOutputStream out = null;
		try {
			String urlStr = json.getToURL();
			urlStr = HttpUtil.replaceSimuURL(urlStr, PropertiesManager.isSimu());
			
			final String email = json.getAttToEmail();
			
			if(forTest){
				System.out.println("[test] report exception to : " + (email!=null?email:urlStr));
			}
			// 创建连接
			final URL url = new URL(urlStr);
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoOutput(true);
			connection.setConnectTimeout(15000);
			if(forTest){
				connection.setReadTimeout(15000);
				connection.setDoInput(true);//for test only
			}
			
			HCAjaxX509TrustManager.setAjaxSSLSocketFactory(url, connection);

			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			
			connection.setRequestProperty("Content-Type", ExceptionJSON.APPLICATION_JSON_CHARSET_UTF_8);
			connection.connect();
			
			// POST请求
			out = new DataOutputStream(connection.getOutputStream());
			out.write(json.getJSONBytesCache());
			out.flush();

			//--------------------------以下接收响应--------------------------
			final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String lines;
			final StringBuffer sb = new StringBuffer(1024);
			
			sb.append("[test] response of report exception (the response will be ignore if NOT test) :\n");
			while ((lines = reader.readLine()) != null) {
				lines = new String(lines.getBytes(), "utf-8");
				sb.append(lines);
				sb.append("\n");
			}
			reader.close();//必须接收，否则发送不成功!
			
			if (forTest && email == null) {
				L.V = L.O ? false : LogManager.log(sb.toString());
			}
		} catch (final Throwable e) {
			// 不处理异常
			if(forTest){
				e.printStackTrace();
			}
		}finally{
			try{
				out.close();
			}catch (final Throwable e) {
			}
			try{
				connection.disconnect();
			}catch (final Throwable e) {
			}
		}
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
	public String getAjaxForSimu(final String url) {
		return HttpUtil.getAjaxForSimu(url);
	}

	@Override
	public Object doBiz(final int rootBizNo, final Object para) {
		 if(rootBizNo == ROOT_BIZ_AJAX_X509_PATH){
			 return "/hc/res/ajax.der";
		 }else if(rootBizNo == ROOT_BIZ_IS_SIMU){
			 return PropertiesManager.isSimu();
		 }else if(rootBizNo == ROOT_BIZ_CHECK_STACK_TRACE){
			 ResourceUtil.checkHCStackTraceInclude(null, null);
		 }else if(rootBizNo == ROOT_RELEASE_EXT_J2SE){
			 ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					try{
						Thread.sleep(1000);
					}catch (final Exception e) {
					}
					 MultiUsingManager.release((J2SESession)para);//需要延后，因为等待先调用MultiUsingWarning.exit()
				}
			});
		 }
		 
		 return null;
	}
}
