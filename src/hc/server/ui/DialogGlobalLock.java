package hc.server.ui;

import hc.core.util.HCURL;
import hc.server.ui.design.J2SESession;

public class DialogGlobalLock extends ResGlobalLock {
	public final int dialogID;
	public IMletCanvas mletCanvas;

	public DialogGlobalLock(final boolean isForSession, final J2SESession[] sessionGroup, final int dialogID, final boolean isWaiting) {
		super(isForSession, sessionGroup, HCURL.DATA_PARA_ROLLBACK_DIALOG_ID, isWaiting);
		this.dialogID = dialogID;
	}
}
