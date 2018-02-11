package hc.server.ui;

import java.util.Map;
import java.util.Vector;

import hc.core.GlobalConditionWatcher;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.UIUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.JarMainMenu;
import hc.server.ui.design.LinkProjectStore;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.hpj.HCjar;
import hc.util.I18NStoreableHashMapWithModifyFlag;


public class MobiMenu {
	final Vector<MenuItem> menuItems = new Vector<MenuItem>(12);
	boolean isFlushedBaseMenu;
	boolean isEnableFlushMenu;
	public int headerStartIdx = 0;//优先显示的工程文件夹数量
	public int tailCount = 0;//扩展QR_ADD按钮的数量
	protected final Object menuLock;
	final String projectID;
	public final ProjResponser resp;
	
	public final int getSessionItemsCount(){
		synchronized(menuLock){
			return getSessionItemsCountInLock();
		}
	}

	protected final int getSessionItemsCountInLock() {
		synchronized(menuLock){
			return menuItems.size() - headerStartIdx - tailCount;
		}
	}
	
	public MobiMenu(final String projectID, final ProjResponser resp){
		this.projectID = projectID;
		this.resp = resp;
		menuLock = this;
	}
	
	public int getModifiableItemsCount(){
		return getSessionItemsCount();
	}
	
	public MenuItem getModifiableItemBy(final String urlLowercase){
		synchronized(menuLock){
			final int modifiableSize = menuItems.size() - tailCount;
			
			for (int i = headerStartIdx; i < modifiableSize; i++) {
				final MenuItem out = menuItems.elementAt(i);
				if(urlLowercase.equals(out.getItemURLLower())){
					return out;
				}
			}
		}
		return null;
	}
	
	/**
	 * 如果越界，返回null
	 * @param pos
	 * @return
	 */
	public final MenuItem getModifiableItemAt(final int pos){
		synchronized(menuLock){
			final int shiftIdx = headerStartIdx + pos;
			final int modifiableSize = menuItems.size() - tailCount;
			
			if(shiftIdx >= modifiableSize){
				return null;
	//			throw new ArrayIndexOutOfBoundsException(pos + " >= " + modifiableSize);
			}
			
			try{
				return menuItems.elementAt(shiftIdx);
			}catch (final Throwable e) {
			}
		}
		return null;
	}

	public final MenuItem removeModifiableItemAt(final int pos){
		MenuItem removed = null;
		synchronized(menuLock){
			final int shiftIdx = headerStartIdx + pos;
			final int modifiableSize = menuItems.size() - tailCount;
			
			if(shiftIdx >= modifiableSize){
				return null;
	//			throw new ArrayIndexOutOfBoundsException(pos + " >= " + modifiableSize);
			}
			
			try{
				removed = menuItems.remove(shiftIdx);
				if(removed != null){
					removed.belongToMenu = null;
				}
			}catch (final Throwable e) {
			}

			if(removed != null){
				if(isFlushedBaseMenu){
					publishToMobi(REMOVE_ITEM, removed, null);
				}
			}
		}
		
		return removed;
	}

	public boolean removeModifiableItem(final MenuItem item){
		boolean isRemoved = false;
		synchronized(menuLock){
			isRemoved = menuItems.remove(item);
			L.V = L.WShop ? false : LogManager.log("[publishMenuToMobi] successful removeModifiableItem : " + item.itemName);
			if(isRemoved){
				item.belongToMenu = null;
			}

			if(isRemoved){
				item.isNeedRefresh = true;
				if(isFlushedBaseMenu){
					publishToMobi(REMOVE_ITEM, item, null);
				}
			}
			
		}
		
		return isRemoved;
	}
	
	public boolean insertModifiableItem(final MenuItem item, final int pos){
		boolean isAddedSucc = false;
		MenuItem tail = null;
		synchronized(menuLock){
			final int shiftIdx = headerStartIdx + pos;
			final int modifiableSize = menuItems.size() - tailCount;
			
			if(shiftIdx > modifiableSize){
				return false;
	//			throw new ArrayIndexOutOfBoundsException(pos + " >= " + modifiableSize);
			}
			
			try{
				if(shiftIdx > 0){
					tail = menuItems.elementAt(shiftIdx - 1);
				}
				menuItems.add(shiftIdx, item);
				item.belongToMenu = this;
				isAddedSucc = true;
			}catch (final Throwable e) {
			}

			if(isAddedSucc){
				if(isFlushedBaseMenu){
					publishToMobi(ADD_ITEM, item, tail);
				}
				return true;
			}
		}
		
		return false;
	}

