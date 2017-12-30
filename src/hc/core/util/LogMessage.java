package hc.core.util;

public class LogMessage {
	public final boolean isError;
	public final String msg;
	
	public LogMessage(final boolean isError, final String msg){
		this.isError = isError;
		this.msg = msg;
	}
}
