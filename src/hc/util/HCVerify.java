package hc.util;

import java.io.File;
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
	 * @param fileName
	 *            非DER格式
	 * @return
	 */
	private static X509Certificate readPublicKeyFromPEM(final File file) {
		try {
			final FileInputStream inStream = new FileInputStream(file);
			final CertificateFactory factory = CertificateFactory.getInstance("X.509");
			final X509Certificate cert = (X509Certificate) factory.generateCertificate(inStream);
			return cert;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean verifyJar(final String filename, final X509Certificate trustedSigner) {
		JarFile jf = null;
		try {
			jf = new JarFile(filename, true);
			final byte[] buf = new byte[4096];

			final Manifest manifest = jf.getManifest();
			if (manifest == null)
				return false;

			final Enumeration<JarEntry> ent = jf.entries();
			while (ent.hasMoreElements()) {
				final JarEntry je = ent.nextElement();
				final InputStream is = jf.getInputStream(je);
				int n;
				try {
					while ((n = is.read(buf, 0, buf.length)) != -1) {
					}
				} finally {
					is.close();
				}

				if (je.isDirectory())
					continue;

				final Certificate[] certs = je.getCertificates();
				if ((certs == null) || (certs.length == 0)) {
					if (!je.getName().startsWith("META-INF", 0))
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
		} catch (final Throwable e) {
		} finally {
			try {
				jf.close();
			} catch (final Throwable e) {
			}
		}
		return false;
	}

	public static X509Certificate getCert() {
		final byte[] bs = { 'h', 'c', '.', 'p', 'e', 'm' };
		final String fileName = new String(bs);
		final X509Certificate c = readPublicKeyFromPEM(
				new File(ResourceUtil.getBaseDir(), fileName));// 注意：此方式不适合于Android服务器
		if (c == null) {
			HCVerify.isPassVerify = false;
		}
		return c;
	}
}
