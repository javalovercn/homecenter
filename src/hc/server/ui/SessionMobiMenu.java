package hc.server.ui;

import hc.core.util.HCURL;
import hc.core.util.UIUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.JarMainMenu;
import hc.server.ui.design.ProjResponser;
import hc.server.util.VoiceCommand;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

public class SessionMobiMenu extends MobiMenu{
	final MobiMenu projectMenu;
	boolean enableQRInMobiMenu, enableWiFiInMobiMenu, enableVoiceCommand;
	final J2SESession coreSS;
	public final int targetMobileIconSize;
	
	public SessionMobiMenu(final J2SESession coreSS, final ProjResponser resp, final boolean isRoot, final MobiMenu pMenu){
		super(resp.projectID, resp);
		
		this.projectMenu = pMenu;
		this.coreSS = coreSS;
		
		targetMobileIconSize = UIUtil.calMenuIconSize(UserThreadResourceUtil.getMobileWidthFrom(coreSS), UserThreadResourceUtil.getMobileHeightFrom(coreSS), UserThreadResourceUtil.getMobileDPIFrom(coreSS));

		if(isRoot){
			//可能新用户重新登录，手机WiFi状态不一
			enableQRInMobiMenu = ResourceUtil.isEnableClientAddHAR() && UserThreadResourceUtil.getMobileAgent(coreSS).hasCamera();// || PropertiesManager.isSimu();
			//关闭WiFi广播HAR
			enableWiFiInMobiMenu = ResourceUtil.isDemoServer() == false && HCURL.isUsingWiFiWPS && ResourceUtil.canCtrlWiFi(coreSS);// || PropertiesManager.isSimu();
		
			enableVoiceCommand = UserThreadResourceUtil.getMobileAgent(coreSS).isEnableVoiceCommand();
		}
		
		initMenuItemArray();
	}
	
	@Override
	public final MenuItem getModifiableItemBy(final String urlLowercase){
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				final MenuItem out = projectMenu.getModifiableItemBy(urlLowercase);
				if(out != null){
					return out;
				}
				
				return super.getModifiableItemBy(urlLowercase);
			}
		}
	}
	
	@Override
	public final boolean isIncrementMode(){
		return projectMenu.isIncrementMode && isIncrementMode;
	}
	
	@Override
	public final int getModifiableItemsCount(){
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				return super.getModifiableItemsCount() + projectMenu.getModifiableItemsCount();
			}
		}
	}
	
	private final int insertToLastTail = -2;
	
	@Override
	public final void addModifiableItem(final MenuItem item){
		insertModifiableItem(item, insertToLastTail);
	}
	
//	@Override
//	public final MenuItem getModifiableItemAt(final int pos){
//		synchronized(projectMenu.menuLock){
//	synchronized (menuLock) {
//			final int pCount = projectMenu.getModifiableItemsCount();
//			final int sCount = super.getModifiableItemsCount();
//			
//			final int totalSize = pCount + sCount;
//			
//			if(pos >= totalSize){
//				return null;
//	//			throw new ArrayIndexOutOfBoundsException(pos + " >= " + totalSize);
//			}
//			
//			if(pos < pCount){
//				return projectMenu.getModifiableItemAt(pos);
//			}else{
//				return super.getModifiableItemAt(pos - pCount);
//			}
//	}
//		}
//	}
	
