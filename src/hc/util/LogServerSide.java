package hc.util;

import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.res.ImageSrc;
import hc.server.util.ServerCUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.sql.Timestamp;

import javax.swing.JOptionPane;

public class LogServerSide implements ILog {
	private FileOutputStream outLogger = null;	
	private OutputStreamWriter osw;
	private BufferedWriter bw;
	
	public LogServerSide() {
		buildOutStream();
	}

	public void buildOutStream() {
		if(LogManager.INI_DEBUG_ON || (IConstant.passwordBS == null)){
		}else{
			final File filebak = new File(ImageSrc.HC_LOG_BAK);
			filebak.delete();
			
			File newLog = new File(ImageSrc.HC_LOG);
			if(newLog.exists()){
				newLog.renameTo(filebak);
				
				//迁移log1的密码到log2上
				String p1 = PropertiesManager.getValue(PropertiesManager.p_LogPassword1);
				if(p1 != null){
					PropertiesManager.setValue(PropertiesManager.p_LogPassword2, p1);
				}
				newLog = new File(ImageSrc.HC_LOG);
			}

			//记存log1的密码
			String pwd = PropertiesManager.getValue(PropertiesManager.p_password);
			PropertiesManager.setValue(PropertiesManager.p_LogPassword1, pwd);
			PropertiesManager.saveFile();
			
			FlushPrintStream printStream = null;
			try{
				outLogger = new FileOutputStream(newLog,false);
				final PrintStream ps = new PrintStream(ServerCUtil.encodeStream((OutputStream)outLogger, IConstant.passwordBS));
				printStream = new FlushPrintStream(ps, this);
				osw = new OutputStreamWriter(ps, "UTF-8");
				bw = new BufferedWriter(osw);
				System.setOut(printStream);
				
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, e.toString());
				CCoreUtil.globalExit();
			}
	
			try{
				System.setErr(printStream);//new PrintStream((OutputStream)errLogger));
			}catch (Exception e) {
				
			}
			
			//记录加密器信息到日志中
			String encryptClass = (String)IConstant.getInstance().getObject("encryptClass");
			if(encryptClass != null){
				if(CUtil.userEncryptor != null){
					hc.core.L.V=hc.core.L.O?false:LogManager.log("Enable user Encryptor [" + encryptClass + "]");
				}else{
					LogManager.err("Error Load Encryptor [" + encryptClass + "]");
				}
			}else{
				hc.core.L.V=hc.core.L.O?false:LogManager.log("use inner encryptor, no customized encryptor.");
			}
		}
	}
	
	public void exit(){
		if(outLogger != null){
			try{
				bw.flush();
				osw.flush();
				outLogger.flush();
				outLogger.close();
			}catch (Exception e) {
				
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

	public void log(String msg){
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
		String pMsg = timestampBuf.toString() + " " + msg + "\n";
		try {
			if(osw != null){
				bw.append(pMsg);
			}else{
				System.out.print(pMsg);
			}
		} catch (IOException e) {
		}
	}
	
	public void errWithTip(String msg){
		err(msg);
		ContextManager.displayMessage((String)ResourceUtil.get(IContext.ERROR), msg, IContext.ERROR, 0);
	}

	public void err(String msg) {
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
	
	public void info(String msg) {
		
	}

	@Override
	public void flush() {
		if(outLogger != null){
			try{
				bw.flush();
				osw.flush();
				outLogger.flush();
			}catch (Exception e) {
				
			}
		}		
	}

}
