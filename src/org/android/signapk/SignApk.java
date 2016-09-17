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

import hc.core.util.StringUtil;
import hc.server.util.BCProvider;
import hc.server.util.SignItem;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.encoders.Base64;

/**
 * HISTORICAL NOTE:
 *
 * Prior to the keylimepie release, SignApk ignored the signature
 * algorithm specified in the certificate and always used SHA1withRSA.
 *
 * Starting with keylimepie, we support SHA256withRSA, and use the
 * signature algorithm in the certificate to select which to use
 * (SHA256withRSA or SHA1withRSA).
 *
 * Because there are old keys still in use whose certificate actually
 * says "MD5withRSA", we treat these as though they say "SHA1withRSA"
 * for compatibility with older releases.  This can be changed by
 * altering the getAlgorithm() function below.
 */


/**
 * Command line tool to sign JAR files (including APKs and OTA
 * updates) in a way compatible with the mincrypt verifier, using RSA
 * keys and SHA1 or SHA-256.
 */
public class SignApk {
    private static final String CERT_SF_NAME = "META-INF/CERT.SF";
    private static final String CERT_RSA_NAME = "META-INF/CERT.RSA";
    private static final String CERT_SF_MULTI_NAME = "META-INF/CERT%d.SF";
    private static final String CERT_RSA_MULTI_NAME = "META-INF/CERT%d.RSA";

    private static final String OTACERT_NAME = "META-INF/com/android/otacert";

    // bitmasks for which hash algorithms we need the manifest to include.
    private static final int USE_SHA1 = 1;
    private static final int USE_SHA256 = 2;
    private static final int USE_SHA384 = 4;
    private static final int USE_SHA512 = 8;

    /**
     * Return one of USE_SHA1 or USE_SHA256 according to the signature
     * algorithm specified in the cert.
     */
    private static int getAlgorithm(final X509Certificate cert) {
        final String sigAlg = cert.getSigAlgName();
        if ("SHA1withRSA".equals(sigAlg) || "MD5withRSA".equals(sigAlg)) {     // see "HISTORICAL NOTE" above.
            return USE_SHA1;
        } else if ("SHA256withRSA".equals(sigAlg)) {
            return USE_SHA256;
        } else if ("SHA384withRSA".equals(sigAlg)) {
            return USE_SHA384;
        } else if ("SHA512withRSA".equals(sigAlg)) {
            return USE_SHA512;
        } else {
            throw new IllegalArgumentException("unsupported signature algorithm \"" + sigAlg +
                                               "\" in cert [" + cert.getSubjectDN());
        }
    }

    // Files matching this pattern are not copied to the output.
    private static Pattern stripPattern =
        Pattern.compile("^(META-INF/((.*)[.](SF|RSA|DSA)|com/android/otacert))|(" +
                        Pattern.quote(JarFile.MANIFEST_NAME) + ")$");

    private static X509Certificate readPublicKey(final File file)
        throws IOException, GeneralSecurityException {
        final FileInputStream input = new FileInputStream(file);
        try {
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(input);
        } finally {
            input.close();
        }
    }

    /**
     * Reads the password from stdin and returns it as a string.
     *
     * @param keyFile The file containing the private key.  Used to prompt the user.
     */
    private static String readPassword(final File keyFile) {
        // TODO: use Console.readPassword() when it's available.
        System.out.print("Enter password for " + keyFile + " (password will not be hidden): ");
        System.out.flush();
        final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        try {
            return stdin.readLine();
        } catch (final IOException ex) {
            return null;
        }
    }

    /**
     * Decrypt an encrypted PKCS 8 format private key.
     *
     * Based on ghstark's post on Aug 6, 2006 at
     * http://forums.sun.com/thread.jspa?threadID=758133&messageID=4330949
     *
     * @param encryptedPrivateKey The raw data of the private key
     * @param keyFile The file containing the private key
     */
    private static KeySpec decryptPrivateKey(final byte[] encryptedPrivateKey, final File keyFile)
        throws GeneralSecurityException {
        EncryptedPrivateKeyInfo epkInfo;
        try {
            epkInfo = new EncryptedPrivateKeyInfo(encryptedPrivateKey);
        } catch (final IOException ex) {
            // Probably not an encrypted key.
            return null;
        }

        final char[] password = readPassword(keyFile).toCharArray();

        final SecretKeyFactory skFactory = SecretKeyFactory.getInstance(epkInfo.getAlgName());
        final Key key = skFactory.generateSecret(new PBEKeySpec(password));

        final Cipher cipher = Cipher.getInstance(epkInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, key, epkInfo.getAlgParameters());

        try {
            return epkInfo.getKeySpec(cipher);
        } catch (final InvalidKeySpecException ex) {
            System.err.println("signapk: Password for " + keyFile + " may be bad.");
            throw ex;
        }
    }

