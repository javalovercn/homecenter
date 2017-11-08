package hc.server.ui;

import hc.core.HCTimer;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.hpj.HCjar;

public class ProjDesignConf {
	public final long compactDayMS;
	
	public ProjDesignConf(final ProjResponser projResponser){
		int day;
		try{
			day = Integer.parseInt((String)projResponser.map.get(HCjar.PROJ_COMPACT_DAYS));
		}catch (final Throwable e) {
			day = 365 / 2;
		}
		compactDayMS = day * HCTimer.ONE_DAY;
	}
}
