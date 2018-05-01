package hc.server.localnet;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Vector;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.Designer;
import hc.server.util.DownlistButton;
import hc.server.util.ListAction;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;

public class LocalDeployManager {

	public static String getRecentPasswordForIP() {
		return PropertiesManager.getValue(PropertiesManager.p_Deploy_RecentPassword);
	}

	public static void saveRecentPasswordForIP(final String password) {
		PropertiesManager.setValue(PropertiesManager.p_Deploy_RecentPassword, password);
		PropertiesManager.saveFile();
	}

	public static void refreshAliveServerFromLocalNetwork(final InetAddress localIA, final DownlistButton actionButton, final Designer designer,
			int startIP, final int endStartIP) {
		// do network search
		if (localIA == null || localIA instanceof Inet6Address) {
			actionButton.removeDownArrow();
			return;
		}

		final String ip = localIA.getHostAddress();
		if (HttpUtil.isLocalNetworkIP(ip) == false) {
			actionButton.removeDownArrow();
			return;
		}

		final Vector<ListAction> newAlive = new Vector<ListAction>();
		final String preIP = ip.substring(0, ip.lastIndexOf('.') + 1);

		final String recentIP = PropertiesManager.getValue(PropertiesManager.p_Deploy_RecentIP);

		// 2 - 254
		for (; startIP < endStartIP; startIP++) {
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
			if (DeploySender.isAlive(testIP, designer)) {
				L.V = L.WShop ? false : LogManager.log("[Deploy] find a live server at " + testIP);
				actionButton.addListAction(item);
			} else {
				actionButton.removeListAction(item);
			}
		}
	}
}
