package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.res.ImageSrc;
import hc.server.util.ServerCUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.Calendar;

public class LogServerSide implements ILog {
	private FileOutputStream outLogger = null;	
	private OutputStreamWriter osw;
	private BufferedWriter bw;
	
	public LogServerSide() {
		buildOutStream();
	}

	public void buildOutStream() {
		if(LogManager.INI_DEBUG_ON || (IConstant.isRegister() == false)){
		}else{
			final File filebak = new File(App.getBaseDir(), ImageSrc.HC_LOG_BAK);
			filebak.delete();
			
			File newLog = new File(App.getBaseDir(), ImageSrc.HC_LOG);
			if(newLog.exists()){
				newLog.renameTo(filebak);
				
				//迁移log1的密码到log2上
				final String p1 = PropertiesManager.getValue(PropertiesManager.p_LogPassword1);
				final String ca1 = PropertiesManager.getValue(PropertiesManager.p_LogCipherAlgorithm1, ServerCUtil.oldCipherAlgorithm);
				if(p1 != null){
					PropertiesManager.setValue(PropertiesManager.p_LogPassword2, p1);
				}
				PropertiesManager.setValue(PropertiesManager.p_LogCipherAlgorithm2, ca1);
				newLog = new File(App.getBaseDir(), ImageSrc.HC_LOG);
			}

			//记存log1的密码
			final String pwd = PropertiesManager.getValue(PropertiesManager.p_password);
			PropertiesManager.setValue(PropertiesManager.p_LogPassword1, pwd);
			PropertiesManager.setValue(PropertiesManager.p_LogCipherAlgorithm1, ServerCUtil.CipherAlgorithm);
			PropertiesManager.saveFile();
			
			FlushPrintStream printStream = null;
			try{
				outLogger = new FileOutputStream(newLog,false);
				final PrintStream ps = new PrintStream(ServerCUtil.encodeStream(outLogger, IConstant.getPasswordBS()));
				printStream = new FlushPrintStream(ps, this);
				osw = new OutputStreamWriter(ps, "UTF-8");
				bw = new BufferedWriter(osw);
				System.setOut(printStream);
				
			} catch (final Exception e) {
				App.showMessageDialog(null, e.toString());
				CCoreUtil.globalExit();
			}
	
			try{
				System.setErr(printStream);//new PrintStream((OutputStream)errLogger));
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
			
			//记录加密器信息到日志中
			final String encryptClass = (String)IConstant.getInstance().getObject("encryptClass");
			if(encryptClass != null){
				if(CUtil.getUserEncryptor() != null){
					L.V = L.O ? false : LogManager.log("Enable user Encryptor [" + encryptClass + "]");
				}else{
					LogManager.err("Error Load Encryptor [" + encryptClass + "]");
				}
			}else{
				L.V = L.O ? false : LogManager.log("use inner encryptor, no customized encryptor.");
			}
			
			if(PropertiesManager.isTrue(PropertiesManager.p_isReportException)){
				ExceptionReporter.appendToLog();
			}
		}
	}
	
	@Override
	public void exit(){
		if(outLogger != null){
			flush();
			
			try{
				outLogger.close();
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		
//		if(errLogger != null){
//			try{
//				errLogger.flush();
//				errLogger.close();
//			}catch (Exception e) {
//				
//			}
//		}
	}

	private final Calendar calendar = Calendar.getInstance();
	
	@Override
	public void log(final String msg){
		final StringBuilder sb = new StringBuilder(25 + msg.length() + 1);

		calendar.setTimeInMillis(System.currentTimeMillis());
		
		sb.append(calendar.get(Calendar.YEAR));
		sb.append("-");
		sb.append((calendar.get(Calendar.MONTH) + 1));
		sb.append("-");
		sb.append(calendar.get(Calendar.DAY_OF_MONTH));
		sb.append(" ");
		sb.append(calendar.get(Calendar.HOUR_OF_DAY));
		sb.append(":");
		sb.append(calendar.get(Calendar.MINUTE));
		sb.append(":");
		sb.append(calendar.get(Calendar.SECOND));
		sb.append(".");
		sb.append(calendar.get(Calendar.MILLISECOND));
		sb.append(" ");
		
		sb.append(msg);
		sb.append("\n");
		
		final String pMsg = sb.toString();
		try {
			if(osw != null){
				bw.append(pMsg);
			}else{
				System.out.print(pMsg);
			}
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	@Override
	public void errWithTip(final String msg){
		err(msg);
		ContextManager.displayMessage((String)ResourceUtil.get(IContext.ERROR), msg, IContext.ERROR, 0);
	}

	@Override
	public void err(final String msg) {
		flush();

		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final StringBuffer timestampBuf = new StringBuffer(25);
		timestampBuf.append((timestamp.getYear() + 1900) + "-");
		final int month = (timestamp.getMonth() + 1);
		timestampBuf.append((month < 10?("0"+month):month) + "-");
		final int day = timestamp.getDate();
		timestampBuf.append(day < 10?("0"+day):day);
		final int hour = timestamp.getHours();
		timestampBuf.append(" " + (hour < 10?("0"+hour):hour) + ":");
		final int minute = timestamp.getMinutes();
		timestampBuf.append((minute < 10?("0"+minute):minute) + ":");
		final int second = timestamp.getSeconds();
		timestampBuf.append((second < 10?("0"+second):second));
		final int nanos = timestamp.getNanos();
		timestampBuf.append("." + (nanos==0?"000":String.valueOf(nanos).substring(0, 3)));

		System.err.println(timestampBuf.toString() + ILog.ERR + msg);
		flush();
	}
	
	@Override
	public void info(final String msg) {
		
	}

	@Override
	public void flush() {
		if(outLogger != null){
			try{
				bw.flush();
//				osw.flush();
//				outLogger.flush();
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}		
	}

	@Override
	public void warning(final String msg) {
		log(WARNING + msg);
	}

}
