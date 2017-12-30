package hc.util;

import hc.core.util.ExceptionReporter;
import hc.core.util.ILog;
import hc.core.util.LogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogServerForRoot implements ILog {
	private FileOutputStream outLogger = null;

	public LogServerForRoot() {
		if (LogManager.INI_DEBUG_ON) {
		} else {
			final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss");

			final File newLog = new File("hc_" + df.format(new Date()) + ".log");

			PrintStream printStream = null;
			try {
				outLogger = new FileOutputStream(newLog, true);
				printStream = new PrintStream(outLogger);
				System.setOut(printStream);
			} catch (final IOException e) {
				ExceptionReporter.printStackTrace(e);
			}

			try {
				System.setErr(printStream);// new
											// PrintStream((OutputStream)errLogger));
			} catch (final Exception e) {

			}
		}
	}

	@Override
	public void exit() {
		if (outLogger != null) {
			try {
				outLogger.flush();
				outLogger.close();
			} catch (final Exception e) {
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

	@Override
	public void log(final String msg) {
		System.out.println((new Timestamp(System.currentTimeMillis()))
				.toString() + " " + msg);
	}

	@Override
	public void errWithTip(final String msg) {
		err(msg);
//		TrayMenuUtil.displayMessage((String) ResourceUtil.get(IContext.ERROR), msg, IContext.ERROR, null,	0);//RootServer close tip
	}


	@Override
	public void err(final String msg) {
		System.err.println((new Timestamp(System.currentTimeMillis()))
				.toString() + ILog.ERR + msg);
	}
	
	@Override
	public void info(final String msg) {

	}

	@Override
	public void flush() {
		if (outLogger != null) {
			try {
				outLogger.flush();
			} catch (final Exception e) {

			}
		}
	}

	@Override
	public void warning(final String msg) {
		System.out.println((new Timestamp(System.currentTimeMillis()))
				.toString() + ILog.WARNING + msg);		
	}

}
