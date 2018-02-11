package hc.server.ui.design;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;

import hc.core.cache.CacheManager;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.DelDeployedProjManager;
import hc.server.util.SafeDataManager;
import hc.server.util.ai.AIPersistentManager;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

public abstract class BaseProjList {
	public static final int COL_NO = 0, COL_IS_ROOT = 1, COL_PROJ_ID = 2, COL_VER = 3, COL_PROJ_ACTIVE = 4, 
			COL_PROJ_LINK_NAME = 5, COL_PROJ_DESC = 6, COL_UPGRADE_URL = 7;
	final int COL_NUM = 2;
	final int IDX_OBJ_STORE = 1;

	public static final String upgradeURL = ResourceUtil.get(8023);
	public static final String ACTIVE = ResourceUtil.get(8020);

	//删除时，后位前移，序号重算
	final Vector<Object[]> data = new Vector<Object[]>();//[COL_NUM];
	int dataRowNum = 0;
	
	final Vector<LinkEditData> delList = new Vector<LinkEditData>(0);
	boolean isChanged = false;

	public BaseProjList(){
		loadDataInUserSysThread();
		dataRowNum = data.size();
	}
	
	protected String getKeepOneProjWarn(final J2SESession coreSS) {
		return ResourceUtil.get(coreSS, 9258);
	}
	
	protected final boolean isDeployed(final int selectedRow){
		final Object[] elementAt = data.elementAt(selectedRow);
		if(elementAt == null){
			return false;
		}
		
		final LinkEditData led = (LinkEditData)elementAt[IDX_OBJ_STORE];
		return led != null && led.status == LinkProjectManager.STATUS_DEPLOYED;
	}
	
	protected final boolean isActive(final int selectedRow){
		final Object[] elementAt = data.elementAt(selectedRow);
		if(elementAt == null){
			return false;
		}
		
		final LinkEditData led = (LinkEditData)elementAt[IDX_OBJ_STORE];
		return led != null && led.lps.isActive();
	}
	
	final void delProjInList(final LinkEditData led) {
		delList.add(led);
		dataRowNum--;
	}
	
	/**
	 * true means no active project.
	 * @return
	 */
	protected final int getActiveProjectNum(){
		int count = 0;
		for (int i = 0; i < dataRowNum; i++) {
			final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
			final LinkProjectStore lps = led.lps;
			
			if(lps.isActive()){
				count++;
			}
		}
		
		return count;
	}
	
	final void restartServerImpl(final boolean isQuery, final JFrame frame) {
		//								LogManager.log("restarting service...");
		//启动时，需要较长时间初始化，有可能用户快速打开并更新保存，所以加锁。
		synchronized (ServerUIUtil.LOCK) {
			ServerUIUtil.promptAndStop(isQuery, frame);
		}
		
		//启动远屏或菜单
		ServerUIUtil.restartResponsorServer(frame, null);
		
		if(isChanged){
			final Designer design = Designer.getInstance();
			if(design != null){
				design.refresh();//注意：要在restartResponsorServer之后
			}
		}
	}
	
	abstract void showNoMenuInRootError();
	
	abstract void resetSave();

	/**
	 * 注意：此过程被PC端的LinkProjectPanel和手机端的ProjMgrDialog共用
	 * @return true means successful save and apply.
	 */
	final boolean saveAndApply() {
		{
			final LinkProjectStore rootLPS = searchRoot();
			if(rootLPS != null){
				final Map<String, Object> rootMap = HCjar.loadHarFromLPS(rootLPS);
				if(LinkProjectManager.hasMenuItemNumInProj(rootMap) == false){
					showNoMenuInRootError();
					return false;
				}
			}
		}
		
		{
			final Vector<LinkProjectStore> stores = new Vector<LinkProjectStore>();
			
			for (int i = 0; i < dataRowNum; i++) {
				final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
				final LinkProjectStore lps = led.lps;
				
				if(lps.isActive()){
					stores.add(lps);
				}
			}
			
			final String text = LinkProjectManager.checkReferencedDependencyForErrorInSysThread(stores);
			if(text != null){
				showReferencedDependencyError(text);
				return false;
			}
		}
		
		final HashMap<String, File> delBackFileMap = new HashMap<String, File>();
		
		final String[] delCacheProjIDS;
		final String[] removedAndNotUpgrade;
		synchronized (ServerUIUtil.LOCK) {
			SafeDataManager.disableSafeBackup();
			
			//将已发布，且准备进行删除的进行删除操作
			{
				final int size = delList.size();
				delCacheProjIDS = new String[size];
				removedAndNotUpgrade = new String[size];
				
				for (int i = 0; i < size; i++) {
					final LinkEditData led = delList.elementAt(i);
					final LinkProjectStore lps = led.lps;
					boolean isRemoved = false;
					if(led.status == LinkProjectManager.STATUS_DEPLOYED){
						final File oldBackEditFile = LinkProjectManager.removeLinkProjectPhic(lps, true);
						isRemoved = true;
						if(oldBackEditFile != null){
							delBackFileMap.put(lps.getProjectID(), oldBackEditFile);
						}
						isChanged = true;
					}
					
					if(led.isUpgrade == false){
//						LinkProjectManager.removeOnlyLPS(lps);
						delCacheProjIDS[i] = lps.getProjectID();
						if(isRemoved){
							DelDeployedProjManager.addDeledDeployed(lps.getProjectID());
							removedAndNotUpgrade[i] = lps.getProjectID();
						}
					}
				}
				delList.removeAllElements();
			}
			
			final Vector<String> noActiveProjs = new Vector<String>();
			
			for (int i = 0; i < dataRowNum; i++) {
				final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
				final LinkProjectStore lps = led.lps;
				if(led.status == LinkProjectManager.STATUS_NEW){
					final File oldBackEditFile = delBackFileMap.get(lps.getProjectID());
					final boolean result = AddHarHTMLMlet.addHarToDeployArea(J2SESession.NULL_J2SESESSION_FOR_PROJECT, 
							led, lps, false, true, oldBackEditFile);
					isChanged = isChanged?true:result;
				}else if(led.op == LinkProjectManager.STATUS_MODIFIED){
					if(lps.isActive() == false){
						noActiveProjs.add(lps.getProjectID());
					}
					isChanged = true;
				}
				led.op = (LinkProjectManager.STATUS_NEW);
			}
			
			if(noActiveProjs.size() > 0){
				final String[] noActive = new String[noActiveProjs.size()];
				for (int i = 0; i < noActive.length; i++) {
					noActive[i] = noActiveProjs.elementAt(i);
				}
				CacheManager.delProjects(noActive);//因为手机端会上线后，进行同步，所以noActive必须执行本操作
			}
			
			resetSave();

			if(isChanged){
				storeData();
				
				//更新后必须reload
				LinkProjectManager.reloadLinkProjects();
				
				//如果是升级型，则可能出现null
				CacheManager.delProjects(delCacheProjIDS);
				AIPersistentManager.removeAndNotUpgrade(removedAndNotUpgrade);
				PropertiesManager.saveFile();
			}
			
			saveUpgradeOptions();
			
			if(isChanged){
				restartServer();
				
				isChanged = false;
			}
			
			SafeDataManager.startSafeBackupProcess(true, false);
		}
		return true;
	}
	
