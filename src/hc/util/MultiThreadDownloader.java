package hc.util;

import hc.App;
import hc.core.IContext;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.util.LogManager;
import hc.server.HCActionListener;
import hc.server.util.HCJFrame;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class MultiThreadDownloader {
	private JFrame frame = new HCJFrame();
	private final JProgressBar progress = new JProgressBar(0, 100);
	private int totalByted;
	int downloadByte;
	boolean isError;
	boolean isCancel;
	Thread refreshProgress = null;
	String fileName;
	DownloadThread[] dts;
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	
	public MultiThreadDownloader(){
		progress.setStringPainted(true);
	}
	
	public JProgressBar getFinishPercent(){
		return progress;
	}
	
	public void download(final Vector url_download, final File file, final String md5,
			final IBiz biz, final IBiz failBiz, final boolean isVisiable, final boolean isCancelableByUser) {
		this.fileName = file.getName();
		final int threadNum = url_download.size();
		dts = new DownloadThread[threadNum];
		
		LogManager.log("ready download file...");
		
        final String firstURL = (String)url_download.get(0);
        try {  
			final URL url = new URL(firstURL);  
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
            conn.setRequestMethod("GET");  
            conn.setReadTimeout(5000);  
            if (conn.getResponseCode() == 200) {  
                totalByted = conn.getContentLength();  
                final RandomAccessFile raf = new RandomAccessFile(file, "rw");  
                raf.setLength(totalByted);  
                raf.close();
                LogManager.log("create file [" + fileName + "] for download.");
                final int block = totalByted / threadNum;  
                int startIdx = 0;
                int endIdx = 0;
                for (int threadId = 0; threadId < threadNum; threadId++) { 
                	if(threadId != (threadNum - 1)){
                		endIdx = startIdx + block;
                		dts[threadId] = new DownloadThread(threadId, this, startIdx, endIdx, file, new URL((String)url_download.get(threadId)));
                		startIdx = endIdx + 1;
                	}else{
                		dts[threadId] = new DownloadThread(threadId, this, startIdx, totalByted, file, new URL((String)url_download.get(threadId)));
                	}
                	dts[threadId].start();
                	LogManager.log("create a download thread and start.");
                }  
            }  
        } catch (final Exception e) {  
        	LogManager.log("fail to connect to : " + firstURL);
            isError = true;  
        }  
        
		final long startMS = System.currentTimeMillis();
		final String dnFileName = firstURL.substring(firstURL.lastIndexOf("/") + 1);
		final String desc_str = buildDownloadMsg(dnFileName, fileName, md5, downloadByte, totalByted, startMS);
        final JLabel desc = new JLabel();
		desc.setText(desc_str);

		if(isVisiable){
	        final JPanel panel = new JPanel(new BorderLayout());
	        panel.add(progress, BorderLayout.NORTH);
	        panel.add(desc, BorderLayout.CENTER);
	        final ActionListener listener = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					isCancel = true;
					try{
						if(frame != null){
							frame.dispose();
						}
						RootServerConnector.notifyLineOffType(SessionManager.getPreparedSocketSession(), "lof=MTD_cancel");
					}catch (final Exception ex) {
					}
				}
			}, threadPoolToken);

			final JButton button = new JButton((String)ResourceUtil.get(1018));
			if(isCancelableByUser == false){
				button.setEnabled(false);
			}
			frame = (JFrame)App.showCenterPanelMain(panel, 0, 0, "download " + dnFileName, false, 
        		button, null, listener, listener, null, false, true, null, false, false);
		}
        refreshProgress = new Thread(){
        	int ms = 0;
        	int totalMS = 0; 
        	@Override
			public void run(){
        		totalMS = totalByted / 1024 / 1024;
        		while(isError == false && isCancel == false && (downloadByte < totalByted)){
        			try{
        				Thread.sleep(1000);
        			}catch (final Exception e) {
					}
        			final String desc_str = buildDownloadMsg(dnFileName, fileName, md5, downloadByte, totalByted, startMS);
        			desc.setText(desc_str);
        			final int percent = (int)process;
					progress.setValue(percent);
					progress.setString("" + percent + "%");//need by JRubyInstaller
					progress.repaint();
					
        			final int newMS = downloadByte / 1024 / 1024;
        			if(newMS != ms){
        				ms = newMS;
        				LogManager.log(fileName + " installed " + ms + "MB, total " + totalMS + "MS.");
        			}
        		}
        		if(frame != null){
        			frame.dispose();
        		}
        		if(isError){
    				final String message = "Error on download file, please retry later.";
    				LogManager.log(message);
        			if(isVisiable){
						App.showMessageDialog(null, message, 
        						(String)ResourceUtil.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
        			}
        			if(failBiz != null){
        				failBiz.start();
        			}
        		}else if(isCancel){
        			return;
        		}else{
        			final String filemd5 = ResourceUtil.getMD5(file);
        			if(filemd5.toLowerCase().equals(md5.toLowerCase())){
        				biz.start();
        			}else{
        				RootServerConnector.notifyLineOffType(SessionManager.getPreparedSocketSession(), "lof=MTD_ERR_MD5");
    					final String message = "File [" + fileName + "] MD5 error, please try download it later!";
    					LogManager.log(message);
        				if(isVisiable){
							App.showMessageDialog(null, message, 
        						(String)ResourceUtil.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE);
        				}
        				if(failBiz != null){
            				failBiz.start();
            			}
        			}
        		}
        	}
        };
        refreshProgress.start();
    }  
	
	int lastDispReaded;
	final int avgSecond = 5;
	int[] lastDispReadedArr = new int[avgSecond];
	int storeLastIdx = 0;
	float process;
	
	private String buildDownloadMsg(final String fromURL, final String storeFile, final String md5, 
			final int readed, final int total, final long startMS){
		final int readedSec = readed - lastDispReaded;
		lastDispReaded = readed;
		
		lastDispReadedArr[storeLastIdx%avgSecond] = readedSec;
		int lastFiveTotal = 0;
		for (int i = 0; i < avgSecond; i++) {
			lastFiveTotal += lastDispReadedArr[i];
		}
		storeLastIdx++;
		int avg = (storeLastIdx<=avgSecond)?(lastFiveTotal/storeLastIdx):(lastFiveTotal/avgSecond);
		avg = avg/1024;
		String out = "<html><BR>";
		process = (float) readed / total * 100;// 算出百分比
		final long costMS = System.currentTimeMillis() - startMS;
		final long leftSeconds = ((readed==0)?3600:((costMS * total / readed - costMS) / 1000));
		final float totalM = (total * 1.0F) / 1024.0F / 1024.0F;
		final float readedM = (readed * 1.0F) / 1024.0F / 1024.0F;
		out += "<STRONG>downloaded :    " + String.format("%.2f", readedM) + "M / " + String.format("%.2f", totalM) + "M, </STRONG><BR><BR>";
		out += "source :      " + fromURL + "<BR>";
		//out += "Download to :   " + storeFile + "<BR>";
		out += "md5 :           " + md5 + "<BR>";
 		out += "speed :         " + ((costMS==0)?0:(avg)) + " KB/s<BR>";
		out += "cost time : " + toHHMMSS((int)(costMS / 1000)) + "<BR>";
		out += "time left :     " + toHHMMSS((int)(leftSeconds)) + "<BR>";
		
		out += "</html>";
		return out;
	}
	private static String toHHMMSS(final int timeSecond){
		final int hour = timeSecond / 60 / 60;
		final int minute = (timeSecond - hour * 60) / 60;
		final int second = timeSecond % 60;
		return (hour > 9?String.valueOf(hour):"0"+String.valueOf(hour)) + ":" + 
				(minute > 9?String.valueOf(minute):"0"+String.valueOf(minute)) + ":" + 
				(second > 9?String.valueOf(second):"0"+String.valueOf(second));
	}
	
	public synchronized boolean searchNewTask(final DownloadThread dt){
		final int fastAvg = dt.getAvgSpeed();
		
		int minspeedThreadid = -1;
		int minleftSecond = 999999999;
		for (int threadId = 0; threadId < dts.length; threadId++) { 
			if(threadId != dt.threadId){
				final DownloadThread dthread = dts[threadId];
				final int lefts = dthread.calLeftSecond();
				if(lefts == -1 && dthread.end > dthread.start){
					minspeedThreadid = threadId;
					break;
				}else if(lefts > 0){
					if(lefts < minleftSecond && lefts > 10){
						if(dthread.getAvgSpeed() < fastAvg){
							minleftSecond = lefts;
							minspeedThreadid = threadId;
						}
					}
				}
			}
		}
		if(minspeedThreadid == -1){
			return false;
		}
		
		//System.out.println("find min speed threadID : " + minspeedThreadid + ", has " + minleftSecond + " seconds task.");
		final DownloadThread cutDT = dts[minspeedThreadid];
		final int avgSpeed = cutDT.getAvgSpeed();
		if(avgSpeed == -1){
			if(cutDT.end > cutDT.start){
				dt.end = cutDT.end;
				dt.start = cutDT.start + cutDT.downloadBS;
				cutDT.end = cutDT.start;
				//System.out.println("move all left from ThreadID:" + cutDT.threadId + ", downloaded : " + cutDT.downloadBS);
				return true;
			}
		}else if(avgSpeed != 0){
			final int leftBS = cutDT.end - cutDT.start - cutDT.downloadBS;
			final int cutBS = leftBS * fastAvg / (avgSpeed + fastAvg);
			dt.end = cutDT.end;
			cutDT.end -= cutBS;
			//System.out.println("ThreadID : " + cutDT.threadId + " start[" + cutDT.start +"," + cutDT.end + "].");
			dt.start = cutDT.end + 1;
			//System.out.println("ThreadID : " + minspeedThreadid + " cut " + cutBS + " bytes.");
			return true;
		}
		
		return false;
	}
}
class DownloadThread extends Thread {  
    int start, end;  
    File file = null;  
    URL url = null;  
    MultiThreadDownloader main;
    long startMS;
    int downloadBS;
    final int threadId;
    boolean isError = false;
    
