package hc.server.ui;

public class ResParameter {
	public ProjectContext ctx;
	public final ResGlobalLock quesLock;

	public ResParameter(final ResGlobalLock quesLock) {
		this.quesLock = quesLock;
	}
}
