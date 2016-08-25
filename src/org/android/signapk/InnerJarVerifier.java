/**
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.android.signapk;

import hc.core.L;

import java.io.File;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class InnerJarVerifier extends AbstractJarVerifier {

	@Override
	public X509Certificate[] verifyJar(final File jarFile, X509Certificate[] trustedSigner) {
		if(trustedSigner == null){
			trustedSigner = new X509Certificate[0];
		}
		
		final int signedLength = trustedSigner.length;

		Certificate[] newCerts = null;
		
		JarFile jf = null;
		try{
			final byte[] buf = new byte[4096];

			jf = new JarFile(jarFile, true);
	
//			final Manifest manifest = jf.getManifest();
//			if (manifest == null)
//				return null;//注意：早期包是不带Manifest
	
			final Enumeration<JarEntry> ent = jf.entries();
			while (ent.hasMoreElements()) {
				final JarEntry je = ent.nextElement();
	
				if (je.isDirectory()){
					continue;
				}
		        if (je.getName().startsWith("META-INF/", 0)) {
		            continue;
		        }
		        
		        final InputStream is = jf.getInputStream(je);
				int n;
				try {
					while ((n = is.read(buf, 0, buf.length)) != -1) {
					}
				} finally {
					is.close();
				}
				
				newCerts = je.getCertificates();// now we can fetch security stuff;
				if ((newCerts == null) || (newCerts.length == 0)) {
					if(signedLength > 0){
						return null;
					}
				} else {
					if(signedLength > 0){
						boolean signedByHC = false;
						for (int i = 0; i < newCerts.length && signedByHC == false; i++) {
							for (int j = 0; j < signedLength; j++) {
								if (trustedSigner[j].equals(newCerts[i])) {
									signedByHC = true;
									break;
								}
							}
						}
						
						if (!signedByHC) {
							return null;
						}
					}
				}
			}
	
			if(newCerts == null){
				return new X509Certificate[0];
			}else{
				final int len = newCerts.length;
				final X509Certificate[] x509 = new X509Certificate[len];
				for (int i = 0; i < len; i++) {
					x509[i] = (X509Certificate)newCerts[i];
				}
				return x509;
			}
		}catch (final Throwable e) {
			if(L.isInWorkshop){
				e.printStackTrace();//no display verified error
			}
		}finally{
			try {
				jf.close();
			} catch (final Throwable e) {
			}
		}
		return null;
	}

}
