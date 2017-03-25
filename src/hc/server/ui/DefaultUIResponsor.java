package hc.server.ui;

import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.L;
import hc.core.SessionManager;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.util.BaseResponsor;

import java.util.HashMap;

import javax.swing.JFrame;

public class DefaultUIResponsor extends BaseResponsor{

	@Override
	public boolean doBiz(final CoreSession coreSS, final HCURL url) {
		if(url.protocal == HCURL.MENU_PROTOCAL){
			if(url.elementID.equals(HCURL.ROOT_MENU)){
				ServerUIUtil.response(coreSS, IConstant.NO_CANVAS_MAIN);
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getObject(final int funcID, final Object para){
		return null;
	}
	
	@Override
	public BaseResponsor checkAndReady(final JFrame owner) throws Exception{
		return this;
	}
	
	@Override
	public void setMap(final HashMap map) {
	}

	@Override
	public void start() {
	}

	@Override
	public void enterContext(final J2SESession socketSession, final String contextName){
	}
	
	@Override
	public void stop() {
		final CoreSession[] coreSSS = SessionManager.getAllSocketSessions();
		for (int i = 0; i < coreSSS.length; i++) {
			final J2SESession coreSS = (J2SESession)coreSSS[i];
			coreSS.notifyMobileLogout();//有可能直接stop，而跳过EVENT_SYS_MOBILE_LOGOUT
		}
		
		super.stop();
	}

	@Override
	public Object onEvent(final J2SESession coreSS, final String event) {
		if(coreSS == null){
			if(L.isInWorkshop){
				LogManager.errToLog("fail to stop UpdateOneTimeRunnable!");
			}
			return null;
		}
		
		if(ProjResponser.isScriptEventToAllProjects(event)){
			//处理可能没有mobile_login，而导致调用mobile_logout事件
			if(event == ProjectContext.EVENT_SYS_MOBILE_LOGIN){
				notifyMobileLogin(coreSS);
			}else if(event == ProjectContext.EVENT_SYS_MOBILE_LOGOUT){
				coreSS.notifyMobileLogout();
			}
		}
		return null;
	}

	@Override
	public void enableLog(final boolean enable) {
	}

	@Override
	public void createClientSession(final J2SESession ss) {
	}

	@Override
	public void releaseClientSession(final J2SESession coreSS) {
	}
}