    /** Read a PKCS 8 format private key. */
    private static PrivateKey readPrivateKey(final File file)
        throws IOException, GeneralSecurityException {
        final DataInputStream input = new DataInputStream(new FileInputStream(file));
        try {
            final byte[] bytes = new byte[(int) file.length()];
            input.read(bytes);

            KeySpec spec = decryptPrivateKey(bytes, file);
            if (spec == null) {
                spec = new PKCS8EncodedKeySpec(bytes);
            }

            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (final InvalidKeySpecException ex) {
                return KeyFactory.getInstance("DSA").generatePrivate(spec);
            }
        } finally {
            input.close();
        }
    }

    /**
     * Add the hash(es) of every file to the manifest, creating it if
     * necessary.
     */
    private static Manifest addDigestsToManifest(final JarFile jar, final int hashes)
        throws IOException, GeneralSecurityException {
        final Manifest input = jar.getManifest();
        final Manifest output = new Manifest();
        final Attributes main = output.getMainAttributes();
//        if (input != null) {
//            main.putAll(input.getMainAttributes());
//        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android SignApk for HomeCenter)");
//        }
          final Provider bcProvider = BCProvider.getBCProvider();
            
        final MessageDigest md_sha = getMessageDisgest(hashes, bcProvider);

        final byte[] buffer = new byte[4096];
        int num;

        // We sort the input entries by name, and add them to the
        // output manifest in sorted order.  We expect that the output
        // map will be deterministic.

        final TreeMap<String, JarEntry> byName = new TreeMap<String, JarEntry>();

        for (final Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            final JarEntry entry = e.nextElement();
            byName.put(entry.getName(), entry);
        }

        for (final JarEntry entry: byName.values()) {
            final String name = entry.getName();
            if (!entry.isDirectory() &&
                (stripPattern == null || !stripPattern.matcher(name).matches())) {
                final InputStream data = jar.getInputStream(entry);
                while ((num = data.read(buffer)) > 0) {
                    md_sha.update(buffer, 0, num);
                }

                Attributes attr = null;
                if (input != null) attr = input.getAttributes(name);
                attr = attr != null ? new Attributes(attr) : new Attributes();
                final String digest = new String(Base64.encode(md_sha.digest()), "ASCII");
                attr.putValue(getDigestHeader(hashes) + "-Digest", digest);
                output.getEntries().put(name, attr);
            }
        }

        return output;
    }
    
    private static String getDigestHeader(final int hash){
        if ((hash & USE_SHA512) != 0) {
            return "SHA-512";
        }else if ((hash & USE_SHA384) != 0) {
            return "SHA-384";
        }else if ((hash & USE_SHA256) != 0) {
            return "SHA-256";
        }else if ((hash & USE_SHA1) != 0) {
            return "SHA1";
        }else{
        	return "unknow";
        }
    }

	private static MessageDigest getMessageDisgest(final int hashes,
			final Provider bcProvider) throws NoSuchAlgorithmException {
		MessageDigest md_sha = null;
        if ((hashes & USE_SHA512) != 0) {
            md_sha = MessageDigest.getInstance("SHA512", bcProvider);
        }else if ((hashes & USE_SHA384) != 0) {
            md_sha = MessageDigest.getInstance("SHA384", bcProvider);
        }else if ((hashes & USE_SHA256) != 0) {
            md_sha = MessageDigest.getInstance("SHA256", bcProvider);
        }else if ((hashes & USE_SHA1) != 0) {
                md_sha = MessageDigest.getInstance("SHA1", bcProvider);
        }
		return md_sha;
	}

