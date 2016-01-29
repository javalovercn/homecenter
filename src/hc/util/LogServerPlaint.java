package hc.util;

import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.res.ImageSrc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogServerPlaint implements ILog {
	private FileOutputStream outLogger = null;

	public LogServerPlaint() {
		if (LogManager.INI_DEBUG_ON) {
		} else {
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss");

			File newLog = new File("hc_" + df.format(new Date()) + ".log");

			PrintStream printStream = null;
			try {
				outLogger = new FileOutputStream(newLog, true);
				printStream = new PrintStream((OutputStream) outLogger);
				System.setOut(printStream);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				System.setErr(printStream);// new
											// PrintStream((OutputStream)errLogger));
			} catch (Exception e) {

			}
		}
	}

	public void exit() {
		if (outLogger != null) {
			try {
				outLogger.flush();
				outLogger.close();
			} catch (Exception e) {

			}
		}

		// if(errLogger != null){
		// try{
		// errLogger.flush();
		// errLogger.close();
		// }catch (Exception e) {
		//
		// }
		// }
	}

	public void log(String msg) {
		System.out.println((new Timestamp(System.currentTimeMillis()))
				.toString() + " " + msg);
	}

	public void errWithTip(String msg) {
		err(msg);
		ContextManager.displayMessage(
				(String) ResourceUtil.get(IContext.ERROR), msg, IContext.ERROR,
				0);
	}


	public void err(String msg) {
		System.err.println((new Timestamp(System.currentTimeMillis()))
				.toString() + ILog.ERR + msg);
	}
	
	public void info(String msg) {

	}

	@Override
	public void flush() {
		if (outLogger != null) {
			try {
				outLogger.flush();
			} catch (Exception e) {

			}
		}
	}

	@Override
	public void warning(String msg) {
		System.out.println((new Timestamp(System.currentTimeMillis()))
				.toString() + ILog.WARNING + msg);		
	}

}
