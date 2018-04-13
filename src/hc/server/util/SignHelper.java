package hc.server.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarFile;

import org.android.signapk.HCJarVerifier;
import org.android.signapk.InnerJarVerifier;
import org.android.signapk.SignApk;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;

public class SignHelper {

	// static {
	// Security.addProvider(SignHelper.bcProvider);
	// }

	/**
	 * 
	 * @return [pubKey, privateKey]
	 */
	public static SignItem generateKeys(final String x500Name, final String alias, final Date notBefore, final Date notAfter) {
		try {
			final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", BCProvider.getBCProvider());
			gen.initialize(1024);
			final KeyPair pair = gen.generateKeyPair();

			final X500Name subject = new X500Name(x500Name);

			final PublicKey publicKey = pair.getPublic();
			final PrivateKey privateKey = pair.getPrivate();
			final X509Certificate x509 = signerSelf(subject, publicKey, privateKey, notBefore, notAfter);

			return new SignItem(alias, x509, privateKey);
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static PrivateKey loadPrivateKey(final File file) throws Throwable {
		FileInputStream fis = null;
		try {
			// PKCS#8 format
			final String PEM_PRIVATE_START = "-----BEGIN PRIVATE KEY-----";
			final String PEM_PRIVATE_END = "-----END PRIVATE KEY-----";

			// PKCS#1 format
			final String PEM_RSA_PRIVATE_START = "-----BEGIN RSA PRIVATE KEY-----";
			final String PEM_RSA_PRIVATE_END = "-----END RSA PRIVATE KEY-----";

			final byte[] encodedKey = new byte[(int) file.length()];
			fis = new FileInputStream(file);
			fis.read(encodedKey);

			String privateKeyPem = StringUtil.bytesToString(encodedKey, 0, encodedKey.length);

			if (privateKeyPem.indexOf(PEM_PRIVATE_START) != -1) { // PKCS#8
																	// format
				privateKeyPem = privateKeyPem.replace(PEM_PRIVATE_START, "").replace(PEM_PRIVATE_END, "");
				privateKeyPem = privateKeyPem.replaceAll("\\s", "");

				final byte[] pkcs8EncodedKey = Base64.decode(privateKeyPem);

				final KeyFactory factory = KeyFactory.getInstance("RSA");
				return factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKey));

			} else {
				// PKCS#1 或其它
				final PEMParser pemParser = new PEMParser(new FileReader(file));
				final Object object = pemParser.readObject();
				final PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build("".toCharArray());// pass 3_homecenter.mobi.key
																													// PKCS#1
				final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BCProvider.getBCProvider());
				KeyPair kp;
				if (object instanceof PEMEncryptedKeyPair) {
					// Encrypted key - we will use provided password
					kp = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
				} else {
					// Unencrypted key - no password needed
					kp = converter.getKeyPair((PEMKeyPair) object);
				}
				return kp.getPrivate();
				// return getPrivateKeyBySun(PEM_RSA_PRIVATE_START,
				// PEM_RSA_PRIVATE_END, privateKeyPem);
			}
		} finally {
			try {
				fis.close();
			} catch (final Throwable e) {
			}
		}
	}

	// private static PrivateKey getPrivateKeyBySun(
	// final String PEM_RSA_PRIVATE_START,
	// final String PEM_RSA_PRIVATE_END, String privateKeyPem)
	// throws IOException, GeneralSecurityException,
	// NoSuchAlgorithmException, InvalidKeySpecException {
	// privateKeyPem = privateKeyPem.replace(PEM_RSA_PRIVATE_START,
	// "").replace(PEM_RSA_PRIVATE_END, "");
	// privateKeyPem = privateKeyPem.replaceAll("\\s", "");
	//
	// final DerInputStream derReader = new
	// DerInputStream(Base64.decode(privateKeyPem));
	//
	// final DerValue[] seq = derReader.getSequence(0);
	//
	// if (seq.length < 9) {
	// throw new GeneralSecurityException("Could not parse a PKCS1 private
	// key.");
	// }
	//
	// // skip version seq[0];
	// final BigInteger modulus = seq[1].getBigInteger();
	// final BigInteger publicExp = seq[2].getBigInteger();
	// final BigInteger privateExp = seq[3].getBigInteger();
	// final BigInteger prime1 = seq[4].getBigInteger();
	// final BigInteger prime2 = seq[5].getBigInteger();
	// final BigInteger exp1 = seq[6].getBigInteger();
	// final BigInteger exp2 = seq[7].getBigInteger();
	// final BigInteger crtCoef = seq[8].getBigInteger();
	//
	// final RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus,
	// publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
	//
	// final KeyFactory factory = KeyFactory.getInstance("RSA");
	//
	// return factory.generatePrivate(keySpec);
	// }

	/**
	 * 
	 * @param jarFile
	 * @param trustedSigner
	 * @return 当前jar正用的证书，可能多个；如果没有，则长度为0，非空。返回null表示验证失败
	 */
	public static X509Certificate[] verifyJar(final File jarFile, final X509Certificate[] trustedSigner) {
		X509Certificate[] certs;
		certs = new HCJarVerifier().verifyJar(jarFile, trustedSigner);
		if (certs != null) {
			if (L.isInWorkshop) {
				LogManager.log("pass verify signature : " + jarFile.getAbsolutePath() + " by " + HCJarVerifier.class.getSimpleName());
			}
			return certs;
		}

		certs = new InnerJarVerifier().verifyJar(jarFile, trustedSigner);
		if (certs != null) {
			LogManager.log("pass verify signature : " + jarFile.getAbsolutePath() + " by " + InnerJarVerifier.class.getSimpleName());
		}

		return certs;
	}

	public static void savePfx(final File pfxFile, final String password, final Vector<SignItem> items) throws Exception {
		// PublicKey pk = certificate.getPublicKey();
		final char[] pwdChars = toPfxPassword(password);

		final KeyStore store = KeyStore.getInstance("PKCS12");// 不能用"BC" provider，因为没有sign jar
		store.load(null, toPfxPassword(""));// initialized

		for (int i = 0; i < items.size(); i++) {
			final SignItem si = items.get(i);
			final X509Certificate[] chain = { si.chain };
			store.setKeyEntry(si.alias, si.privateKey, pwdChars, chain);
		}

		FileOutputStream fos;
		fos = new FileOutputStream(pfxFile);

		try {
			store.store(fos, pwdChars);
			fos.flush();
		} finally {
			try {
				fos.close();
			} catch (final Exception e) {
			}
		}
	}

	private static X509Certificate signerSelf(final X500Name subject, final PublicKey publicKey, final PrivateKey privateKey,
			final Date notBefore, final Date notAfter) throws Exception {
		// Supported Algorithms
		// By default, the jarsigner command signs a JAR file using one of the
		// following algorithms:
		// Digital Signature Algorithm (DSA) with the SHA1 digest algorithm
		// RSA algorithm with the SHA256 digest algorithm
		// Elliptic Curve (EC) cryptography algorithm with the SHA256 with
		// Elliptic Curve Digital Signature Algorithm (ECDSA).
		// final String signatureAlgorithm = "SHA1with" +
		// privateKey.getAlgorithm();
		final String signatureAlgorithm = "SHA512with" + privateKey.getAlgorithm();
		return signer(subject, publicKey, subject, privateKey, signatureAlgorithm, notBefore, notAfter);
	}

	private static X509Certificate signer(final X500Name subject, final PublicKey subjectPublicKey, final X500Name issuer,
			final PrivateKey issuerPrivateKey, final String signatureAlgorithm, final Date notBefore, final Date notAfter)
			throws Exception {

		final BigInteger sn = new BigInteger(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

		final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(subjectPublicKey.getEncoded());
		final ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(issuerPrivateKey);

		final X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuer, sn, notBefore, notAfter, subject, publicKeyInfo);

		// builder.addExtension(X509Extensions.SubjectKeyIdentifier, false, new
		// SubjectKeyIdentifierStructure(pubKey));

		final byte[] certBytes = builder.build(signer).getEncoded();

		final X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X509")
				.generateCertificate(new ByteArrayInputStream(certBytes));

		return certificate;
	}

	// Date notBefore = new Date();
	// Date notAfter = new Date(notBefore.getTime() + 365L * 24 * 60 * 60 *
	// 1000);
	public X509Certificate createCACert(final PublicKey publicKey, final PrivateKey privateKey, final Date notBefore, final Date notAfter,
			final String x500Name) throws Exception {
		// C=CN/ST=HuNan/L=ShaoYang/O=HomeCenter.MOBI CA/OU=HomeCenter.MOBI
		// CA/CN=homecenter.mobi
		// C=CountryCode/O=Organization/OU=OrganizationUnit/CN=CommonName
		final X500Name issuerName = new X500Name(x500Name);// "CN=www.mockserver.com,
															// O=MockServer,
															// L=London,
															// ST=England, C=UK"
		final X509Certificate cert = signerSelf(issuerName, publicKey, privateKey, notBefore, notAfter);
		cert.checkValidity(new Date());
		cert.verify(publicKey);

		return cert;
	}

	public static boolean sign(final File filePfx, final String password, final File inJar, final File outJar) throws Exception {
		if (inJar.equals(outJar)) {
			throw new Exception("inJar must unique to outJar");
		}

		final SignItem[] items = getContentformPfx(filePfx, password);

		if (items.length == 0) {
			throw new Exception("no alias (publicKey and privateKey) in pfx");
		}

		SignApk.sign(new JarFile(inJar, false), new FileOutputStream(outJar), items);
		return true;
	}

	public static final String FAIL_LOAD_PFX = "fail to open certificates in pfx file, maybe password error!";

	/**
	 * 
	 * @param filePfx
	 * @param strPassword
	 * @return return null or three object [X509Certificate[], PrivateKey[], String[alias]]
	 */
	public static SignItem[] getContentformPfx(final File filePfx, final String strPassword) throws Exception {
		FileInputStream fis = null;
		try {
			final KeyStore ks = KeyStore.getInstance("PKCS12");// 不能使用bcProvider
			fis = new FileInputStream(filePfx);

			final char[] pwdChars = toPfxPassword(strPassword);

			ks.load(fis, pwdChars);

			final Enumeration enumas = ks.aliases();
			final Vector<PrivateKey> out = new Vector<PrivateKey>();
			final Vector<X509Certificate> outPub = new Vector<X509Certificate>();
			final Vector<String> outAlias = new Vector<String>();
			while (enumas.hasMoreElements()) {
				final String keyAlias = (String) enumas.nextElement();
				outAlias.add(keyAlias);
				// System.out.println("alias=[" + keyAlias + "]");
				// Now once we know the alias, we could get the keys.
				// System.out.println("is key entry=" +
				// ks.isKeyEntry(keyAlias));
				final PrivateKey prikey = (PrivateKey) ks.getKey(keyAlias, pwdChars);
				out.add(prikey);
				final X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
				outPub.add(cert);
				// System.out.println("cert class = " +
				// cert.getClass().getName());
				// System.out.println("cert = " + cert);
				// System.out.println("public key = " + pubkey);
				// System.out.println("private key = " + prikey);
			}

			final int len = outAlias.size();
			final SignItem[] items = new SignItem[len];
			for (int i = 0; i < items.length; i++) {
				items[i] = new SignItem(outAlias.get(i), outPub.get(i), out.get(i));
			}

			return items;
		} catch (final Throwable e) {
			if (L.isInWorkshop) {
				e.printStackTrace();// maybe password error
			}
			throw new Exception(FAIL_LOAD_PFX);
		} finally {
			try {
				fis.close();
			} catch (final Throwable e) {
			}
		}
	}

	private static char[] toPfxPassword(final String strPassword) {
		char[] pwdChars = null;
		if ((strPassword == null) || strPassword.trim().equals("")) {// password
																		// is
																		// empty
																		// string
			pwdChars = null;
		} else {
			pwdChars = strPassword.toCharArray();
		}
		return pwdChars;
	}
}
