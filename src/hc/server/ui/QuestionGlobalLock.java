package hc.server.ui;

import hc.core.util.HCURL;
import hc.server.ui.design.J2SESession;

public class QuestionGlobalLock extends ResGlobalLock {
	boolean isForMultiple;
	
	public QuestionGlobalLock(final boolean isForSession, final J2SESession[] sessionGroup, final boolean isWaiting){
		super(isForSession, sessionGroup, HCURL.DATA_PARA_ROLLBACK_QUESTION_ID, isWaiting);
		isForMultiple = sessionGroup.length > 1;
	}
	
}
