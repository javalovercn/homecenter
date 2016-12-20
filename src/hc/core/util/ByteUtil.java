package hc.core.util;


import hc.core.IConstant;
import hc.core.MsgBuilder;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class ByteUtil {
	public static final ByteArrayCacher byteArrayCacher = new ByteArrayCacher(2048, MsgBuilder.MAX_BYTE_ARRAY_LEN, 2);
	
	public static final String toString(final byte[] bs, final int offset, final int len){
		try {
			return new String(bs, offset, len, IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			return new String(bs, offset, len);
		}
	}
	
	public static boolean isSame(final byte[] src1, final int offset1, final int len1, final byte[] src2, final int offset2, final int len2){
		if(len2 == len1){
			for (int i = offset1; i < len1; i++) {
				if(src1[i] != src2[offset2 + i]){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public static final String buildString(final byte[] bs, final int offset, final int len, final String charset){
		try{
			return new String(bs, offset, len, charset);
		}catch (final Exception e) {
			return new String(bs, offset, len);
		}
	}
	
	public static final byte[] getBytes(final String str, final String charset){
		try {
			return str.getBytes(charset);
		} catch (final UnsupportedEncodingException e) {
			return str.getBytes();
		} 
	}
	
//	static public String urlParaEncode(String para) {
//		StringBuffer urlOK = new StringBuffer();
//		for (int i = 0; i < para.length(); i++) {
//			char ch = para.charAt(i);
//			switch (ch) {
//			case '<':
//				urlOK.append("%3C");
//				break;
//			case '>':
//				urlOK.append("%3E");
//				break;
//			case '/':
//				urlOK.append("%2F");
//				break;
//			case ' ':
//				urlOK.append("%20");
//				break;
//			case ':':
//				urlOK.append("%3A");
//				break;
//			case '-':
//				urlOK.append("%2D");
//				break;
//			case '=':
//				urlOK.append("%3D");
//				break;
//			case '?':
//				urlOK.append("%3F");
//				break;
//			case '&':
//				urlOK.append("%26");
//				break;
//			case '%':
//				urlOK.append("%25");
//				break;
//			case '@':
//				urlOK.append("%40");
//				break;
//			default:
//				urlOK.append(ch);
//				break;
//			}
//		}
//		return urlOK.toString();
//	}
	
//	public static String urlParaEncode(String para) {
//		StringBuffer urlOK = new StringBuffer();
//		for (int i = 0; i < para.length(); i++) {
//			char ch = para.charAt(i);
//			switch (ch) {
//			case '<':
//			case '>':
//			case '/':
//			case ' ':
//			case ':':
//			case '-':
//			case '=':
//			case '?':
//			case '&':
//			case '%':
//			case '@':
//			case '$':
//				urlOK.append('%');
//				urlOK.append(Integer.toHexString(ch));
//				break;
//			default:
//				urlOK.append(ch);
//				break;
//			}
//		}
//		return urlOK.toString();
//	}
//	
//	static public String urlParaDecode(String sUrl) {
//		StringBuffer urlOK = new StringBuffer();
//		char ch, a, b;
//		for (int i = 0; i < sUrl.length(); ) {
//			ch = sUrl.charAt(i);
//			if(ch == '%'){
//				char c = (char)Integer.valueOf(String.valueOf(sUrl.charAt(i + 1)) + String.valueOf(sUrl.charAt(i + 2)), 16).intValue();
//				urlOK.append(c);
//				i += 3;
//			}else{
//				urlOK.append(ch);
//				i++;
//			}
//		}
//		return urlOK.toString();
//	}
	
	public static int convertToSize(final byte[] data, final int len, final byte[] out) {
		int size = 0;

		int i = 0;
		int b1, b2, b3, b4;

		while (i < len) {

			/* b1 */
			do {
				b1 = ByteUtil.base64DecodeChars[data[i++]];
			} while (i < len && b1 == -1);
			if (b1 == -1) {
				break;
			}

			/* b2 */
			do {
				b2 = ByteUtil.base64DecodeChars[data[i++]];
			} while (i < len && b2 == -1);
			if (b2 == -1) {
				break;
			}
			out[size++] = (byte) (((b1 << 2) | ((b2 & 0x30) >>> 4)));

			/* b3 */
			do {
				b3 = data[i++];
				if (b3 == 61) {
					return size;
				}
				b3 = ByteUtil.base64DecodeChars[b3];
			} while (i < len && b3 == -1);
			if (b3 == -1) {
				break;
			}
			out[size++] = (byte) ((((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));

			/* b4 */
			do {
				b4 = data[i++];
				if (b4 == 61) {
					return size;
				}
				b4 = ByteUtil.base64DecodeChars[b4];
			} while (i < len && b4 == -1);
			if (b4 == -1) {
				break;
			}
			out[size++] = (byte) ((((b3 & 0x03) << 6) | b4));
		}
		return size;
	}

	
	public static final long TWO_15 = 32767;
	public static final long TWO_31 = 2147483647L;
	public static final long TWO_63 = 9223372036854775807L;
	
	public static String toHexEnableZeroBegin(final byte v){
		final String hexStringB = Integer.toHexString(v&0xff);
		return ((hexStringB.length()%2==0)?hexStringB:("0"+hexStringB));
	}

	/**
	 * 与toBytesFromHexStr相反
	 * @param bs
	 * @param offset
	 * @param len
	 * @return
	 */
	public static String toHex(final byte[] bs, final int offset, final int len){
		final StringBuffer sb = StringBufferCacher.getFree();
		for (int i = 0; i < len; i++) {
			sb.append(toHexEnableZeroBegin(bs[i + offset]));
		}
		final String out = sb.toString();
		StringBufferCacher.cycle(sb);
		return out;
	}
	
	/**
	 * 与toBytesFromHexStr相反
	 * @param bs
	 * @return
	 * @see #toBytesFromHexStr(String)
	 */
	public static String toHex(final byte[] bs){
		return toHex(bs, 0, bs.length);
	}
	
	public static byte toByteFromHexStr(final String hex){
		return (byte)Integer.parseInt(hex, 16);
	}
	
	/**
	 * 与toHex相反
	 * @param hex
	 * @return
	 * @see #toHex(byte[])
	 */
	public static byte[] toBytesFromHexStr(final String hex){
		final int length = hex.length();
		final byte[] out = new byte[length / 2];
		for (int i = 0; i < length;) {
			final int endIndex = i + 2;
			out[i / 2] = (byte)Integer.parseInt(hex.substring(i, endIndex), 16);
			i = endIndex;
		}
		return out;
	}

	public static void stringToBytes(final String s, int fixLen, final byte[] target, final int offset){
		final int s_len = s.length();
		if(s_len < fixLen){
			fixLen = s_len;
		}
		try {
			System.arraycopy(s.getBytes(IConstant.UTF_8), 0, target, 0, fixLen);
		} catch (final UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	public static String bytesToStr(final byte[] bytes, final int offset, final int len){
		try {
			return new String(bytes, offset, len, IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
	
	public static byte integerToOneByte(final int value) {
		if ((value > TWO_15) || (value < 0)) {
			return 0;
		}
		return (byte) (value & 0xFF);
	}

	public static void integerToOneByte(final int value, final byte[] bs, final int store_index) {
		if ((value > TWO_15) || (value < 0)) {
			return ;
		}
		bs[store_index] = (byte) (value & 0xFF);
	}

	public static byte[] integerToTwoBytes(final int value) {
		final byte[] result = new byte[2];
		if ((value > TWO_31) || (value < 0)) {
			return result;
		}
		result[0] = (byte) ((value >>> 8) & 0xFF);
		result[1] = (byte) (value & 0xFF);
		return result;
	}

	public static void integerToTwoBytes(final int value, final byte[] bs, int store_index) {
		if ((value > TWO_31) || (value < 0)) {
			return ;
		}
		bs[store_index++] = (byte) ((value >>> 8) & 0xFF);
		bs[store_index] = (byte) (value & 0xFF);
	}

	public static byte[] integerToFourBytes(final int value) {
		final byte[] result = new byte[4];
		if (value < 0) {
			return result;
		}
		result[0] = (byte) ((value >>> 24) & 0xFF);
		result[1] = (byte) ((value >>> 16) & 0xFF);
		result[2] = (byte) ((value >>> 8) & 0xFF);
		result[3] = (byte) (value & 0xFF);
		return result;
	}

	public static void integerToFourBytes(final int value, final byte[] bs, int store_index) {
		if (value < 0) {
			return ;
		}
		bs[store_index++] = (byte) ((value >>> 24) & 0xFF);
		bs[store_index++] = (byte) ((value >>> 16) & 0xFF);
		bs[store_index++] = (byte) ((value >>> 8) & 0xFF);
		bs[store_index] = (byte) (value & 0xFF);
	}

	public static int oneByteToInteger(final byte value) {
		return value & 0xFF;
	}

	public static int oneByteToInteger(final byte[] value, final int index) {
		return value[index] & 0xFF;
	}
	
	public static int twoBytesToInteger(final byte[] value) {
		if (value.length < 2) {
			return 0;
		}
		final int temp0 = value[0] & 0xFF;
		final int temp1 = value[1] & 0xFF;
		return ((temp0 << 8) + temp1);
	}

	public static int threeBytesToInteger(final byte[] value, final int index) {
		final int temp1 = value[index] & 0xFF;
		final int temp2 = value[index + 1] & 0xFF;
		final int temp3 = value[index + 2] & 0xFF;
		return ((temp1 << 16) + (temp2 << 8) + temp3);
	}

	public static void integerToThreeBytes(final int value, final byte[] bs, int store_index) {
		bs[store_index++] = (byte) ((value >>> 16) & 0xFF);
		bs[store_index++] = (byte) ((value >>> 8) & 0xFF);
		bs[store_index] = (byte) (value & 0xFF);
	}

	public static long fourBytesToLong(final byte[] value) {
		if (value.length < 4) {
			return 0;
		}
		final int temp0 = value[0] & 0xFF;
		final int temp1 = value[1] & 0xFF;
		final int temp2 = value[2] & 0xFF;
		final int temp3 = value[3] & 0xFF;
		return (((long) temp0 << 24) + (temp1 << 16) + (temp2 << 8) + temp3);
	}

	public static int twoBytesToInteger(final byte[] value, int index) {
		if (value.length < 2) {
			return 0;
		}
		final int temp0 = value[index++] & 0xFF;
		final int temp1 = value[index] & 0xFF;
		return ((temp0 << 8) + temp1);
	}

	public static long fourBytesToLong(final byte[] value, int index) {
		if (value.length < 4) {
			return 0;
		}
		final int temp0 = value[index++] & 0xFF;
		final int temp1 = value[index++] & 0xFF;
		final int temp2 = value[index++] & 0xFF;
		final int temp3 = value[index++] & 0xFF;
		return (((long) temp0 << 24) + (temp1 << 16) + (temp2 << 8) + temp3);
	}

	public static String encodeURI(final String str, final String charset) {
		final StringBuffer sb = StringBufferCacher.getFree();
		
		try{
			final String isoStr = new String(str.getBytes(charset), "ISO-8859-1");		
			final char[] chars = isoStr.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				final char c = chars[i];
				if ((c <= 'z' && c >= 'a') || (c <= 'Z' && c >= 'A') || (c <= '9' && c >= '0') || c == '%' 
						|| c == '-' || c == '/' || c == '.' || c == '&' || c == '#'
						|| c == ':' || c == '_' || c == '?' || c == '=' || c == '!'
						|| c == '~' || c == '*' || c == '\''
						|| c == '(' || c == ')' || c == ';'
						|| c == '@' || c == '+' || c == '$' || c == ',') {
					sb.append(c);
				} else {
					sb.append('%');
					sb.append(Integer.toHexString(c).toUpperCase());
				}
			}
			return sb.toString();
		}catch (final Exception e) {
			return null;
		}finally{
			StringBufferCacher.cycle(sb);
		}
	}

	/**
	 * @param data
	 * @param index
	 * @param len
	 * @param chars
	 * @param char_idx
	 * @return base64串的长度
	 */
	public static int encodeBase64(final byte[] data, int index, final int len, final char[] chars, final int char_idx) {    
		int char_step_idx = char_idx;
		final int endIdx = index + len;
		
	    while (index < endIdx) {    
	        final int b1 = data[index++] & 0xff;
	        if (index == endIdx) {
	            chars[char_step_idx++] = (ByteUtil.base64EncodeChars[b1 >>> 2]);
	            chars[char_step_idx++] = (ByteUtil.base64EncodeChars[(b1 & 0x3) << 4]);
	            chars[char_step_idx++] = '=';
	            chars[char_step_idx++] = '=';
	            break;    
	        }    
	        final int b2 = data[index++] & 0xff;    
	        if (index == endIdx) {
	            chars[char_step_idx++] = (ByteUtil.base64EncodeChars[b1 >>> 2]);
	            chars[char_step_idx++] = (ByteUtil.base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
	            chars[char_step_idx++] = (ByteUtil.base64EncodeChars[(b2 & 0x0f) << 2]);    
	            chars[char_step_idx++] = '=';
	            break;
	        }    
	        final int b3 = data[index++] & 0xff;    
	        chars[char_step_idx++] = (ByteUtil.base64EncodeChars[b1 >>> 2]);    
	        chars[char_step_idx++] = (ByteUtil.base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);    
	        chars[char_step_idx++] = (ByteUtil.base64EncodeChars[((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)]);    
	        chars[char_step_idx++] = (ByteUtil.base64EncodeChars[b3 & 0x3f]);    
	    }
	    
	    return char_step_idx - char_idx;
	}
	
	public static String encodeBase64(final byte[] data, final int index, final int len) {  
		final char[] chars = new char[getBase64Len(len)];
		encodeBase64(data, index, len, chars, 0);
		return new String(chars);
	}

	public static int getBase64Len(final int len) {
		return (len / 3 + ((len % 3)>0?1:0)) * 4;
	}
	
	public static String encodeBase64(final byte[] data) {
		return encodeBase64(data, 0, data.length);
	}
	
	public static int charToByte(final char[] chars, final int index_chars, final int len, final byte[] bytes, final int index_bytes){
		final String strBase64 = String.valueOf(chars, index_chars, len);
		final byte[] base64bs = strBase64.getBytes();
		System.arraycopy(base64bs, 0, bytes, index_bytes, base64bs.length);
		return base64bs.length;
	}

	public static final char[] base64EncodeChars = new char[] {    
	'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',    
	'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',    
	'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',    
	'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',    
	'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',    
	'o', 'p', 'q', 'r', 's', 't', 'u', 'v',    
	'w', 'x', 'y', 'z', '0', '1', '2', '3',    
	'4', '5', '6', '7', '8', '9', '+', '/' };
	public static final byte[] base64DecodeChars = new byte[] {    
	-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,    
	-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,    
	-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,    
	52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,    
	-1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,    
	15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,    
	-1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,    
	41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };

	public static byte[] decodeBase64(final String str) {    
	    byte[] data;
		try {
			data = str.getBytes(IConstant.UTF_8);
		    final int len = data.length;    
		    final ByteArrayOutputStream buf = new ByteArrayOutputStream(len);    
		    int i = 0;    
		    int b1, b2, b3, b4;    
		
		    while (i < len) {    
		
		        /* b1 */    
		        do {    
		            b1 = base64DecodeChars[data[i++]];    
		        } while (i < len && b1 == -1);    
		        if (b1 == -1) {    
		            break;    
		        }    
		
		        /* b2 */    
		        do {    
		            b2 = base64DecodeChars[data[i++]];    
		        } while (i < len && b2 == -1);    
		        if (b2 == -1) {    
		            break;    
		        }    
		        buf.write(((b1 << 2) | ((b2 & 0x30) >>> 4)));    
		
		        /* b3 */    
		        do {    
		            b3 = data[i++];    
		            if (b3 == 61) {    
		                return buf.toByteArray();    
		            }    
		            b3 = base64DecodeChars[b3];    
		        } while (i < len && b3 == -1);    
		        if (b3 == -1) {    
		            break;    
		        }    
		        buf.write((((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));    
		
		        /* b4 */    
		        do {    
		            b4 = data[i++];    
		            if (b4 == 61) {    
		                return buf.toByteArray();    
		            }    
		            b4 = base64DecodeChars[b4];    
		        } while (i < len && b4 == -1);    
		        if (b4 == -1) {    
		            break;    
		        }    
		        buf.write((((b3 & 0x03) << 6) | b4));    
		    }    
		    return buf.toByteArray();    
		} catch (final UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}    
		return null;
	}

	public static void encodeFileXOR(final byte[] bs, final int offset, final int len, final byte[] coderbs, final int offsetCoder, final int lenCoder) {
			{//重置code数组。
				final int endIdx = offsetCoder + lenCoder;
				for (int i = offsetCoder; i < endIdx; i++) {
	//				System.out.println("--------coderBs index : " + i);
					coderbs[i] = 0;
				}
			}
			
			byte total = 0;
			{
				final int endCoder = offsetCoder + lenCoder;
				final int endIdx = offset + len;
				for (int i = offset, j = offsetCoder; i < endIdx; i++, j++) {
	//				System.out.println("--------srcBs index : " + i);
					final byte oneByte = bs[i];
					total += oneByte;
					if(j == endCoder){
						j = offsetCoder;
					}
	//				System.out.println("--------coderBs index : " + j);
					coderbs[j] ^= oneByte;
				}
			}
			
			final int halfLenCoder = lenCoder / 2;
			{
				final int endIdx = offsetCoder + halfLenCoder;
				for (int i = offsetCoder; i < endIdx; i++) {
	//				System.out.println("--------coderBs index : " + i);
					coderbs[i] ^= total;
				}
			}
			
			{
				final int endIdx = offsetCoder + lenCoder;
				for (int i = offsetCoder + halfLenCoder; i < endIdx; i++) {
	//				System.out.println("--------coderBs index : " + i);
					coderbs[i] ^= lenCoder;
				}
			}
		}
	
}
