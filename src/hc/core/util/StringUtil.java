package hc.core.util;

import hc.core.HCConfig;
import hc.core.IConstant;

import java.io.Reader;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

public class StringUtil {
	public static final String SPLIT_LEVEL_1_AT = "@@@";
	public static final String SPLIT_LEVEL_2_JING = "###";
	public static final String SPLIT_LEVEL_3_DOLLAR = "$$$";

	public final static String JAD_EXT = ".jad";
	public final static String URL_EXTERNAL_PREFIX = "http";
	
	public static String replace(String src, final String find, final String replaceTo){
		int index = 0;
		String out = src;
		while(index >= 0){
			index = src.indexOf(find, index);
			if(index >= 0){
				out = src.substring(0, index) + replaceTo + src.substring(index + find.length());
				//如替换:80为:8080，没有下行命令，则会进入循环
				index += replaceTo.length();
				src = out;
			}
		}
		return out;
	}

	public static String toSerialBySplit(final String[] item){
		if(item.length == 0){
			return "";
		}
		
		final StringBuffer sb = new StringBuffer("");
		
		final int minusOne = item.length - 1;
		for (int i = 0; i < minusOne; i++) {
			sb.append(item[i]);
			sb.append(HCConfig.CFG_SPLIT);
		}
		sb.append(item[minusOne]);
		return sb.toString();
	}
	
	/**
	 * int red => 00FF0000
	 * 注意：对于CSS，不使用useAlpha，即useAlpha=false。因为A=0，颜色不出现
	 */
	public static String toARGB(final int color, final boolean useAlpha){
		if(useAlpha){
			return Integer.toHexString(color);
		}else{
			return Integer.toHexString(color & 0x00FFFFFF);
		}
	}
	
	public static byte[] getBytes(final String str){
		try{
			return str.getBytes(IConstant.UTF_8);
		}catch (final Exception e) {
			return str.getBytes();
		}
	}
	
	public static String bytesToString(final byte[] bs, final int offset, final int len){
		try{
			return new String(bs, offset, len, IConstant.UTF_8);
		}catch (final Exception e) {
			return new String(bs, offset, len);
		}
	}

	/**
	 * {IP, port, [1:NATType;2:代理上线（即取代1，则不是HomeCenter.mobi来实现双方IP交换）], upnpip, upnpport, relayIP, relayPort}
	 * @param msg
	 * @return
	 */
	public static String[] extractIPAndPort(final String msg) {
		final String[] out = StringUtil.splitToArray(msg, ";");
		return out;
	}

	/**
	 * 
	 * @param src
	 * @param split 支持如单字符:或组合###
	 * @return
	 */
	public static String[] splitToArray(final String src, final String split){
	    final int split_length = split.length();
	
	    final String[] out = new String[StringUtil.splitCount(src, split)];
	    int c = 0;
	    int idx = 0;
	    int nextIdx = src.indexOf(split, 0);
		while(nextIdx >= 0){
	        out[c++] = src.substring(idx, nextIdx);
	        
			idx = nextIdx + split_length;
			nextIdx = src.indexOf(split, idx); 
		}
		
		out[c] = src.substring(idx);
	    return out;
	}

	private static int splitCount(final String src, final String split){
		final int split_length = split.length();
	    int c = 0;
	    int idx = 0;
	    int nextIdx = src.indexOf(split, 0);
		while(nextIdx >= 0){
	        c++;
	        
			idx = nextIdx + split_length;
			nextIdx = src.indexOf(split, idx); 
		}

		return ++c;
	}

	/**
	 * 如果ver1(1.2.1.3)低于ver2(1.2.11)，则返回true;
	 * @param ver1
	 * @param ver2
	 * @return
	 */
	public static boolean lower(final String ver1, final String ver2){
		return ((ver1.equals(ver2) || higher(ver1, ver2)) == false);
	}
	
	/**
	 * 如果ver1(1.2.11)高于ver2(1.2.1.3)，则返回true;
	 * @param ver1
	 * @param ver2
	 * @return
	 */
	public static boolean higher(final String ver1, final String ver2){
		final int s1_index = ver1.indexOf(".", 0);
		
		final int s2_index = ver2.indexOf(".", 0);
		
		int ss1, ss2;
		if(s1_index > 0){
			ss1 = Integer.parseInt(ver1.substring(0, s1_index));
	
			if(s2_index < 0){
				ss2 = Integer.parseInt(ver2);
				
				if( ss1 == ss2 ){
					return higher(ver1.substring(s1_index + 1), "0");//fix 7.0 > 7
				}else {
					return ss1 > ss2;
				}
			}else{
				ss2 = Integer.parseInt(ver2.substring(0, s2_index));
				
				if(ss1 == ss2){
					return higher(ver1.substring(s1_index + 1), ver2.substring(s2_index + 1));
				}else{
					return (ss1 > ss2);
				}
			}
		}else{
			ss1 = Integer.parseInt(ver1);
			
			if(s2_index > 0){
				ss2 = Integer.parseInt(ver2.substring(0, s2_index));
				
				return (ss1 > ss2);
			}else{
				
				ss2 = Integer.parseInt(ver2);
				return ss1 > ss2;
			}
		}
	}

