package hc.server.ui;

public class ResParameter {
	public ProjectContext ctx;
	protected final ResGlobalLock quesLock;

	public ResParameter(final ResGlobalLock quesLock){
		this.quesLock = quesLock;
	}
}
