/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.android.signapk;

import hc.core.L;
import hc.server.util.SignHelper;
import hc.util.ResourceUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

/**
 * Non-public class used by {@link JarFile} and {@link JarInputStream} to manage
 * the verification of signed JARs. {@code JarFile} and {@code JarInputStream}
 * objects are expected to have a {@code JarVerifier} instance member which
 * can be used to carry out the tasks associated with verifying a signed JAR.
 * These tasks would typically include:
 * <ul>
 * <li>verification of all signed signature files
 * <li>confirmation that all signed data was signed only by the party or parties
 * specified in the signature block data
 * <li>verification that the contents of all signature files (i.e. {@code .SF}
 * files) agree with the JAR entries information found in the JAR manifest.
 * </ul>
 */
public class JarVerifier {
    /**
     * List of accepted digest algorithms. This list is in order from most
     * preferred to least preferred.
     */
    private static final String[] DIGEST_ALGORITHMS = new String[] {
        "SHA-512",
        "SHA-384",
        "SHA-256",
        "SHA1",
    };

    private final String jarName;
    private final Manifest manifest;
    private final HashMap<String, byte[]> metaEntries;
//    private final int mainAttributesEnd;

    private final Hashtable<String, HashMap<String, Attributes>> signatures =
            new Hashtable<String, HashMap<String, Attributes>>(5);

    private final Hashtable<String, Certificate[]> certificates =
            new Hashtable<String, Certificate[]>(5);

    private final Hashtable<String, Certificate[][]> verifiedEntries =
            new Hashtable<String, Certificate[][]>();

    private final HashMap<String, Chunk> chunks;
            
    /**
     * Stores and a hash and a message digest and verifies that massage digest
     * matches the hash.
     */
    static class VerifierEntry extends OutputStream {

        private final String name;

        private final MessageDigest digest;

        private final byte[] hash;

        private final Certificate[][] certChains;

        private final Hashtable<String, Certificate[][]> verifiedEntries;

        VerifierEntry(final String name, final MessageDigest digest, final byte[] hash,
                final Certificate[][] certChains, final Hashtable<String, Certificate[][]> verifedEntries) {
            this.name = name;
            this.digest = digest;
            this.hash = hash;
            this.certChains = certChains;
            this.verifiedEntries = verifedEntries;
        }

        /**
         * Updates a digest with one byte.
         */
        @Override
        public void write(final int value) {
            digest.update((byte) value);
        }

        /**
         * Updates a digest with byte array.
         */
        @Override
        public void write(final byte[] buf, final int off, final int nbytes) {
            digest.update(buf, off, nbytes);
        }

        /**
         * Verifies that the digests stored in the manifest match the decrypted
         * digests from the .SF file. This indicates the validity of the
         * signing, not the integrity of the file, as its digest must be
         * calculated and verified when its contents are read.
         *
         * @throws SecurityException
         *             if the digest value stored in the manifest does <i>not</i>
         *             agree with the decrypted digest as recovered from the
         *             <code>.SF</code> file.
         */
        void verify() {
            final byte[] d = digest.digest();
            if (!MessageDigest.isEqual(d, Base64.decode(hash))) {
                throw invalidDigest(JarFile.MANIFEST_NAME, name, name);
            }
            verifiedEntries.put(name, certChains);
        }
    }

    private static SecurityException invalidDigest(final String signatureFile, final String name,
            final String jarName) {
        throw new SecurityException(signatureFile + " has invalid digest for " + name +
                " in " + jarName);
    }

    private static SecurityException failedVerification(final String jarName, final String signatureFile) {
        throw new SecurityException(jarName + " failed verification of " + signatureFile);
    }

    /**
     * Constructs and returns a new instance of {@code JarVerifier}.
     *
     * @param name
     *            the name of the JAR file being verified.
     */
    public JarVerifier(final String name, final Manifest manifest, final HashMap<String, byte[]> metaEntries) {
        jarName = name;
        chunks = new HashMap<String, Chunk>();
        this.manifest = manifest;
        this.metaEntries = metaEntries;
//        this.mainAttributesEnd = manifest.getMainAttributesEnd();
    }

