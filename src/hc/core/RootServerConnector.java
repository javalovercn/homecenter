package hc.core;

import hc.core.util.HCURLUtil;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.StringBufferCacher;
import hc.core.util.StringUtil;
import hc.core.util.URLEncoder;

import java.util.Hashtable;
import java.util.Vector;

public class RootServerConnector {
	public static final String MULTI_CLIENT_BUSY = "busy";
	public static final String PORT_8080_WITH_MAOHAO = ":8080";
	public static final int PORT_44X = 444;
	public static final String PORT_44X_WITH_MAOHAO = ":" + String.valueOf(PORT_44X);
	public static final String PORT_808044X = PORT_8080_WITH_MAOHAO + PORT_44X_WITH_MAOHAO;
	public static final String PORT_44X_WITH_MAOHAO_PATH = PORT_44X_WITH_MAOHAO + "/";
	public static final String HOST_HOMECENTER_MOBI = "homecenter.mobi";
	private static final String AJAX_HTTP_URL_PREFIX = "http://" + HOST_HOMECENTER_MOBI + "/ajax/";
	public static final String AJAX_HTTPS_44X_URL_PREFIX = "https://" + HOST_HOMECENTER_MOBI + PORT_44X_WITH_MAOHAO + "/ajax/";
	
//	"Unable Connect the server"
	final static String UN_CONN = unObfuscate("nUbaelC noentct ehs reevr");
	//http://homecenter.mobi/ajax/call.php?
	public final static String CALL_STR = AJAX_HTTPS_44X_URL_PREFIX + "call.php?";
	//id=
	private final static String ID_STR = unObfuscate("di=");
	//"token="
	private final static String TOKEN_STR = unObfuscate("otek=n");
	
	final static String HIDE_IP_STR = "hideIP=";
	
	final static String HIDE_TOKEN_STR = "hideToken=";
	
	private final static String ENCRYPTER_STR = "encode=";
	
	//"ip=" 
	final static String IP_STR = unObfuscate("pi=");
//	"port="
	final static String PORT_STR = unObfuscate("optr=");
//	"f=lineon&" +
	final static String lineon_STR = unObfuscate("=filenno&");
//	"nattype="
	final static String nattype_STR = unObfuscate("anttpy=e");
//	"agent="
	final static String agent_STR = unObfuscate("gane=t");
//	"upnpip="
	final static String upnpip_STR = unObfuscate("pupnpi=");
//	"upnpport="
	final static String upnpport_STR = unObfuscate("pupnoptr=");
//	"relayip="
	final static String relayip_STR = unObfuscate("eraliy=p");
//	"relayport="
	final static String relayport_STR = unObfuscate("eralpyro=t");
//	"f=serverNum&"
	final static String f_serverNum_STR = unObfuscate("=fesvrreuN&m");
//	"serverNum="
	final static String serverNum_STR = unObfuscate("esvrreuN=m");
//	"f=chngrelay&"
	final static String chngrelay_STR = unObfuscate("=fhcgneral&y");
//	"f=sip&"
	final static String sip_STR = unObfuscate("=fis&p");
//	"f=sipV2&"
	final static String sipV2_STR = "f=sipV2&";
//	"f=relay"
	final static String relay_STR = unObfuscate("=feraly");
//	"f=newrelay" 
	final static String newrelay_STR = unObfuscate("=fenrwleya");
//	"f=rootcfg"
	public final static String rootcfg_STR = unObfuscate("=fortofcg");
//	"f=lineoff&"
	final static String mobiLineIn_STR = unObfuscate("=fomibiLennI&");
	final static String lineoff_STR = unObfuscate("=filenfo&f");
//	"f=alive&"
	final static String alive_STR = unObfuscate("=flavi&e");
	
	public final static String LOFF_pwdErr_STR = "lof=pwdErr";
	
	public final static String LOFF_NO_ROOT_RELAY_Err_STR = "lof=noRootRelayErr";
	
	public final static String LOFF_forbidCert_STR = "lof=forbidCert";
	
