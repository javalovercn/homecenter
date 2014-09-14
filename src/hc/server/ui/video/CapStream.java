package hc.server.ui.video;

import hc.core.L;
import hc.core.RemoveableHCTimer;
import hc.core.util.LogManager;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Controller;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.Player;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.FormatControl;
import javax.media.control.FrameGrabbingControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.CaptureDevice;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.SourceCloneable;

import jmapps.util.JMFUtils;

public class CapStream implements ControllerListener, DataSinkListener {
	private DataSource dataSource = null;
	private Processor processor = null;
	private DataSink dataSink = null;
	private String strContentType = null;
	private String strContentTypeExt = null;
	private boolean boolSaving = false;
	private ProgressThread threadProgress = null;
	private Player player = null;
	private String strFailMessage = null;
	private int capStatus = CAP_NO_WORKING;
	public static final int CAP_NO_WORKING = 0, CAP_RECORDING = 1, CAP_PAUSEING = 2, CAP_DISABLE = 3, CAP_ENABLE = 4;
	private FrameGrabbingControl controlGrabber;
	private static CapStream instance;
	private HCBufferToImage bufferToImage;
	private BufferedImage snapBI;
//	private Graphics snapGraphics;
	private int snapWidth, snapHeight;
	private int[] rgbArray;
//	private int[] rgbSampleArray;
	private String strFileName = null;

	public String getCurrRecordFileNameNoExt(){
		if(strFileName != null){
			return strFileName.substring(0, strFileName.lastIndexOf("."));
		}else{
			return null;
		}
	}
	
	boolean stateFailed = false;
	final String strDeviceName;
	final Format format;

	private String strDirName;
	private DataSource cloneabledatasource = null;
	private DataSource cloneddatasource = null;
	private RemoveableHCTimer autoDetector, hourCutTimer;
	
	// private Component monitor = null;
	private static DataSource createDataSource(MediaLocator sourceLocator)
			throws IOException, NoDataSourceException {
		DataSource source = null;
		for (Enumeration protoList = Manager.getDataSourceList(
				sourceLocator.getProtocol()).elements(); protoList
				.hasMoreElements();) {
			String protoClassName = (String) protoList.nextElement();
			try {
				Class protoClass = Class.forName(protoClassName);
				source = (DataSource) protoClass.newInstance();
				source.setLocator(sourceLocator);
				// source.connect();
				break;
			} catch (Throwable e) {
				source = null;
				String err = "Error instantiating class: " + protoClassName
						+ " : " + e;
				throw new NoDataSourceException(err);
			}
		}

		if (source == null) {
			throw new NoDataSourceException("Cannot find a DataSource for: "
					+ sourceLocator);
		} else {
			return source;
		}
	}

	public DataSource initializeCaptureDataSource() {
		DataSource ds = null;
		if (strDeviceName == null)
			return null;
		if (format == null)
			return ds;
		try {
			ds = JMFUtils.createCaptureDataSource(null, null, strDeviceName, format); 
			if (ds == null)
				return null;
		} catch (Throwable ndse) {
			return null;
		}
		return ds;
	}

	/**
	 * 如果是isShutdown模式调用，则有可能返回null对象
	 * 如果设备禁用，则返回null对象
	 * @param isShutdown
	 * @return
	 */
	public static CapStream getInstance(boolean isShutdown) {
		if (instance == null && (isShutdown == false)) {
			instance = buildCapStream();
		}
		return instance;
	}

	private static CapStream getInstance(CapStream cs) {
		if (instance == null) {
			instance = cs;
			return cs;
		}
		return instance;
	}

	public int getCapStatus() {
		return capStatus;
	}

	public static CapStream buildCapStream() {
		final CaptureConfig cc = CaptureConfig.getInstance();
		if (cc.useVideo == false) {
			return null;
		}
		return new CapStream(cc.strDeviceName, (VideoFormat)cc.formatDefault);
	}

	CaptureDeviceInfo cdi;

