package hc.server;

import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.LogManager;
import hc.util.TokenManager;

public abstract class CommJ2SEContext extends IContext{
	final boolean isRoot;
	public CommJ2SEContext(boolean isRoot) {
		this.isRoot = isRoot;
	}
	
	@Override
	public void run() {
		if(IConstant.serverSide){
			L.V = L.O ? false : LogManager.log("Init AliveRefresher");
			
			//每小时刷新alive变量到Root服务器
			//采用58秒，能保障两小时内可刷新两次。
			
			int refreshMS = isRoot?(1000 * 60 * 5):RootConfig.getInstance().getIntProperty(RootConfig.p_RootDelNotAlive);
			new HCTimer("AliveRefresher", refreshMS, true){
				public final void doBiz() {
					if(isRoot == false){
						L.V = L.O ? false : LogManager.log("refresh server online info.");
					}
					String back = RootServerConnector.refreshRootAlive(TokenManager.getToken(),
							!J2SEContext.isEnableTransNewCertNow(), RootServerConnector.getHideToken());
					if(back == null || (back.equals(RootServerConnector.ROOT_AJAX_OK) == false)){
						if(isRoot){
							//服务器出现错误，需要进行重启服务
							LogManager.errToLog("fail notify Root Server Alive");
							notifyShutdown();
						}else{
							LogManager.errToLog("fail to refresh server online info, reconnect...");
							SIPManager.notifyRelineon(false);
						}
					}
				}};		
		}
	}
}