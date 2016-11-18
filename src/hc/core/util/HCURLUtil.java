package hc.core.util;

import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.MsgBuilder;

import java.io.UnsupportedEncodingException;

/**
 * {menu|form}://{host}/id
 * host可省
 *
 */
public class HCURLUtil {
	private static final String[] PROTOCALS = HCURL.URL_PROTOCAL; 
		//{HCURL.MENU_PROTOCAL, HCURL.FORM_PROTOCAL, HCURL.SCREEN_PROTOCAL, HCURL.CMD_PROTOCAL};
	
	private static final byte[][] PROTOCALS_BYTES = initProtocolBytes(PROTOCALS);
	
	private static byte[][] initProtocolBytes(String[] array){
		byte[][] out = new byte[array.length][];
		for (int i = 0; i < out.length; i++) {
			try {
				out[i] = array[i].getBytes(IConstant.UTF_8);
			} catch (UnsupportedEncodingException e) {
				ExceptionReporter.printStackTrace(e);
				out[i] = array[i].getBytes();
			}
		}
		return out;
	}
	
	private static final short NUM_PROTOCAL = (short)PROTOCALS.length;

	public static final HCURLCacher hcurlCacher = HCURLCacher.getInstance();
	
	/**
	 * 如果没有已识别的协议，返回null，
	 * 注意：要回收HCURL
	 * {menu|form}://{host}/id
	 * host可省
	 * @param url
	 * @return
	 */
	public static HCURL extract(String url){
		return extract(url, true);
	}
	
	public static HCURL extract(String url, final boolean isDecodeValue){
		try {
			byte[] bs = url.getBytes(IConstant.UTF_8);
			
			HCURL hcurl = hcurlCacher.getFree();
			hcurl.url = url;

			boolean isSame = true;
			int startCtxIdx = 0;
			for (int i = 0; i < NUM_PROTOCAL; i++) {
				isSame = true;
				byte[] bytes = PROTOCALS_BYTES[i];
				for (int j = 0; j < bytes.length; j++) {
					if(bs[j] != bytes[j]){
						isSame = false;
						break;
					}
				}
				if(isSame){
					hcurl.protocal = PROTOCALS[i];
					startCtxIdx = hcurl.protocal.length() + 3;
					break;
				}
			}
			
			final int end = bs.length;
			int cmdIdx = startCtxIdx;
			int lastSplashIdx = 0;
			for (; cmdIdx < end; cmdIdx++) {
				final byte b = bs[cmdIdx];
				if(b == '/'){
					lastSplashIdx = cmdIdx;
				}else if(b == '?'){
					break;
				}
			}
			if(lastSplashIdx == 0){
				hcurl.context = "";
				cmdIdx = startCtxIdx;
			}else{
				hcurl.context = new String(bs, startCtxIdx, lastSplashIdx - startCtxIdx, IConstant.UTF_8);
				cmdIdx = lastSplashIdx + 1;
			}
			
			final int j = cmdIdx;
			boolean isReplace = false;
			
			int lenJ = end - j;
			for (int j2 = j + 1; j2 < end; j2++) {
				if(bs[j2] == '?'){
					if(bs[j2 - 1] != '\\'){
						lenJ = j2 - j;
						
						pushParaValues(hcurl, bs, j2 + 1, null, isDecodeValue);
						break;
					}
				}else{
					isReplace = true;
				}
			}

			
			String string = new String(bs, j, lenJ, IConstant.UTF_8);
			hcurl.elementID = isReplace?StringUtil.replace(string, "\\?", "?"):string;
			return hcurl;
		} catch (UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		return null;
	}
	
	/**
	 * 发布状态从手机到服务器，或从服务器到手机
	 * @param coreSS 
	 * @param id
	 * @param status
	 */
	public final static void publishStatus(CoreSession coreSS, final String id, final String status){
		final String[] paras = {HCURL.DATA_PARA_PUBLISH_STATUS_ID, HCURL.DATA_PARA_PUBLISH_STATUS_VALUE};
		final String[] values = {id, status};
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, paras, values);
	}
	