    /**
     * Invoked for each new JAR entry read operation from the input
     * stream. This method constructs and returns a new {@link VerifierEntry}
     * which contains the certificates used to sign the entry and its hash value
     * as specified in the JAR MANIFEST format.
     *
     * @param name
     *            the name of an entry in a JAR file which is <b>not</b> in the
     *            {@code META-INF} directory.
     * @return a new instance of {@link VerifierEntry} which can be used by
     *         callers as an {@link OutputStream}.
     */
    VerifierEntry initEntry(final String name) {
        // If no manifest is present by the time an entry is found,
        // verification cannot occur. If no signature files have
        // been found, do not verify.
        if (manifest == null || signatures.isEmpty()) {
            return null;
        }

        final Attributes attributes = manifest.getAttributes(name);
        // entry has no digest
        if (attributes == null) {
            return null;
        }

        final ArrayList<Certificate[]> certChains = new ArrayList<Certificate[]>();
        final Iterator<Map.Entry<String, HashMap<String, Attributes>>> it = signatures.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, HashMap<String, Attributes>> entry = it.next();
            final HashMap<String, Attributes> hm = entry.getValue();
            if (hm.get(name) != null) {
                // Found an entry for entry name in .SF file
                final String signatureFile = entry.getKey();
                final Certificate[] certChain = certificates.get(signatureFile);
                if (certChain != null) {
                    certChains.add(certChain);
                }
            }
        }

        // entry is not signed
        if (certChains.isEmpty()) {
            return null;
        }
        final Certificate[][] certChainsArray = certChains.toArray(new Certificate[certChains.size()][]);