    /**
     * Add a copy of the public key to the archive; this should
     * exactly match one of the files in
     * /system/etc/security/otacerts.zip on the device.  (The same
     * cert can be extracted from the CERT.RSA file but this is much
     * easier to get at.)
     */
    private static void addOtacert(final JarOutputStream outputJar,
                                   final File publicKeyFile,
                                   final long timestamp,
                                   final Manifest manifest,
                                   final int hash)
        throws IOException, GeneralSecurityException {
        final MessageDigest md = getMessageDisgest(hash, BCProvider.getBCProvider());

        final JarEntry je = new JarEntry(OTACERT_NAME);
        je.setTime(timestamp);
        outputJar.putNextEntry(je);
        final FileInputStream input = new FileInputStream(publicKeyFile);
        final byte[] b = new byte[4096];
        int read;
        while ((read = input.read(b)) != -1) {
            outputJar.write(b, 0, read);
            md.update(b, 0, read);
        }
        input.close();

        final Attributes attr = new Attributes();
        attr.putValue(hash == USE_SHA1 ? "SHA1-Digest" : "SHA-256-Digest",
                      new String(Base64.encode(md.digest()), "ASCII"));
        manifest.getEntries().put(OTACERT_NAME, attr);
    }


    /** Write to another stream and track how many bytes have been
     *  written.
     */
    private static class CountOutputStream extends FilterOutputStream {
        private int mCount;

        public CountOutputStream(final OutputStream out) {
            super(out);
            mCount = 0;
        }

        @Override
        public void write(final int b) throws IOException {
            super.write(b);
            mCount++;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            super.write(b, off, len);
            mCount += len;
        }

        public int size() {
            return mCount;
        }
    }

    /** Write a .SF file with a digest of the specified manifest. */
    private static void writeSignatureFile(final Manifest manifest, final OutputStream out,
                                           final int hash)
        throws IOException, GeneralSecurityException {
        final Manifest sf = new Manifest();
        final Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android SignApk)");

        final MessageDigest md = getMessageDisgest(hash, BCProvider.getBCProvider());
        final PrintStream print = new PrintStream(
            new DigestOutputStream(new ByteArrayOutputStream(), md),
            true, "UTF-8");

        // Digest of the entire manifest
        manifest.write(print);
        print.flush();
        main.putValue(getDigestHeader(hash) + "-Digest-Manifest", new String(Base64.encode(md.digest()), "ASCII"));

        final Map<String, Attributes> entries = manifest.getEntries();
        for (final Map.Entry<String, Attributes> entry : entries.entrySet()) {
            // Digest of the manifest stanza for this entry.
            print.print("Name: " + entry.getKey() + "\r\n");
            for (final Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
                print.print(att.getKey() + ": " + att.getValue() + "\r\n");
            }
            print.print("\r\n");
            print.flush();

            final Attributes sfAttr = new Attributes();
            sfAttr.putValue((hash == USE_SHA1 ? "SHA1-Digest-Manifest" : (getDigestHeader(hash) + "-Digest")),
                            new String(Base64.encode(md.digest()), "ASCII"));
            sf.getEntries().put(entry.getKey(), sfAttr);
        }

        final CountOutputStream cout = new CountOutputStream(out);
        sf.write(cout);

