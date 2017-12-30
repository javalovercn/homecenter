package hc.server.ui.design.hpj;

public abstract class EditorJumpRunnable implements Runnable {
	int offset;
	int len;
	Object userObj;
	
	public final void setLocation(final int offset, final int len){
		this.offset = offset;
		this.len = len;
	}

	public final void setUserObject(final Object userObj){
		this.userObj = userObj;
	}
}
