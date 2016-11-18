package hc.j2se;

import hc.core.RootServerConnector;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;

import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * java6中，支持SHA-1、SHA-256、SHA-384、SHA-512四种算法
 *
 */
public class HCAjaxX509TrustManager{

	private static X509Certificate readPublicKeyForAjax() {
		try{
			final CertificateFactory factory = CertificateFactory.getInstance("X.509");
			final String ajaxX509Path = (String)RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_AJAX_X509_PATH, null);
			final X509Certificate cert = (X509Certificate) factory
					.generateCertificate(HCAjaxX509TrustManager.class.getResourceAsStream(ajaxX509Path));
			return cert;
		}catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	final static SSLSocketFactory ajaxSslSocketFactory = buildSSLSocketFactory(getKeyStoreManager());
	final static HostnameVerifier ajaxHostVerfier = new HostnameVerifier() {
		@Override
		public boolean verify(final String host, final SSLSession arg1) {
			//arg1.getPeerCertificates()[0].equals(other)
			return RootServerConnector.checkAjaxSSLDomain(host);
		}
	};
	
	public static SSLSocketFactory getAjaxSSLSocketFactory(){
		return ajaxSslSocketFactory;
	}
	
	public static HostnameVerifier getAjaxHostVerfier(){
		return ajaxHostVerfier;
	}
	
	private static TrustManager getKeyStoreManager(){
		try{
			final String keyStoreType = KeyStore.getDefaultType();
			final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", readPublicKeyForAjax());
			 
			final String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
			tmf.init(keyStore);
			
			return tmf.getTrustManagers()[0];
		}catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static SSLSocketFactory buildSSLSocketFactory(final TrustManager manager) {
		SSLContext context = null;
		
//		SSL	Supports some version of SSL; may support other versions
//		SSLv2	Supports SSL version 2 or higher; may support other versions
//		SSLv3	Supports SSL version 3; may support other versions
//		TLS	Supports some version of TLS; may support other versions
//		TLSv1	Supports RFC 2246: TLS version 1.0 ; may support other versions
//		TLSv1.1	Supports RFC 4346: TLS version 1.1 ; may support other versions
		
		final String[] algorithms = {"SSL", "SSLv2", "SSLv3", "TLS", "TLSv1", "TLSv1.1"};
		
		for (int i = algorithms.length - 1; i >= 0; i--) {
			try{
				final String algo = algorithms[i];
				context = SSLContext.getInstance(algo);
				break;
			}catch (final Exception e) {
			}
		}
		if(context == null){
			LogManager.errToLog("No SSLContext algorithms for SSL.");
			return null;
		}
		
		try{
			final TrustManager[] tm = { manager };
			  
			context.init(null, tm, null);
			
			final SSLSocketFactory ssf = context.getSocketFactory();
			return ssf;
		}catch (final Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
	
	public static void initSSLSocketFactory() {
		HttpsURLConnection.setDefaultSSLSocketFactory(buildSSLSocketFactory(new TrustAllManager()));
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(final String hostname, final SSLSession session) {
				return true;
			}
		});
	}
	
	public static void setAjaxSSLSocketFactory(final URL url, final URLConnection conn) {
		if(conn instanceof HttpsURLConnection){
			if(url.getPort() == RootServerConnector.PORT_44X 
					&& url.getProtocol().equals("https")){//getProtocal会转为小写
				final String host = url.getHost();
				if(	RootServerConnector.checkAjaxSSLDomain(host)){
//					System.out.println("======> use getAjaxSSLSocketFactory");
					((HttpsURLConnection)conn).setSSLSocketFactory(getAjaxSSLSocketFactory());
					((HttpsURLConnection)conn).setHostnameVerifier(getAjaxHostVerfier());
				}
			}
		}
	}

}

class TrustAllManager implements TrustManager, X509TrustManager{
	@Override
	public void checkClientTrusted(final X509Certificate[] arg0, final String arg1)
			throws CertificateException {
//		L.V = L.O ? false : LogManager.log("TrustAllManager checkClientTrusted");
	}

	@Override
	public void checkServerTrusted(final X509Certificate[] chain, final String authType)
			throws CertificateException {
//		L.V = L.O ? false : LogManager.log("TrustAllManager checkServerTrusted");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
//		L.V = L.O ? false : LogManager.log("TrustAllManager getAcceptedIssuers");
		return null;
	}
	
}
