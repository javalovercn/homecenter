package hc.server.ui;

import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

public class ResGlobalLock {
	final String lockType;
	public final J2SESession[] sessionGroup;
	public boolean isProcessed;

	public ResGlobalLock(final J2SESession[] sessionGroup, final String lockType){
		this.sessionGroup = sessionGroup;
		this.lockType = lockType;
	}
	
	private final void cancelOthers(final int questionID, final J2SESession processingSession){
		for (int i = 0; i < sessionGroup.length; i++) {
			final J2SESession coreSS = sessionGroup[i];
			if(coreSS != processingSession){
				if(UserThreadResourceUtil.isInServing(coreSS.context)){
					ServerUIAPIAgent.removeQuestionDialogFromMap(coreSS, questionID, true);
					dismiss(coreSS, questionID);
				}
			}
		}
	}

	public final void dismiss(final J2SESession coreSS, final int questionID) {
		final String[] para = { lockType };
		final String[] values = {String.valueOf(questionID)};//注意：必须在外部转换
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, para, values);
	}
	
	public final boolean isProcessed(final J2SESession coreSS, final int id, final String processedMsg){
		synchronized (this) {
			if(isProcessed){
				final J2SESession[] coreSSS = {coreSS};
				ServerUIAPIAgent.sendMovingMsg(coreSSS, processedMsg);
				
				return true;
			}
			isProcessed = true;
		}
		
		//撤消其它
		cancelOthers(id, coreSS);
		return false;
	}
}
