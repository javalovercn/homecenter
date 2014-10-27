package hc.server.ui.design;

import hc.core.L;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.SystemEventListener;
import hc.util.BaseResponsor;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class MobiUIResponsor extends BaseResponsor {
	final String[] contexts = new String[LinkProjectManager.MAX_LINK_PROJ_NUM];
	final ProjResponser[] responsors = new ProjResponser[LinkProjectManager.MAX_LINK_PROJ_NUM];
	int responserSize; 
	
	public MobiUIResponsor() {
		final Iterator<LinkProjectStore> lpsIt = LinkProjectManager.getLinkProjsIterator(true);
		while(lpsIt.hasNext()){
			LinkProjectStore lps = lpsIt.next();
			if(!lps.isActive()){
				continue;
			}
			final String context = lps.getProjectID();
			File har_load = new File(lps.getHarParentDir(), lps.getHarFile());
			Map<String, Object> map = HCjar.loadHar(har_load, false);
			ProjResponser resp = new ProjResponser(map, this, lps);
			contexts[responserSize] = context;
			responsors[responserSize++] = resp;
		}
	}

	public void setMap(HashMap map){
		
	}
	
	public void start(){
		onEvent(ProjectContext.EVENT_SYS_PROJ_STARTUP);
	}
	

	@Override
	public void enterContext(String contextName){
		this.currContext = contextName;
	}
	
	@Override
	public void stop(){
		onEvent(ProjectContext.EVENT_SYS_PROJ_SHUTDOWN);
		
		//terminate
		for (int i = 0; i < responserSize; i++) {
			try{
				responsors[i].hcje.terminate();
			}catch (Throwable e) {
				LogManager.errToLog("fail terminate JRuby engine : " + e.toString());
				e.printStackTrace();
			}
		}
		
		listsProjectContext.clear();
	}
	
	public Object onEvent(Object event) {
		//logout或shutdown属于后入先出型，应最后执行ROOT
		boolean isReturnBack = false;
		
		if(ProjResponser.isSysEvent(event)){
			//处理可能没有mobile_login，而导致调用mobile_logout事件
			if(event == ProjectContext.EVENT_SYS_MOBILE_LOGIN){
				isMobileLogined = true;
			}else if(event == ProjectContext.EVENT_SYS_MOBILE_LOGOUT){
				isReturnBack = true;
				
				currContext = findRootContextID();
				if(isMobileLogined == false){
					return null;
				}
				isMobileLogined = false;
			}else if(event == ProjectContext.EVENT_SYS_PROJ_STARTUP){
				isProjStarted = true;
				
				currContext = findRootContextID();
			}else if(event == ProjectContext.EVENT_SYS_PROJ_SHUTDOWN){
				isReturnBack = true;
				
				if(isProjStarted == false){
					return null;
				}
				isProjStarted = false;
			}
		}
		
		if(isReturnBack == false){
			//login或start时，先执行ROOT
			responsors[rootIdx].onEvent(event);
		}
		
		//执行非ROOT
		for (int i = 0; i < responserSize; i++) {
			if(i != rootIdx){
				responsors[i].onEvent(event);
			}
		}

		if(isReturnBack){
			//logout或shutdown时，最后执行ROOT
			responsors[rootIdx].onEvent(event);
		}
		
		Enumeration<ProjectContext> enu = listsProjectContext.elements();
		while(enu.hasMoreElements()){
			ProjectContext pc = enu.nextElement();
			Enumeration sels = pc.getSystemEventListener();
			while(sels.hasMoreElements()){
				SystemEventListener sel = (SystemEventListener)sels.nextElement();
				try{
					sel.onEvent(event.toString());
				}catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}

	Vector<ProjectContext> listsProjectContext = new Vector<ProjectContext>();
	
	@Override
	public void addProjectContext(ProjectContext pc){
		listsProjectContext.add(pc);
	}
	
	private String currContext;
	private int rootIdx;
	public boolean isMobileLogined = false, isProjStarted = false;
	
	public String findRootContextID(){
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResponser = responsors[i];
			if(projResponser.menu[projResponser.mainMenuIdx].isRoot){
				rootIdx = i;
				return contexts[i];
			}
		}
		return null;
	}
	
	private ProjResponser findContext(final String context){
		for (int i = 0; i < responserSize; i++) {
			if(contexts[i].equals(context)){
				return responsors[i];
			}
		}
		return null;
	}

	@Override
	public boolean doBiz(HCURL url) {

		//拦截Menu处理
		if(url.protocal == HCURL.MENU_PROTOCAL){
			final String newContext = url.elementID;
			
			if(newContext.equals(HCURL.ROOT_MENU) == false //保留支持旧的ROOT_ID
					&& newContext.equals(currContext) == false){
				final ProjResponser resp = findContext(newContext);
				final JarMainMenu linkMenu = resp.menu[resp.mainMenuIdx];
				
				ServerUIUtil.response(linkMenu.buildJcip());
				
				ScreenServer.pushScreen(linkMenu);
				
				enterContext(newContext);

				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "open link menu : [" 
						+ LinkProjectManager.getProjByID(newContext).getLinkScriptName() + "]");
				return true;
			}
		}
		
		return findContext(currContext).doBiz(url);
	}
	
}
