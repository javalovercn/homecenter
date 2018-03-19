package hc.core.util;

import hc.core.CoreSession;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class WiFiDeviceManager {
	public static final WiFiDeviceManager getInstance(
			final CoreSession coreSS) {
		// 注意：由于服务器端可能手机连接或断开，所以WifiDeviceManager会变动，不能暂存
		return coreSS.context.getWiFiDeviceManager();
	}

	public static final String DATA_PARA_WIFI_P0 = "p0";
	public static final String DATA_PARA_WIFI_P1 = "p1";
	public static final String DATA_PARA_WIFI_P2 = "p2";

	public static final String START_WI_FI_AP = "startWiFiAP";
	public static final String BROADCAST_WI_FI_ACCOUNT_AS_SSID = "broadcastWiFiAccountAsSSID";
	public static final String CLEAR_WI_FI_ACCOUNT_GROUP = "clearWiFiAccountGroup";

	/**
	 * ConfigManager.NET_TYPE_WIFI
	 * 
	 * @return
	 */
	public abstract boolean isWiFiConnected();

	/**
	 * 成功返回true
	 * 
	 * @param ssid
	 * @param pwd
	 * @param securityOption
	 * @return
	 */
	public abstract void startWiFiAP(String ssid, String pwd,
			String securityOption) throws Exception;

	public abstract void clearWiFiAccountGroup(String cmdGroup);

	public abstract void broadcastWiFiAccountAsSSID(final String[] commands,
			final String cmdGroup);

	public abstract OutputStream createWiFiMulticastStream(
			final String multicastIP, final int port);

	public abstract boolean hasWiFiModule();

	/**
	 * 服务器端可能存在有WiFi模块，但不能创建AP； 客户端可能存在有WiFi模块，但不能创建AP
	 * 
	 * @return
	 */
	public abstract boolean canCreateWiFiAccount();

	public abstract String[] getSSIDListOnAir();

	public abstract InputStream listenFromWiFiMulticast(
			final String multicastIP, final int port);

	/**
	 * 返回J2SE中WiFiAccount中的[UUID, Password,
	 * ConfigManager.WIFI_SECURITY_OPTION_NO_PASSWORD|WIFI_SECURITY_OPTION_WEP|WIFI_SECURITY_OPTION_WPA_WPA2_PSK]
	 * 
	 * @return
	 */
	public abstract String[] getWiFiAccount();
}