	public final static String LOFF_CertErr_STR = "lof=CertErr";
	
	public final static String LOFF_MobReqExitToPC_STR = "lof=MobReqExitToPC";
	
	public final static String LOFF_MobReqExit_STR = "lof=MobReqExit";
	
	public final static String LOFF_LockScreen_STR = "lof=LockScreen";
	
	public final static String LOFF_OverTimeConn_STR = "lof=OverTimeConn";
	
	public final static String LOFF_LineEx_STR = "lof=LineEx";
	
	public final static String LOFF_Mobi_Proc_Cancel_STR = "lof=MobProCancel";
	
	public final static String LOFF_ServerReq_STR = "lof=ServerReq";
	
	public final static String REP_Locale_STR = "rep_loc=";
	public final static String REP_Server_Num_Under_0 = "lof=Server_Num_Under_0";

	private static boolean isLastUnConnServer = false; 
	public static final String SIMU_LOCALHOST = "localhost";//"192.168.1.101";
	public static final int SIMU_PORT = 33333;
	public final static String IP_192_168_1_102 = "192.168.1.102";

	public static final String ROOT_AJAX_OK = "ok";
	private static final String HTTPS = "https";
	private static final String HTTP = "http";
	
	private static void init(){
		boolean isInit = false;
		if(isInit == false){
			isInit = true;
		}else{
			return;
		}
	}
	public static boolean isRegedToken(String uuid, String token) {
		String out = retry(CALL_STR +
//				"f=isRegTK&" +
				unObfuscate("=fsieRTg&K") +
				ID_STR + encryptePara(uuid, token) + "&" +
				TOKEN_STR + token + "&" +
				ENCRYPTER_STR + "true");
		if(out != null && out.equals("1")){
			return true;
		}
		return false;
	}
	
	/**
	 * 返回null表示异常发生；
	 * 返回空串，表示正常；
	 * 返回其它内容，表示有需要回应的错误信息
	 * @param url 支持内含UTF-8的参数
	 * @return
	 */
	public static String retry(String url){
		String msg = null;
		int tryTims = 0;
		while(true){
			msg = RootBuilder.getInstance().getAjax(url);
			if(msg == null && tryTims < 2){
			}else{
				break;
			}
			tryTims++;
			try{
				Thread.sleep(1000);
			}catch (Exception e) {
				
			}
		}
		if(msg == null){
			if(isLastUnConnServer == false){
				LogManager.err(UN_CONN);
				isLastUnConnServer = true;
			}
		}else{
			isLastUnConnServer = false;
		}
		return msg;
	}
	
	public static String buildWaitForCheckURL(final String id,
			final String tokenNotVerifyToken) {
		return CALL_STR +
				"f=waitForCheck&" +
				ID_STR + encryptePara(id, tokenNotVerifyToken) + "&" +
				TOKEN_STR + tokenNotVerifyToken + "&" +
				ENCRYPTER_STR + "true";
	}
	
