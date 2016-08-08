package hc.core.util;


public class HCURL {

	public static final String HTTP_SPLITTER = "://";

	public String toString(){
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
	
	public static final String ROOT_MENU = "root";
	
	public static final String DATA_IOT_DEVICE = "Device";
	public static final String DATA_IOT_CONVERTER = "Converter";
	public static final String DATA_IOT_ROBOT = "Robot";
	
	public static final String DATA_CMD_EXIT = "exit";
	public static final String DATA_CMD_CONFIG = "config";
	public static final String DATA_CMD_ALERT = "_alert";
	public static final String DATA_CMD_MSG = "_msg";
	public static final String DATA_CMD_MOVING_MSG = "_mv_msg";
	public static final String DATA_CMD_CTRL_BTN_TXT = "_ctl_btn_txt";
	
	//关闭，并合并入DATA_CMD_EXIT
//	public static final String DATA_CMD_BYE_SCREEN = "_byeScreen";
	
	public static final String DATA_CMD_SendPara = "_Send";
	
	public static final String DATA_PARA_INPUT = "Input";

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
	public static final String DATA_PARA_QUESTION_RESULT = "ques_result";
	public static final String DATA_PARA_PROC_ADD_HAR_URL = "addHARUrl";
	public static final String DATA_PARA_WIFI_MANAGER = "WiFiManager";
	public static final String DATA_PARA_PUBLISH_STATUS_ID = "publishStatusID";
	public static final String DATA_PARA_PUBLISH_STATUS_VALUE = "publishStatusValue";
	
	public static final String DATA_RECEIVER_MLET = "rev_mlet";
	public static final String DATA_RECEIVER_HTMLMLET = "rev_htmlmlet";
	
	public static final String REMOTE_HOME_SCREEN = "home";
	public static final String ADD_HAR_QR = "addHARByQR";
	public static final String ADD_HAR_WIFI = "addHARByWiFi";
	
	public static final String CMD_PROTOCAL = "cmd";
	public static final String SCREEN_PROTOCAL = "screen";
	public static final String CONTROLLER_PROTOCAL = "controller";
	public static final String FORM_PROTOCAL = "form";
	public static final String CFG_PROTOCAL = "cfg";
	public static final String MENU_PROTOCAL = "menu";
	
	public static final String URL_CMD_EXIT = buildStandardURL(HCURL.CMD_PROTOCAL, HCURL.DATA_CMD_EXIT);
	public static final String URL_HOME_SCREEN = buildStandardURL(HCURL.SCREEN_PROTOCAL, HCURL.REMOTE_HOME_SCREEN);
	public static final String URL_CFG_ADD_DEVICE_BY_QR = buildStandardURL(HCURL.CFG_PROTOCAL, ADD_HAR_QR);
	public static final String URL_CFG_ADD_DEVICE_BY_WIFI = buildStandardURL(HCURL.CFG_PROTOCAL, ADD_HAR_WIFI);
	
	public static final String URL_DEFAULT_MLET_ALIAS = "";
	
	public static final boolean isUsingWiFiWPS = false;
	
	public static final String[] URL_PROTOCAL = {CMD_PROTOCAL, SCREEN_PROTOCAL, CONTROLLER_PROTOCAL,
		FORM_PROTOCAL, CFG_PROTOCAL, MENU_PROTOCAL};
	public static final String[] WAIT_URL_PROTOCAL = {SCREEN_PROTOCAL, CONTROLLER_PROTOCAL,
		FORM_PROTOCAL, CFG_PROTOCAL, MENU_PROTOCAL};
	
	public static String buildStandardURL(final String protocal, final String target){
		return protocal + HTTP_SPLITTER + target;
	}
	
	private static String FORM_MLET_PREFIX = buildStandardURL(FORM_PROTOCAL, "");
	
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
	
	public void removeAllParaValues(){
		if(paras != null){
			paras.removeAllElements();
			values.removeAllElements();
		}
	}
	
	public int getParaSize(){
		if(paras == null){
			paras = new Stack();
			values = new Stack();
		}
		return paras.size();
	}
	
	public String getParaAtIdx(final int idx){
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
	
	public void addParaVales(final String p, final String v){
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
	public String getValueofPara(final String p){
		if(paras != null){
			final int idx = paras.search(p);
			if(idx >= 0){
				return (String)values.elementAt(idx);
			}
		}
		return null;
	}
	
	private static int paraIdx = 1;
	private static HCURL contextHCURL;
	public static void setToContext(final HCURL hcurl){
		contextHCURL = hcurl;
		paraIdx = 1;
	}
	
	public static String getNextParaValue(){
		final String key = contextHCURL.getParaAtIdx(paraIdx++);
		if(key == null){
			return null;
		}else{
			return contextHCURL.getValueofPara(key);
		}
	}
	
}
