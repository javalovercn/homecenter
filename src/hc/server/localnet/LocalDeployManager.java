package hc.server.localnet;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.Designer;
import hc.server.util.DownlistButton;
import hc.server.util.ListAction;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Vector;

public class LocalDeployManager {

	public static String getRecentPasswordForIP() {
		return PropertiesManager.getValue(PropertiesManager.p_Deploy_RecentPassword);
	}

	public static void saveRecentPasswordForIP(final String password) {
		PropertiesManager.setValue(PropertiesManager.p_Deploy_RecentPassword, password);
		PropertiesManager.saveFile();
	}

	public static void refreshAliveServerFromLocalNetwork(final DownlistButton actionButton,
			final String projectID, final Designer designer) {
		// do network search
		final InetAddress ia = HttpUtil.getLocal();
		if (ia == null || ia instanceof Inet6Address) {
			actionButton.removeDownArrow();
			return;
		}

		final String ip = ia.getHostAddress();
		if (HttpUtil.isLocalNetworkIP(ip) == false) {
			actionButton.removeDownArrow();
			return;
		}

		final Vector<ListAction> newAlive = new Vector<ListAction>();
		final String preIP = ip.substring(0, ip.lastIndexOf('.') + 1);

		final String recentIP = PropertiesManager.getValue(PropertiesManager.p_Deploy_RecentIP);

		int startIP = 1;
		// 2 - 254
		for (; startIP < 255; startIP++) {
			if (designer.isDisposed()) {
				return;
			}

			final String testIP;
			if (startIP == 1) {
				if (recentIP == null) {
					continue;
				} else {
					testIP = recentIP;
				}
			} else {
				testIP = (preIP + startIP);
				if (testIP.equals(recentIP)) {
					continue;
				}
			}
			if (ip.equals(testIP)) {
				if (PropertiesManager.isSimu()) {// 模拟环境下，因为测试需要，所以必须加自己
				} else {
					continue;// 不加自己
				}
			}

			final ListAction item = new ListAction(testIP);
			if (DeploySender.isAlive(testIP, projectID)) {
				L.V = L.WShop ? false : LogManager.log("[Deploy] find a live server at " + testIP);
				actionButton.addListAction(item);
			} else {
				actionButton.removeListAction(item);
			}
		}
	}
}
