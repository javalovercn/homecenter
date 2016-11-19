package hc.server.ui.design.hpj;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;

public class MouseExitHideDocForMouseMovTimer extends HCTimer {
	public MouseExitHideDocForMouseMovTimer(final String name, final int ms, final boolean enable) {
		super(name, ms, enable);
	}

	protected boolean isTriggerOn;
	public boolean isUsingByDoc, isUsingByCode;
	
	public final boolean isTriggerOn(){
		return isTriggerOn;
	}
	
	public final void triggerOn(){
		isTriggerOn = true;
	}
	
	@Override
	public void doBiz() {
	}
	
	@Override
	public void setEnable(final boolean enable){
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log(getName() + " setEnable : " + enable);
		}
		super.setEnable(enable);
	}

}
