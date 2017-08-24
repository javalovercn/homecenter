package hc.core.util;

import hc.core.CoreSession;


public class HCURL {

	public static final String HTTP_SPLITTER = "://";

	public final String toString(){
		String ps = "";

		if(paras != null){
			final int size = paras.size();
			for (int i = 0; i < size; i++) {
				ps += paras.elementAt(i) + "=" + values.elementAt(i) + ", ";
			}
		}
		return protocal + ", " + elementID + ", " + url + "[" + ps +"]";
	}
	
	public String protocal, host, context = "", elementID;
	private Stack paras, values;
	public String url;
	private String urlLower;
	private String mletAliasURL;
	private String elementIDLower;
	
	public final String getElementIDLower(){
		if(elementIDLower == null){
			elementIDLower = elementID.toLowerCase();
		}
		return elementIDLower;
	}
	
	public final String getMletAliasURL(){
		if(mletAliasURL == null){
			mletAliasURL = HCURL.buildMletAliasURL(getURLLower());
		}
		return mletAliasURL;
	}
	
	public final String getURLLower(){
		if(urlLower == null){
			urlLower = url.toLowerCase();
		}
		return urlLower;
	}
	
	public static final String CANCEL_HC_CMD = "_Cancel_HC_Cmd";

	public static final String CMD_PROTOCAL = "cmd";
	public static final String SCREEN_PROTOCAL = "screen";
	public static final String CONTROLLER_PROTOCAL = "controller";
	public static final String FORM_PROTOCAL = "form";//注意：如果增加新项，请同步增加到FAST_RESP_PROTOCAL
	public static final String[] FAST_RESP_PROTOCAL = {CMD_PROTOCAL, FORM_PROTOCAL, CONTROLLER_PROTOCAL, SCREEN_PROTOCAL};
	public static final String CFG_PROTOCAL = "cfg";
	public static final String MENU_PROTOCAL = "menu";
	
	public static final String ROOT_MENU = "root";
	
	public static final String DATA_IOT_DEVICE = "Device";
	public static final String DATA_IOT_CONVERTER = "Converter";
	public static final String DATA_IOT_ROBOT = "Robot";
	
	public static final String DATA_CMD_EXIT = "exit";
	public static final String DATA_CMD_CONFIG = "config";
	
	
	//注意：以_开始为特权cmd
	public static final String CMD_FOR_SUPER_PRE = buildStandardURL(CMD_PROTOCAL, "_");
	public static final String DATA_CMD_ALERT = "_alert";
	public static final String DATA_CMD_MSG = "_msg";
	public static final String DATA_CMD_MOVING_MSG = "_mv_msg";
	public static final String DATA_CMD_VOICE = "_voice";
	public static final String DATA_CMD_PUBLISH_LOCATION_MS = "_publish_loc_ms";
	public static final String DATA_CMD_CTRL_BTN_TXT = "_ctl_btn_txt";
	//关闭，并合并入DATA_CMD_EXIT
//	public static final String DATA_CMD_BYE_SCREEN = "_byeScreen";
	public static final String DATA_CMD_SendPara = "_Send";
	
	public static final String DATA_PARA_INPUT = "Input";