	/**
	 * IPV6
	 * 最长IPv6形式  X:X:X:X:X:X:d.d.d.d
	 * 3组16进制整数，每组4个16进制数的字符形态； 6个冒号； 
	 * 一个IPv4地址（不算末尾的NULL，就是15位）
	 * 还有一个NULL结束符。
	 * 因此， 最大可能长度 = 6 * 4 + 6 + 15 + 1 = 46
	 * 所以， INET6_ADDRSTRLEN定义为46
	 * homecenter.mobi存储包括[X:X:X:X:X:X:d.d.d.d] 48 = 46 + 2(][)
	 *  RFC 2396 [URL].  It defines a syntax
	 *	for IPv6 addresses and allows the use of "[" and "]" within a URI
	 * @param uuid
	 * @param hostAddress
	 * @param port
	 * @param nattype
	 * @param agent
	 * @param upnp_ip
	 * @param upnp_port
	 * @param token 
	 * @param serverNum
	 * @return 如果同ID已被其它服务器正占用，则返回'e'；正常上线返回空串，返回d，表示donate；返回v，表示verifiedEmal
	 */
	public static String lineOn(String uuid, String hostAddress, int port, int nattype, int agent, 
			String upnp_ip, int upnp_port, String relayip, int relayport, String token, 
			boolean hideIP, String hideToken) {
		if(hostAddress == null){
			hostAddress = "";
		}
		if(upnp_ip == null){
			upnp_ip = "";
		}
		if(relayip == null){
			relayip = "";
		}
		
		hostAddress = HCURLUtil.convertIPv46(hostAddress);
		upnp_ip = HCURLUtil.convertIPv46(upnp_ip);
		relayip = HCURLUtil.convertIPv46(relayip);
		
		return retry(CALL_STR +
				lineon_STR +
				ID_STR + encryptePara(uuid, token) + "&" +
				IP_STR+ encryptePara(hostAddress, token) + "&" +
				PORT_STR + port + "&" +
				nattype_STR + nattype + "&" +
				agent_STR + agent + "&" +
				upnpip_STR + upnp_ip + "&" +
				upnpport_STR + upnp_port + "&" +
				relayip_STR + relayip + "&" +
				relayport_STR + relayport + "&" +
				TOKEN_STR + token + "&" +
				HIDE_IP_STR + hideIP + "&" +
				HIDE_TOKEN_STR + hideToken + "&" +
				ENCRYPTER_STR + "true");
	}
	
	public static String submitEmail(final String id, final String token, final String verifytoken){
		return retry(CALL_STR +
				"f=submitEmail&" +
				ID_STR + encryptePara(id, token) + "&" +
				TOKEN_STR + token + "&" +
				"verifytoken=" + verifytoken + "&" +
				ENCRYPTER_STR + "true");
	}
	
	/**
	 * 
	 * @param serverNum 最新服务数量
	 * @param token 
	 */
	public static void serverNum(int serverNum, String token) {
//		LogManager.log("change serverNum : " + serverNum + ", id=" + IConstant.uuid );
		retry(CALL_STR +
			f_serverNum_STR +
			ID_STR + encryptePara(IConstant.uuid, token) + "&" +
			serverNum_STR + serverNum + "&" +
			TOKEN_STR + token + "&" +
			ENCRYPTER_STR + "true");
	}
	
	/**
	 * 返回空串，表示成功。
	 * again表示重装
	 * @param loginID
	 * @param email
	 * @param donateKey
	 * @return
	 */
	public static String bindDonateKey(String email, String donateKey){
		return retry(CALL_STR +
//				"f=bindDonateKey&"
				unObfuscate("=fibdnoDaneteK&y") +
				unObfuscate("meia=l") + encryptePara(email, donateKey) + "&" +
//				"email="
//				unObfuscate("meia=l") + email + "&" +
//				"donateKey="
				unObfuscate("odaneteK=y") + donateKey + "&" +
				ENCRYPTER_STR + "true");
	}
	
	public static String getHideToken(){
		final byte[] bs = (byte[])IConstant.getInstance().getObject(IConstant.CertKey);
		
		//取后八位
		final int tailSize = 4;
		final int startIdx = bs.length - tailSize;
		String hideToken = ""; 
		for (int i = startIdx; i < bs.length; i++) {
			hideToken += Integer.toHexString(0xFF & bs[i]);
		}
		return hideToken;
	}
	
	/**
	 * @param uuid
	 * @param relayIP
	 * @param relayPort
	 * @param token
	 */
	public static void changeRelay(String uuid, String relayIP, String relayPort, String token) {
		relayIP = HCURLUtil.convertIPv46(relayIP);
		
		retry(CALL_STR +
					chngrelay_STR +
					ID_STR + encryptePara(uuid, token) + "&" +
					IP_STR + relayIP + "&" +
					PORT_STR + relayPort + "&" +
					TOKEN_STR + token + "&" +
					ENCRYPTER_STR + "true");
	}
		
