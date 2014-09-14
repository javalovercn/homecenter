package hc.core.util;

import hc.core.IConstant;

import java.util.Vector;

public class StringUtil {
	public static String replace(String src, String find, String replaceTo){
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
	
	public static byte[] getBytes(final String str){
		try{
			return str.getBytes(IConstant.UTF_8);
		}catch (Exception e) {
			return str.getBytes();
		}
	}

	/**
	 * {IP, port, [1:NATType;2:代理上线（即取代1，则不是HomeCenter.mobi来实现双方IP交换）], upnpip, upnpport, relayIP, relayPort}
	 * @param msg
	 * @return
	 */
	public static String[] extractIPAndPort(String msg) {
		String[] out = StringUtil.splitToArray(msg, ";");
		return out;
	}

	public static String[] splitToArray(String src, String split){
	    int split_length = split.length();
	
	    String[] out = new String[StringUtil.splitCount(src, split)];
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

	private static int splitCount(String src, String split){
		int split_length = split.length();
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
	 * 如果ver1(1.2.11)高于ver2(1.2.1.3)，则返回true;
	 * @param ver1
	 * @param ver2
	 * @return
	 */
	public static boolean higer(String ver1, String ver2){
		int s1_index = ver1.indexOf(".", 0);
		
		int s2_index = ver2.indexOf(".", 0);
		
		int ss1, ss2;
		if(s1_index > 0){
			ss1 = Integer.parseInt(ver1.substring(0, s1_index));
	
			if(s2_index < 0){
				ss2 = Integer.parseInt(ver2);
				
				if( ss1 == ss2 ){
					return true;
				}else {
					return ss1 > ss2;
				}
			}else{
				ss2 = Integer.parseInt(ver2.substring(0, s2_index));
				
				if(ss1 == ss2){
					return higer(ver1.substring(s1_index + 1), ver2.substring(s2_index + 1));
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

	public static Vector split(String msg, String split) {
		Vector v = new Vector(8);
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
}
