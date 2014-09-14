package hc.core.util;

import hc.core.IConstant;

import java.util.TimeZone;

public class Jcip {
	protected char[] chars;
	
	protected int index, charArrLen;
	protected char c;
	
	public Jcip(String xml) {
		ini(xml);
	}
	
	protected void ini(String xml) {
		index = 0;
		chars = xml.toCharArray();
        charArrLen = chars.length;
		c = chars[index];
	}
	
	public String getString(){
		if(index == charArrLen 
					|| c == ']'){//判断是否数组结束
				return null;
		}
		while(c != '\''){
			c = chars[index++];
			//尝试init时，会向后取内容，所以加下检查条件
			if(index == charArrLen 
					|| c == ']'){//判断是否数组结束
				return null;
			}
		}
		
		int start = index;
		
		boolean needchange = false;
        String temp = "";
		
		//走过前面的'
		c = chars[index++];
		
		while(! (c == '\'' && chars[index - 2] != '\\')){
			if(c == '\''){
				needchange = true;
                temp = temp + String.valueOf(chars, start, index-2 - start) + "'";
                start = index;
			}
			c = chars[index++];
		}
		
		int end = index-1;

        if(index == charArrLen){
            
        }else{
    		//走过后面的'
            c = chars[index++];
        }
        
		if(needchange){
			temp = temp + String.valueOf(chars, start, index-2 - start);
		}else{
			temp = String.valueOf(chars, start, end - start);
		}
		//hc.core.L.V=hc.core.L.O?false:LogManager.log("Jcip String item:" + temp);
		return temp;
	}


	public boolean getBool(){
		String data = getString();
		if(data != null && data.equals(IConstant.TRUE)){
			return true;
		}else{
			return false;
		}
	}

	public TimeZone getTimezone(){
		return TimeZone.getTimeZone(getString());
	}
	
	public int getInt(){
		return Integer.parseInt(getString());
	}
		
	public boolean[] convert(String[] data){
		boolean[] out = new boolean[data.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = data[i].equals(IConstant.TRUE);
		}
		return out;
	}

	public String[] getArrString(){
		//hc.core.L.V=hc.core.L.O?false:LogManager.log("Enter ArrString");
		final Stack v = new Stack(4);
		while(c != ']'){
			while(c != '\'' && c != ']'){
				c = chars[index++];
			}
			
			//数组内无成员的情形
			if(c == ']'){
				break;
			}
			
			//hc.core.L.V=hc.core.L.O?false:LogManager.log("StringArr : new String");
			Object obj = getString();
			v.push(obj);
			
			//尾部需要检查并退出。
			while(c != ']' && c != '\''){
				c = chars[index++];
			}
		}
		//hc.core.L.V=hc.core.L.O?false:LogManager.log("Exit ArrString");
		if(index == charArrLen){
			
		}else{
			//取下一位
			c = chars[index++];
		}
		
		String[] items = new String[v.size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = (String)v.elementAt(i);
		}
		return items;
	}
}
