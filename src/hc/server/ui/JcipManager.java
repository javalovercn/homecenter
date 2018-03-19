package hc.server.ui;

import hc.core.util.ILog;
import hc.core.util.Jcip;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ScreenServer;
import hc.server.ui.design.J2SESession;
import hc.server.util.ai.AIObjectCache;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.AnalysableData;
import hc.server.util.ai.FormData;

public class JcipManager {

	public static void responseCtrlSubmit(final J2SESession coreSS, final String jcip_str) {
		LogManager.log(ILog.OP_STR + "mobile request:" + jcip_str);
		final Jcip jcip = new Jcip(coreSS, jcip_str);
		final String screenID = jcip.getString();

		final ICanvas icanvas = ScreenServer.getCurrScreen(coreSS);

		if (icanvas instanceof ServCtrlCanvas) {
			final ServCtrlCanvas ctrlCanvas = (ServCtrlCanvas) icanvas;
			if (ctrlCanvas.getScreenID().equals(screenID)) {
				onClick(coreSS, jcip, ctrlCanvas);
				return;
			}
		}

		final ServCtrlCanvas searchCtrlCanvas = ScreenServer.searchCtrlCanvas(coreSS, screenID);
		if (searchCtrlCanvas != null) {
			onClick(coreSS, jcip, searchCtrlCanvas);
			return;
		}

		LogManager.errToLog("fail to search CtrlResponse : " + screenID);
	}

	private static void onClick(final J2SESession coreSS, final Jcip jcip, final ServCtrlCanvas ctrlCanvas) {
		final CtrlResponse cr = ctrlCanvas.cr;
		final String keyValue = jcip.getString();

		final ProjectContext context = ServerUIAPIAgent.getProjectContextFromCtrl(cr);
		ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(context),
				new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final int key = Integer.parseInt(keyValue);
						cr.click(key);

						if (AIPersistentManager.isEnableAnalyseFlow && AIPersistentManager.isEnableHCAI()) {
							final FormData data = AIObjectCache.getFormData();
							data.uiType = FormData.UI_TYPE_CTRLRESP;
							data.uiObject = cr;
							data.ctrlClickKey = key;
							data.snap(context.getProjectID(), context.getClientLocale(), AnalysableData.DIRECT_IN);
							AIPersistentManager.processFormData(data);
						}
						return null;
					}
				});
	}

	public static void appendArray(final StringBuilder sb, final String[] strs, final boolean withDouhao) {
		sb.append('[');
		for (int i = 0; i < strs.length; i++) {
			if (i != 0) {
				sb.append(',');
			}
			final String tmp = strs[i];
			appendStringItem(sb, tmp);
		}
		sb.append(']');

		if (withDouhao) {
			sb.append(',');
		}
	}

	public static void appendStringItem(final StringBuilder sb, final String tmp) {
		sb.append('\'');
		if (tmp.indexOf('\'') >= 0) {
			sb.append(tmp.replace("'", "\\'"));
		} else {
			sb.append(tmp);
		}
		sb.append('\'');
	}
}
