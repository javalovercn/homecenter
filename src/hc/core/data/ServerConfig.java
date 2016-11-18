package hc.core.data;

import hc.core.CoreSession;
import hc.core.HCConfig;

public class ServerConfig extends HCConfig{
	
	//注意：以下每项依赖于HCConfig，不能删除或变动索引结构
	public static final short p_HC_VERSION = 0;
	public static final short p_MIN_MOBI_VER_REQUIRED_BY_PC = 1;//服务器要求手机端最低版本
	public static final short P_SERVER_COLOR_ON_RELAY = 2;
	public static final short P_SERVER_WIDTH = 3;
	public static final short P_SERVER_HEIGHT = 4;
	
	public static void setInstance(final CoreSession coreSS, final ServerConfig sc){
		coreSS.j2meServerConfig = sc;
	}
	
	public static ServerConfig getInstance(final CoreSession coreSS){
		return coreSS.j2meServerConfig;
	}
	
	public ServerConfig(final String str){
		super(str);
	}
	
	/**
	 * 存储0表示，要进行MTU最优发现
	 */
//	private int MTU;
//	
//	private int tryMaxMTU;
//	
//	private boolean isForceUpdateStun;
//	
//	private String serverToken;

//	public String getServerTokenXXX() {
//		return serverToken;
//	}

//	public void setServerTokenXXX(String serverToken) {
//		this.serverToken = serverToken;
//	}

//	public void readFrom(DataServerConfig dsc){
//		MTU = dsc.getMTU();
//		tryMaxMTU = dsc.getMaxMTU();
//		isForceUpdateStun = dsc.isForceUpdateStun();
//		serverToken = dsc.getTokenXXX();
//	}
//	
//	public void writeTo(DataServerConfig dsc){
//		dsc.setMTU(MTU);
//		dsc.setMaxMTU(tryMaxMTU);
//		dsc.setForceUpdateStun(isForceUpdateStun);
//		dsc.setTokenXXX(serverToken);
//	}

//	public int getMTU() {
//		return MTU;
//	}
//
//	public void setMTU(int mtu) {
//		MTU = mtu;
//	}
//
//	public int getTryMaxMTU() {
//		return tryMaxMTU;
//	}
//
//	public void setTryMaxMTU(int maxMTU) {
//		this.tryMaxMTU = maxMTU;
//	}
//
//	public boolean isForceUpdateStunXXXX() {
//		return isForceUpdateStun;
//	}
//
//	public void setForceUpdateStunXXXX(boolean isForceUpdateStun) {
//		this.isForceUpdateStun = isForceUpdateStun;
//	}
}
