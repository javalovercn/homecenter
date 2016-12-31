package hc.server.ui.design.hpj;

public abstract class CSSJumpRunnable implements Runnable {
	int offset;
	int len;
	
	public final void setLocation(final int offset, final int len){
		this.offset = offset;
		this.len = len;
	}

}
