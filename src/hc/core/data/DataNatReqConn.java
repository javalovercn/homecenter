package hc.core.data;

import java.io.UnsupportedEncodingException;

import hc.core.IConstant;
import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;
import hc.core.util.HCURLUtil;

public class DataNatReqConn extends HCData {
	private final int localIP_Len_index = MsgBuilder.INDEX_MSG_DATA;
	private final int localIP_index = localIP_Len_index + 2;
	private final int localIP_Port_index = localIP_index + 48;
	private final int localIP_Nattype_index = localIP_Port_index + 2;
	
	private final int remoteIP_Len_index = localIP_Nattype_index + 1;
	private final int remoteIP_index = remoteIP_Len_index + 2;
	private final int remoteIP_Port_index = remoteIP_index + 48;
	private final int remoteIP_Nattype_index = remoteIP_Port_index + 2;
	
	private final int indexTokenLen = remoteIP_Nattype_index + 1;
	private final int indexTokenValue = indexTokenLen + 2;//128
	
	//注意：结构变更后，请同步更新cloneTo方法
	
	public int getLength() {
		return (2 + 48 + 2 + 1) * 2 + 2 + 128;
	}
	
	public int getLocalNattype(){
		return ByteUtil.oneByteToInteger(bs, localIP_Nattype_index);
	}
	
	public int getRemoteNattype(){
		return ByteUtil.oneByteToInteger(bs, remoteIP_Nattype_index);
	}
		
	public int getLocalPort(){
		return ByteUtil.twoBytesToInteger(bs, localIP_Port_index);
	}
	
	public int getRemotePort(){
		return ByteUtil.twoBytesToInteger(bs, remoteIP_Port_index);
	}
	
	public void setLocalNattype(int type){
		ByteUtil.integerToOneByte(type, bs, localIP_Nattype_index);
	}
	
	public void setRemoteNattype(int type){
		ByteUtil.integerToOneByte(type, bs, remoteIP_Nattype_index);
	}
		
	public void setLocalPort(int port){
		ByteUtil.integerToTwoBytes(port, bs, localIP_Port_index);
	}
	
	public void setRemotePort(int port){
		ByteUtil.integerToTwoBytes(port, bs, remoteIP_Port_index);
	}
	
	public String getLocalIP(){
		int len = ByteUtil.twoBytesToInteger(bs, localIP_Len_index);
		return new String(bs, localIP_index, len);
	}
	
	public String getRemoteIP(){
		int len = ByteUtil.twoBytesToInteger(bs, remoteIP_Len_index);
		return new String(bs, remoteIP_index, len);
	}

	public void setRemoteIP(String ip){
		ip = HCURLUtil.convertIPv46(ip);
		byte[] ipbs = ip.getBytes();
		
		ByteUtil.integerToTwoBytes(ipbs.length, bs, remoteIP_Len_index);
		System.arraycopy(ipbs, 0, bs, remoteIP_index, ipbs.length);
	}
	
	public String getToken(){
		int len = ByteUtil.twoBytesToInteger(bs, indexTokenLen);
		try {
			return new String(bs, indexTokenValue, len, IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			return new String(bs, indexTokenValue, len);
		}
	}
	
	public void setToken(String token){
		byte[] tokenBS;
		try {
			tokenBS = token.getBytes(IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			tokenBS = token.getBytes();
		}
		ByteUtil.integerToTwoBytes(tokenBS.length, bs, indexTokenLen);
		System.arraycopy(tokenBS, 0, bs, indexTokenValue, tokenBS.length);
	}
	
	public void setLocalIP(String ip){
		if(ip == null){
			ip = "";
		}
		ip = HCURLUtil.convertIPv46(ip);
		byte[] ipbs = ip.getBytes();
		
		ByteUtil.integerToTwoBytes(ipbs.length, bs, localIP_Len_index);
		System.arraycopy(ipbs, 0, bs, localIP_index, ipbs.length);
	}
	
}