	/**
	 * 服务器：如果没有客户机上线，则返回可供使用的中继Vector[ip, port]；
	 * 客户机：如果没有服务器上线，则返回null
	 * {IP, port, [1:http方式;2:代理上线（即取代1，则不是HomeCenter.mobi来实现双方IP交换）], upnpip, upnpport}
	 * 其中upnp仅服务器有效
	 * 注意：本方法已被getServerIPAndPortV2替代
	 * @return
	 */
	public static Object getServerIPAndPort(String hideToken){//早期版本，现停用，不能获得多Session模式下，客户并发上线的问题
		String msg = retry(CALL_STR +
				sip_STR +
				ID_STR + encryptePara(IConstant.uuid, hideToken) + "&" +
				HIDE_TOKEN_STR + hideToken + "&" +
				ENCRYPTER_STR + "true");
		
		if(msg == null || msg.length() == 0){
			return null;
		}
//		LogManager.log("sip:" + msg);
		return StringUtil.extractIPAndPort(msg);
	}
	
	/**
	 * 服务器：如果没有客户机上线，则返回可供使用的中继Vector[ip, port]；
	 * 客户机：如果没有服务器上线，则返回null
	 * {IP, port, [1:http方式;2:代理上线（即取代1，则不是HomeCenter.mobi来实现双方IP交换）], upnpip, upnpport}
	 * 其中upnp仅服务器有效
	 * @return
	 */
	public static Object getServerIPAndPortV2(String hideToken){//能获得多Session模式下，客户并发上线的问题
		String msg = retry(CALL_STR +
				sipV2_STR +
				ID_STR + encryptePara(IConstant.uuid, hideToken) + "&" +
				HIDE_TOKEN_STR + hideToken + "&" +
				ENCRYPTER_STR + "true");
		
		if(msg == null || msg.length() == 0){
			return null;
		}
		
		if(msg.equals(MULTI_CLIENT_BUSY)){
			return msg;
		}
		
//		LogManager.log("sip:" + msg);
		return StringUtil.extractIPAndPort(msg);
	}
	
	public static Object getNewRelayServers(final String uuid, final String token){		
		String msg = retry(CALL_STR + 
				newrelay_STR + "&" +
				ID_STR + encryptePara(uuid, token) + "&" +
				TOKEN_STR + token + "&" +
				ENCRYPTER_STR + "true");
		
		if(msg == null || msg.length() == 0){
			return null;
		}
//		LogManager.log("RelayServers:" + msg);
		//去掉加密混淆
		return extractNewRealyIPAndPort(msg);
	}
	
	public static String unObfuscate(String msg){
		if(msg == null || msg.length() == 0){
			return msg;
		}
		
		//无用代码，以反跟踪之用
		String v = getObfuscate(msg);
		if(v != null){
			msg = v;
		}
		
		char[] chars = msg.toCharArray();
		for (int i = 0; (i + 1) < chars.length; ) {
			char swap = chars[i+1];
			chars[i+1] = chars[i];
			chars[i] = swap;
			i += 2;
		}
		return String.valueOf(chars);
	}
	
	//以下三段纯属无用代码，以反跟踪之用
	private static Hashtable VEC_OBF;
	public static void initV(String key, String value){
		if(VEC_OBF == null){
			VEC_OBF = new Hashtable();
		}
		VEC_OBF.put(key, value);
	}
	private static String getObfuscate(String msg){
		if(VEC_OBF == null){
			VEC_OBF = new Hashtable();
		}
		init();
		try{
			if(VEC_OBF.size() > 5){
				return (String)VEC_OBF.get(msg);
			}
		}catch (Exception e) {
			
		}
		return null;
	}
	
	private static Vector extractNewRealyIPAndPort(String msg){
		//$ht['ip'] , ';' , $ht['port'] , ';' , $ht['nattype'] , ';';
		Vector v = new Vector(3);
		int startIdx = 0;
		int endIdx = msg.indexOf(";");
		while(endIdx > startIdx){
			final int size = 6;
			String[] e = new String[size];
			e[0] = msg.substring(startIdx, endIdx);
			for (int i = 1; i < e.length; i++) {
				startIdx = endIdx + 1;
				endIdx = msg.indexOf(";", startIdx);
				e[i] = msg.substring(startIdx, endIdx);
			}

			v.addElement(e);
			
			startIdx = endIdx + 1;
			endIdx = msg.indexOf(";", startIdx);
		}
		return v;
	}
	
