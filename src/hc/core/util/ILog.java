package hc.core.util;

public interface ILog {
	public static final String ERR = " ERR : ";
	public static final String WARNING = " Warning : ";
	public static final String OP_STR = " OP ";

	public void log(String msg);

	public void errWithTip(String msg);

	public void err(String msg);

	public void info(String msg);

	public void warning(String msg);

	public void flush();

	public void exit();
}
