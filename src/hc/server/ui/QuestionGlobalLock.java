package hc.server.ui;

import hc.core.util.HCURL;
import hc.server.ui.design.J2SESession;

public class QuestionGlobalLock extends ResGlobalLock {
	
	public QuestionGlobalLock(final J2SESession[] sessionGroup){
		super(sessionGroup, HCURL.DATA_PARA_ROLLBACK_QUESTION_ID);
	}
	
}