        for (int i = 0; i < DIGEST_ALGORITHMS.length; i++) {
            final String algorithm = DIGEST_ALGORITHMS[i];
            final String hash = attributes.getValue(algorithm + "-Digest");
            if (hash == null) {
                continue;
            }
            final byte[] hashBytes = hash.getBytes(Charset.forName("ISO-8859-1"));

            try {
                return new VerifierEntry(name, MessageDigest.getInstance(algorithm, SignHelper.getBCProvider()), hashBytes,
                        certChainsArray, verifiedEntries);
            } catch (final NoSuchAlgorithmException ignored) {
            }
        }
        return null;
    }

    /**
     * Add a new meta entry to the internal collection of data held on each JAR
     * entry in the {@code META-INF} directory including the manifest
     * file itself. Files associated with the signing of a JAR would also be
     * added to this collection.
     *
     * @param name
     *            the name of the file located in the {@code META-INF}
     *            directory.
     * @param buf
     *            the file bytes for the file called {@code name}.
     * @see #removeMetaEntries()
     */
    void addMetaEntry(final String name, final byte[] buf) {
        metaEntries.put(name.toUpperCase(Locale.US), buf);
    }

    /**
     * If the associated JAR file is signed, check on the validity of all of the
     * known signatures.
     *
     * @return {@code true} if the associated JAR is signed and an internal
     *         check verifies the validity of the signature(s). {@code false} if
     *         the associated JAR file has no entries at all in its {@code
     *         META-INF} directory. This situation is indicative of an invalid
     *         JAR file.
     *         <p>
     *         Will also return {@code true} if the JAR file is <i>not</i>
     *         signed.
     * @throws SecurityException
     *             if the JAR file is signed and it is determined that a
     *             signature block file contains an invalid signature for the
     *             corresponding signature file.
     */
    synchronized boolean readCertificates() {
        if (metaEntries.isEmpty()) {
            return false;
        }

        final Iterator<String> it = metaEntries.keySet().iterator();
        while (it.hasNext()) {
            final String key = it.next();
            if (key.endsWith(".DSA") || key.endsWith(".RSA") || key.endsWith(".EC")) {
                verifyCertificate(key);
                it.remove();
            }
        }
        return true;
    }
    
    private static X509Certificate findCert(final Principal issuer, final X509Certificate[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (issuer.equals(candidates[i].getSubjectDN())) {
                return candidates[i];
            }
        }
        return null;
    }
    
    private static X509Certificate[] createChain(final X509Certificate signer,
            final X509Certificate[] candidates) {
        Principal issuer = signer.getIssuerDN();

        // Signer is self-signed
        if (signer.getSubjectDN().equals(issuer)) {
            return new X509Certificate[] { signer };
        }

        final ArrayList<X509Certificate> chain = new ArrayList<X509Certificate>(candidates.length + 1);
        chain.add(0, signer);

        X509Certificate issuerCert;
        int count = 1;
        while (true) {
            issuerCert = findCert(issuer, candidates);
            if (issuerCert == null) {
                break;
            }
            chain.add(issuerCert);
            count++;
            issuer = issuerCert.getIssuerDN();
            if (issuerCert.getSubjectDN().equals(issuer)) {
                break;
            }
        }
        return chain.toArray(new X509Certificate[count]);
    }
    
	public static Certificate[] verifySignature(final byte[] sfBytes, final byte[] signatureBlock) throws Exception {
		final CMSSignedData signedData = new CMSSignedData(signatureBlock);
    	if (signedData == null) {
    		throw new IOException("No SignedData found");
    	}
    	final Store store = signedData.getCertificates();
        final SignerInformationStore signers = signedData.getSignerInfos(); 
        final Collection c = signers.getSigners(); 
    	if (c.isEmpty()) {
    		return null;
    	}
    	final X509Certificate[] certs = new X509Certificate[c.size()];
    	final Iterator it = c.iterator();
    	int certIdx = 0;
    	SignerInformation sigInfo = null;
    	X509Certificate certInfo;
    	while (it.hasNext()) { 
            final SignerInformation signer = (SignerInformation) it.next(); 
            final Collection certCollection = store.getMatches(signer.getSID()); 
            final Iterator certIt = certCollection.iterator();
            final X509CertificateHolder certHolder = (X509CertificateHolder) certIt.next();
            final X509Certificate certFromSignedData = new JcaX509CertificateConverter().setProvider(SignHelper.getBCProvider()).getCertificate(certHolder);
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(SignHelper.getBCProvider()).build(certFromSignedData))) {
//                System.out.println("Signature verified");
            } else {
            	throw new SecurityException("fail to verify signatureBlock");
//                System.out.println("Signature verification failed");
            }
            if(certIdx == 0){
            	sigInfo = signer;
            	certInfo = certFromSignedData;
            }
            certs[certIdx++] = certFromSignedData;
        }
    	
    	final X500Name issuer = sigInfo.getSID().getIssuer();
    	// Certificate serial number
		final BigInteger snum = sigInfo.getSID().getSerialNumber();//sigInfo.getSerialNumber();
		// Locate the certificate
		int issuerSertIndex = 0;
		int i;
		for (i = 0; i < certs.length; i++) {
			if (issuer.toString().equals(certs[i].getIssuerDN().toString()) &&
					snum.equals(certs[i].getSerialNumber())) {
				issuerSertIndex = i;
				break;
			}
		}
		if (i == certs.length) { // No issuer certificate found
			return null;
		}
		final X509Certificate x509Certificate = certs[issuerSertIndex];
		if (x509Certificate.hasUnsupportedCriticalExtension()) {
			throw new SecurityException("Can not recognize a critical extension");
		}
		final String sigAlgName = x509Certificate.getSigAlgName();

