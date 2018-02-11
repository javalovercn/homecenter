package hc.server.ui;

public class QuestionParameter extends ResParameter{
	public Runnable yesRunnable;
	public Runnable noRunnable;
	public Runnable cancelRunnable;
	
	public String questionDesc;
	final int questionID;
	
	public QuestionParameter(final QuestionGlobalLock quesLock, final int questionID){
		super(quesLock);
		this.questionID = questionID;
	}
	
	public final QuestionGlobalLock getGlobalLock(){
		return (QuestionGlobalLock)quesLock;
	}
	
	@Override
	public String toString() {
		return "Question [" + questionID + "]";
	}
}