	public abstract void showReferencedDependencyError(final String text);

	public abstract void saveUpgradeOptions();
	
	public abstract void restartServer();
	
	final void storeData() {
		final int size = dataRowNum;
		
		final LinkProjectStore[] lpss = new LinkProjectStore[size];
		for (int i = 0; i < size; i++) {
			lpss[i] = ((LinkEditData)data.elementAt(i)[IDX_OBJ_STORE]).lps;
		}
		
		final PropertiesSet projIDSet = LinkProjectManager.newLinkProjSetInstance();
		LinkProjectManager.saveLinkStore(lpss, projIDSet);
	}
	
	/**
	 * true means root to others.
	 * @return
	 */
	final boolean transRootToOtherActive(final LinkProjectStore lps){
		final int length = data.size();
		for (int i = 0; i < length; i++) {
			final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
			if(led != null){
				if(lps != led.lps && led.lps.isActive()){
					led.lps.setRoot(true);
					led.op = LinkProjectManager.STATUS_MODIFIED;
					return true;
				}
			}
		}
		return false;
	}

	final LinkProjectStore searchRoot(){
		final int size = data.size();
		for (int i = 0; i < size; i++) {
			final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
			if(led != null){
				if(led.lps.isRoot()){
					return led.lps;
				}
			}else{
				return null;
			}
		}
		return null;
	}

	final void clickOnRoot(final LinkEditData led, final LinkProjectStore lps, final Boolean isRoot) {
		if(lps.isRoot()){
			lps.setRoot(false);
			if(transRootToOtherActive(lps) == false){
				lps.setActive(false);//没有其它root，且关闭全部active
			}
		}else{
			final LinkProjectStore root_lps = searchRoot();
			if(root_lps != null){
				root_lps.setRoot(false);
//							if(root_lps.getProjectID().equals(HCURL.ROOT_MENU)){
//								root_lps.setProjectID("oldroot");
//							}
			}
			lps.setRoot(isRoot);
			lps.setActive(true);
		}
		notifyNeedToSave();
		led.op = (LinkProjectManager.STATUS_MODIFIED);
	}

	final void clickOnActive(final LinkEditData led, final LinkProjectStore lps, final Boolean isActive) {
		lps.setActive(isActive);
		if(isActive){
			if(searchRoot() == null){
				lps.setRoot(true);
			}
		}else{
			if(lps.isRoot()){
				lps.setRoot(false);
				//将root移交给其它工程
				transRootToOtherActive(lps);
			}
		}
		notifyNeedToSave();
		led.op = (LinkProjectManager.STATUS_MODIFIED);
	}
	
	public abstract void notifyNeedToSave();
	
	protected final void loadDataInUserSysThread() {
		data.clear();
		
		int i = 0;
		final Iterator<LinkProjectStore> itx = LinkProjectManager.getLinkProjsIteratorInUserSysThread(true);
		while(itx.hasNext()){
			final LinkEditData led = new LinkEditData();
			led.lps = itx.next();
			led.op = (LinkProjectManager.STATUS_NEW);
			led.status = (LinkProjectManager.STATUS_DEPLOYED);

			data.add(new Object[COL_NUM]);
			data.elementAt(i)[0] = String.valueOf(i+1);
			data.elementAt(i++)[IDX_OBJ_STORE] = led;
		}
	}
}
