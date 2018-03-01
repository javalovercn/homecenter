package hc.server.util;

import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.util.CCoreUtil;
import hc.core.util.StringUtil;
import hc.server.PlatformManager;
import hc.server.TrayMenuUtil;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;

public class DiskManager {
	private static final long minSpace = 300;
	private static final long lowDisk = minSpace * 1024L * 1024;
	private static HCTimer monitor;

	public static boolean isLowDiskFreeSpace() {
		return PlatformManager.getService().getAvailableSize() < lowDisk;
	}

	public static void startDiskSpaceMonitor() {
		if (monitor != null) {
			CCoreUtil.checkAccess();
			monitor.setEnable(true);
			return;
		}

		monitor = new HCTimer("FreeSpaceDiskMonitor", HCTimer.ONE_MINUTE * 30, true) {
			@Override
			public void doBiz() {
				if (isLowDiskFreeSpace()) {
					try {
						TrayMenuUtil.displayMessage(ResourceUtil.getWarnI18N(), buildWarnMsg(null),
								IConstant.WARN, null, 0);
					} catch (final Throwable e) {
						e.printStackTrace();
					}

					// 发送到客户端
					final J2SESession[] coreSSS = J2SESessionManager.getAllOnlineSocketSessions();
					if (coreSSS != null && coreSSS.length > 0) {
						for (int i = 0; i < coreSSS.length; i++) {
							final J2SESession oneSession = coreSSS[i];
							sendWarnToSession(oneSession);
						}
					}
				}
			}
		};

		new HCTimer("delayMonitor", HCTimer.ONE_MINUTE * 1, true) {
			@Override
			public void doBiz() {
				remove();
				monitor.doNowAsynchronous();
			}
		};
	}

	public static void disableDiskSpaceMonitor() {
		if (monitor != null) {
			CCoreUtil.checkAccess();
			monitor.setEnable(false);
		}
	}

	private static String buildWarnMsg(final J2SESession coreSS) {
		final String msg = ResourceUtil.get(coreSS, 9274);// available storage
															// space of server
															// is not enough
															// {freeMin} M.
		return StringUtil.replace(msg, "{freeMin}", String.valueOf(minSpace));
	}

	public static void checkFreeSpaceForSessionInSys(final J2SESession coreSS) {
		if (isLowDiskFreeSpace()) {
			sendWarnToSession(coreSS);
		}
	}

	private static void sendWarnToSession(final J2SESession oneSession) {
		final J2SESession[] toArray = { oneSession };
		ServerUIAPIAgent.sendMessageViaCoreSSInUserOrSys(toArray,
				ResourceUtil.getWarnI18N(oneSession), buildWarnMsg(oneSession),
				ProjectContext.MESSAGE_WARN, null, 0);
	}
}
