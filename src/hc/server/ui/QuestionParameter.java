package hc.server.ui;

public class QuestionParameter extends ResParameter{
	public Runnable yesRunnable;
	public Runnable noRunnable;
	public Runnable cancelRunnable;
	
	public String questionDesc;
	
	public QuestionParameter(final QuestionGlobalLock quesLock){
		super(quesLock);
	}
	
	public final QuestionGlobalLock getGlobalLockMaybeNull(){
		return (QuestionGlobalLock)quesLock;
	}
}
