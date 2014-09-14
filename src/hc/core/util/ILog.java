package hc.core.util;


public interface ILog {
	public static final String ERR = " ERR ";

	public void log(String msg);
	
	public void errWithTip(String msg);
	
	public void err(String msg);
	
	public void info(String msg);
	
	public void flush();
	
	public void exit();
}
