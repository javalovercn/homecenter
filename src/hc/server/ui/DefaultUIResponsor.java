package hc.server.ui;

import hc.core.IConstant;
import hc.core.util.HCURL;
import hc.server.ui.design.ProjResponser;
import hc.util.BaseResponsor;

import java.awt.Frame;
import java.util.HashMap;

public class DefaultUIResponsor extends BaseResponsor{

	@Override
	public boolean doBiz(HCURL url) {
		if(url.protocal == HCURL.MENU_PROTOCAL){
			if(url.elementID.equals(HCURL.ROOT_MENU)){
				ServerUIUtil.response(IConstant.NO_CANVAS_MAIN);
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getObject(int funcID, Object para){
		return null;
	}
	
	@Override
	public BaseResponsor checkAndReady(final Frame owner) throws Exception{
		return null;
	}
	
	@Override
	public void setMap(HashMap map) {
	}

	@Override
	public void start() {
	}

	@Override
	public void enterContext(String contextName){
	}
	
	@Override
	public void stop() {
		notifyMobileLogout(true);//有可能直接stop，而跳过EVENT_SYS_MOBILE_LOGOUT
		super.stop();
	}

	@Override
	public Object onEvent(Object event) {
		if(ProjResponser.isScriptEvent(event)){
			//处理可能没有mobile_login，而导致调用mobile_logout事件
			if(event == ProjectContext.EVENT_SYS_MOBILE_LOGIN){
				notifyMobileLogin();
			}else if(event == ProjectContext.EVENT_SYS_MOBILE_LOGOUT){
				notifyMobileLogout(false);
			}
		}
		return null;
	}

	@Override
	public void addProjectContext(ProjectContext pc){
	}

	@Override
	public void enableLog(boolean enable) {
	}
}
