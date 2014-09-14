package hc.server.ui;

import hc.core.IConstant;
import hc.core.util.HCURL;
import hc.util.BaseResponsor;

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
	}

	@Override
	public Object onEvent(Object event) {
		return null;
	}

}
