package hc.server.ui;

import hc.core.util.HCURL;
import hc.server.ui.design.J2SESession;

public class DialogGlobalLock extends ResGlobalLock {
	public final int dialogID;
	public IMletCanvas mletCanvas;
	public DialogParameter dialogParameter;
	
	public DialogGlobalLock(final J2SESession[] sessionGroup, final int dialogID){
		super(sessionGroup, HCURL.DATA_PARA_ROLLBACK_DIALOG_ID);
		this.dialogID = dialogID;
	}
}