	private Format getRGBOrYUVFormat(VideoFormat vf) {
		final CaptureDeviceInfo deviceInfo = CaptureDeviceManager.getDevice(strDeviceName);
		if (deviceInfo != null) {
			Format[] vfs = deviceInfo.getFormats();
			for (int j = 0; j < vfs.length; j++) {
				final Format format2 = vfs[j];
				if (format2.matches(vf)) {
					final VideoFormat format22 = (VideoFormat) format2;
					if (format22.getSize().equals(vf.getSize())) {
						this.cdi = deviceInfo;
						final String encoding = vf.getEncoding();
						if (encoding.equalsIgnoreCase(format22.getEncoding())) {
							return format22;
						}
					}
				}
			}
		}
		return null;
	}

	private static final String STR_MSVIDEO = "MSVideo (avi)";
	private static final String STR_QUICKTIME = "QuickTime (mov)";
	public static final String VIDEO_PREFIX = "Capture";
	public static final String SNAP_PREFIX = "Snap";
	public static final String SNAP_END = ".jpg";
	public static final String VIDEO_END = ".mov";

	public CapStream(String strDeviceName, VideoFormat formatDefault) {
		this.strDeviceName = strDeviceName;
		this.format = getRGBOrYUVFormat(formatDefault);
	}

	private boolean init() {
		if (this.dataSource == null) {
			final Object[] ds = new Object[2];
			new Thread() {
				public void run() {
					ds[0] = initializeCaptureDataSource();
					try {
						Thread.sleep(500);
					} catch (Exception e) {
					}
					if (ds[1] != null) {
						DataSource dsource = (DataSource) ds[0];
						dsource.disconnect();
					}
				}
			}.start();

			int countSum = 0;
			final int millis = 100;
			while (countSum < 10000) {
				try {
					Thread.sleep(millis);
				} catch (Exception e) {
				}
				if (ds[0] != null) {
					this.dataSource = (DataSource) ds[0];
					break;
				}
				countSum += millis;
			}

			if (this.dataSource == null) {
				ds[1] = "fail";
				return false;
			}
			cloneabledatasource = Manager.createCloneableDataSource(dataSource);
		}

		this.strContentType = this.dataSource.getContentType();
		try {
			this.processor = Manager.createProcessor(this.cloneabledatasource);
		} catch (Exception exception) {
			L.V = L.O ? false : LogManager.log("jmstudio:" + exception);
			exception.printStackTrace();
			return false;
		}
		this.processor.addControllerListener(this);

		boolean boolResult = waitForState(this.processor, 180);
		if (!(boolResult)) {
			return false;
		}

		changeContentType();

		return true;
	}

