package hc.server.msb;

import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.ExceptionViewer;
import hc.server.util.HCEventQueue;
import hc.server.util.HCLimitSecurityManager;

public class MSBException extends Error {
	Message msg;
	Processor proc;
	public static final int TOO_MUCH_FORWARD = 30;
	private static final Thread eventDispatchThread = HCLimitSecurityManager.getEventDispatchThread();
	private static final HCEventQueue hcEventQueue = HCLimitSecurityManager.getHCEventQueue();
	
	public static void init(){
		//do nothing,because had init field.
	}
	
	protected static final String UN_RE_DISPATCH = Message.class.getSimpleName() + " can't be in used more than two tasks";
	protected static final String UN_MODIFIED = Message.class.getSimpleName() + " can't be modified after dispatch or in process";
	protected static final String TOO_MUCH_FORWARD_STR = "too much times (" + TOO_MUCH_FORWARD + ") forwarding, may be in surround";
	protected static final String EMPTY_DEV_ID = Message.class.getSimpleName() + " can't be empty device ID";
//	protected static final String EMPTY_BIND_ID = Message.class.getSimpleName() + " error in isInitiative value of dispatch method.";
	protected static final String SAME_KEY_PROC = Message.class.getSimpleName() + " can't be added in same key processor";
	
	public MSBException(final String message, final Message msg, final Processor proc) {
        super(message);
        this.msg = msg;
        this.proc = proc;
        
        doExt();
    }
	
	public MSBException(final String message, final Throwable cause, final Message msg, final Processor proc) {
        super(message, cause);
        this.msg = msg;
        this.proc = proc;
        
        doExt();
    }
	
	public MSBException(final Throwable cause, final Message msg, final Processor proc) {
        super(cause);
        this.msg = msg;
        this.proc = proc;
        
        doExt();
    }
	
	private void doExt(){
		String tmp = getMessage();
		if(msg != null){
			tmp += " at " + msg.toString() + ",";
		}
		if(proc != null){
			tmp += " at " + proc.getIoTDesc();
		}else{
			ContextSecurityConfig csc = null;
			final Thread currentThread = Thread.currentThread();
			if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
				tmp += " in project[" + csc.projID + "]";
			}
		}
		
		ExceptionViewer.pushIn(tmp);
		
	}
}