	public static final String DATA_PARA_PUBLISH_LOCATION = "publish_location";
	public static final String DATA_PARA_LOCATION_STR_LATITUDE = "Latitude";
	public static final String DATA_PARA_LOCATION_STR_LONGITUDE = "Longitude";
	public static final String DATA_PARA_LOCATION_STR_ALTITUDE = "Altitude";
	public static final String DATA_PARA_LOCATION_STR_COURSE = "Course";
	public static final String DATA_PARA_LOCATION_STR_SPEED = "Speed";
	public static final String DATA_PARA_LOCATION_STR_IS_GPS = "IsGPS";
	public static final String DATA_PARA_LOCATION_STR_IS_FRESH = "IsFresh";
	
//	public static final String DATA_PARA_TOKEN = "Token";//采用HideToken来代替
	public static final String DATA_PARA_RIGHT_CLICK = "rc";
	public static final String DATA_PARA_VALUE_CTRL = "ctrl";
	public static final String DATA_PARA_VALUE_SHIFT = "shift";
	//双向传递
	public static final String DATA_PARA_CLASS = "classViaStr";
	public static final String DATA_PARA_TRANSURL = "transURL";
	public static final String DATA_PARA_CERT_RECEIVED = "certRved";
	public static final String DATA_PARA_FORBID_CERT_UPDATE = "forbidCertUpd";
	public static final String DATA_PARA_RELOAD_THUMBNAIL = "reloadThumbnail";
	public static final String DATA_PARA_REPORT_JUMP_EXCEPTION = "jmpExcp";
	public static final String DATA_PARA_NOTIFY_RECEIVER = "notiRecv";
	public static final String DATA_PARA_NOTIFY_RECEIVER_PARA = "notiRecvPara";
	public static final String DATA_PARA_REMOVE_SCREEN_BY_IDX = "removeScreenByIdx";
	public static final String DATA_PARA_SHIFT_SCREEN_TO_TOP_FROM_IDX = "sftTopIdx";
	public static final String DATA_PARA_SHIFT_SCREEN_TO_TOP_SIZE = "sftTopSize";
	public static final String DATA_PARA_NOTIFY_PROJ_LIST = "notiCacheProjList";
	public static final String DATA_PARA_QUESTION_ID = "ques_id";
	public static final String DATA_PARA_ROLLBACK_QUESTION_ID = "rollback_ques_id";//撤消其它非应答
	public static final String DATA_PARA_ROLLBACK_DIALOG_ID = "rollback_dialog_id";//撤消其它非应答
	public static final String DATA_PARA_QUESTION_RESULT = "ques_result";
	public static final String DATA_PARA_DISMISS_QUES_DIALOG_BY_BACK = "disQuesDiaBack";
	public static final String DATA_PARA_PROC_ADD_HAR_URL = "addHARUrl";
	public static final String DATA_PARA_WIFI_MANAGER = "WiFiManager";
	public static final String DATA_PARA_PUBLISH_STATUS_ID = "publishStatusID";
	public static final String DATA_PARA_PUBLISH_STATUS_VALUE = "publishStatusValue";
	public static final String DATA_PARA_VOICE_COMMANDS = CCoreUtil.SYS_PREFIX + "voiceCommand";
	public static final String DATA_PARA_MGR_PROJS_COMMANDS = CCoreUtil.SYS_PREFIX + "MgrProjs";
	public static final String DATA_PARA_CHANGE_PASSWORD = "changePassword";
	public static final String DATA_PARA_SCAN_QR_CODE = "scanQRCode";

	public static final String DATA_RECEIVER_MLET = "rev_mlet";
	public static final String DATA_RECEIVER_HTMLMLET = "rev_htmlmlet";
	
	public static final String REMOTE_HOME_SCREEN = "home";
	public static final String ADD_HAR_QR = "addHARByQR";
	public static final String ADD_HAR_WIFI = "addHARByWiFi";
	public static final String VOICE_COMMAND = DATA_PARA_VOICE_COMMANDS;
	public static final String MGR_PROJS_COMMAND = DATA_PARA_MGR_PROJS_COMMANDS;
	
	
	public static final String URL_CMD_EXIT = buildStandardURL(HCURL.CMD_PROTOCAL, HCURL.DATA_CMD_EXIT);
	public static final String URL_CMD_CONFIG = buildStandardURL(HCURL.CMD_PROTOCAL, HCURL.DATA_CMD_CONFIG);
	//注意：如果增加新项，请同步增加到ServerUIAPIAgent.FAST_NOT_RESP_URL
	
	public static final String URL_HOME_SCREEN = buildStandardURL(HCURL.SCREEN_PROTOCAL, HCURL.REMOTE_HOME_SCREEN);
	public static final String URL_CFG_ADD_DEVICE_BY_QR = buildStandardURL(HCURL.CFG_PROTOCAL, ADD_HAR_QR);
	public static final String URL_CFG_ADD_DEVICE_BY_WIFI = buildStandardURL(HCURL.CFG_PROTOCAL, ADD_HAR_WIFI);
	public static final String URL_CMD_VOICE_COMMAND = buildStandardURL(HCURL.CMD_PROTOCAL, VOICE_COMMAND);
	public static final String URL_CMD_MGR_PROJS_COMMAND = buildStandardURL(HCURL.CMD_PROTOCAL, MGR_PROJS_COMMAND);
	
