package hc.server.msb;

import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.SerialUtil;
import hc.core.util.WiFiDeviceManager;
import hc.core.util.io.HCInputStream;
import hc.core.util.io.StreamBuilder;
import hc.core.util.io.StreamReader;

import java.io.InputStream;
import java.io.OutputStream;

public class WiFiManagerRemoteWrapper extends WiFiDeviceManager {
	final CoreSession coreSS;

	public WiFiManagerRemoteWrapper(final CoreSession coreSS) {
		this.coreSS = coreSS;
	}

	@Override
	public boolean isWiFiConnected() {
		return false;
	}

	@Override
	public void startWiFiAP(final String ssid, final String pwd, final String securityOption) {
		final String[] paras = { HCURL.DATA_PARA_WIFI_MANAGER, WiFiDeviceManager.DATA_PARA_WIFI_P0,
				WiFiDeviceManager.DATA_PARA_WIFI_P1, WiFiDeviceManager.DATA_PARA_WIFI_P2 };
		final String[] values = { WiFiDeviceManager.START_WI_FI_AP, ssid, pwd, securityOption };
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, paras, values);
	}

	@Override
	public void broadcastWiFiAccountAsSSID(final String[] commands, final String cmdGroup) {
		final String[] paras = { HCURL.DATA_PARA_WIFI_MANAGER, WiFiDeviceManager.DATA_PARA_WIFI_P0,
				WiFiDeviceManager.DATA_PARA_WIFI_P1 };
		final String[] values = { WiFiDeviceManager.BROADCAST_WI_FI_ACCOUNT_AS_SSID,
				SerialUtil.serial(commands), cmdGroup };
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, paras, values);
	}

	@Override
	public void clearWiFiAccountGroup(final String cmdGroup) {
		final String[] paras = { HCURL.DATA_PARA_WIFI_MANAGER,
				WiFiDeviceManager.DATA_PARA_WIFI_P0 };
		final String[] values = { WiFiDeviceManager.CLEAR_WI_FI_ACCOUNT_GROUP, cmdGroup };
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, paras, values);
	}

	@Override
	public OutputStream createWiFiMulticastStream(final String multicastIP, final int port) {
		final byte[] parameter = ByteUtil.getBytes(multicastIP + "&" + port, IConstant.UTF_8);
		return coreSS.streamBuilder.buildOutputStream(
				StreamBuilder.S_WiFiDeviceManager_createWiFiMulticastStream, parameter, 0,
				parameter.length, true);
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
		final HCInputStream is = coreSS.streamBuilder.buildInputStream(
				StreamBuilder.S_WiFiDeviceManager_getSSIDListOnAir, null, 0, 0, true);
		final StreamReader sr = new StreamReader(ByteUtil.byteArrayCacher.getFree(1024), is);
		sr.readFull();
		if (sr.storeIdx == 0) {
			if (L.isInWorkshop) {
				LogManager.log("None getSSIDListOnAir");
			}
			sr.recycle();
			return new String[0];
		} else {
			final String buildString = ByteUtil.buildString(sr.bs, 0, sr.storeIdx, IConstant.UTF_8);
			sr.recycle();
			return SerialUtil.unserialToStringArray(buildString);
		}
	}

	@Override
	public InputStream listenFromWiFiMulticast(final String multicastIP, final int port) {
		final byte[] parameter = ByteUtil.getBytes(multicastIP + "&" + port, IConstant.UTF_8);
		return coreSS.streamBuilder.buildInputStream(
				StreamBuilder.S_WiFiDeviceManager_listenFromWiFiMulticast, parameter, 0,
				parameter.length, true);
	}

	@Override
	public String[] getWiFiAccount() {
		return new String[0];
	}

}
