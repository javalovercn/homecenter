package hc.server.ui;

import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.MsgBuilder;

public class FormTimer extends HCTimer{
	IFormTimer timer;
	
	public FormTimer(IFormTimer t) {
		super("FT", t.getSecondMS(), true);
		timer = t;
	}
	public void doBiz() {
		String content = timer.doAutoResponse();
		ContextManager.getContextInstance().send(MsgBuilder.E_JCIP_FORM_REFRESH, content);
	}
}