        // A bug in the java.util.jar implementation of Android platforms
        // up to version 1.6 will cause a spurious IOException to be thrown
        // if the length of the signature file is a multiple of 1024 bytes.
        // As a workaround, add an extra CRLF in this case.
        if ((cout.size() % 1024) == 0) {
            cout.write('\r');
            cout.write('\n');
        }
    }

    /** Sign data and write the digital signature to 'out'. */
    private static void writeSignatureBlock(
        final CMSTypedData data, final X509Certificate publicKey, final PrivateKey privateKey,
        final OutputStream out)
        throws IOException,
               CertificateEncodingException,
               OperatorCreationException,
               CMSException {
        final ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>(1);
        certList.add(publicKey);
        final JcaCertStore certs = new JcaCertStore(certList);

        final Provider bcProvider = BCProvider.getBCProvider();
        
        final CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        final String digestHeader = getDigestHeader(getAlgorithm(publicKey));
		final ContentSigner signer = new JcaContentSignerBuilder(StringUtil.replace(digestHeader, "-", "") + "withRSA")
            .setProvider(bcProvider)
            .build(privateKey);
        gen.addSignerInfoGenerator(
            new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                .setProvider(bcProvider)
                .build())
            .setDirectSignature(true)
            .build(signer, publicKey));
        gen.addCertificates(certs);
        final CMSSignedData sigData = gen.generate(data, true);

        final ASN1InputStream asn1 = new ASN1InputStream(sigData.getEncoded());
        final DEROutputStream dos = new DEROutputStream(out);
        dos.writeObject(asn1.readObject());
    }

    /**
     * Copy all the files in a manifest from input to output.  We set
     * the modification times in the output to a fixed time, so as to
     * reduce variation in the output file and make incremental OTAs
     * more efficient.
     */
    private static void copyFiles(final Manifest manifest,
                                  final JarFile in, final JarOutputStream out, final long timestamp) throws IOException {
        final byte[] buffer = new byte[4096];
        int num;

        final Map<String, Attributes> entries = manifest.getEntries();
        final ArrayList<String> names = new ArrayList<String>(entries.keySet());
        Collections.sort(names);
        for (final String name : names) {
            final JarEntry inEntry = in.getJarEntry(name);
            JarEntry outEntry = null;
            if (inEntry.getMethod() == JarEntry.STORED) {
                // Preserve the STORED method of the input entry.
                outEntry = new JarEntry(inEntry);
            } else {
                // Create a new entry so that the compressed len is recomputed.
                outEntry = new JarEntry(name);
            }
            outEntry.setTime(timestamp);
            out.putNextEntry(outEntry);

            final InputStream data = in.getInputStream(inEntry);
            while ((num = data.read(buffer)) > 0) {
                out.write(buffer, 0, num);
            }
            out.flush();
        }
    }

    private static class WholeFileSignerOutputStream extends FilterOutputStream {
        private boolean closing = false;
        private final ByteArrayOutputStream footer = new ByteArrayOutputStream();
        private final OutputStream tee;

        public WholeFileSignerOutputStream(final OutputStream out, final OutputStream tee) {
            super(out);
            this.tee = tee;
        }

        public void notifyClosing() {
            closing = true;
        }

        public void finish() throws IOException {
            closing = false;

            final byte[] data = footer.toByteArray();
            if (data.length < 2)
                throw new IOException("Less than two bytes written to footer");
            write(data, 0, data.length - 2);
        }

        public byte[] getTail() {
            return footer.toByteArray();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (closing) {
                // if the jar is about to close, save the footer that will be written
                footer.write(b, off, len);
            }
            else {
                // write to both output streams. out is the CMSTypedData signer and tee is the file.
                out.write(b, off, len);
                tee.write(b, off, len);
            }
        }

        @Override
        public void write(final int b) throws IOException {
            if (closing) {
                // if the jar is about to close, save the footer that will be written
                footer.write(b);
            }
            else {
                // write to both output streams. out is the CMSTypedData signer and tee is the file.
                out.write(b);
                tee.write(b);
            }
        }
    }

    private static class CMSSigner implements CMSTypedData {
        private final JarFile inputJar;
        private final File publicKeyFile;
        private final X509Certificate publicKey;
        private final PrivateKey privateKey;
        private String outputFile;
        private final OutputStream outputStream;
        private final ASN1ObjectIdentifier type;
        private WholeFileSignerOutputStream signer;

        public CMSSigner(final JarFile inputJar, final File publicKeyFile,
                         final X509Certificate publicKey, final PrivateKey privateKey,
                         final OutputStream outputStream) {
            this.inputJar = inputJar;
            this.publicKeyFile = publicKeyFile;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.outputStream = outputStream;
            this.type = new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId());
        }

        @Override
		public Object getContent() {
            throw new UnsupportedOperationException();
        }

        @Override
		public ASN1ObjectIdentifier getContentType() {
            return type;
        }

        @Override
		public void write(final OutputStream out) throws IOException {
            try {
                signer = new WholeFileSignerOutputStream(out, outputStream);
                final JarOutputStream outputJar = new JarOutputStream(signer);

                final int hash = getAlgorithm(publicKey);

                // Assume the certificate is valid for at least an hour.
                final long timestamp = publicKey.getNotBefore().getTime() + 3600L * 1000;

                final Manifest manifest = addDigestsToManifest(inputJar, hash);
                copyFiles(manifest, inputJar, outputJar, timestamp);
                addOtacert(outputJar, publicKeyFile, timestamp, manifest, hash);

                signFile(manifest, inputJar,
                         new X509Certificate[]{ publicKey },
                         new PrivateKey[]{ privateKey },
                         outputJar);

                signer.notifyClosing();
                outputJar.close();
                signer.finish();
            }
            catch (final Exception e) {
                throw new IOException(e);
            }
        }

        public void writeSignatureBlock(final ByteArrayOutputStream temp)
            throws IOException,
                   CertificateEncodingException,
                   OperatorCreationException,
                   CMSException {
            SignApk.writeSignatureBlock(this, publicKey, privateKey, temp);
        }

        public WholeFileSignerOutputStream getSigner() {
            return signer;
        }
    }

    private static void signWholeFile(final JarFile inputJar, final File publicKeyFile,
                                      final X509Certificate publicKey, final PrivateKey privateKey,
                                      final OutputStream outputStream) throws Exception {
        final CMSSigner cmsOut = new CMSSigner(inputJar, publicKeyFile,
                                         publicKey, privateKey, outputStream);

        final ByteArrayOutputStream temp = new ByteArrayOutputStream();

        // put a readable message and a null char at the start of the
        // archive comment, so that tools that display the comment
        // (hopefully) show something sensible.
        // TODO: anything more useful we can put in this message?
        final byte[] message = "signed by SignApk".getBytes("UTF-8");
        temp.write(message);
        temp.write(0);

        cmsOut.writeSignatureBlock(temp);

        final byte[] zipData = cmsOut.getSigner().getTail();

        // For a zip with no archive comment, the
        // end-of-central-directory record will be 22 bytes long, so
        // we expect to find the EOCD marker 22 bytes from the end.
        if (zipData[zipData.length-22] != 0x50 ||
            zipData[zipData.length-21] != 0x4b ||
            zipData[zipData.length-20] != 0x05 ||
            zipData[zipData.length-19] != 0x06) {
            throw new IllegalArgumentException("zip data already has an archive comment");
        }

        final int total_size = temp.size() + 6;
        if (total_size > 0xffff) {
            throw new IllegalArgumentException("signature is too big for ZIP file comment");
        }
        // signature starts this many bytes from the end of the file
        final int signature_start = total_size - message.length - 1;
        temp.write(signature_start & 0xff);
        temp.write((signature_start >> 8) & 0xff);
        // Why the 0xff bytes?  In a zip file with no archive comment,
        // bytes [-6:-2] of the file are the little-endian offset from
        // the start of the file to the central directory.  So for the
        // two high bytes to be 0xff 0xff, the archive would have to
        // be nearly 4GB in size.  So it's unlikely that a real
        // commentless archive would have 0xffs here, and lets us tell
        // an old signed archive from a new one.
        temp.write(0xff);
        temp.write(0xff);
        temp.write(total_size & 0xff);
        temp.write((total_size >> 8) & 0xff);
        temp.flush();

        // Signature verification checks that the EOCD header is the
        // last such sequence in the file (to avoid minzip finding a
        // fake EOCD appended after the signature in its scan).  The
        // odds of producing this sequence by chance are very low, but
        // let's catch it here if it does.
        final byte[] b = temp.toByteArray();
        for (int i = 0; i < b.length-3; ++i) {
            if (b[i] == 0x50 && b[i+1] == 0x4b && b[i+2] == 0x05 && b[i+3] == 0x06) {
                throw new IllegalArgumentException("found spurious EOCD header at " + i);
            }
        }

        outputStream.write(total_size & 0xff);
        outputStream.write((total_size >> 8) & 0xff);
        temp.writeTo(outputStream);
    }

    private static void signFile(final Manifest manifest, final JarFile inputJar,
            final X509Certificate[] certs, final PrivateKey[] privateKeys,
            final JarOutputStream outputJar) throws Exception{
    	final int size = certs.length;
    	final SignItem[] items = new SignItem[size];
    	
    	for (int i = 0; i < size; i++) {
			items[i] = new SignItem(null, certs[i], privateKeys[i]);
		}
    	signFile(manifest, inputJar, items, outputJar);
    }
    
    private static void signFile(final Manifest manifest, final JarFile inputJar,
                                 final SignItem[] items,
                                 final JarOutputStream outputJar)
        throws Exception {
        // Assume the certificate is valid for at least an hour.
        final long timestamp = items[0].chain.getNotBefore().getTime() + 3600L * 1000;

        // MANIFEST.MF
        JarEntry je = new JarEntry(JarFile.MANIFEST_NAME);
        je.setTime(timestamp);
        outputJar.putNextEntry(je);
        manifest.write(outputJar);

        final int numKeys = items.length;
        for (int k = 0; k < numKeys; ++k) {
            // CERT.SF / CERT#.SF
            je = new JarEntry(numKeys == 1 ? CERT_SF_NAME :
                              (String.format(CERT_SF_MULTI_NAME, k)));
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeSignatureFile(manifest, baos, getAlgorithm(items[k].chain));
            final byte[] signedData = baos.toByteArray();
            outputJar.write(signedData);

            // CERT.RSA / CERT#.RSA
            je = new JarEntry(numKeys == 1 ? CERT_RSA_NAME :
                              (String.format(CERT_RSA_MULTI_NAME, k)));
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureBlock(new CMSProcessableByteArray(signedData),
            		items[k].chain, items[k].privateKey, outputJar);
        }
    }

    private static void usage() {
        System.err.println("Usage: signapk [-w] " +
                           "publickey.x509[.pem] privatekey.pk8 " +
                           "[publickey2.x509[.pem] privatekey2.pk8 ...] " +
                           "input.jar output.jar");
        System.exit(2);
    }
    
    public static void sign(final JarFile inputJar, final FileOutputStream outputFile,
    		final SignItem[] items) throws Exception{
    	final long timestamp = items[0].chain.getNotBefore().getTime() + 3600L * 1000;
    	
    	 int hashes = 0;
            for (int i = 0; i < items.length; ++i) {
                hashes |= getAlgorithm(items[i].chain);
            }
    	
    	try{
	        final JarOutputStream outputJar = new JarOutputStream(outputFile);
	
	        // For signing .apks, use the maximum compression to make
	        // them as small as possible (since they live forever on
	        // the system partition).  For OTA packages, use the
	        // default compression level, which is much much faster
	        // and produces output that is only a tiny bit larger
	        // (~0.1% on full OTA packages I tested).
	        outputJar.setLevel(9);
	
	        final Manifest manifest = addDigestsToManifest(inputJar, hashes);
	        copyFiles(manifest, inputJar, outputJar, timestamp);
	        signFile(manifest, inputJar, items, outputJar);
	        outputJar.close();
    	}finally{
    		try{
    			if (inputJar != null) inputJar.close();
    		}catch (final Throwable e) {
			}
    		
    		try{
    			if (outputFile != null) outputFile.close();
    		}catch (final Throwable e) {
			}
    	}
    }

    public static void main(final String[] args) {
        if (args.length < 4) usage();

        boolean signWholeFile = false;
        int argstart = 0;
        if (args[0].equals("-w")) {
            signWholeFile = true;
            argstart = 1;
        }

        if ((args.length - argstart) % 2 == 1) usage();
        final int numKeys = ((args.length - argstart) / 2) - 1;
        if (signWholeFile && numKeys > 1) {
            System.err.println("Only one key may be used with -w.");
            System.exit(2);
        }

        final String inputFilename = args[args.length-2];
        final String outputFilename = args[args.length-1];

        JarFile inputJar = null;
        FileOutputStream outputFile = null;
        int hashes = 0;

        try {
            final File firstPublicKeyFile = new File(args[argstart+0]);

            final X509Certificate[] publicKey = new X509Certificate[numKeys];
            try {
                for (int i = 0; i < numKeys; ++i) {
                    final int argNum = argstart + i*2;
                    publicKey[i] = readPublicKey(new File(args[argNum]));
                    hashes |= getAlgorithm(publicKey[i]);
                }
            } catch (final IllegalArgumentException e) {
                System.err.println(e);
                System.exit(1);
            }

            // Set the ZIP file timestamp to the starting valid time
            // of the 0th certificate plus one hour (to match what
            // we've historically done).
            final long timestamp = publicKey[0].getNotBefore().getTime() + 3600L * 1000;

            final PrivateKey[] privateKey = new PrivateKey[numKeys];
            for (int i = 0; i < numKeys; ++i) {
                final int argNum = argstart + i*2 + 1;
                privateKey[i] = readPrivateKey(new File(args[argNum]));
            }
            inputJar = new JarFile(new File(inputFilename), false);  // Don't verify.

            outputFile = new FileOutputStream(outputFilename);


            if (signWholeFile) {
                SignApk.signWholeFile(inputJar, firstPublicKeyFile,
                                      publicKey[0], privateKey[0], outputFile);
            } else {
                final JarOutputStream outputJar = new JarOutputStream(outputFile);

                // For signing .apks, use the maximum compression to make
                // them as small as possible (since they live forever on
                // the system partition).  For OTA packages, use the
                // default compression level, which is much much faster
                // and produces output that is only a tiny bit larger
                // (~0.1% on full OTA packages I tested).
                outputJar.setLevel(9);

                final Manifest manifest = addDigestsToManifest(inputJar, hashes);
                copyFiles(manifest, inputJar, outputJar, timestamp);
                signFile(manifest, inputJar, publicKey, privateKey, outputJar);
                outputJar.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (inputJar != null) inputJar.close();
                if (outputFile != null) outputFile.close();
            } catch (final IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}