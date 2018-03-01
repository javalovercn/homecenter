package hc.server.util;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SignItem {
	public X509Certificate chain;
	public PrivateKey privateKey;
	public String alias;

	public SignItem() {
	}

	public SignItem(final String alias, final X509Certificate cert, final PrivateKey privateKey) {
		this.alias = alias;
		this.chain = cert;
		this.privateKey = privateKey;
	}
}