	public static void waitForStatePlayer(final Player player, final int status) {
		while (player.getState() != status) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
		}
	}

	public Component getMonitor() {
		snapShot();
		return player.getVisualComponent();
	}

	public synchronized boolean startRecord(boolean isContinueMode) {
		if (this.processor == null) {
			boolean isSuccInit = false;
			try {
				isSuccInit = init();
			} catch (Throwable e) {
			}
			if (isSuccInit == false) {
				LogManager.errToLog("Error Capture device : " + strDeviceName);
				return false;
			}
		}

		strFileName = new String(VIDEO_PREFIX);

		// 注意：如果文件名格式发生变化，请同步更新和Snap, CapViewer提取文件
		final int lastHao = strDeviceName.lastIndexOf(":");
		strFileName += strDeviceName.substring(lastHao + 1,
				strDeviceName.length());

		strFileName += "_" + ResourceUtil.getTimeStamp()
				+ this.strContentTypeExt;
		strDirName = CaptureConfig.getInstance().capSaveDir;

		try {
			this.processor.setContentDescriptor(new FileTypeDescriptor(
					this.strContentType));

			boolean boolResult = waitForState(this.processor, 300);
			if (!(boolResult)) {
				this.processor.close();
				return false;
			}

			boolResult = waitForState(this.processor, Processor.Prefetched);
			if (!(boolResult)) {
				this.processor.close();
				return false;
			}

			DataSource dataOutSource = this.processor.getDataOutput();

			File dir = new File(strDirName, strFileName);
			// dir.toURL()==>"file:///c:/Documents and Settings/Capture0.avi"
			// final URL url =
			// dir.toURI().toURL();//==>"file:///c:/Documents%20and%20Settings/Capture0.avi"
			MediaLocator mediaDest = new MediaLocator(dir.toURL());// 不能采用dir.toURI().toURL()
			this.dataSink = Manager.createDataSink(dataOutSource, mediaDest);
			this.boolSaving = true;

			Time duration = this.processor.getDuration();
			int nMediaDuration = (int) duration.getSeconds();

			this.dataSink.addDataSinkListener(this);
			try {
				this.dataSink.open();
			} catch (Exception e) {
				this.processor.close();
				throw e;
			}

			this.dataSink.start();
			this.processor.start();
			boolResult = waitForState(this.processor, this.processor.Started);
			if (!(boolResult)) {
				this.processor.close();
				return false;
			}

			this.threadProgress = new ProgressThread(this.processor);
			this.threadProgress.start();

			L.V = L.O ? false : LogManager.log("capture:[start]");

			capStatus = CAP_RECORDING;
			CapManager.notifyMsg(capStatus);
			
			if(isEnableSnap()){
				L.V = L.O ? false : LogManager.log("Snap when capture:[start]");
				if(autoDetector == null){
					final int startDelayMS = 5000;
					autoDetector = new RemoveableHCTimer("", startDelayMS, true) {
						boolean isAfterInitLightChanged = false;
						MovDetector md;
						@Override
						public void doBiz() {
							final CapStream cs = (CapStream)getBizObject();
							if(cs == null){
								return;
							}
							if(isAfterInitLightChanged == false){
								final BufferedImage bi = cs.snapShot();
								if(bi == null){
									return;
								}
								isAfterInitLightChanged = true;
								final int ms = Integer.parseInt(CaptureConfig.getSnapMS());
								L.V = L.O ? false : LogManager.log("SnapShot detect ... / " + ms + " (ms)");
								setIntervalMS(ms);
								md = new MovDetector(bi.getWidth(), bi.getHeight(),
										CaptureConfig.getSnapWidth(), CaptureConfig.getSnapHeight());
								return;
							}
	//						long ms = System.currentTimeMillis();
							cs.snapShot();
							if(md.putIn(cs.getSnapArray())){
								cs.saveSnap();
							}
	//						System.out.println("getRGB cost : " + (System.currentTimeMillis() - ms));
	
						}
						@Override
						public void setEnable(final boolean enable){
	//						System.out.println("SnapShot HCTimer : " + enable);
							if(enable == false){
								setIntervalMS(startDelayMS);
								isAfterInitLightChanged = false;
							}
							super.setEnable(enable);
						}
					};
				}else{
					autoDetector.resetTimerCount();
					autoDetector.setEnable(true);
				}
				autoDetector.setBizObject(this);
			}
			if(hourCutTimer == null){
				hourCutTimer = new RemoveableHCTimer("", hourCutTimer.ONE_HOUR, true) {
					@Override
					public void doBiz() {
						CapStream cs = (CapStream)getBizObject();
						cs.stop();
						
						getInstance(cs).startRecord(true);

						//删除指定天数前的
						Calendar now =Calendar.getInstance();  
						now.setTimeInMillis(System.currentTimeMillis());
						final int days = Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_CapDelDays, "5")) + 1;
						now.set(Calendar.DATE, now.get(Calendar.DATE) - days);  
						final Timestamp timestamp = new Timestamp(now.getTimeInMillis());
						String delDays = "";
						delDays += (timestamp.getYear() + 1900);
						final int month = (timestamp.getMonth() + 1);
						delDays += (month < 10?("0"+month):month);
						final int day = timestamp.getDate();
						delDays += day;
						final int dayInt = Integer.parseInt(delDays);
						File dir = new File(CaptureConfig.getInstance().capSaveDir);
						
						String[] files = dir.list();
						for (int i = 0; i < files.length; i++) {
							final String aFile = files[i];
							if((aFile.startsWith(CapStream.VIDEO_PREFIX) 
									|| aFile.startsWith(CapStream.SNAP_PREFIX))){
								final int start_ = aFile.indexOf("_") + 1;
								if(start_ > 0){
									final int end_ = aFile.indexOf("_", start_);
									if(end_ > 0){
										final String date = aFile.substring(start_, end_);
										
										if(dayInt >= Integer.parseInt(date)){
											new File(dir, aFile).delete();
										}
									}
								}
							}
						}						
					}
				};
			}else{
				if(isContinueMode == false){
					hourCutTimer.resetTimerCount();
				}
				hourCutTimer.setEnable(true);
			}
			hourCutTimer.setBizObject(this);
		} catch (Exception exception) {
			exception.printStackTrace();
			this.boolSaving = false;
			return false;
		}
		return true;
	}

	private boolean isEnableSnap() {
		return PropertiesManager.isTrue(PropertiesManager.p_CapNotSnap) == false;
	}

	public boolean actionPerformed(String strCmd) {
		if (strCmd.equals(ProgressThread.ACTION_RECORD)) {
			if (startRecord(false) == false) {
				return false;
			}
		} else if (strCmd.equals(ProgressThread.ACTION_STOP)
				&& (this.boolSaving == true)) {
			stop();
		} else if ((strCmd.equals(ProgressThread.ACTION_PAUSE))
				&& (this.boolSaving == true)) {
			this.processor.stop();
			this.threadProgress.pauseThread();
			
			if(autoDetector != null){
				autoDetector.setEnable(false);
			}
			hourCutTimer.setEnable(false);
			
			capStatus = CAP_PAUSEING;
			
			CapManager.notifyMsg(capStatus);
		} else if ((strCmd.equals(ProgressThread.ACTION_RESUME))
				&& (this.boolSaving == true)) {
			this.processor.start();
			this.threadProgress.resumeThread();
			
			if(autoDetector != null && (isEnableSnap())){
				autoDetector.setEnable(true);
			}
			hourCutTimer.setEnable(true);
			
			capStatus = CAP_RECORDING;
			
			CapManager.notifyMsg(capStatus);
		} else {
		}
		return true;
	}

	public void controllerUpdate(ControllerEvent event) {
		if (event instanceof ControllerErrorEvent) {
			this.strFailMessage = ((ControllerErrorEvent) event).getMessage();

			if (this.boolSaving == true) {
				stop();
				LogManager.errToLog("jmstudio.error.processor.savefile:"
						+ "jmstudio.error.controller" + this.strFailMessage);
			} else {
				LogManager.errToLog("jmstudio.error.controller:"
						+ this.strFailMessage);
			}
		} else if ((event instanceof EndOfMediaEvent)
				&& (this.boolSaving == true)) {
			stop();
		}
	}

	public void dataSinkUpdate(DataSinkEvent event) {
		if (event instanceof EndOfStreamEvent) {
			closeDataSink();
		} else if (event instanceof DataSinkErrorEvent) {
			stop();
		}
	}

	private void closeDataSink() {
		synchronized (this) {
			if (this.dataSink != null)
				this.dataSink.close();
			this.dataSink = null;
		}
	}

	public synchronized void stop() {
		if(autoDetector != null && autoDetector.isEnable()){
			autoDetector.setEnable(false);
			autoDetector.setBizObject(null);
		}
		if(hourCutTimer != null && hourCutTimer.isEnable()){
			hourCutTimer.setEnable(false);
			hourCutTimer.setBizObject(null);
		}
		
		L.V = L.O ? false : LogManager.log("capture:[stop]");
		this.boolSaving = false;

		if (this.threadProgress != null) {
			try {
				this.threadProgress.terminateNormaly();
			} catch (Throwable e) {
			}
			this.threadProgress = null;
		}
		if (this.processor != null) {
			try {
				this.processor.stop();
			} catch (Throwable e) {
			}
			try {
				this.processor.close();
			} catch (Throwable e) {
			}
			this.processor = null;
		}

		if (this.player != null) {
			try {
				this.player.close();
			} catch (Exception e) {
			}
			try {
				this.player.deallocate();
			} catch (Exception e) {
			}
			this.player = null;
		}

		if (this.dataSource != null) {
			try {
				this.dataSource.disconnect();
			} catch (Throwable e) {
				e.printStackTrace();
			}
			this.dataSource = null;
		}

//		if (snapGraphics != null) {
//			snapGraphics.dispose();
//		}

		instance = null;
		capStatus = CAP_NO_WORKING;
		CapManager.notifyMsg(capStatus);

		//当前正在录制的文件，释放该名称，以可刷新和播放，所以要置于最后
		strFileName = null;
		
	}
	
	private synchronized boolean waitForState(final Processor p, final int state) {
		this.stateFailed = false;

		if (state == 180) {
			p.configure();
		} else if (state == 300) {
			p.realize();
		} else if (state == p.Prefetched) {
			p.prefetch();
		}

		while ((p.getState() < state) && (!(this.stateFailed))) {
			try {
				Thread.sleep(200);
			} catch (Throwable ie) {
			}
		}
		return (!(this.stateFailed));
	}

	private void changeContentType() {
		String strValue = STR_QUICKTIME;
		if (strValue.equals(STR_MSVIDEO)) {
			this.strContentType = "video.x_msvideo";
			this.strContentTypeExt = ".avi";
		} else if (strValue.equals(STR_QUICKTIME)) {
			this.strContentType = "video.quicktime";
			this.strContentTypeExt = VIDEO_END;
		} else {
			this.strContentType = strValue;
			this.strContentTypeExt = "movie";
		}

		if (this.processor.setContentDescriptor(new FileTypeDescriptor(
				this.strContentType)) == null)
			System.err.println("Error setting content descriptor on processor");
	}

	public void saveSnap() {
		// strDirName
		String strFileName = SNAP_PREFIX;
		final int lastHao = strDeviceName.lastIndexOf(":");
		strFileName += strDeviceName.substring(lastHao + 1,
				strDeviceName.length());

		File outFile = new File(strDirName, strFileName + "_"
				+ ResourceUtil.getTimeStamp() + SNAP_END);
		try {
			ImageIO.write(snapBI, "jpg", outFile);
			L.V = L.O ? false : LogManager.log("Save [Snap] " + outFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BufferedImage snapShot() {
		if (this.player == null) {
			cloneddatasource = ((SourceCloneable) cloneabledatasource).createClone();

			try {
				player = Manager.createRealizedPlayer(cloneddatasource);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			player.realize();
			waitForStatePlayer(player, Controller.Realized);
			player.prefetch();
			waitForStatePlayer(player, Controller.Prefetched);
			player.start();
			waitForStatePlayer(player, Controller.Started);
			controlGrabber = (FrameGrabbingControl) player
					.getControl("javax.media.control.FrameGrabbingControl");
		}
		
		if(bufferToImage == null){
			Image image = null;
			while (image == null) {
				Buffer bufferFrame = this.controlGrabber.grabFrame();
				if(bufferFrame != null){
					try{
						bufferToImage = new HCBufferToImage(
								(VideoFormat) bufferFrame.getFormat());
						image = bufferToImage.createImage(bufferFrame);
					}catch (Exception e) {
					}
				}
				if (image == null) {
					try {
						Thread.sleep(200);
					} catch (Exception e) {
					}
					continue;
				}
				snapWidth = image.getWidth(null);
				snapHeight = image.getHeight(null);
//				snapBI = new BufferedImage(snapWidth, snapHeight,
//						BufferedImage.TYPE_INT_RGB);
//
				rgbArray = new int[snapWidth * snapHeight];
//				rgbSampleArray = new int[snapWidth * snapHeight * 3];
//				snapGraphics = snapBI.getGraphics();
//				rgbRaster = snapBI.getData();
			}
		}

		Buffer bufferFrame = this.controlGrabber.grabFrame();
		snapBI = (BufferedImage)bufferToImage.createImage(bufferFrame);
		
		return snapBI;
	}

	public int[] getSnapArray() {
		snapBI.getRGB(0, 0, snapWidth, snapHeight, rgbArray, 0, snapWidth);
		// return rgbRaster.getPixels(0, 0, snapWidth, snapHeight, rgbArray);
//		rgbSampleArray = (int[])rgbRaster.getDataElements(0, 0, snapWidth, snapHeight, rgbSampleArray);
//		System.out.println("getSnapArray Sample : " + rgbSampleArray[0]);
		// rgbRaster.getSamples(0, 0, snapWidth, snapHeight, rgbArray.length,
		// rgbArray);
		return rgbArray;
	}
	
	
}