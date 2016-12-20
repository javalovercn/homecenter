package hc.server.ui;

public class DialogParameter extends ResParameter {
	
	public DialogParameter(final DialogGlobalLock quesLock){
		super(quesLock);
	}
	
	public final DialogGlobalLock getGlobalLock(){
		return (DialogGlobalLock)quesLock;
	}
}
