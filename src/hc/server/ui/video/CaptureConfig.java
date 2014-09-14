package hc.server.ui.video;

import hc.core.IConstant;
import hc.util.PropertiesManager;

import java.awt.Dimension;

public class CaptureConfig {
	boolean useVideo = false;
	String strDeviceName = "";
	String encoding;
	String width;
	String height;
	int maxDataLength;
	String dataTypeClassname;
	String frameRate;
	Object formatDefault;
	String capSaveDir;
	String autoRecord;
	
	public static final String PREFIX = "Cap_";
	public static final String USE_VIDEO = "useVideo";
	public static final String DEVICE_NAME = "deviceName";
	public static final String ENCODING = "encoding";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String MAX_DATA_LEN = "maxDataLen";
	public static final String DATA_TYPE_CLASS_NAME = "dataType";
	public static final String FRAME_RATE = "frameRate";
	public static final String AUTO_RECORD = "autoCap";
	public static final String CAP_DIR = "capDir";
	
	private static String get(String p){
		return PropertiesManager.getValue(PREFIX + p);
	}
	
	public static String get(String p, String defaultValue){
		String v = get(p);
		return v==null?defaultValue:v;
	}
	
	private static void set(String p, String value){
		PropertiesManager.setValue(PREFIX + p, value);
	}
	
	public void save(){
		set(USE_VIDEO, String.valueOf(useVideo));
		set(DEVICE_NAME, strDeviceName);
		set(ENCODING, encoding);
		set(WIDTH, width);
		set(HEIGHT, height);
		set(MAX_DATA_LEN, String.valueOf(maxDataLength));
		set(DATA_TYPE_CLASS_NAME, dataTypeClassname);
		set(FRAME_RATE, frameRate);
		set(CAP_DIR, capSaveDir);
		set(AUTO_RECORD, autoRecord);
		
		PropertiesManager.saveFile();
	}
	
	public static CaptureConfig getInstance(){
		return new CaptureConfig(Boolean.valueOf(get(USE_VIDEO, IConstant.FALSE)), 
			get(DEVICE_NAME, ""), get(ENCODING), get(WIDTH), get(HEIGHT), 
			Integer.parseInt(get(MAX_DATA_LEN, "0")),
			get(DATA_TYPE_CLASS_NAME), get(FRAME_RATE));
	}
	
	public CaptureConfig(boolean useVideo, String strDeviceName, 
			String encoding, String width, String height, int maxDataLength, 
			String dataTypeclassname, String frameRate){
		this.useVideo = useVideo;
		this.strDeviceName = strDeviceName;
		this.encoding = encoding;
		this.width = width;
		this.height = height;
		this.maxDataLength = maxDataLength;
		this.dataTypeClassname =  dataTypeclassname;
		this.frameRate = frameRate;
		try {
			if(encoding != null){
				final Class dataType = Class.forName(dataTypeclassname);
				
				formatDefault = new javax.media.format.VideoFormat(encoding, 
					new Dimension(Integer.parseInt(width), Integer.parseInt(height)), 
					maxDataLength, 
					dataType, 
					Float.parseFloat(frameRate));
			}
		} catch (Throwable e) {
		}
		this.capSaveDir = getSaveDir();
		this.autoRecord = getAutoRecord();
	}
	
	public void toSerialize(Object format){
		formatDefault = format;
		
		javax.media.format.VideoFormat vf = (javax.media.format.VideoFormat)format;
		encoding = vf.getEncoding();
		Dimension d = vf.getSize();
		width = String.valueOf(d.width);
		height = String.valueOf(d.height);
		maxDataLength = vf.getMaxDataLength();
		dataTypeClassname = vf.getDataType().getName();
		frameRate = String.valueOf(vf.getFrameRate());
	}

	public static String getAutoRecord() {
		return get(AUTO_RECORD, IConstant.FALSE);
	}

	public static String getSaveDir() {
		return get(CAP_DIR, System.getProperty("user.home"));
	}
	
	public static boolean isCapEnable(){
		return get(CaptureConfig.USE_VIDEO, IConstant.FALSE).equals(IConstant.TRUE);
	}
	
	public static String getSnapMS() {
		return PropertiesManager.getValue(PropertiesManager.p_CapSnapMS, "1000");
	}
	
	public static String getSnapWidth(){
		return PropertiesManager.getValue(PropertiesManager.p_CapSnapWidth, "20");
	}
	
	public static String getSnapHeight(){
		return PropertiesManager.getValue(PropertiesManager.p_CapSnapHeight, "20");
	}
	
}
