package hc.server.ui.design.hpj;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;

public class MouseExitHideDocForMouseMovTimer extends HCTimer {
	public MouseExitHideDocForMouseMovTimer(final String name, final int ms, final boolean enable) {
		super(name, ms, enable);
	}

	public final void reset() {
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] reset MouseExitHideDocForMouseMovTimer");
		}
		super.setEnable(false);
		isTriggerOn = false;
		isUsingByCode = false;
		isUsingByDoc = false;
	}
	
	protected boolean isTriggerOn;
	public boolean isUsingByDoc, isUsingByCode;
	
	public final boolean isTriggerOn(){
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] isTriggerOn : " + isTriggerOn);
		}
		
		return isTriggerOn;
	}
	
	public final void triggerOn(){
		if(L.isInWorkshop){
			LogManager.log("[CodeTip] triggerON.");
		}
		isTriggerOn = true;
	}
	
	@Override
	public void doBiz() {
	}
	
	@Override
	public void setEnable(final boolean enable){
		if(L.isInWorkshop){
			LogManager.log(getName() + " setEnable : " + enable);
		}
		super.setEnable(enable);
	}

}
