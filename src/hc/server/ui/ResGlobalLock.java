package hc.server.ui;

import hc.core.util.BooleanValue;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

public class ResGlobalLock {
	final String lockType;
	public final J2SESession[] sessionGroup;
	public boolean isProcessed;
	final boolean isWaiting;
	final boolean isSessionWait;
	final BooleanValue waitingLock;

	public ResGlobalLock(final boolean isForSession, final J2SESession[] sessionGroup, final String lockType, final boolean isWaiting) {
		this.isSessionWait = (isForSession && isWaiting);
		this.sessionGroup = sessionGroup;
		this.lockType = lockType;
		this.isWaiting = isWaiting;
		if (isWaiting) {
			waitingLock = new BooleanValue();
		} else {
			waitingLock = null;
		}
	}

	public final boolean waitingResult(final J2SESession coreSS) {
		if (isWaiting) {
			synchronized (waitingLock) {
				if (coreSS != null) {
					ServerUIUtil.checkLineOnForAPI(coreSS);
				}

				try {
					waitingLock.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				return waitingLock.value;
			}
		} else {
			return true;
		}
	}

	private boolean isSetNotifyResult;

	public final void notifyWaitStop(final J2SESession coreSS, final boolean isFromCancel, final boolean isFromLineOff) {
		if (isWaiting == false) {
			return;
		}

		synchronized (waitingLock) {
			if (isFromLineOff) {
				for (int i = 0; i < sessionGroup.length; i++) {
					final J2SESession one = sessionGroup[i];
					if (one != coreSS) {
						if (one.isExchangeStatus()) {
							return;
						}
					}
				}
			}

			if (isSetNotifyResult == false) {
				isSetNotifyResult = true;

				if (isSessionWait) {
					waitingLock.value = (isFromCancel == false);
				} else {
					waitingLock.value = (isFromLineOff == false);
				}
			}
			waitingLock.notifyAll();
		}
	}

	private final void cancelOthers(final int questionID, final J2SESession processingSession) {
		for (int i = 0; i < sessionGroup.length; i++) {
			final J2SESession coreSS = sessionGroup[i];
			if (coreSS != processingSession) {
				if (UserThreadResourceUtil.isInServing(coreSS.context)) {
					ServerUIAPIAgent.removeQuestionDialogFromMap(coreSS, questionID, true, false);
					dismiss(coreSS, questionID);
				}
			}
		}
	}

	public final void dismiss(final J2SESession coreSS, final int questionID) {
		final String[] para = { lockType };
		final String[] values = { String.valueOf(questionID) };// 注意：必须在外部转换
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, para, values);
	}

	public final boolean isProcessed(final J2SESession coreSS, final int id, final String processedMsg) {
		synchronized (this) {
			if (isProcessed) {
				final J2SESession[] coreSSS = { coreSS };
				ServerUIAPIAgent.sendMovingMsg(coreSSS, processedMsg);

				return true;
			}
			isProcessed = true;
		}

		// 撤消其它
		cancelOthers(id, coreSS);
		return false;
	}
}