    public int getAvgSpeed(){
    	if(isError){
    		return -1;
    	}
    	if(downloadBS == 0){
    		return 0;
    	}
    	final int speed = (int)(downloadBS / 1024 * 1000 / (System.currentTimeMillis() - startMS));
    	//System.out.println("ThreadID : " + threadId + " avgSpeed : " + speed + "KB/s");
		return speed;
    }
    
    public int calLeftSecond(){
    	if(isError){
    		return -1;
    	}
    	if(downloadBS == 0){
    		return 0;
    	}
    	final int avgSpeed = getAvgSpeed();
    	if(avgSpeed <= 0){
    		return -1;
    	}
		final int leftBytes = end - start - downloadBS;
		final int seconds = leftBytes / 1024 / avgSpeed;
    	//System.out.println("ThreadID :" + threadId + " has left seconds:" + seconds + ", leftBytes : " + leftBytes);
    	return seconds;
    }
    
    public DownloadThread(final int threadId, final MultiThreadDownloader main, final int start, final int end, final File file, final URL url) {  
        this.main = main;
        this.threadId = threadId;
    	this.start = start;
    	this.end = end;
        this.file = file;  
        this.url = url;  
    }  
  
    @Override
	public void run() {
    	RandomAccessFile raf = null;
    	boolean getNewTask = true;
        try {  
            raf = new RandomAccessFile(file, "rw");  

            while(getNewTask){
	            getNewTask = false;
	            //System.out.println("ThreadID : " + threadId + " starting block[" + start + "," + end + "].");
	            
	            resetSpeed();

	            downloadHttp206(raf);

            	getNewTask = main.searchNewTask(this);
            }
        } catch (final Exception e) { 
        	System.err.println("Error multi-thread download source:" + url.toString());
        	isError = true;
        	main = null;
        	RootServerConnector.notifyLineOffType(SessionManager.getPreparedSocketSession(), "lof=MTD_" + url.toString());
        }  
        if(raf != null){
	        try {
				raf.close();
			} catch (final Throwable e) {
			}
        }
    }

