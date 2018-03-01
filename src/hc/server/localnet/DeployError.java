package hc.server.localnet;

import java.io.IOException;

public class DeployError extends IOException {
	public final byte[] errorBS;

	public DeployError(final byte[] errorbs) {
		this.errorBS = errorbs;
	}
}
