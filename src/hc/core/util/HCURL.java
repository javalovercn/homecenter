package hc.core.util;


public class HCURL {
	public String toString(){
		String ps = "";

		if(paras != null){
			int size = paras.size();
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
	public static final String DATA_PARA_CLASS = "class";
	public static final String DATA_PARA_TRANSURL = "transURL";
	public static final String DATA_PARA_CERT_RECEIVED = "certRved";
	public static final String DATA_PARA_FORBID_CERT_UPDATE = "forbidCertUpd";
	public static final String DATA_PARA_RELOAD_THUMBNAIL = "reloadThumbnail";
	
	public static final String REMOTE_HOME_SCREEN = "home";
	
	
	public static final String CMD_PROTOCAL = "cmd";
	public static final String SCREEN_PROTOCAL = "screen";
	public static final String CONTROLLER_PROTOCAL = "controller";
	public static final String FORM_PROTOCAL = "form";
	public static final String MENU_PROTOCAL = "menu";
	
	public static final String URL_CMD_EXIT = buildStandardURL(HCURL.CMD_PROTOCAL, HCURL.DATA_CMD_EXIT);
	public static final String URL_HOME_SCREEN = buildStandardURL(HCURL.SCREEN_PROTOCAL, HCURL.REMOTE_HOME_SCREEN);

	public static final String[] URL_PROTOCAL = {CMD_PROTOCAL, SCREEN_PROTOCAL, CONTROLLER_PROTOCAL,
		FORM_PROTOCAL, MENU_PROTOCAL};
	
	public static String buildStandardURL(final String protocal, final String target){
		return protocal + "://" + target;
	}

	public static int getURLProtocalIdx(String protocal){
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
	
	public String getParaAtIdx(int idx){
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
	
	public void addParaVales(String p, String v){
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
	public String getValueofPara(String p){
		if(paras != null){
			int idx = paras.search(p);
			if(idx >= 0){
				return (String)values.elementAt(idx);
			}
		}
		return null;
	}
	
	private static int paraIdx = 1;
	private static HCURL contextHCURL;
	public static void setToContext(HCURL hcurl){
		contextHCURL = hcurl;
		paraIdx = 1;
	}
	
	public static String getNextParaValue(){
		String key = contextHCURL.getParaAtIdx(paraIdx++);
		if(key == null){
			return null;
		}else{
			return contextHCURL.getValueofPara(key);
		}
	}
	
}