//	@Override
//	public final MenuItem removeModifiableItemAt(final int pos){
//		synchronized(projectMenu.menuLock){
//	synchronized (menuLock) {
//			final int pCount = projectMenu.getModifiableItemsCount();
//			final int sCount = super.getModifiableItemsCount();
//			
//			final int totalSize = pCount + sCount;
//			
//			if(pos >= totalSize){
//	//			throw new ArrayIndexOutOfBoundsException(pos + " >= " + totalSize);
//				return null;
//			}
//			
//			if(pos < pCount){
//				return projectMenu.removeModifiableItemAt(pos);
//			}else{
//				return super.removeModifiableItemAt(pos - pCount);
//			}
//		}
//	}
//	}
	
	@Override
	public final boolean insertModifiableItem(final MenuItem item, int pos){
		if(pos == -1){
			return false;
		}
		
		if(pos > 0){//0位是特例，需要从projectMenu中取tail
			return super.insertModifiableItem(item, pos);
		}
		
		boolean isAddedSucc = false;
		MenuItem tail = null;
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				if(pos == insertToLastTail){
					pos = super.getSessionItemsCountInLock();
				}
				
				{
					final int modifiableSize = projectMenu.menuItems.size() - projectMenu.tailCount;
					if(modifiableSize > 0){
						tail = projectMenu.menuItems.elementAt(modifiableSize - 1);
					}
				}
				
				final int shiftIdx = headerStartIdx + pos;
				final int modifiableSize = menuItems.size() - tailCount;
				
				if(shiftIdx > modifiableSize){
					return false;
		//			throw new ArrayIndexOutOfBoundsException(pos + " >= " + modifiableSize);
				}
				
				try{
					menuItems.add(shiftIdx, item);
					item.belongToMenu = this;
					isAddedSucc = true;
				}catch (final Throwable e) {
				}
			}
		}
		
		if(isAddedSucc){
			if(isIncrementMode){
				publishToMobi(ADD_ITEM, item, tail);
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	public final boolean removeModifiableItem(final MenuItem item){
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				final boolean removed = super.removeModifiableItem(item);
				if(removed == false){
					return projectMenu.removeModifiableItem(item);
				}else{
					return removed;
				}
			}
		}
	}
	
	public final Vector<MenuItem> getFlushMenuItems(){
		final Vector<MenuItem> out = new Vector<MenuItem>(projectMenu.menuItems.size() + menuItems.size() + 2);
		
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				appendAllItems(projectMenu.menuItems, out);
				appendAllItems(menuItems, out);
			}
		}
		
		return out;
	}
	
	public final MenuItem searchMenuItem(final String urlLower, final String aliasUrlLower){
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				final MenuItem out = searchMenuItem(projectMenu.menuItems, urlLower, aliasUrlLower);
				if(out != null){
					return out;
				}
				
				return searchMenuItem(menuItems, urlLower, aliasUrlLower);
			}
		}
	}
	
	public final MenuItem searchMenuItemByVoiceCommand(final VoiceCommand voiceCommand){
		synchronized(projectMenu.menuLock){
			synchronized (menuLock) {
				final MenuItem out = searchMenuItemByVoiceCommand(projectMenu.menuItems, voiceCommand);
				if(out != null){
					return out;
				}
				
				return searchMenuItemByVoiceCommand(menuItems, voiceCommand);
			}
		}
	}
			
	private final MenuItem searchMenuItemByVoiceCommand(final Vector<MenuItem> from, final VoiceCommand voiceCommand){
		final int size = from.size();
		for (int j = 0; j < size; j++) {
			final MenuItem item = from.elementAt(j);
			
			if(item.itemType == JarMainMenu.FOLD_TYPE){//不能找folder
				continue;
			}
			
			if(item.itemURL == HCURL.URL_CMD_VOICE_COMMAND){//不能找自己
				continue;
			}
			
			if(voiceCommand.equalsIgnoreCase(item.itemName)){
				return item;
			}
			
			final I18NStoreableHashMapWithModifyFlag i18n = item.i18nName;
			try{
				final Enumeration e = i18n.keys();
				while(e.hasMoreElements()){
					final String oneKey = (String)e.nextElement();
					if(voiceCommand.equalsIgnoreCase((String)i18n.get(oneKey))){
						return item;
					}
				}
			}catch (final NoSuchElementException e) {
			}catch (final Throwable e) {
				e.printStackTrace();
			}
			
		}
		return null;
	}
	
	private final MenuItem searchMenuItem(final Vector<MenuItem> from, final String urlLower, final String aliasUrlLower){
		final int size = from.size();
		for (int j = 0; j < size; j++) {
			final MenuItem item = from.elementAt(j);
			
			final String menuURLLower = ServerUIAPIAgent.getMobiMenuItem_URLLower(item);//menuItems[j][JarMainMenu.ITEM_URL_IDX];
			if(menuURLLower.equals(urlLower) || menuURLLower.equals(aliasUrlLower)){
				return item;
			}
		}
		return null;
	}

	private final void appendAllItems(final Vector<MenuItem> from, final Vector<MenuItem> out) {
		final int size = from.size();
		for (int i = 0; i < size; i++) {
			out.add(from.elementAt(i));
		}
	}
	
	private final void initMenuItemArray() {
		//添加"增加设备"按钮
		final int res_add = 9016;
		final int fold_type = JarMainMenu.FOLD_TYPE;
		
		if(enableQRInMobiMenu){
			final String name = (String)ResourceUtil.get(res_add);
			final int type = fold_type;
			final String image = UIUtil.SYS_ADD_DEVICE_BY_QR_ICON;
			final String url = HCURL.URL_CFG_ADD_DEVICE_BY_QR;
			final I18NStoreableHashMapWithModifyFlag i18nName = ResourceUtil.getI18NByResID(res_add);
			final String listen = ""; 
			final String extend_map = "";
			
			addTail(ServerUIAPIAgent.buildMobiMenuItem(name, type, image, url, i18nName, listen, extend_map));
		}
		
		if(enableWiFiInMobiMenu){
			final String name = (String)ResourceUtil.get(res_add);
			final int type = fold_type;
			final String image = UIUtil.SYS_ADD_DEVICE_BY_WIFI_ICON;
			final String url = HCURL.URL_CFG_ADD_DEVICE_BY_WIFI;
			final I18NStoreableHashMapWithModifyFlag i18nName = ResourceUtil.getI18NByResID(res_add);
			final String listen = ""; 
			final String extend_map = "";
			
			addTail(ServerUIAPIAgent.buildMobiMenuItem(name, type, image, url, i18nName, listen, extend_map));
		}
		
		if(enableVoiceCommand){
			final int res_voice = 9244;
			final String name = (String)ResourceUtil.get(res_voice);
			final int type = fold_type;
			final String image = UIUtil.SYS_VOICE_COMMAND;
			final String url = HCURL.URL_CMD_VOICE_COMMAND;
			final I18NStoreableHashMapWithModifyFlag i18nName = ResourceUtil.getI18NByResID(res_voice);
			final String listen = ""; 
			final String extend_map = "";
			
			addTail(ServerUIAPIAgent.buildMobiMenuItem(name, type, image, url, i18nName, listen, extend_map));
		}
	}
	
	/**
	 * publish to session only.
	 * @param op
	 * @param item
	 * @param itemBefore 位于新增之前的item，null表示位于最前
	 */
	@Override
	public final void publishToMobi(final String op, final MenuItem item, final MenuItem itemBefore){
		switchCoreSS(coreSS, op, item, itemBefore);
	}
}
