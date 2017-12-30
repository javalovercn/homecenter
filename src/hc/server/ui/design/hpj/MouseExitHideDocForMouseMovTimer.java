package hc.server.ui.design.hpj;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;
import hc.util.ClassUtil;

public class MouseExitHideDocForMouseMovTimer extends HCTimer {
	public MouseExitHideDocForMouseMovTimer(final String name, final int ms, final boolean enable) {
		super(name, ms, enable);
	}

	@Override
	public void doBiz() {
	}
	
	@Override
	public void setEnable(final boolean enable){
		if(L.isInWorkshop){
			LogManager.log(getName() + " setEnable : " + enable);
//			if(enable){
//				ClassUtil.printCurrentThreadStack(getName() + ", setEnable");
//			}
		}
		super.setEnable(enable);
	}

}