	private static void pushParaValues(HCURL url, byte[] bs, int index, String p, boolean isDecodeValue){
		if(p == null){
			for (int i = index; i < bs.length; i++) {
				if(bs[i] == '='){
					if(bs[i - 1] != '\\'){
						String string = null;
						try {
							string = new String(bs, index, i - index, IConstant.UTF_8);
						} catch (UnsupportedEncodingException e) {
							string = new String(bs, index, i - index);
							ExceptionReporter.printStackTrace(e);
						}
						String v = StringUtil.replace(string, "\\&", "&");
						v = StringUtil.replace(v, "\\=", "=");
						pushParaValues(url, bs, i + 1, v, isDecodeValue);
						return;
					}else{
						continue;
					}
				}
			}
		}else{
			for (int i = index; i < bs.length; i++) {
				if(bs[i] == '&'){
					if(bs[i - 1] != '\\'){
						String string = null;
						try {
							string = new String(bs, index, i - index, IConstant.UTF_8);
						} catch (UnsupportedEncodingException e) {
							string = new String(bs, index, i - index);
							ExceptionReporter.printStackTrace(e);
						}
						String v = StringUtil.replace(string, "\\&", "&");
						v = StringUtil.replace(v, "\\=", "=");
						url.addParaVales(p, isDecodeValue?decode(v, IConstant.UTF_8):v);
						if(i + 1 < bs.length){
							pushParaValues(url, bs, i + 1, null, isDecodeValue);
						}
						return;
					}else{
						continue;
					}
				}
			}
			String string = null;
			try {
				string = new String(bs, index, bs.length - index, IConstant.UTF_8);
			} catch (UnsupportedEncodingException e) {
				string = new String(bs, index, bs.length - index);
				ExceptionReporter.printStackTrace(e);
			}
			String v = StringUtil.replace(string, "\\&", "&");
			v = StringUtil.replace(v, "\\=", "=");
			url.addParaVales(p, isDecodeValue?decode(v, IConstant.UTF_8):v);
		}
	}
	
	public static String decode(String s, String enc) {
		boolean needToChange = false;
		int numChars = s.length();
		StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2
				: numChars);
		int i = 0;

		if (enc.length() == 0) {
			return enc;
		}

