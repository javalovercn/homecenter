package hc.server;

import hc.App;
import hc.core.IConstant;
import hc.core.util.CCoreUtil;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

public class LineFailManager {
	private static Window lineFailWindow;

	public static synchronized void showLineFailWindow(final J2SESession coreSS, final String msg) {
		if (lineFailWindow != null) {
			return;
		}

		final JPanel panel = App.buildMessagePanel(msg, App.getSysIcon(App.SYS_ERROR_ICON));
		lineFailWindow = App.showCenterPanelMain(panel, 0, 0, (String) ResourceUtil.get(IConstant.ERROR), false, null, null,
				new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						coreSS.context.notifyShutdown();
					}
				}, null, null, false, true, null, false, false);
	}

	public static synchronized void closeLineFailWindow() {
		if (lineFailWindow != null) {
			lineFailWindow.dispose();
			lineFailWindow = null;
		}
	}

}
