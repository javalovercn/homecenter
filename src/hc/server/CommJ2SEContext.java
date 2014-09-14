package hc.server;

import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.util.LogManager;
import hc.util.TokenManager;

public abstract class CommJ2SEContext extends IContext{
	public CommJ2SEContext() {
		super();
	}
	
	public void run() {
		if(IConstant.serverSide){
			L.V = L.O ? false : LogManager.log("Init AliveRefresher");
			
			//每小时刷新alive变量到Root服务器
			//采用58秒，能保障两小时内可刷新两次。
			new HCTimer("AliveRefresher", 
					RootConfig.getInstance().getIntProperty(RootConfig.p_RootDelNotAlive), true){
				public void doBiz() {
					hc.core.L.V=hc.core.L.O?false:LogManager.log("Notify Root Server Alive");
					RootServerConnector.refreshRootAlive(TokenManager.getToken(),
							!J2SEContext.isEnableTransNewCertNow(), RootServerConnector.getHideToken());
				}};		
		}
	}
}