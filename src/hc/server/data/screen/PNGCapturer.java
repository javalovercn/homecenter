package hc.server.data.screen;

import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.data.DataInputEvent;
import hc.core.data.DataPNG;
import hc.core.sip.SIPManager;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.ICanvas;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public abstract class PNGCapturer extends Thread implements ICanvas {
	final protected Rectangle capRect = new Rectangle();
	int locX, locY;

	byte[] pngCaptureID;
	protected String captureID;
	public void setCaptureID(String captureID){
		this.captureID = captureID;
		
		pngCaptureID = StringUtil.getBytes(captureID);
	}
	
	final Boolean LOCK = new Boolean(false);
	final int MIN_BLOCK_CAP;
	final int[] rgb;
	
	private int getBlockSize() {
		try{
			return Integer.parseInt(RootConfig.getInstance().getProperty(
				RootConfig.p_ScreenCapMinBlockSize));
		}catch (Throwable e) {
			return 90;
		}
	}

	final static int DEFAULT_BACK_COLOR = (192 << 16) | (192 << 8) | 192;
	
	final Rectangle blockCapRect = new Rectangle();

	public static String OP_STR = " OP ";
	
	final int[] clientSnap;
//	final boolean isScreenCap;
	final int PNG_STORE_BS_START_IDX = MsgBuilder.INDEX_MSG_DATA + DataPNG.HEAD_LENGTH;

	protected final DataPNG dataPNG = new DataPNG();
	protected final HCByteArrayOutputStream byteArrayOutputStream = new HCByteArrayOutputStream();
	protected final Boolean WAITING = new Boolean(false);
	protected boolean isShutDown = false;
	final IContext ic = ContextManager.getContextInstance();

	protected static int refreshMillSecond = 3000;
//	final int maxCapW, maxCapH;
	final boolean isScreenCap;
	final int fixColorMask;
	
	public PNGCapturer(int w, int h, boolean isScreenCap, int fixMask) {
		super();
		this.isScreenCap = isScreenCap;
		this.fixColorMask = fixMask;//非电脑远屏，指定色彩级别
		
		super.setPriority(Thread.MIN_PRIORITY);
		
		clientSnap = new int[w * h];
		
//		maxCapH = h;
//		maxCapW = w;
		
		initClientSnap();
		
		//关闭Mlet的全屏传送
//		if(isMlet){
//			MIN_BLOCK_CAP = ((w > h)?w:h);
//			rgb = new int[clientSnap.length];
//		}else{
			MIN_BLOCK_CAP = getBlockSize();
			rgb = new int[MIN_BLOCK_CAP * MIN_BLOCK_CAP];
//		}
			
		//预处理及发送前的数据处理块，不作回收操作
		dataPNG.setBytes(new byte[50 * 1024]);
		byteArrayOutputStream.reset(dataPNG.bs, PNG_STORE_BS_START_IDX);

	}

	protected void initClientSnap() {
		for (int i = 0, len = clientSnap.length; i < len; i++) {
			clientSnap[i] = DEFAULT_BACK_COLOR;
		}
	}
	
	public abstract int grabImage(Rectangle bc, int[] rgb);
	public abstract boolean actionInput(DataInputEvent e);
	
	public static void setColorBit(final int cBit){
		mask = getMaskFromBit(cBit);
	}

	public static int getMaskFromBit(final int cBit) {
		int colorBit = 0;
		if(cBit == IConstant.COLOR_64_BIT){
			colorBit = IConstant.COLOR_64_BIT;
		}else if(cBit == IConstant.COLOR_32_BIT){
			colorBit = IConstant.COLOR_32_BIT;
		}else if(cBit == IConstant.COLOR_16_BIT){
			colorBit = IConstant.COLOR_16_BIT;
		}else if(cBit == IConstant.COLOR_8_BIT){
			colorBit = IConstant.COLOR_8_BIT;
		}else if(cBit == IConstant.COLOR_4_BIT){
			colorBit = IConstant.COLOR_4_BIT;
		}else{
			colorBit = IConstant.COLOR_8_BIT;
		}
		final int mask_one = 0xFF - ((0x01 << colorBit) - 1);
		return (mask_one << 16) | (mask_one << 8) | mask_one;
	}
	
	static int mask;
	protected boolean isStopCap = false;
	
	protected void enableStopCap(final boolean sc){
		//多次调用，也只出现一次
		hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + ((!isStopCap && sc)?"pause":"resume") + " Screen [" + captureID + "]");
		
		this.isStopCap = sc;
		
		synchronized (WAITING) {
			WAITING.notify();
		}
	}
	
	public void run() {	
		sleepBeforeRun();

		cycleCapture();
	}

	protected void cycleCapture() {
		while(!isShutDown){
			if(isStopCap){
				synchronized (WAITING) {
					try {
						WAITING.wait();
					} catch (InterruptedException ignored) {
						ignored.printStackTrace();
					}
				}
				continue;
			}
			
//			L.V = L.O ? false : LogManager.log("Auto Refresh Rect:" + System.currentTimeMillis());
			try{
				sendPNG(capRect, capRect.width, true);
			}catch (Exception e) {
				//考虑数据溢出，故忽略
			}
			
//			L.V = L.O ? false : LogManager.log("Auto Refresh Rect Finish:" + System.currentTimeMillis());
			
			try{
				Thread.sleep(refreshMillSecond);
			}catch (Exception e) {
				
			}
		}
		hc.core.L.V=hc.core.L.O?false:LogManager.log(" exit Screen [" + captureID +"]");
	}

	protected void sleepBeforeRun() {
		try{
			int delayMS = 1200;
			if(SIPManager.isOnRelay()){
				delayMS = 1000;
			}
			Thread.sleep(delayMS);
		}catch (Exception e) {
		}
	}
	
	protected void sendPNG(final Rectangle capRect, final int maxCapWidth, final boolean isAutoRefresh) {
			final int oriX = capRect.x;
			final int oriY = capRect.y;
			final int rectWidth = capRect.width;
			final int rectHeight = capRect.height;
			
//			if(isAutoRefresh == false){
//				L.V = L.O ? false : LogManager.log("Cap x : " + oriX + ", y : " + oriY + ", w : " + rectWidth + ", h : " + rectHeight);
//			}
			
			synchronized (LOCK) {
				final int endY = oriY + rectHeight;
				for (int j = oriY; j < endY; ) {
					blockCapRect.y = j;
					
					final int capH = endY - j;
					final int tailHeight = (capH >= MIN_BLOCK_CAP?MIN_BLOCK_CAP:capH);
					
					blockCapRect.height = tailHeight;
	
					final int endX = oriX + rectWidth;
					for (int i = oriX; i < endX;) {
						
						blockCapRect.x = i;
	
						final int capW = endX - i;
						final int tailWidth = (capW >= MIN_BLOCK_CAP?MIN_BLOCK_CAP:capW);

						blockCapRect.width = tailWidth;
	
						final int length = grabImage(blockCapRect, rgb);
						
						//非远屏传输画面时，采用全彩
						final int currMask = isScreenCap?mask:fixColorMask;
						for (int idxRGB = 0; idxRGB < length; idxRGB++) {
							rgb[idxRGB] &= currMask;
						}
						//比较色块
						{
							int diffTopLeftX = -1, diffTopLeftY = -1, diffDownRightX = -1, diffDownRightY = -1;
							//刷新方式，则检查缓存是否发生变化，如果没有变化，则不进行数据传输
							for (int n = 0; n < tailHeight; n++) {
								int clientSnapIdx = (j + n - locY) * maxCapWidth + (i - locX);
								int idxPixel = n * tailWidth;
//								L.V = L.O ? false : LogManager.log("snap idx:" + clientSnapIdx + ", rgb idx:" + idxPixel + ", locY:" + locY + ", oriY:" + oriY);
								for (int m = 0; m < tailWidth; m++) {
									final int rgb_v = rgb[idxPixel];
									if(rgb_v != clientSnap[clientSnapIdx]){
										if(diffTopLeftX == -1){
											diffTopLeftX = m;
											diffTopLeftY = n;
											diffDownRightX = m;
											diffDownRightY = n;
//											L.V = L.O ? false : LogManager.log("first Diff x : " + m + ", y : " + n +", snap idx:" + clientSnapIdx + ", rgb idx:" + idxPixel + ", locY:" + locY + ", oriY:" + oriY);
										}else{
											if(m >= diffTopLeftX){
												if(m > diffDownRightX){
													diffDownRightX = m;
												}
											}else{
												diffTopLeftX = m;
											}
											
											if(n > diffDownRightY){
												diffDownRightY = n;
											}
										}
										clientSnap[clientSnapIdx] = rgb_v;
									}
									clientSnapIdx++;
									idxPixel++;
								}
							}
							
							//发现差异色块
							if(diffDownRightX != -1 && (diffDownRightX > diffTopLeftX || diffDownRightY > diffTopLeftY)){
								//复制差异块到rgb数组中
								final int copyWidth = diffDownRightX - diffTopLeftX + 1;
								final int copyHeight = diffDownRightY - diffTopLeftY + 1;
								
								if(copyHeight == tailHeight && copyWidth == tailWidth){
									//全差，不用制作小块，全传送
								}else{
									//重新编制块信息数据到rgb变量中
//	//								L.V = L.O ? false : LogManager.log("Send Block, width:" + copyWidth + ", height:" + copyHeight);
									int idxCopy = 0;
									for (int kY = 0; kY < copyHeight; kY++) {
										int clientSnapIdx = (j + diffTopLeftY + kY - locY) * maxCapWidth + (i + diffTopLeftX - locX);
										for (int kX = 0; kX < copyWidth; kX++) {
											rgb[idxCopy++] = clientSnap[clientSnapIdx++];
										}
									}
								}
								sendBlock(i + diffTopLeftX, j + diffTopLeftY, rgb, copyWidth * copyHeight, copyWidth, copyHeight, isAutoRefresh, MsgBuilder.E_IMAGE_PNG);
							}
						}
						i += tailWidth;
					}
					j += tailHeight;
				}
			}
		}
	
	void sendBlock(final int clientX, final int clientY, final int[] rgb,
			final int rgb_length, final int width, final int height, final boolean isAutoRefresh, final byte tag) {
//					L.V = L.O ? false : LogManager.log("Send Block: clientX:" + clientX + ", clientY:" 
//						+ clientY + ", width:" + width + ", height:" + height);
					final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					bi.setRGB(0, 0, width, height, rgb, 0, width);
					
					final int maxLen = rgb_length + PNG_STORE_BS_START_IDX;
//					hc.core.L.V=hc.core.L.O?false:LogManager.log("PNG max:" + maxLen);
					
					//ScreenCapturersk可能同时传送远屏和远屏缩略图，所以加并发锁
					synchronized (LOCK) {
						final int doubleSize = maxLen<<1;
						if(dataPNG.bs.length < doubleSize){
//							hc.core.L.V=hc.core.L.O?false:LogManager.log("dataPNG set to max:" + doubleSize);
							dataPNG.setBytes(new byte[doubleSize]);
							byteArrayOutputStream.reset(dataPNG.bs, PNG_STORE_BS_START_IDX);
						}else{
							byteArrayOutputStream.reset();
						}
						try {
							ImageIO.write(bi, "png", byteArrayOutputStream);
						} catch (IOException e1) {
							L.V = L.O ? false : LogManager.log("Trans Screen Exception:" + e1.toString());
							e1.printStackTrace();
							return;
						}
			//			byte[] out = byteArrayOutputStream.toByteArray();
						
						final int pngDataLen = byteArrayOutputStream.size();
						dataPNG.setTargetID(pngDataLen, pngCaptureID, 0, pngCaptureID.length);
						
						final int dataTransLen = pngDataLen + DataPNG.HEAD_LENGTH + 1 + pngCaptureID.length;
//						hc.core.L.V=hc.core.L.O?false:LogManager.log("Trans dataPNG size pngDataLen :" + pngDataLen);
						dataPNG.setPNGDataLen(pngDataLen, clientX, clientY, width, height);
						
			//			if(isAutoRefresh){
			//				dataPNG.setRefreshID(refreshID++, blockID);
			//			}else{
			//				dataPNG.setIsRefresh(false);
			//			}
						
			//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Send out Screen, Refresh mode:" + isRefresh);
			//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Img old:" + rgb.length + ", deflate:" + deflateLen + ", byteLen:" + byteLen);
			//			totalPNG += pngDataLen;
			//			L.V = L.O ? false : LogManager.log("Total PNG:" + totalPNG);
						ic.sendWrap(tag, dataPNG.bs, MsgBuilder.INDEX_MSG_DATA, dataTransLen);			
					}
				}

	public static void setRefreshMillSecond(int ms) {
		refreshMillSecond = ms;		
	}

	@Override
	public void onExit() {
		isShutDown = true;
		try{
			this.interrupt();
		}catch (Exception e) {
		}
		synchronized (WAITING) {
			WAITING.notify();
		}
	}

}