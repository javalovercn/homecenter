package hc.server.msb;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.SerialUtil;
import hc.core.util.WiFiDeviceManager;
import hc.core.util.io.StreamBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WiFiManagerRemoteWrapper extends WiFiDeviceManager{

	@Override
	public boolean isWiFiConnected() {
		return false;
	}

	@Override
	public void startWiFiAP(final String ssid, final String pwd, final String securityOption) {
		final String[] paras = {HCURL.DATA_PARA_WIFI_MANAGER, WiFiDeviceManager.DATA_PARA_WIFI_P0, WiFiDeviceManager.DATA_PARA_WIFI_P1, WiFiDeviceManager.DATA_PARA_WIFI_P2};
		final String[] values = {WiFiDeviceManager.START_WI_FI_AP, ssid, pwd, securityOption};
		HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, paras, values);
	}

	@Override
	public void broadcastWiFiAccountAsSSID(final String[] commands,
			final String cmdGroup) {
		final String[] paras = {HCURL.DATA_PARA_WIFI_MANAGER, WiFiDeviceManager.DATA_PARA_WIFI_P0, WiFiDeviceManager.DATA_PARA_WIFI_P1};
		final String[] values = {WiFiDeviceManager.BROADCAST_WI_FI_ACCOUNT_AS_SSID, SerialUtil.serial(commands), cmdGroup};
		HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, paras, values);
	}

	@Override
	public void clearWiFiAccountGroup(final String cmdGroup) {
		final String[] paras = {HCURL.DATA_PARA_WIFI_MANAGER, WiFiDeviceManager.DATA_PARA_WIFI_P0};
		final String[] values = {WiFiDeviceManager.CLEAR_WI_FI_ACCOUNT_GROUP, cmdGroup};
		HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, paras, values);
	}

	@Override
	public OutputStream createWiFiMulticastStream(final String multicastIP, final int port) {
		final byte[] parameter = ByteUtil.getBytes(multicastIP + "&" + port, IConstant.UTF_8);
		return StreamBuilder.buildOutputStream(WiFiDeviceManager.S_WiFiDeviceManager_createWiFiMulticastStream, parameter, 0, parameter.length, true);
	}

	@Override
	public boolean hasWiFiModule() {
		return false;
	}

	@Override
	public boolean canCreateWiFiAccount() {
		return false;
	}

	@Override
	public String[] getSSIDListOnAir() {
		byte[] parameter = ByteUtil.byteArrayCacher.getFree(1024);
		final InputStream is = StreamBuilder.buildInputStream(
				WiFiDeviceManager.S_WiFiDeviceManager_getSSIDListOnAir, parameter, 0, 0, true);
		int storeIdx = 0;
		try{
			while(true){
				final int n = is.read();
				if(n == -1){
					break;
				}else{
					parameter[storeIdx++] = (byte)n;
					final int lastLen = parameter.length;
					if(storeIdx == lastLen){
						final byte[] nextPara = ByteUtil.byteArrayCacher.getFree(lastLen * 2);
						System.arraycopy(parameter, 0, nextPara, 0, lastLen);
						ByteUtil.byteArrayCacher.cycle(parameter);
						parameter = nextPara;
					}
				}
			}
		}catch (final Exception e) {
		}finally{
			try {
				is.close();
			} catch (final IOException e) {
			}
		}
		if(storeIdx == 0){
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("None getSSIDListOnAir");
			}
			ByteUtil.byteArrayCacher.cycle(parameter);
			return new String[0];
		}else{
			final String buildString = ByteUtil.buildString(parameter, 0, storeIdx, IConstant.UTF_8);
			ByteUtil.byteArrayCacher.cycle(parameter);
			return SerialUtil.unserialToStringArray(buildString);
		}
	}

	@Override
	public InputStream listenFromWiFiMulticast(final String multicastIP, final int port) {
		final byte[] parameter = ByteUtil.getBytes(multicastIP + "&" + port, IConstant.UTF_8);
		return StreamBuilder.buildInputStream(WiFiDeviceManager.S_WiFiDeviceManager_listenFromWiFiMulticast, parameter, 0, parameter.length, true);
	}

	@Override
	public String[] getWiFiAccount() {
		return new String[0];
	}

}