	/**
	 * add session menu item to this menu.
	 * @param item
	 */
	public void addModifiableItem(final MenuItem item){
		MenuItem tail = null;
		synchronized (menuLock) {
			final int size = menuItems.size();
			final int tailEndIdx = size - tailCount;
			if(tailEndIdx> 0){
				tail = menuItems.elementAt(tailEndIdx - 1);
			}
			menuItems.insertElementAt(item, tailEndIdx);
			item.belongToMenu = this;

			if(isFlushedBaseMenu){
				publishToMobi(ADD_ITEM, item, tail);
			}
		}
	}
	
	/**
	 * 添加系统级菜单项，比如工程Folder。
	 * @param item
	 */
	public final void addHeader(final MenuItem item){
		MenuItem tail = null;
		synchronized (menuLock) {
			if(headerStartIdx > 0){
				tail = menuItems.elementAt(headerStartIdx - 1);
			}
			menuItems.insertElementAt(item, headerStartIdx++);
			item.belongToMenu = this;

			if(isFlushedBaseMenu){
				publishToMobi(ADD_ITEM, item, tail);
			}
		}
	}
	
	/**
	 * 添加系统级菜单项，比如QR，语音，工程维护
	 * @param item
	 */
	public final void addTail(final MenuItem item){
		MenuItem tail = null;
		synchronized (menuLock) {
			final int size = menuItems.size();
			if(size> 0){
				tail = menuItems.elementAt(size - 1);
			}
			menuItems.add(item);
			item.belongToMenu = this;
			tailCount++;

			if(isFlushedBaseMenu){
				publishToMobi(ADD_ITEM, item, tail);
			}
		}
	}
	
	public boolean isEnableFlushMenu(){
		synchronized (menuLock) {
			return isEnableFlushMenu;
		}
	}
	
	public final void notifyChangeIcon(final MenuItem item){
		synchronized (menuLock) {
			if(isFlushedBaseMenu){
				publishToMobi(CHANGE_ICON_ITEM, item, null);
			}
		}
	}
	
	public final void notifyModify(final MenuItem item){
		synchronized (menuLock) {
			if(isFlushedBaseMenu){
				synchronized (item) {
					if(item.isNeedRefresh){
						return;
					}
					item.isNeedRefresh = true;
				}
				publishToMobi(MODIFY_ITEM, item, null);
			}
		}
	}
	
	public final void notifyEnableFlushMenu(){
		synchronized (menuLock) {
			isEnableFlushMenu = true;
		}
	}
	
	protected static final String ADD_ITEM = "add";
	private static final String REMOVE_ITEM = "remove";
	private static final String MODIFY_ITEM = "modify";
	private static final String CHANGE_ICON_ITEM = "change_icon";
	
	/**
	 * publish to all session.
	 * <BR>
	 * 它被SessionMobiMenu重载，仅publish to session only.
	 * @param op
	 * @param item
	 * @param itemBefore 位于新增之前的item，null表示位于最前
	 */
	public void publishToMobi(final String op, final MenuItem item, final MenuItem itemBefore){
		switchCoreSS(TO_PROJECT_LEVEL, op, item, itemBefore);
	}

	protected final void switchCoreSS(final J2SESession coreSS, final String op, final MenuItem item,
			final MenuItem itemBefore) {
		L.V = L.WShop ? false : LogManager.log("[publishMenuToMobi] [" + op + "] one menu item : " + item.itemName);
		
		if(ADD_ITEM.equals(op)){
			flushRefresh(item, itemBefore, coreSS, op);
		}else{
			publishToMobiToWatcher(coreSS, op, item, itemBefore);
		}
	}
	
