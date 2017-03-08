package hc.server.ui;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;

public class DialogProcessedChecker {
	final DialogGlobalLock resLock;
	
	boolean isForDialogAndNoCheck = true;
	boolean isForDialogAndResponseContinue;
	
	public DialogProcessedChecker(final DialogGlobalLock resLock){
		this.resLock = resLock;
	}
	
	public final boolean isContinueProcess(final J2SESession coreSS){
		if(isForDialogAndNoCheck){
			isForDialogAndNoCheck = false;
			
			final int dialogID = resLock.dialogID;
			if(resLock.isProcessed(coreSS, dialogID, (String)ResourceUtil.get(coreSS, 9240))){//Dialog is processed by other!
				isForDialogAndResponseContinue = false;//其它session已获得处理权
			}else{
				isForDialogAndResponseContinue = true;
			}
			
//			ServerUIAPIAgent.removeQuestionFromMap(coreSS, dialogID, false);//需要接收事件，不能remove
		}
		
		if(isForDialogAndResponseContinue == false){
			if(L.isInWorkshop){
				LogManager.log("dialog action from JSInput, but it is canceled by other session.");
			}
			return false;
		}
		
		return true;
	}
}
