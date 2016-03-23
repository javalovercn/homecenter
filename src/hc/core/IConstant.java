package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;

import java.io.UnsupportedEncodingException;

public abstract class IConstant {
	public static final String ForceRelay = "ForceRelay";
//	public static final String HCEventCache = "2";
//	public static final String DatagramCache = "3";
//	public static final String BizServerMinSize = "4";
//	public static final String BizServerSplitMaxSize = "5";//停止使用
	public static final String IS_J2ME = "6";
	public static final String CertKey = "7";
	public static final String RelayMax = "8";
	public static final String IS_FORBID_UPDATE_CERT = "9";
	
	//当前线程要发起另一个线程任务，并等待其状态变化
	public static final int THREAD_WAIT_INNER_MS = 10;
	
	public static String propertiesFileName = null;
	public static boolean enableInitLock = true;
	protected static String uuid, password;
	
	public static final String getUUID(){
		CCoreUtil.checkAccess();
		
		return uuid;
	}
	
	/**
	 * 
	 * @return true:is HC Server. false : Relay Server or other
	 */
	public static boolean isHCServer(){
		if(propertiesFileName == null){//TestCase时可能为null
			return false;
		}
		return propertiesFileName.startsWith("hc_config");
	}
	
	public static final String getPassword(){
		CCoreUtil.checkAccess();
		
		return password;
	}
	
	public static boolean serverSide;

	private static IConstant instance;
	
	public static IConstant getInstance(){
		return instance;
	}

	public static void setInstance(final IConstant ic){
		CCoreUtil.checkAccess();
		
		instance = ic;
	}

//	public static final String STATUS_PWD_ERROR = "S_pwd_E";
//	public static final String STATUS_CERTKEY_ERROR = "S_CerTKey_E";
//	public static final String STATUS_ISBUSS = "S_IsB_E";
	public static final String NO_CANVAS_MAIN = "N_Cvs";
	
	static byte[] passwordBS, uuidBS;
	
	public static boolean isRegister(){
		return getPasswordBS() != null;
	}
	
	public static byte[] getPasswordBS(){
		CCoreUtil.checkAccess();
		return passwordBS;
	}
	
	public static byte[] getUUIDBS(){
		CCoreUtil.checkAccess();
		return uuidBS;
	}
	
	public static final String UTF_8 = "UTF-8";
	public static final String ISO_8859_1 = "ISO-8859-1";
//	public static final byte DATA_PROTOCAL_HEAD_H = 'H';
//	public static final byte DATA_PROTOCAL_HEAD_C = 'c';
	//注意：J2SEReceiveServer和J2MEReceiveServer固化了长度2，换言之，此处以后不可再更改
//	public static final byte[] DATA_PROTOCAL_HEAD = {DATA_PROTOCAL_HEAD_H, DATA_PROTOCAL_HEAD_C};
//	public static final int LEN_PROTOCAL = 2;
	
	public static final int IOMODE_NO_INOUT = 0;
	public static final int IOMODE_ONLY_OUT = 1;
	public static final int IOMODE_ONLY_IN	 = 2;
	public static final int IOMODE_IN_OUT   = 3;
	
	public static final short COLOR_4_BIT = 7;//256 / 4;//64
	public static final short COLOR_8_BIT = 6;//256 / 8;//32
	public static final short COLOR_16_BIT = 5;//256 / 16;//16
	public static final short COLOR_32_BIT = 4;//256 / 32;//8
	public static final short COLOR_64_BIT = 3;
	public static final short COLOR_STAR_TOP = 8;
	
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public abstract int getInt(String p);
	
	public abstract Object getObject(String p);

	public abstract void setObject(String key, Object value);
	
	public static boolean checkUUID(final String uuid) {
		byte[] bs;
		try {
			bs = uuid.getBytes(IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			bs = uuid.getBytes();
		}
		if(bs.length < 6 || bs.length > MsgBuilder.LEN_MAX_UUID_VALUE 
				|| uuid.indexOf("\"") >= 0 || uuid.indexOf("&") >= 0 
				|| uuid.indexOf("=") >= 0){
			return false;
		}
		return true;
	}

	public static void setServerSide(final boolean s) {
		serverSide = s;
	}

	public static void setUUID(final String uid){
		uuid = uid;
		
		try {
			uuidBS = uuid.getBytes(IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			uuidBS = uuid.getBytes();
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static void setPassword(final String pwd){
		password = pwd;
		try {
			passwordBS = password.getBytes(IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
			passwordBS = password.getBytes();
		}
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("PWD byte len:" + passwordBS.length + ", " + password);
		CUtil.setCertKey((byte[])IConstant.getInstance().getObject(IConstant.CertKey));
	}

}
