package hc.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Calendar;

import javax.swing.JPanel;

import hc.App;
import hc.core.HCConnection;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.StringBufferCacher;
import hc.res.ImageSrc;
import hc.server.TrayMenuUtil;
import hc.server.data.StoreDirManager;
import hc.server.util.ServerCUtil;

/**
 * log到文件或console。它被AndroidLogServerSide继承
 *
 */
public class LogServerSide implements ILog {
	private FileOutputStream outLogger = null;
	private OutputStreamWriter osw;
	private BufferedWriter bw;
	protected final boolean isLogToFile;

	public LogServerSide() {
		buildOutStream();
		isLogToFile = osw != null;
	}

	private final void copyToLogsArea(final File srcFile) {
		final String pwd = PropertiesManager.getValue(PropertiesManager.p_LogPassword2);
		if (pwd == null) {
			return;
		}

		final String cipherAlgorithm = PropertiesManager.getValue(PropertiesManager.p_LogCipherAlgorithm2);
		if (cipherAlgorithm == null) {
			return;
		}

		long createMS = ResourceUtil.getFileCreateTime(srcFile);
		if (createMS == 0) {
			createMS = srcFile.lastModified();
		}

		final File toLogFile = new File(StoreDirManager.LOGS_DIR, ResourceUtil.toYYYYMMDD_HHMMSS(createMS) + ".log");
		try {
			final byte[] pwdBS = ResourceUtil.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
			final InputStream is = ServerCUtil.decodeStream(new FileInputStream(srcFile), pwdBS, cipherAlgorithm);
			ResourceUtil.saveToFile(is, toLogFile);
		} catch (final Throwable e) {
		}
	}

	private final void deleteLogsLongTimeAgo() {
		final int maxDays = PropertiesManager.getIntValue(PropertiesManager.p_LogMaxDays, 20);
		final long tooLongMS = System.currentTimeMillis() - (HCTimer.ONE_DAY * maxDays);

		final File[] files = StoreDirManager.LOGS_DIR.listFiles();
		final int size = files.length;
		for (int i = 0; i < size; i++) {
			final File file = files[i];
			if (file.lastModified() < tooLongMS) {
				file.delete();
			}
		}
	}

	public final synchronized void buildOutStream() {
		final boolean isLog = ResourceUtil.isLoggerOn();
		if (isLog == false || LogManager.INI_DEBUG_ON || PropertiesManager.isSimu() || (IConstant.isRegister() == false)) {
			//to console
		} else {
			if (osw != null) {
				return;
			}

			deleteLogsLongTimeAgo();

			final File filebak = new File(ResourceUtil.getBaseDir(), ImageSrc.HC_LOG_BAK);
			if (filebak.exists()) {
				copyToLogsArea(filebak);
				filebak.delete();
			}

			File newLog = new File(ResourceUtil.getBaseDir(), ImageSrc.HC_LOG);
			if (newLog.exists()) {
				newLog.renameTo(filebak);

				// 迁移log1的密码到log2上
				final String p1 = PropertiesManager.getValue(PropertiesManager.p_LogPassword1);
				final String ca1 = PropertiesManager.getValue(PropertiesManager.p_LogCipherAlgorithm1, ServerCUtil.oldCipherAlgorithm);
				if (p1 != null) {
					PropertiesManager.setValue(PropertiesManager.p_LogPassword2, p1);
				}
				PropertiesManager.setValue(PropertiesManager.p_LogCipherAlgorithm2, ca1);
				newLog = new File(ResourceUtil.getBaseDir(), ImageSrc.HC_LOG);
			}

			// 记存log1的密码
			final String pwd = PropertiesManager.getValue(PropertiesManager.p_password);
			PropertiesManager.setValue(PropertiesManager.p_LogPassword1, pwd);
			PropertiesManager.setValue(PropertiesManager.p_LogCipherAlgorithm1, ServerCUtil.CipherAlgorithm);
			PropertiesManager.saveFile();

			FlushPrintStream printStream = null;
			try {
				outLogger = new FileOutputStream(newLog, false);
				final PrintStream ps = new PrintStream(ServerCUtil.encodeStream(outLogger, IConstant.getPasswordBS()));
				printStream = new FlushPrintStream(ps, this);
				osw = new OutputStreamWriter(ps, "UTF-8");
				bw = new BufferedWriter(osw);
				System.setOut(printStream);

			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);

				final JPanel panel = App.buildMessagePanel(e.toString(), App.getSysIcon(App.SYS_ERROR_ICON));
				App.showCenterPanelMain(panel, 0, 0, ResourceUtil.get(IConstant.ERROR), false, null, null, new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						CCoreUtil.globalExit();
					}
				}, null, null, false, true, null, false, false);