	public static Vector split(final String msg, final String split) {
		final Vector v = new Vector(8);
		if(msg == null || msg.length() == 0){
			return v;
		}
		
		int idx = 0;
		int nextidx = msg.indexOf(split, idx);
		final int len = split.length();
		while(nextidx >= 0){
			v.addElement(msg.substring(idx, nextidx));
			idx = nextidx + len;
	
			nextidx = msg.indexOf(split, idx);
		}
		v.addElement(msg.substring(idx));
		return v;
	}

	public static int adjustFontSize(int screenWidth, final int screenHeight) {
		screenWidth=screenWidth>screenHeight?screenWidth:screenHeight;  
	    final int rate = (int)(7*(float) screenWidth/320);
	    if (rate<16){
	    	return 16;
	    }else if(rate > 36){//乐1 screenWidth:1854, rate:40
	    	return rate;
	    }else{
	    	return rate;
	    }
	}

	/**
	 * 读取文件流到串
	 */
	public static String load(final Reader stream) {
		final char[] buf = new char[1024];
		final StringBuffer sb = new StringBuffer();
		
		try{
			int len;
			do{
				len = stream.read(buf);
				sb.append(buf, 0, len);
			}while(len > 0);
		}catch (final Exception e) {
		}finally{
			try{
				stream.close();
			}catch (final Throwable throwable) {
			}
		}
		
		return sb.toString();
	}
	
	public static void load(final Reader stream, final Hashtable table) {
		final LineInputStream line = new LineInputStream(stream);
		final char[] Buf = new char[1024];
		int limit;
		int keyLen;
		int start;
		char c;
		boolean hasSep;
		boolean backslash;
	
		try {
			while ((limit = line.readLine()) >= 0) {
				c = 0;
				keyLen = 0;
				start = limit;
				hasSep = false;
	
				backslash = false;
				while (keyLen < limit) {
					c = line.lineBuf[keyLen];
					// need check if escaped.
					if ((c == '=' || c == ':') && !backslash) {
						start = keyLen + 1;
						hasSep = true;
						break;
					} else if ((c == ' ' || c == '\t' || c == '\f')
							&& !backslash) {
						start = keyLen + 1;
						break;
					}
					if (c == '\\') {
						backslash = !backslash;
					} else {
						backslash = false;
					}
					keyLen++;
				}
				while (start < limit) {
					c = line.lineBuf[start];
					if (c != ' ' && c != '\t' && c != '\f') {
						if (!hasSep && (c == '=' || c == ':')) {
							hasSep = true;
						} else {
							break;
						}
					}
					start++;
				}
				final String key = StringUtil.loadConvert(line.lineBuf, 0, keyLen, Buf);
				final String value = StringUtil.loadConvert(line.lineBuf, start, limit - start, Buf);
				
				table.put(key, value);
			}
		} catch (final Exception e) {
	
		}finally{
			try {
				stream.close();
			} catch (final Exception e) {
			}
		}
	}

	private static String loadConvert(final char[] in, int off, final int len,
			char[] convtBuf) {
		if (convtBuf.length < len) {
			final int newLen = len * 2;
			convtBuf = new char[newLen];
		}
		char aChar;
		final char[] out = convtBuf;
		int outLen = 0;
		final int end = off + len;
	
		while (off < end) {
			aChar = in[off++];
			if (aChar == '\\') {
				aChar = in[off++];
				if (aChar == 'u') {
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = in[off++];
						switch (aChar) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							value = (value << 4) + aChar - '0';
							break;
						case 'a':
						case 'b':
						case 'c':
						case 'd':
						case 'e':
						case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A':
						case 'B':
						case 'C':
						case 'D':
						case 'E':
						case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
							throw new IllegalArgumentException("Error encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar == 't')
						aChar = '\t';
					else if (aChar == 'f')
						aChar = '\f';
					else if (aChar == 'r')
						aChar = '\r';
					else if (aChar == 'n')
						aChar = '\n';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	public static final String formatJS(final String js){
		return js.replace('"', '\'');
	}

	/**
	 * 将三段的zh-Hans-CN转为两段zh-CN
	 * @param userLang
	 * @return
	 */
	public static String toStandardLocale(final String userLang){
		final int firstIdx = userLang.indexOf('-');
		if(firstIdx > 0){
			final int secondIdx = userLang.indexOf('-', firstIdx + 1);
			if(secondIdx > 0){
				return userLang.substring(0, firstIdx) + userLang.substring(secondIdx, userLang.length());
			}
		}
		return userLang;
	}

	/**
	 * 注意：长度不固定
	 * @return
	 */
	public static String genUID(final long random){
		byte[] bs = new byte[6];
		CCoreUtil.generateRandomKey(random, bs, 0, bs.length);
		
		final Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		StringBuffer sb = new StringBuffer();
		sb.append(c.get(Calendar.YEAR));
		sb.append(c.get(Calendar.MONTH) + 1);
		sb.append(c.get(Calendar.DAY_OF_MONTH));
		sb.append(c.get(Calendar.HOUR_OF_DAY));
		sb.append(c.get(Calendar.MINUTE));
		sb.append(c.get(Calendar.SECOND));
		sb.append(c.get(Calendar.MILLISECOND));
		
		sb.append(ByteUtil.encodeBase64(bs));
		String out = replace(sb.toString(), "=", "");
		out = replace(out, "+", "");
		out = replace(out, "/", "");
		return out;
	}
}