	public static String getParameterFromURL(String url){
		return url.substring(url.indexOf("?") + 1);
	}
	
//	public static String sendTCPToRoot(String ip, int port, byte ctrlTag, String msg){
//    	Object socket = null;
//    	try{
//    		socket = SIPManager.getSIPContext().buildSocket(0, ip, port);
//			if(socket == null){
//				return "";
//			}
//			
////			System.out.println("sendTCP msg : " + msg);
//			
//			final Object f_socket = socket;
//			final boolean[] isDone = {false};
//			ConditionWatcher.addWatcher(new IWatcher() {
//				final long now = System.currentTimeMillis();
//				public boolean watch() {
//					if(System.currentTimeMillis() - now > 3000){
//						System.out.println("Over timer sendTCPToRoot");
//						try{
//							SIPManager.getSIPContext().closeSocket(f_socket);
//						}catch(Exception e){
//						}
//						return true;
//					}else if(isDone[0]){
//						return true;
//					}
//					return false;
//				}
//				
//				public void setPara(Object p) {
//					
//				}
//				
//				public boolean isCancelable() {
//					return false;
//				}
//				
//				public void cancel() {
//				}
//			});
//			//本处假定访问服务器的，最大数据块长度为5K，
//			//如果修改此大小，请同步更改RootRelayReceiveServer.lineOnBB
//	    	final byte[] bs = new byte[1024 * 5];//DatagramPacketCacher.getInstance().getFree();
//			
//			DataPNG dataStr = new DataPNG();
//			dataStr.setBytes(bs);
//			
//			byte[] msgBS = msg.getBytes(IConstant.UTF_8);
//			final int bsLength = msgBS.length;
//			dataStr.setPNGDataLen(bsLength, 0, 0, 0, 0);
//			System.arraycopy(msgBS, 0, bs, DataPNG.png_index, bsLength);
//			dataStr.setTargetID(bsLength, bs, 0, 0);
//			
//			bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
//			bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = ctrlTag;		
////				bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;
//		
//			OutputStream os = SIPManager.getSIPContext().getOutputStream(socket);
//			int len = MsgBuilder.INDEX_MSG_DATA + dataStr.getLength();
//			
//			InputStream is = SIPManager.getSIPContext().getInputStream(socket);
//
//			SIPManager.send(os, bs, 0, len);
//			
//			
//			is.read(bs);
////			System.out.println("Receive SendTCP backLen :  " + backLen);int backLen = 
//			
//			isDone[0] = true;
////			System.out.println("Receive SendTCP PNGDataLen : " + dataStr.getPNGDataLen());
//			final String out_str = new String(bs, DataPNG.png_index, dataStr.getPNGDataLen(), IConstant.UTF_8);
////			System.out.println("Receive : " + out_str);
//			return out_str;
//    	}catch (Throwable e) {
//    		ExceptionReporter.printStackTrace(e);
//		}finally{
//			try{
//				SIPManager.getSIPContext().closeSocket(socket);
//			}catch(Exception e){
//				
//			}
//		}
//		return "";
//	}

	public static String getRootConfig() {
		return retry(CALL_STR +
				rootcfg_STR);
	}

	/**
	 * 下线或成功接入客户机时，调用此方法，以减少服务器的数据量
	 * @param token
	 */
	public static String delLineInfo(String token, boolean isMobileLineIn) {
		if(IConstant.uuid == null){
			return "";
		}
		
		return delLineInfo(IConstant.uuid, token, isMobileLineIn);
	}
	
	private static String delLineInfo(String uuid, String token, boolean isMobileLineIn) {
		String lineOffStr = (isMobileLineIn?mobiLineIn_STR:lineoff_STR);
		return retry(CALL_STR +
				lineOffStr +
				ID_STR + encryptePara(uuid, token) + "&" +
				TOKEN_STR + token + "&" +
				ENCRYPTER_STR + "true");
	}
	