//		// Get Signature instance
		final Signature sig = Signature.getInstance(sigAlgName, SignHelper.getBCProvider());
		
		// We couldn't find a valid Signature type.
		if (sig == null) {
			return null;
		}
		sig.initVerify(x509Certificate);//issuerSertIndex
		// If the authenticatedAttributes field of SignerInfo contains more than zero attributes,
		// compute the message digest on the ASN.1 DER encoding of the Attributes value.
		// Otherwise, compute the message digest on the data.
		final AttributeTable atr = sigInfo.getSignedAttributes();//getAuthenticatedAttributes();
		if (atr == null) {
			sig.update(sfBytes);
		} else {
			sig.update(sigInfo.getEncodedSignedAttributes());//getEncodedAuthenticatedAttributes
			// If the authenticatedAttributes field contains the message-digest attribute,
			// verify that it equals the computed digest of the signature file
			final byte[] existingDigest = null;
			// RFC section 9.2: final it authAttrs final is present, it must final have a
			// message digest entry.
			if (existingDigest == null) {
				throw new SecurityException("Missing MessageDigest in Authenticated Attributes");
			}
			MessageDigest md = null;

			final int withIdx = sigAlgName.toUpperCase().indexOf("WITH");
			final String daName = (withIdx>0?sigAlgName.substring(0, withIdx):sigAlgName);

			if (md == null && daName != null) {
				md = MessageDigest.getInstance(daName, SignHelper.getBCProvider());
			}
			if (md == null) {
				return null;
			}
			final byte[] computedDigest = md.digest(sfBytes);
			if (!Arrays.equals(existingDigest, computedDigest)) {
				throw new SecurityException("Incorrect MD");
			}
		}
		if (!sig.verify(sigInfo.getSignature())) {//getEncryptedDigest
			throw new SecurityException("Incorrect signature");
		}
		return createChain(x509Certificate, certs);//issuerSertIndex
	}
	
	
	public static void main(final String[] args){
//		final byte[] sfbs = ResourceUtil.getContent(new File("testcase/sign", "CERT.SF"));
//		final byte[] rsabs = ResourceUtil.getContent(new File("testcase/sign", "CERT.RSA"));
//		try {
//			final Certificate[] certs = verifySignature(sfbs, rsabs);
//			final int j = 0;
//		} catch (final Throwable e) {
//			e.printStackTrace();
//		}
		
		
		try{
			final File zipFile = new File("testcase/sign", "sample_test_2_0_signed.har");
			final ZipFile jf = new ZipFile(zipFile, ZipFile.OPEN_READ);
			final HashMap<String, byte[]> metaEntries = readMetaEntries(jf, true);
			System.out.println(ResourceUtil.getMD5(metaEntries.get("META-INF/CERT.RSA")));
			final ByteArrayInputStream bis = new ByteArrayInputStream(metaEntries.get(JarFile.MANIFEST_NAME));
			System.out.println("==============" + JarFile.MANIFEST_NAME + "=============>");
			System.out.println(new String(metaEntries.get(JarFile.MANIFEST_NAME)));
			System.out.println("==============META-INF/CERT.SF=============>");
			System.out.println(new String(metaEntries.get("META-INF/CERT.SF")));
			final org.android.signapk.JarVerifier myJarVerifier = new org.android.signapk.JarVerifier("", new Manifest(bis), metaEntries);
			
			myJarVerifier.readCertificates();
			myJarVerifier.verifyEntry(jf);
			
			final Certificate[] out = myJarVerifier.getCerificateHCAPI("META-INF/CERT.SF");
			final int i = 100;
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	public final void verifyEntry(final ZipFile zipFile){
		final Enumeration<? extends ZipEntry> allEntries = zipFile.entries();
		final byte[] buffer = new byte[1024];
        while (allEntries.hasMoreElements()) {
            final ZipEntry ze = allEntries.nextElement();
            final String zName = ze.getName();
			if (zName.startsWith(META_DIR)) {
            }else{
        		final JarVerifier.VerifierEntry entry = initEntry(zName);
        		try{
	        		final InputStream is = zipFile.getInputStream(ze);
			        int count;
			        while ((count = is.read(buffer)) != -1) {
			            entry.write(buffer, 0, count);
			        }
        		}catch (final Throwable e) {
        			throw invalidDigest(JarFile.MANIFEST_NAME, zName, zName);
				}
        		entry.verify();
            }
        }

	}
	
    // The directory containing the manifest.
    static final String META_DIR = "META-INF/";
    
    /**
     * Returns all the ZipEntry's that relate to files in the
     * JAR's META-INF directory.
     */
    private static List<ZipEntry> getMetaEntries(final ZipFile zipFile) {
        final List<ZipEntry> list = new ArrayList<ZipEntry>(8);

        final Enumeration<? extends ZipEntry> allEntries = zipFile.entries();
        while (allEntries.hasMoreElements()) {
            final ZipEntry ze = allEntries.nextElement();
            if (ze.getName().startsWith(META_DIR)
                    && ze.getName().length() > META_DIR.length()) {
                list.add(ze);
            }
        }

        return list;
    }
    
    static HashMap<String, byte[]> readMetaEntries(final ZipFile jarFile,
            final boolean verificationRequired) throws Throwable {
        // Get all meta directory entries
        final List<ZipEntry> metaEntries = getMetaEntries(jarFile);

        final HashMap<String, byte[]> metaEntriesMap = new HashMap<String, byte[]>();

        for (final ZipEntry entry : metaEntries) {
            final String entryName = entry.getName();
            // Is this the entry for META-INF/MANIFEST.MF ?
            //
            // TODO: Why do we need the containsKey check ? Shouldn't we discard
            // files that contain duplicate entries like this as invalid ?.
            if (entryName.equalsIgnoreCase(JarFile.MANIFEST_NAME) &&
                    !metaEntriesMap.containsKey(JarFile.MANIFEST_NAME)) {

                metaEntriesMap.put(JarFile.MANIFEST_NAME, readFully(
                        jarFile.getInputStream(entry)));

                // If there is no verifier then we don't need to look any further.
                if (!verificationRequired) {
                    break;
                }
            } else if (verificationRequired) {
                // Is this an entry that the verifier needs?
                if (endsWithIgnoreCase(entryName, ".SF")
                        || endsWithIgnoreCase(entryName, ".DSA")
                        || endsWithIgnoreCase(entryName, ".RSA")
                        || endsWithIgnoreCase(entryName, ".EC")) {
                    final InputStream is = jarFile.getInputStream(entry);
                    metaEntriesMap.put(entryName.toUpperCase(Locale.US), readFully(is));
                }
            }
        }

        return metaEntriesMap;
    }

    private static boolean endsWithIgnoreCase(final String s, final String suffix) {
        return s.regionMatches(true, s.length() - suffix.length(), suffix, 0, suffix.length());
    }
    
    /**
     * Returns a byte[] containing the remainder of 'in', closing it when done.
     */
    public static byte[] readFully(final InputStream in) throws Throwable {
        try {
            return readFullyNoClose(in);
        } finally {
            in.close();
        }
    }

    /**
     * Returns a byte[] containing the remainder of 'in'.
     */
    public static byte[] readFullyNoClose(final InputStream in) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }
    
    /**
     * @param certFile
     */
    private void verifyCertificate(final String certFile) {
        // Found Digital Sig, .SF should already have been read
        final String signatureFile = certFile.substring(0, certFile.lastIndexOf('.')) + ".SF";
        final byte[] sfBytes = metaEntries.get(signatureFile);
        if (sfBytes == null) {
            return;
        }

        final byte[] manifestBytes = metaEntries.get(JarFile.MANIFEST_NAME);
        // Manifest entry is required for any verifications.
        if (manifestBytes == null) {
            return;
        }

        final byte[] sBlockBytes = metaEntries.get(certFile);
        try {
            final Certificate[] signerCertChain = verifySignature(sfBytes, sBlockBytes);
            if (signerCertChain != null) {
                certificates.put(signatureFile, signerCertChain);
            }
        } catch (final Exception e) {
        	if(L.isInWorkshop){
        		e.printStackTrace();//no display verified error
        	}
            throw failedVerification(jarName, signatureFile);
        }

        // Verify manifest hash in .sf file
        final Attributes attributes = new Attributes();
        final HashMap<String, Attributes> entries = new HashMap<String, Attributes>();
        try {
            final ManifestReader im = new ManifestReader(sfBytes, attributes);
            im.readEntries(entries, chunks);
        } catch (final IOException e) {
            return;
        }

        // Do we actually have any signatures to look at?
        if (attributes.get(Attributes.Name.SIGNATURE_VERSION) == null) {
            return;
        }

        boolean createdBySigntool = false;
        final String createdBy = attributes.getValue("Created-By");
        if (createdBy != null) {
            createdBySigntool = createdBy.indexOf("signtool") != -1;
        }

//        // Use .SF to verify the mainAttributes of the manifest
//        // If there is no -Digest-Manifest-Main-Attributes entry in .SF
//        // file, such as those created before java 1.5, then we ignore
//        // such verification.
//        if (mainAttributesEnd > 0 && !createdBySigntool) {
//            final String digestAttribute = "-Digest-Manifest-Main-Attributes";
//            if (!verify(attributes, digestAttribute, manifestBytes, 0, mainAttributesEnd, false, true)) {
//                throw failedVerification(jarName, signatureFile);
//            }
//        }

        // Use .SF to verify the whole manifest.
        final String digestAttribute = createdBySigntool ? "-Digest" : "-Digest-Manifest";
        if (!verify(attributes, digestAttribute, manifestBytes, 0, manifestBytes.length, false, false)) {
            final Iterator<Map.Entry<String, Attributes>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Attributes> entry = it.next();
                final Chunk chunk = chunks.get(entry.getKey());
                if (chunk == null) {
                    return;
                }
                if (!verify(entry.getValue(), "-Digest", manifestBytes,
                        chunk.start, chunk.end, createdBySigntool, false)) {
                    throw invalidDigest(signatureFile, entry.getKey(), jarName);
                }
            }
        }
        metaEntries.put(signatureFile, null);
        signatures.put(signatureFile, entries);
    }

    /**
     * Returns a <code>boolean</code> indication of whether or not the
     * associated jar file is signed.
     *
     * @return {@code true} if the JAR is signed, {@code false}
     *         otherwise.
     */
    boolean isSignedJar() {
        return certificates.size() > 0;
    }
    
    /**
     * 
     * @param fileName META-INF/CERT.SF
     * @return
     */
    public Certificate[] getCerificateHCAPI(final String fileName){
    	final Vector<Certificate> vector = new Vector<Certificate>();
    	
    	final Enumeration<String> keys = certificates.keys();
    	while(keys.hasMoreElements()){
    		final String oldKey = keys.nextElement();
			final String key = oldKey.toUpperCase();
    		if(key.startsWith("META-INF/", 0) && key.endsWith(".SF")){
    			final Certificate[] certs = certificates.get(oldKey);
    			for (int i = 0; i < certs.length; i++) {
					vector.add(certs[i]);
				}
    		}
    	}
    	
    	if(vector.size() == 0){
    		return null;
    	}else{
    		final Certificate[] out = new Certificate[vector.size()];
    		return vector.toArray(out);
    	}
    }

    private boolean verify(final Attributes attributes, final String entry, final byte[] data,
            final int start, final int end, final boolean ignoreSecondEndline, final boolean ignorable) {
        for (int i = 0; i < DIGEST_ALGORITHMS.length; i++) {
            final String algorithm = DIGEST_ALGORITHMS[i];
            final String hash = attributes.getValue(algorithm + entry);
            if (hash == null) {
                continue;
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance(algorithm, SignHelper.getBCProvider());
            } catch (final NoSuchAlgorithmException e) {
                continue;
            }
            if (ignoreSecondEndline && data[end - 1] == '\n' && data[end - 2] == '\n') {
                md.update(data, start, end - 1 - start);
            } else {
                md.update(data, start, end - start);
            }
            final byte[] b = md.digest();
            final byte[] hashBytes = hash.getBytes(Charset.forName("ISO-8859-1"));
            final boolean equal = MessageDigest.isEqual(b, Base64.decode(hashBytes));
			return equal;
        }
        return ignorable;
    }

    /**
     * Returns all of the {@link java.security.cert.Certificate} chains that
     * were used to verify the signature on the JAR entry called
     * {@code name}. Callers must not modify the returned arrays.
     *
     * @param name
     *            the name of a JAR entry.
     * @return an array of {@link java.security.cert.Certificate} chains.
     */
    Certificate[][] getCertificateChains(final String name) {
        return verifiedEntries.get(name);
    }

    /**
     * Remove all entries from the internal collection of data held about each
     * JAR entry in the {@code META-INF} directory.
     */
    void removeMetaEntries() {
        metaEntries.clear();
    }
}
