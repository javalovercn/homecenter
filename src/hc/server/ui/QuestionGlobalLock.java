package hc.server.ui;

import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;

public class QuestionGlobalLock {
	public boolean isProcessed;
	
	public final J2SESession[] sessionGroup;
	
	public QuestionGlobalLock(final J2SESession[] sessionGroup){
		this.sessionGroup = sessionGroup;
	}
	
	public final void cancelOthers(final int questionID, final J2SESession processingSession){
		for (int i = 0; i < sessionGroup.length; i++) {
			final J2SESession coreSS = sessionGroup[i];
			if(coreSS != processingSession){
				if(UserThreadResourceUtil.isInServing(coreSS.context)){
					ServerUIAPIAgent.removeQuestionFromMap(coreSS, questionID);
					
					final String[] para = { HCURL.DATA_PARA_ROLLBACK_QUESTION_ID};
					final String[] values = {String.valueOf(questionID)};//注意：必须在外部转换
					HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, para, values);
				}
			}
		}
	}
}