				// 等待上行关闭退出
				while (true) {
					try {
						Thread.sleep(100 * 1000);
					} catch (final InterruptedException e1) {
					}
				}
			}

			try {
				System.setErr(printStream);// new
											// PrintStream((OutputStream)errLogger));
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}

			// 记录加密器信息到日志中
			final String encryptClass = HCConnection.getEncryptorClass();
			if (encryptClass != null) {
				LogManager.log("customized encryptor [" + encryptClass + "]");
			} else {
				LogManager.log("use inner encryptor, no customized encryptor.");
			}

			if (PropertiesManager.isTrue(PropertiesManager.p_isReportException)) {
				ExceptionReporter.printReporterStatusToLog();
			}
		}
	}

	@Override
	public final void exit() {
		if (outLogger != null) {
			flush();

			try {
				outLogger.close();
			} catch (final Exception e) {
				// ExceptionReporter.printStackTrace(e);
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

	private final Calendar calendar = Calendar.getInstance();

	@Override
	public void log(final String msg) {// 被AndroidLogServerSide覆盖
		final StringBuilder sb = StringBuilderCacher.getFree();

		synchronized (calendar) {
			calendar.setTimeInMillis(System.currentTimeMillis());

			sb.append(calendar.get(Calendar.YEAR));
			sb.append("-");
			final int month = calendar.get(Calendar.MONTH) + 1;
			if (month < 10) {
				sb.append('0');
			}
			sb.append(month);
			sb.append("-");
			final int day = calendar.get(Calendar.DAY_OF_MONTH);
			if (day < 10) {
				sb.append('0');
			}
			sb.append(day);
			sb.append(" ");
			final int hour = calendar.get(Calendar.HOUR_OF_DAY);
			if (hour < 10) {
				sb.append('0');
			}
			sb.append(hour);
			sb.append(":");
			final int minute = calendar.get(Calendar.MINUTE);
			if (minute < 10) {
				sb.append('0');
			}
			sb.append(minute);
			sb.append(":");
			final int second = calendar.get(Calendar.SECOND);
			if (second < 10) {
				sb.append('0');
			}
			sb.append(second);
			sb.append(".");
			final int ms = calendar.get(Calendar.MILLISECOND);
			if (ms < 10) {
				sb.append('0');
				sb.append('0');
			} else if (ms < 100) {
				sb.append('0');
			}
			sb.append(ms);
			sb.append(" ");

			sb.append(msg);
			sb.append('\n');

			final String pMsg = sb.toString();
			try {
				if (osw != null) {
					bw.append(pMsg);
				} else {
					System.out.print(pMsg);
				}
			} catch (final IOException e) {
				// ExceptionReporter.printStackTrace(e);
			}
		}
		StringBuilderCacher.cycle(sb);
	}

	@Override
	public final void errWithTip(final String msg) {
		err(msg);
		TrayMenuUtil.displayMessage(ResourceUtil.get(IConstant.ERROR), msg, IConstant.ERROR, null, 0);
	}

	@Override
	public void err(final String msg) {// 被AndroidLogServerSide覆盖
		final StringBuffer sb = StringBufferCacher.getFree();

		synchronized (calendar) {
			calendar.setTimeInMillis(System.currentTimeMillis());

			sb.append(calendar.get(Calendar.YEAR));
			sb.append("-");
			final int month = calendar.get(Calendar.MONTH) + 1;
			if (month < 10) {
				sb.append('0');
			}
			sb.append(month);
			sb.append("-");
			final int day = calendar.get(Calendar.DAY_OF_MONTH);
			if (day < 10) {
				sb.append('0');
			}
			sb.append(day);
			sb.append(" ");
			final int hour = calendar.get(Calendar.HOUR_OF_DAY);
			if (hour < 10) {
				sb.append('0');
			}
			sb.append(hour);
			sb.append(":");
			final int minute = calendar.get(Calendar.MINUTE);
			if (minute < 10) {
				sb.append('0');
			}
			sb.append(minute);
			sb.append(":");
			final int second = calendar.get(Calendar.SECOND);
			if (second < 10) {
				sb.append('0');
			}
			sb.append(second);
			sb.append(".");
			final int ms = calendar.get(Calendar.MILLISECOND);
			if (ms < 10) {
				sb.append('0');
				sb.append('0');
			} else if (ms < 100) {
				sb.append('0');
			}
			sb.append(ms);

			sb.append(ILog.ERR);
			sb.append(msg);
			sb.append('\n');

			final String pMsg = sb.toString();
			try {
				if (osw != null) {
					bw.append(pMsg);
				} else {
					System.err.print(pMsg);
				}
			} catch (final Exception e) {
			}
		}
		StringBufferCacher.cycle(sb);

		flush();
	}

	@Override
	public final void info(final String msg) {

	}

	@Override
	public final void flush() {
		if (outLogger != null) {
			try {
				bw.flush();
				// osw.flush();
				// outLogger.flush();
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	}

	@Override
	public void warning(final String msg) {// 被AndroidLogServerSide覆盖
		log(WARNING + msg);
	}

}