		char c;
		byte[] bytes = null;
		while (i < numChars) {
			c = s.charAt(i);
			switch (c) {
			case '+':
				sb.append(' ');
				i++;
				needToChange = true;
				break;
			case '%':
				try {
					if (bytes == null)
						bytes = new byte[(numChars - i) / 3];
					int pos = 0;

					while (((i + 2) < numChars) && (c == '%')) {
						bytes[pos++] = (byte) Integer.parseInt(
								s.substring(i + 1, i + 3), 16);
						i += 3;
						if (i < numChars)
							c = s.charAt(i);
					}

					if ((i < numChars) && (c == '%'))
						throw new IllegalArgumentException(
								"URLDecoder: Incomplete trailing escape (%) pattern");

					String utf_str;
					try {
						utf_str = new String(bytes, 0, pos, enc);
					} catch (UnsupportedEncodingException e) {
						try {
							utf_str = new String(bytes, 0, pos,
									IConstant.ISO_8859_1);
						} catch (UnsupportedEncodingException e1) {
							utf_str = new String(bytes, 0, pos);
						}
					}
					sb.append(utf_str);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal hex characters");
				}
				needToChange = true;
				break;
			default:
				sb.append(c);
				i++;
				break;
			}
		}
		return (needToChange ? sb.toString() : s);
	}
	
	public static boolean process(final CoreSession coreSS, String url, IHCURLAction action){
		boolean isDone = false;
		HCURL hu = HCURLUtil.extract(url);
		try{
			isDone = action.doBiz(coreSS, hu);
		}catch (Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		hcurlCacher.cycle(hu);
		return isDone;
	}

	/**
	 * 如果是IPv6且非规范，则转为规范，
	 * 如果是IPV4，则不作转换
	 * @param ip
	 * @return
	 */
	public static String convertIPv46(String ip) {
		if(ip.indexOf(":") >= 0 && (ip.charAt(0) != '[')){//ip.startsWith("[") == false
			//IPv6
			ip = "[" + ip + "]";
		}
		return ip;
	}

	public static void sendGoPara(CoreSession coreSS, String para, String value) {
		sendCmd(coreSS, HCURL.DATA_CMD_SendPara, para, value);
	}
	
	public static void sendCmd(CoreSession coreSS, String cmdType, String para, String value){
		pSendCmd(coreSS, MsgBuilder.E_GOTO_URL, cmdType, encode(para), encode(value));
	}

	public static String encode(String value) {
		if(value.indexOf("+") >= 0){
			value = StringUtil.replace(value, "+", "%2b");
		}
		if(value.indexOf("&") >= 0){
			value = StringUtil.replace(value, "&", "\\&");
		}
		if(value.indexOf("=") >= 0){
			value = StringUtil.replace(value, "=", "\\=");
		}
		return value;
	}

	private static void pSendCmd(CoreSession coreSS, byte tag, String cmdType, String para, String value) {
		coreSS.context.send(tag, HCURL.CMD_PROTOCAL + HCURL.HTTP_SPLITTER + cmdType + "?" + para + "=" + value);
	}

	public static void sendCmdUnXOR(CoreSession coreSS, String cmdType, String para, String value){
		pSendCmd(coreSS, MsgBuilder.E_GOTO_URL_UN_XOR, cmdType, para, value);
	}
	
	public static void sendEClass(CoreSession coreSS, final String className, final String classPara){
		final byte[] bs = StringUtil.getBytes(classPara);
		sendEClass(coreSS, className, bs, 0, bs.length);
	}
	
	/**
	 * 可以从服务器端调用，也可从手机端调用
	 * @param coreSS 
	 * @param className
	 * @param bs
	 * @param offset
	 * @param len
	 */
	public static void sendEClass(CoreSession coreSS, final String className, final byte[] bs, final int offset, final int len){
		final byte[] classBS = ByteUtil.getBytes(className, IConstant.UTF_8);
		
		//classNameLen(4) + classBS + paraLen(4) + paraBS
		final int classBSLen = classBS.length;
		final int newLen = 4 + classBSLen + 4 + len;
		final ByteArrayCacher cache = ByteUtil.byteArrayCacher;
		
		final byte[] out = cache.getFree(newLen);
		int nextStoreIdx = 0;
		ByteUtil.integerToFourBytes(classBSLen, out, nextStoreIdx);
		nextStoreIdx += 4;
		System.arraycopy(classBS, 0, out, nextStoreIdx, classBSLen);
		nextStoreIdx += classBSLen;
		ByteUtil.integerToFourBytes(len, out, nextStoreIdx);
		nextStoreIdx += 4;
		System.arraycopy(bs, offset, out, nextStoreIdx, len);
		
		coreSS.context.sendWrap(MsgBuilder.E_CLASS, out, 0, newLen);
		cache.cycle(out);
	}
	
	public static final String CLASS_BODY_TO_MOBI = "BODY_TO_MOBI";
	public static final String CLASS_GO_EXTERNAL_URL = "goExternalURL";
	public static final String CLASS_CHANGE_PROJECT_ID = "changeProjID";
	public static final String CLASS_ERR_ON_CACHE = "errOnCache";
	public static final String CLASS_TRANS_SERVER_UID = "transServerUID";

	
	public static final int HTTPS_CONN_TIMEOUT = 10 * 1000;
	public static final int HTTPS_READ_TIMEOUT = 10 * 1000;
			
	public static void sendCmd(CoreSession coreSS, String cmdType, String[] para, String[] value){
//		sendWrap进行了拦截
		
		String pv = "";
		for (int i = 0; i < para.length; i++) {
			if(pv.length() > 0){
				pv += "&";
			}
			pv += encode(para[i]) + "=" + encode(value[i]);
		}
		coreSS.context.send(MsgBuilder.E_GOTO_URL, 
				HCURL.CMD_PROTOCAL + HCURL.HTTP_SPLITTER + cmdType + "?" + pv);
	}

}
