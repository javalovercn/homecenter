package hc.server.ui.design;

import hc.core.L;
import hc.core.data.ServerConfig;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.server.ui.ICanvas;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjar;
import hc.util.BaseResponsor;

import java.util.Iterator;
import java.util.Map;

public class JarMainMenu extends MCanvasMenu implements ICanvas {
	public final String[][] items;
	public final int[] itemTypes;
	public final String[] extendMap;
	public static final int itemDescNum = 3;
	private final String menuName;
	public final String menuId;
	private final int menuColNum;
	public final String[] listener;
	public final boolean isRoot;
	
	public static final int ITEM_NAME_IDX = 0;
	public static final int ITEM_IMG_IDX = 1;
	public static final int ITEM_URL_IDX = 2;
	private BaseResponsor baseRe;
	private String linkMenuName;
	
	public JarMainMenu(int menuIdx, Map map, boolean isRoot, BaseResponsor baseRep, String linkMenuName) {
		this.isRoot = isRoot;
		this.baseRe  = baseRep;
		this.linkMenuName = linkMenuName;
		
		menuName = HCjar.getMenuName(map, menuIdx);
		menuId = (String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_ID, menuIdx));
		menuColNum = Integer.parseInt((String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_COL_NUM, menuIdx)));
		int appendMenuItemNum = 0;
		synchronized (ServerUIUtil.LOCK) {
			if(isRoot){
				final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(false);
				while(it.hasNext()){
					if(it.next().isActive()){
						appendMenuItemNum++;
					}
				}
			}
			Object menuItemObj = map.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, menuIdx));
			if(menuItemObj != null){
				final int itemCount = Integer.parseInt((String)menuItemObj);
				items = new String[itemCount + appendMenuItemNum][itemDescNum];
				itemTypes = new int[itemCount + appendMenuItemNum];
				listener = new String[itemCount + appendMenuItemNum];
				extendMap = new String[itemCount + appendMenuItemNum];
				
				if(appendMenuItemNum > 0){
					//添加LinkProject
					Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(false);
					int i = 0;
					while(it.hasNext()) {
						final LinkProjectStore lps = it.next();
						if(!lps.isActive()){
							continue;
						}
						final int itemIdx = (i++);
						items[itemIdx][ITEM_NAME_IDX] = lps.getLinkScriptName();
						itemTypes[itemIdx] = 0;
						items[itemIdx][ITEM_IMG_IDX] = ServerConfig.SYS_FOLDER_ICON;
						items[itemIdx][ITEM_URL_IDX] = HCURL.buildStandardURL(HCURL.MENU_PROTOCAL, lps.getProjectID());
						
						listener[itemIdx] = ""; 
						extendMap[itemIdx] = "";
					}
				}

				final String Iheader = HCjar.replaceIdxPattern(HCjar.MENU_ITEM_HEADER, menuIdx);
				for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
					String header = Iheader + itemIdx + ".";
					
					final int storeIdx = itemIdx + appendMenuItemNum;
					
					items[storeIdx][ITEM_NAME_IDX] = (String)map.get(header + HCjar.ITEM_NAME);
					itemTypes[storeIdx] = Integer.parseInt((String)map.get(header + HCjar.ITEM_TYPE));
					items[storeIdx][ITEM_IMG_IDX] = (String)map.get(header + HCjar.ITEM_IMAGE);
					items[storeIdx][ITEM_URL_IDX] = (String)map.get(header + HCjar.ITEM_URL);
					
					listener[storeIdx] = (String)map.get(header + HCjar.ITEM_LISTENER);
					extendMap[storeIdx] = (String)map.get(header + HCjar.ITEM_EXTENDMAP); 
				}
				
			}else{
				items = new String[0][itemDescNum];
				itemTypes = new int[0];
				listener = new String[0];
				extendMap = new String[0];
			}
		}
	}

	public String[] getIconLabels() {
		String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][ITEM_NAME_IDX];
		}
		return out;
	}

	public String[] getIcons() {
		String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][ITEM_IMG_IDX];
		}
		return out;
	}

	public int getNumCol() {
		return menuColNum;
	}

	public int getNumRow() {
		return (items.length/3 + 1);
	}

	public int getSize() {
		return items.length;
	}

	public String getTitle() {
		return menuName;
	}

	public String[] getURLs() {
		String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][ITEM_URL_IDX];
		}
		return out;
	}

	public boolean isFullMode() {
		return false;
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onPause() {
	}

	@Override
	public void onResume() {
	}

	@Override
	public void onExit() {
		//返回root主菜单
		if(isRoot){
			//当前是root
		}else{
			L.V = L.O ? false : LogManager.log(" exit/back link menu [" + linkMenuName + "]");
			if(baseRe instanceof MobiUIResponsor){
				final MobiUIResponsor mobiUIResponsor = (MobiUIResponsor)baseRe;
				mobiUIResponsor.enterContext(mobiUIResponsor.findRootContextID());
			}
			//千万别执行如下，确保每次手机连接使用同一服务实例，从而共享数据状态
//			backServer = null;
		}
	}

}