	public HttpURLConnection downloadHttp206(final RandomAccessFile raf)
			throws IOException, ProtocolException, Exception {
    	HttpURLConnection conn = null;
		conn = (HttpURLConnection) url.openConnection();  
		conn.setRequestMethod("GET");  
//		conn.setReadTimeout(0);//无穷  
		conn.setRequestProperty("Range", "bytes=" + start + "-" + end);  
		if (conn.getResponseCode() == 206) {  
		    raf.seek(start);  
		    InputStream inStream = conn.getInputStream();  
		    final byte[] b = new byte[1024 * 10];  
		    int len = 0;  
		    boolean hasIOException = false;
		    
		    do{
		    	hasIOException = false;
		    	try{
				    while ((!main.isCancel) && ((start + downloadBS) <= end) && (len = inStream.read(b)) != -1) {  
				    	raf.write(b, 0, len);  
				        synchronized (main) {
				        	main.downloadByte += len;
						}
				        downloadBS += len;
				        
//				        if(threadId == 0 && downloadBS > 1024 * 1024 * 2){
//                    		//System.out.println("ThreadID : 3 raise exception.");
//                    		throw new IOException();
//                    	}
		//	                    if(threadId == 2){
		//	                    	try{
		//	                    		Thread.sleep(100);
		//	                    	}catch (Exception e) {
		//							}
		//	                    }else if(threadId == 3){
		//	                    	if(downloadBS > 100000){
		//	                    		//System.out.println("ThreadID : 3 raise exception.");
		//	                    		throw new Exception();
		//	                    	}
		//	                    }else{
		//	                    	try{
		//	                    		Thread.sleep(20);
		//	                    	}catch (Exception e) {
		//							}
		//	                    }
				    } 
		    	}catch (final IOException e) {
		    		LogManager.log("JRuby IOException, try...");
		    		hasIOException = true;
		    		start += downloadBS;
		    		downloadBS = 0;
		    		
		    		try{
		    			conn.disconnect();
		    		}catch (final Exception ex) {
					}
		    		try{
		    			Thread.sleep(5000);
		    		}catch (final Exception ex) {
					}
		    		
		    		conn = (HttpURLConnection) url.openConnection();  
		    		conn.setRequestMethod("GET");  
//		    		conn.setReadTimeout(0);//无穷  
		    		conn.setRequestProperty("Range", "bytes=" + start + "-" + end);  
	    		    inStream = conn.getInputStream();  
				}
		    }while(hasIOException);
		}else{
			throw new Exception("No 206");
		}
		conn.disconnect();
		return conn;
	}

	private void resetSpeed() {
		startMS = System.currentTimeMillis();
		downloadBS = 0;
	}  
}  