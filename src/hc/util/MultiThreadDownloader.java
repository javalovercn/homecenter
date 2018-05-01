package hc.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import hc.App;
import hc.core.IConstant;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.j2se.HCAjaxX509TrustManager;
import hc.server.util.HCJFrame;

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

	/**
	 * 注意：该类可能先于SecurityDataProtector执行，所以勿加载过多功能和逻辑。
	 */
	public MultiThreadDownloader() {
		progress.setStringPainted(true);
	}

	public JProgressBar getFinishPercent() {
		return progress;
	}

	public final void shutdown() {
		isCancel = true;
	}
	
	public static void main(final String[] args) {
//		final DownloadInfoPanel downInfoPanel = new DownloadInfoPanel("thisIsTest", "test_md5_12345678");
//		final MultiThreadDownloader m = new MultiThreadDownloader();
//		m.buildDownloadMsg(1234, 876543, System.currentTimeMillis(), downInfoPanel);
//		
//		final JPanel panel = new JPanel(new BorderLayout());
//		panel.add(new JProgressBar(0, 0, 100), BorderLayout.NORTH);
//		panel.add(downInfoPanel, BorderLayout.CENTER);
//		final ActionListener listener = null;
//		final JButton button;
//		final String downloading = ResourceUtil.get(9275);//downloading...，原为""download " + dnFileName"
//		App.showCenterPanelMain(panel, 0, 0, downloading, false, null, null, listener, listener, null, false, true,
//				null, false, false);
	}

	public void download(final Vector url_download, final File file, final CheckSum checkSum, final IBiz biz, final IBiz failBiz,
			final boolean isVisiable, final boolean isCancelableByUser) {
		this.fileName = file.getName();
		final int threadNum = url_download.size();
		dts = new DownloadThread[threadNum];

		LogManager.log("ready download file...");

		final String firstURL = (String) url_download.get(0);
		try {
			final URL url = new URL(firstURL);
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5000);
			HCAjaxX509TrustManager.setAjaxSSLSocketFactory(url, conn);
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
					if (threadId != (threadNum - 1)) {
						endIdx = startIdx + block;
						dts[threadId] = new DownloadThread(threadId, this, startIdx, endIdx, file,
								new URL((String) url_download.get(threadId)));
						startIdx = endIdx + 1;
					} else {
						dts[threadId] = new DownloadThread(threadId, this, startIdx, totalByted, file,
								new URL((String) url_download.get(threadId)));
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
		final DownloadInfoPanel downInfoPanel = new DownloadInfoPanel(dnFileName, checkSum.md5);
		buildDownloadMsg(downloadByte, totalByted, startMS, downInfoPanel);

		if (isVisiable) {
			final JPanel panel = new JPanel(new BorderLayout());
			panel.add(progress, BorderLayout.NORTH);
			panel.add(downInfoPanel, BorderLayout.CENTER);
			final ActionListener listener;
			final JButton button;
			if (isCancelableByUser) {
				button = App.buildDefaultCancelButton();
				listener = new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						isCancel = true;
						try {
							if (frame != null) {
								frame.dispose();
							}
							RootServerConnector.notifyLineOffType(SessionManager.getPreparedSocketSession(), "lof=MTD_cancel");
						} catch (final Exception ex) {
						}
					}
				};
			}else {
				button = App.buildDefaultCloseButton();
				listener = new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						try {
							if (frame != null) {
								frame.dispose();
							}
						} catch (final Exception ex) {
						}
					}
				};
			}
			final String downloading = ResourceUtil.get(9275);//downloading...，原为""download " + dnFileName"
			frame = (JFrame) App.showCenterPanelMain(panel, 0, 0, downloading, false, button, null, listener, listener, null, false, true,
					null, false, false);
		}
		refreshProgress = new Thread() {
			int ms = 0;
			int totalMS = 0;

			@Override
			public void run() {
				totalMS = totalByted / 1024 / 1024;
				while (isError == false && isCancel == false && (downloadByte < totalByted)) {
					try {
						Thread.sleep(1000);
					} catch (final Exception e) {
					}
					buildDownloadMsg(downloadByte, totalByted, startMS, downInfoPanel);
					final int percent = (int) process;
					progress.setValue(percent);
					progress.setString("" + percent + "%");// need by
															// JRubyInstaller
					progress.repaint();

					final int newMS = downloadByte / 1024 / 1024;
					if (newMS != ms) {
						ms = newMS;
						LogManager.log(fileName + " installed " + ms + "MB, total " + totalMS + "MS.");
					}
				}
				if (frame != null) {
					frame.dispose();
				}
				if (isError) {
					final String message = "Error on download file, please retry later.";
					LogManager.log(message);
					if (isVisiable) {
						App.showMessageDialog(null, message, ResourceUtil.get(IConstant.ERROR), JOptionPane.ERROR_MESSAGE,
								App.getSysIcon(App.SYS_ERROR_ICON));
					}
					if (failBiz != null) {
						failBiz.start();
					}
				} else if (isCancel) {
					return;
				} else {
					final String filemd5 = ResourceUtil.getMD5(file);
					final String filesha512 = ResourceUtil.getSHA512(file);
					if (checkSum.isEquals(filemd5, filesha512)) {
						biz.start();
					} else {
						LogManager.err("fail to checksum : " + fileName);
						RootServerConnector.notifyLineOffType(null, "lof=MTD_ERR_CHECKSUM");
						final String message = "File [" + fileName + "] MD5 error, please try download it later!";
						LogManager.log(message);
						if (isVisiable) {
							App.showMessageDialog(null, message, ResourceUtil.get(IConstant.ERROR), JOptionPane.ERROR_MESSAGE);
						}
						if (failBiz != null) {
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

	private final void buildDownloadMsg(final int readed, final int total, final long startMS, final DownloadInfoPanel downInfo) {
		final int readedSec = readed - lastDispReaded;
		lastDispReaded = readed;

		lastDispReadedArr[storeLastIdx % avgSecond] = readedSec;
		int lastFiveTotal = 0;
		for (int i = 0; i < avgSecond; i++) {
			lastFiveTotal += lastDispReadedArr[i];
		}
		storeLastIdx++;
		int avg = (storeLastIdx <= avgSecond) ? (lastFiveTotal / storeLastIdx) : (lastFiveTotal / avgSecond);
		avg = avg / 1024;
		process = (float) readed / total * 100;// 算出百分比
		final long costMS = System.currentTimeMillis() - startMS;
		final long leftSeconds = ((readed == 0) ? 3600 : ((costMS * total / readed - costMS) / 1000));
		final float totalM = (total * 1.0F) / 1024.0F / 1024.0F;
		final float readedM = (readed * 1.0F) / 1024.0F / 1024.0F;
		String downedTotal = ResourceUtil.get(9294);//注意：不宜带。号结束。download : {down}M, total : {total}M
		downedTotal = StringUtil.replace(downedTotal, "{down}", String.format("%.2f", readedM));
		downedTotal = StringUtil.replace(downedTotal, "{total}", String.format("%.2f", totalM));
		
		downInfo.totalDown.setText(downedTotal);
		
		downInfo.avgValue.setText(((costMS == 0) ? 0 : (avg)) + " KB/s");
		downInfo.costValue.setText(ResourceUtil.toHHMMSS((int) (costMS / 1000)));
		downInfo.leftValue.setText(ResourceUtil.toHHMMSS((int) (leftSeconds)));
	}
	
	public synchronized boolean searchNewTask(final DownloadThread dt) {
		final int fastAvg = dt.getAvgSpeed();

		int minspeedThreadid = -1;
		int minleftSecond = 999999999;
		for (int threadId = 0; threadId < dts.length; threadId++) {
			if (threadId != dt.threadId) {
				final DownloadThread dthread = dts[threadId];
				final int lefts = dthread.calLeftSecond();
				if (lefts == -1 && dthread.end > dthread.start) {
					minspeedThreadid = threadId;
					break;
				} else if (lefts > 0) {
					if (lefts < minleftSecond && lefts > 10) {
						if (dthread.getAvgSpeed() < fastAvg) {
							minleftSecond = lefts;
							minspeedThreadid = threadId;
						}
					}
				}
			}
		}
		if (minspeedThreadid == -1) {
			return false;
		}

		// System.out.println("find min speed threadID : " + minspeedThreadid +
		// ", has " + minleftSecond + " seconds task.");
		final DownloadThread cutDT = dts[minspeedThreadid];
		final int avgSpeed = cutDT.getAvgSpeed();
		if (avgSpeed == -1) {
			if (cutDT.end > cutDT.start) {
				dt.end = cutDT.end;
				dt.start = cutDT.start + cutDT.downloadBS;
				cutDT.end = cutDT.start;
				// System.out.println("move all left from ThreadID:" +
				// cutDT.threadId + ", downloaded : " + cutDT.downloadBS);
				return true;
			}
		} else if (avgSpeed != 0) {
			final int leftBS = cutDT.end - cutDT.start - cutDT.downloadBS;
			final int cutBS = leftBS * fastAvg / (avgSpeed + fastAvg);
			dt.end = cutDT.end;
			cutDT.end -= cutBS;
			// System.out.println("ThreadID : " + cutDT.threadId + " start[" +
			// cutDT.start +"," + cutDT.end + "].");
			dt.start = cutDT.end + 1;
			// System.out.println("ThreadID : " + minspeedThreadid + " cut " +
			// cutBS + " bytes.");
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

	public int getAvgSpeed() {
		if (isError) {
			return -1;
		}
		if (downloadBS == 0) {
			return 0;
		}
		final int speed = (int) (downloadBS / 1024 * 1000 / (System.currentTimeMillis() - startMS));
		// System.out.println("ThreadID : " + threadId + " avgSpeed : " + speed
		// + "KB/s");
		return speed;
	}

	public int calLeftSecond() {
		if (isError) {
			return -1;
		}
		if (downloadBS == 0) {
			return 0;
		}
		final int avgSpeed = getAvgSpeed();
		if (avgSpeed <= 0) {
			return -1;
		}
		final int leftBytes = end - start - downloadBS;
		final int seconds = leftBytes / 1024 / avgSpeed;
		// System.out.println("ThreadID :" + threadId + " has left seconds:" +
		// seconds + ", leftBytes : " + leftBytes);
		return seconds;
	}

	public DownloadThread(final int threadId, final MultiThreadDownloader main, final int start, final int end, final File file,
			final URL url) {
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

			while (getNewTask) {
				getNewTask = false;
				// System.out.println("ThreadID : " + threadId + " starting
				// block[" + start + "," + end + "].");

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
		if (raf != null) {
			try {
				raf.close();
			} catch (final Throwable e) {
			}
		}
	}

	public HttpURLConnection downloadHttp206(final RandomAccessFile raf) throws IOException, ProtocolException, Exception {
		HttpURLConnection conn = null;
		conn = (HttpURLConnection) url.openConnection();
		HCAjaxX509TrustManager.setAjaxSSLSocketFactory(url, conn);
		conn.setRequestMethod("GET");
		// conn.setReadTimeout(0);//无穷
		conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
		if (conn.getResponseCode() == 206) {
			raf.seek(start);
			InputStream inStream = conn.getInputStream();
			final byte[] b = new byte[1024 * 10];
			int len = 0;
			boolean hasIOException = false;

			do {
				hasIOException = false;
				try {
					while ((!main.isCancel) && ((start + downloadBS) <= end) && (len = inStream.read(b)) != -1) {
						raf.write(b, 0, len);
						synchronized (main) {
							main.downloadByte += len;
						}
						downloadBS += len;

						// if(threadId == 0 && downloadBS > 1024 * 1024 * 2){
						// //System.out.println("ThreadID : 3 raise
						// exception.");
						// throw new IOException();
						// }
						// if(threadId == 2){
						// try{
						// Thread.sleep(100);
						// }catch (Exception e) {
						// }
						// }else if(threadId == 3){
						// if(downloadBS > 100000){
						// //System.out.println("ThreadID : 3 raise
						// exception.");
						// throw new Exception();
						// }
						// }else{
						// try{
						// Thread.sleep(20);
						// }catch (Exception e) {
						// }
						// }
					}
				} catch (final IOException e) {
					LogManager.log("JRuby IOException, try...");
					hasIOException = true;
					start += downloadBS;
					downloadBS = 0;

					try {
						conn.disconnect();
					} catch (final Exception ex) {
					}
					try {
						Thread.sleep(5000);
					} catch (final Exception ex) {
					}

					conn = (HttpURLConnection) url.openConnection();
					HCAjaxX509TrustManager.setAjaxSSLSocketFactory(url, conn);
					conn.setRequestMethod("GET");
					// conn.setReadTimeout(0);//无穷
					conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
					inStream = conn.getInputStream();
				}
			} while (hasIOException);
		} else {
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