	protected final void publishToMobiToWatcher(final J2SESession coreSS, final String op, final MenuItem item, final MenuItem itemBefore){
		if(L.isInWorkshop){
			LogManager.log("[publishMenuToMobi] for " + op);
		}
		
		final IWatcher watcher = buildRefreshWatcher(item, itemBefore, coreSS, op);
		if(coreSS == TO_PROJECT_LEVEL){
			ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {//runAndWait for synch
				@Override
				public Object run() throws Throwable {
					GlobalConditionWatcher.addWatcher(watcher);
					return null;
				}
			});
		}else{
			coreSS.eventCenterDriver.addWatcher(watcher);
		}
	}
	
	private static final J2SESession TO_PROJECT_LEVEL = null;
	
	protected final void sendRefreshMenuData(final J2SESession coreSS, final MenuItem item, final MenuItem itemBefore, final String op){
		if(L.isInWorkshop){
			LogManager.log("flush MenuItem E_MENU_REFRESH [53] data, op [" + op + "], item [" + item.itemName + "].");
		}
		
		if(MODIFY_ITEM.equals(op)){
			final String menuData = resp.jarMainMenu.buildItemRefreshJcip(coreSS, item);
			coreSS.context.send(MsgBuilder.E_MENU_REFRESH, menuData);
		}else if(CHANGE_ICON_ITEM.equals(op)){
			final String menuData = resp.jarMainMenu.buildChangeIconJcip(coreSS, item);
			coreSS.context.send(MsgBuilder.E_MENU_REFRESH, menuData);
		}else if(ADD_ITEM.equals(op)){
			final String menuData = resp.jarMainMenu.buildItemAddJcip(coreSS, item, itemBefore);
			coreSS.context.send(MsgBuilder.E_MENU_REFRESH, menuData);
		}else if(REMOVE_ITEM.equals(op)){
			final String menuData = resp.jarMainMenu.buildItemRemoveJcip(coreSS, item);
			coreSS.context.send(MsgBuilder.E_MENU_REFRESH, menuData);
		}
	}

	/**
	 * 
	 * @param item
	 * @param coreSS 如果为null，表示projectLevel
	 * @return
	 */
	protected final IWatcher buildRefreshWatcher(final MenuItem item, final MenuItem itemBefore, final J2SESession coreSS, final String op) {
		final IWatcher watcher = new IWatcher() {
			@Override
			public boolean watch() {
				synchronized (item) {
					if(item.isNeedRefresh){
						flushRefresh(item, itemBefore, coreSS, op);
						item.isNeedRefresh = false;
					}
				}
				return true;
			}
			
			@Override
			public void setPara(final Object p) {
			}
			
			@Override
			public boolean isCancelable() {
				return false;
			}
			
			@Override
			public void cancel() {
			}
		};
		return watcher;
	}

	/**
	 * 动态添加project folder
	 * @param map
	 * @param lps
	 */
	public final void addFolderItem(final Map<String, Object> map, final LinkProjectStore lps){
		final String linkName = lps.getLinkName();
		
		final String name = (linkName==null||linkName.length()==0)?(String)map.get(HCjar.PROJ_NAME):linkName;
		final int type = JarMainMenu.FOLD_TYPE;
		final String image = UIUtil.SYS_FOLDER_ICON;
		final String url = HCURL.buildStandardURL(HCURL.MENU_PROTOCAL, lps.getProjectID());
		final I18NStoreableHashMapWithModifyFlag i18nName = HCjar.buildI18nMapFromSerial((String)map.get(HCjar.PROJ_I18N_NAME));
		final String listen = ""; 
		final String extend_map = "";
		
		addHeader(ServerUIAPIAgent.buildMobiMenuItem(name, type, image, url, i18nName, listen, extend_map));
	}

	protected final void flushRefresh(final MenuItem item, final MenuItem itemBefore,
			final J2SESession coreSS, final String op) {
		//refresh to mobile，如果此动作先传送menu，则手机端进行丢弃。
		
		ServerUIAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				if(coreSS == TO_PROJECT_LEVEL){
					final J2SESession[] coreSSS = J2SESessionManager.getAllOnlineSocketSessions();
					if(coreSSS != null){
						for (int i = 0; i < coreSSS.length; i++) {
							sendRefreshMenuData(coreSSS[i], item, itemBefore, op);
						}
					}
				}else{
					sendRefreshMenuData(coreSS, item, itemBefore, op);
				}
				return null;
			}
		});

	}
}
