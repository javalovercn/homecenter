package hc.server.ui.design;

import hc.core.L;
import hc.core.sip.SIPManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.server.data.screen.PNGCapturer;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.ICanvas;
import hc.server.ui.MenuItem;
import hc.server.ui.MobiMenu;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SessionMobiMenu;
import hc.server.ui.design.hpj.HCjar;
import hc.util.I18NStoreableHashMapWithModifyFlag;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * 注意：一个实例能同时服务多个Session
 */
public class JarMainMenu extends MCanvasMenu implements ICanvas {
	private final HCByteArrayOutputStream baos = ServerUIUtil.buildForMaxIcon();//buildJcip时synchronzied
	public static final int itemDescNum = 3;
	public final String menuId;
	private final int menuColNum;
	public final boolean isRoot;
	public final MobiMenu projectMenu;
	public static final int ITEM_NAME_IDX = 0;
	public static final int ITEM_IMG_IDX = 1;
	public static final int ITEM_URL_IDX = 2;
	private final MobiUIResponsor baseRe;
	public final String linkName, linkOrProjectName;
	private final int menuIdx;
	final Map<String, Object> map;
	final I18NStoreableHashMapWithModifyFlag projNameI18nMap;
	
	public JarMainMenu(final int menuIdx, final Map<String, Object> map, final boolean isRoot, final MobiUIResponsor baseRep, 
			final String linkMenuName, final ProjResponser resp) {
		super((String)map.get(HCjar.PROJ_ID), resp);
		
		projectMenu = new MobiMenu(projectID, resp);
		this.isRoot = isRoot;
		this.baseRe  = baseRep;
		this.linkName = linkMenuName;
		this.linkOrProjectName = (linkMenuName == null || linkMenuName.length() == 0)?(String)map.get(HCjar.PROJ_NAME):linkMenuName;
		
		//menuName = linkMenuName;//HCjar.getMenuName(map, menuIdx);
		projNameI18nMap = HCjar.buildI18nMapFromSerial((String)map.get(HCjar.PROJ_I18N_NAME));
		
		menuId = (String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_ID, menuIdx));
		menuColNum = Integer.parseInt((String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_COL_NUM, menuIdx)));
		this.menuIdx = menuIdx;
		
		this.map = map;
		
		synchronized (ServerUIUtil.LOCK) {
			initMenuItemArray();
		}
		
	}
	
	@Override
	public final String toString(){
		return this.getClass().getSimpleName() + ":" + ((linkName!=null&& linkName.length()>0)?linkName:linkOrProjectName);
	}
	
	public static final int FOLD_TYPE = 0;

	private final void initMenuItemArray() {
		boolean hasMenuItemsAndActive = false;//新增按钮及子工程的文件夹数量
		if(isRoot){
			final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(false);
			while(it.hasNext()){
				final LinkProjectStore lps = it.next();
				if( hasMenuItemsAndActive(lps)){
					hasMenuItemsAndActive = true;
					break;
				}
			}
		}
		final Object menuItemObj = map.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, menuIdx));
		
		if(menuItemObj != null){
			final int itemCount = Integer.parseInt((String)menuItemObj);
			
			if(hasMenuItemsAndActive){
				//添加LinkProject文件夹
				final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(false);
				while(it.hasNext()) {
					final LinkProjectStore lps = it.next();
					if(hasMenuItemsAndActive(lps)){
					}else{
						LogManager.warning("there is no menu item in [" + lps.getProjectID() + "], skip build folder for [" + lps.getProjectID() + "].");
						//如果工程内没有菜单项，则不显示。含事件或未来其它
						continue;
					}
					final Map<String, Object> subProjmap = baseRe.getProjResponser(lps.getProjectID()).map;
					projectMenu.addFolderItem(subProjmap, lps);
				}
			}

			final String Iheader = HCjar.replaceIdxPattern(HCjar.MENU_ITEM_HEADER, menuIdx);
			for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
				final String header = Iheader + itemIdx + ".";
				
				final String name = (String)map.get(header + HCjar.ITEM_NAME);
				final int type = Integer.parseInt((String)map.get(header + HCjar.ITEM_TYPE));
				final String image = (String)map.get(header + HCjar.ITEM_IMAGE);
				final String url = (String)map.get(header + HCjar.ITEM_URL);
				final I18NStoreableHashMapWithModifyFlag i18nName = HCjar.buildI18nMapFromSerial((String)map.get(header + HCjar.ITEM_I18N_NAME));
				final String listen = (String)map.get(header + HCjar.ITEM_LISTENER);
				final String extend_map = (String)map.get(header + HCjar.ITEM_EXTENDMAP); 
				
				projectMenu.addModifiableItem(ServerUIAPIAgent.buildMobiMenuItem(name, type, image, url, i18nName, listen, extend_map));
			}
		}
	}

	public final boolean hasMenuItemsAndActive(final LinkProjectStore lps) {
		return lps.isActive() && LinkProjectManager.hasMenuItemNumInProj(map);
	}
	
	public final void appendProjToMenuItemArray(final Map<String, Object>[] maps, final int mapSize, final ArrayList<LinkProjectStore> appendLPS){
		//将增量菜单更新到全部联机Session
		for (int i = 0; i < appendLPS.size(); i++) {
			final LinkProjectStore lps = appendLPS.get(i);
			final String lps_projectID = lps.getProjectID();
			
			Map<String, Object> map = null;
			for (int j = 0; j < mapSize; j++) {
				final Map<String, Object> tmpMap = maps[j];
				if(lps_projectID.equals(tmpMap.get(HCjar.PROJ_ID))){
					map = tmpMap;
					break;
				}
			}
			
			projectMenu.addFolderItem(map, lps);
		}
	}
	
	@Override
	public final String getIconLabel(final J2SESession coreSS, final MenuItem menuItem){
		final String mobileLocale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);

		final I18NStoreableHashMapWithModifyFlag storeMap = ServerUIAPIAgent.getMobiMenuItem_I18nName(menuItem);
		if(storeMap != null){
			try{
				final String mapLang = ProjectContext.matchLocale(mobileLocale, storeMap);
				if(mapLang != null){
					return mapLang;
				}
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		return ServerUIAPIAgent.getMobiMenuItem_Name(menuItem);//items[i][ITEM_NAME_IDX];//缺省名字
	}

	@Override
	public final String[] getIconLabels(final J2SESession coreSS, final Vector<MenuItem> menuItems) {
		
		final String[] out = new String[menuItems.size()];
		for (int i = 0; i < out.length; i++) {
			final MenuItem menuItem = menuItems.elementAt(i);
			out[i] = getIconLabel(coreSS, menuItem);
		}
		return out;
	}
	
	public final String getBitmapBase64ForMobile(final J2SESession coreSS, final BufferedImage bi, final String unChangedBase64){
		final boolean menuTrueColor = UserThreadResourceUtil.getMobileAgent(coreSS).isMenuTrueColor();
		
		if(unChangedBase64 != null && (menuTrueColor || SIPManager.isOnRelay(coreSS.hcConnection) == false)){
			return unChangedBase64;
		}
		
		if(menuTrueColor){
			return ServerUIUtil.imageToBase64(bi, baos);
		}
		
		final int mobileMask = PNGCapturer.getUserMobileColorMask(coreSS);
		final int height = bi.getHeight();
		final int width = bi.getWidth();
		final int pngSize = height * width;
		final int pngDataLen = pngSize;
		
		final BufferedImage transImg;
		final int[] base64PngData = coreSS.base64PngData;
		synchronized (base64PngData) {
			bi.getRGB(0, 0, width, height, base64PngData, 0, width);
			
			for (int i = 0; i < pngDataLen; i++) {
				final int c = base64PngData[i];
				if(c != 0xFF000000){//纯黑保持不变
					base64PngData[i] = c | mobileMask;
				}
			}
			
			transImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			transImg.setRGB(0, 0, width, height, base64PngData, 0, width);
		}
		
		return ServerUIUtil.imageToBase64(transImg, baos);
	}

	@Override
	public final String[] getIcons(final J2SESession coreSS, final Vector<MenuItem> menuItems) {
		final int size = menuItems.size();
		final String[] out = new String[size];
		for (int i = 0; i < size; i++) {
			final MenuItem item = menuItems.elementAt(i);
			final String imgData = getIcon(coreSS, item);
			out[i] = imgData;
		}
		return out;
	}

	@Override
	public final String getIcon(final J2SESession coreSS, final MenuItem item) {
		final SessionMobiMenu sessionMobiMenu = coreSS.getMenu(projectID);
		final String imgData = ServerUIAPIAgent.getMobiMenuItem_Image(this, coreSS, item, sessionMobiMenu.targetMobileIconSize);//items[i][ITEM_IMG_IDX];
		return imgData;
	}

	@Override
	public final int getNumCol() {
		return menuColNum;
	}

	@Override
	public final int getNumRow(final J2SESession coreSS, final Vector<MenuItem> menuItems) {
		return (menuItems.size()/3 + 1);
	}

	@Override
	public final int getSize(final J2SESession coreSS, final Vector<MenuItem> menuItems) {
		return menuItems.size();
	}
	
	@Override
	public final String getTitle(final J2SESession coreSS) {
		//优先返回linkName
		if(linkName != null && linkName.length() > 0){
			return linkName;
		}
		
		final String mobileLocale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
		final String mapLang = ProjectContext.matchLocale(mobileLocale, projNameI18nMap);
		if(mapLang != null){
			return mapLang;
		}else{
			return linkOrProjectName;
		}
	}
	
	@Override
	public final String getURL(final J2SESession coreSS, final MenuItem menuItem) {
		return ServerUIAPIAgent.getMobiMenuItem_URL(menuItem);
	}

	@Override
	public final String[] getURLs(final J2SESession coreSS, final Vector<MenuItem> menuItems) {
		final String[] out = new String[menuItems.size()];
		for (int i = 0; i < out.length; i++) {
			final MenuItem item = menuItems.elementAt(i);
			out[i] = getURL(coreSS, item);//items[i][ITEM_URL_IDX];
		}
		return out;
	}

	@Override
	public final boolean isFullMode() {
		return false;
	}

	@Override
	public final void onStart() {
	}

	@Override
	public final void onPause() {
	}

	@Override
	public final void onResume() {
	}

	@Override
	public final void onExit() {
		//返回root主菜单
		if(isRoot){
			//当前是root
		}else{
			LogManager.log(ILog.OP_STR + "exit/back menu [" + linkOrProjectName + "]");
//			LogManager.log(ScreenCapturer.OP_STR + "exit/back project : [" + projectID + "]");//可能引起退出工程岐义
			
			baseRe.enterContext((J2SESession)SessionThread.getWithCheckSecurityX(), baseRe.findRootContextID());
			//千万别执行如下，确保每次手机连接使用同一服务实例，从而共享数据状态
//			backServer = null;
		}
	}

}
