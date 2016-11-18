package hc.server;


public class HCJRubyException extends HCException {
	public final Throwable t;
	public final String scripts;
	
	public HCJRubyException(final CallContext callCtx){
		super("no message");
		this.t = callCtx.rubyThrowable;
		this.scripts = callCtx.rubyScripts;
	}
	
	public HCJRubyException(final CallContext callCtx, final String msg){
		super(msg);
		this.t = callCtx.rubyThrowable;
		this.scripts = callCtx.rubyScripts;
	}
}