	public static final String URL_DEFAULT_MLET_ALIAS = "";
	
	public static final String DIALOG_PRE = CCoreUtil.SYS_PREFIX + "DIALOG_";
	public static final String URL_DIALOG_PRE = buildStandardURL(HCURL.FORM_PROTOCAL, DIALOG_PRE);
	
	public static final String HTML_LOAD_DONE = "window.hcloader.stop();";

	public static final boolean isUsingWiFiWPS = false;
	
	public static final String[] URL_PROTOCAL = {CMD_PROTOCAL, SCREEN_PROTOCAL, CONTROLLER_PROTOCAL,
		FORM_PROTOCAL, CFG_PROTOCAL, MENU_PROTOCAL};
	public static final String[] WAIT_URL_PROTOCAL = {SCREEN_PROTOCAL, CONTROLLER_PROTOCAL,
		FORM_PROTOCAL, CFG_PROTOCAL, MENU_PROTOCAL};
	
	public static String buildStandardURL(final String protocal, final String target){
		return protocal + HTTP_SPLITTER + target;
	}
	
	public final static String FORM_MLET_PREFIX = buildStandardURL(FORM_PROTOCAL, "");
	
	/**
	 * 将新版的form://xx生成一个支持旧版的screen://xx。以兼容旧的。
	 * @param mletURL
	 * @return
	 */
	public static String buildMletAliasURL(final String mletURL){
		if(mletURL.startsWith(FORM_MLET_PREFIX)){
			return SCREEN_PROTOCAL + mletURL.substring(FORM_PROTOCAL.length());
		}
		return URL_DEFAULT_MLET_ALIAS;
	}
	
	public static int getURLProtocalIdx(final String protocal){
		for (int i = 0; i < URL_PROTOCAL.length; i++) {
			if(URL_PROTOCAL[i].equals(protocal)){
				return i;
			}
		}
		return -1;
	}
	
	public final void reset(){
		urlLower = null;
		mletAliasURL = null;
		elementIDLower = null;
		
		if(paras != null){
			paras.removeAllElements();
			values.removeAllElements();
		}
	}
	
	public final int getParaSize(){
		if(paras == null){
			paras = new Stack();
			values = new Stack();
		}
		return paras.size();
	}
	
	public final String getParaAtIdx(final int idx){
		if(paras == null){
			paras = new Stack();
			values = new Stack();
		}
		if(idx > paras.size()){
			return null;
		}else{
			return (String)paras.elementAt(idx);
		}
	}
	
	public final void addParaVales(final String p, final String v){
		if(paras == null){
			paras = new Stack();
			values = new Stack();
		}
		paras.push(p);
		values.push(v);
	}
	
	/**
	 * 没有找到，返回null
	 * @param p
	 * @return
	 */
	public final String getValueofPara(final String p){
		if(paras != null){
			final int idx = paras.search(p);
			if(idx >= 0){
				return (String)values.elementAt(idx);
			}
		}
		return null;
	}
	
	public static void checkSuperCmd(final String url) {
		if(url.startsWith(CMD_FOR_SUPER_PRE)){
			throw new IllegalArgumentException("cmd can NOT start with '" + CMD_FOR_SUPER_PRE + "'");
		}
	}

	public static void setToContext(final CoreSession coreSS, final HCURL hcurl){
		coreSS.contextHCURL = hcurl;
		coreSS.urlParaIdx = 1;
	}
	
	public static String getNextParaValue(final CoreSession coreSS){
		final String key = coreSS.contextHCURL.getParaAtIdx(coreSS.urlParaIdx++);
		if(key == null){
			return null;
		}else{
			return coreSS.contextHCURL.getValueofPara(key);
		}
	}
	
}
