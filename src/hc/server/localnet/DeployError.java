package hc.server.localnet;

import java.io.IOException;

public class DeployError extends IOException {
	public final String error;
	
	public DeployError(final String error) {
		this.error = error;
	}
}