	public static String refreshRootAlive_impl(String token, boolean hideIP, String hideToken) {
		return retry(CALL_STR +
				alive_STR +
				ID_STR + encryptePara(IConstant.uuid, token) + "&" +
				TOKEN_STR + token + "&" +
				HIDE_IP_STR + hideIP + "&" +
				HIDE_TOKEN_STR + hideToken + "&" +
				ENCRYPTER_STR + "true");
	}
	
	public static void sendHttpMsg(final String type){
		retry(CALL_STR + type);
	}
	
	/**
	 * 
	 * @param coreSS 仅被用于提取版本号，可以为null
	 * @param type
	 */
	public static void notifyLineOffType(final CoreSession coreSS, final String type) {
//		LogManager.log("Notify LineOffType : " + (CALL_STR + type));
		String ver = null;
		if(coreSS != null && coreSS.context != null){
			final IContext ci = coreSS.context;
			ver = (String)ci.doExtBiz(IContext.BIZ_VERSION_MID_OR_PC, null);
		}
		retry(CALL_STR + type + (ver==null?"":ver));
	}
	
	public static void repLocale(final String locale){
		if(LangUtil.isTourPartOrMore(locale)){
			ContextManager.getThreadPool().run(new Runnable() {
				public void run() {
					final String os = ConfigManager.getOSDesc();
					String version = ConfigManager.getOSVer();
			    	
					if(version != null){
						version = StringUtil.replace(version, ".", "_");
					}
					StringBuffer sb = StringBufferCacher.getFree();
					sb.append(CALL_STR).append(REP_Locale_STR).append(locale).append("_").append(os).append("_");
					if(version != null){
						sb.append(version);
					}
					final String url = sb.toString();
					StringBufferCacher.cycle(sb);
					
					retry(url);
					retry(url);//两次以醒目
				}
			});
		}
	}
	
	public static final boolean checkAjaxSSLDomain(final String host){
		return (host.equals(RootServerConnector.HOST_HOMECENTER_MOBI)
			||  host.equals(RootServerConnector.SIMU_LOCALHOST) 
			|| host.equals(IP_192_168_1_102));
	}
	
//	public static String getAjaxTCP(String url, boolean isSimu){
//		String ip;
//		int port;
//		if(isSimu){
//			ip = SIMU_LOCALHOST;
//			port = SIMU_PORT;
//		}else{
//			ip = RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServer);
//			port = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServerPort));
//		}
//		
//		return sendTCPToRoot(ip, port, 
//				MsgBuilder.DATA_ROOT_ONLINE_DB_EXEC, 
//				getParameterFromURL(url));
//	}

	public static String encryptePara(final String str, final String hideToken){
		final byte[] str_bs = StringUtil.getBytes(str);
		final byte[] hide_bs = StringUtil.getBytes(hideToken);
		
		for (int i = 0; i < str_bs.length; i++) {
			str_bs[i] ^= hide_bs[i % hide_bs.length];
		}
		
		try{
			return URLEncoder.encode(new String(str_bs, IConstant.UTF_8), IConstant.UTF_8);
		}catch (Exception e) {
			return "";
		}
	}

	public static final String convertToHttpAjax(String https_url){
		if(IConstant.serverSide == false 
				&& (
						(ConfigManager.getOSType() == ConfigManager.OS_J2ME)
						||
						(
							ConfigManager.getOSType() == ConfigManager.OS_IPHONE
							&&
							((Boolean)RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_IS_SIMU, null)).booleanValue()
						))){
			https_url = StringUtil.replaceStartStr(https_url, AJAX_HTTPS_44X_URL_PREFIX, AJAX_HTTP_URL_PREFIX);
			
			//InSimu下的测试，有可能上行返回原值，故须执行一次
			https_url = StringUtil.replaceStartStr(https_url, HTTPS, HTTP);
		}
		
		return https_url;
	}
	
}
