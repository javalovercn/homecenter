package hc.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class HCVerify {
	public static boolean isPassVerify = true;
	
	/**
	 * 
	 * @param fileName 非DER格式
	 * @return
	 */
	private static X509Certificate readPublicKeyFromPEM(String fileName) {
		try{
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) factory
					.generateCertificate(new FileInputStream(fileName));
			return cert;
		}catch (Exception e) {
		}
		return null;
	}

	public static boolean verifyJar(String filename, X509Certificate trustedSigner) {
		JarFile jf = null;
		try{
			jf = new JarFile(filename);
			final byte[] buf = new byte[4096];
	
			Manifest manifest = jf.getManifest();
			if (manifest == null)
				return false;
	
			final Enumeration<JarEntry> ent = jf.entries();
			while (ent.hasMoreElements()) {
				JarEntry je = ent.nextElement();
				InputStream is = jf.getInputStream(je);
				int n;
				try {
					while ((n = is.read(buf, 0, buf.length)) != -1) {
					}
				} finally {
					is.close();
				}
	
				if (je.isDirectory())
					continue;
	
				Certificate[] certs = je.getCertificates();
				if ((certs == null) || (certs.length == 0)) {
					if (!je.getName().startsWith("META-INF"))
						return false;
				} else {
					boolean signedByHC = false;
					for (int i = 0; i < certs.length; i++) {
						if (trustedSigner.equals(certs[i])) {
							signedByHC = true;
							break;
						}
					}
					
					if (!signedByHC) {
						return false;
					}
				}
			}
	
			return true;
		}catch (Throwable e) {
		}finally{
			try {
				jf.close();
			} catch (Throwable e) {
			}
		}
		return false;
	}

	public static X509Certificate getCert(){
		byte[] bs = {'h', 'c', '.', 'p', 'e', 'm'};
		final String fileName = new String(bs);
		X509Certificate c = readPublicKeyFromPEM(fileName);
		if(c == null){
			HCVerify.isPassVerify = false;
		}
		return c;
	}
}
