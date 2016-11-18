package hc.server.ui;

public class QuestionParameter {
	public ProjectContext ctx;
	public Runnable yesRunnable;
	public Runnable noRunnable;
	public Runnable cancelRunnable;
	public final QuestionGlobalLock quesLock;
	
	public String questionDesc;
	
	public QuestionParameter(final QuestionGlobalLock quesLock){
		this.quesLock = quesLock;
	}
}
