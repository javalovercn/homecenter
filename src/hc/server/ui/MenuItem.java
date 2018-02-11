package hc.server.ui;

import java.awt.image.BufferedImage;
import java.util.Map;

import hc.core.util.ByteUtil;
import hc.core.util.HCURL;
import hc.core.util.StringValue;
import hc.core.util.UIUtil;
import hc.server.ui.design.hpj.HPNode;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;

/**
 * An implementation of an item in a menu. A menu item is essentially a button sitting in a list.
 * <BR><BR>
 * A menu presented on the mobile client is composed of menu items of project level and session level, 
 * items of session level are visible only to owned session and will automatically gone after logout or line-off.
 * <BR><BR>
 * The menu items in [Items Tree] in designer are project level, invoking {@link ProjectContext#getMenuItemBy(String, String)} to get it.
 * @see ProjectContext#addMenuItem(MenuItem)
 * @see ProjectContext#insertMenuItemToProjectLevel(MenuItem, int)
 * @see ProjectContext#insertMenuItemToSessionLevel(MenuItem, int)
 * @see ProjectContext#getMenuItemFromProjectLevel(int)
 * @see ProjectContext#getMenuItemFromSessionLevel(int)
 * @see ProjectContext#getMenuItemsSizeOfProjectLevel()
 * @see ProjectContext#getMenuItemsSizeOfSessionLevel()
 * @see ProjectContext#removeMenuItemFromProjectLevel(int)
 * @see ProjectContext#removeMenuItemFromSessionLevel(int)
 * @since 7.20
 */
public class MenuItem {
	String itemName;
	int itemType;
	String itemImage;
	String itemURL;
	String itemURLLower;
	I18NStoreableHashMapWithModifyFlag i18nName;
	String itemListenerOri;
	final StringValue itemListener = new StringValue();
	String extendMap;
	
	BufferedImage cacheOriImage;
	private BufferedImage settedImg;

	MobiMenu belongToMenu;
	boolean isNeedRefresh;
	boolean isEnable = true;
	
	final String getItemURLLower(){
		if(itemURLLower == null){
			itemURLLower = itemURL.toLowerCase();
		}
		return itemURLLower;
	}
	
	/**
	 * the command scheme.
	 * @see #MenuItem(String, String, String, BufferedImage, String)
	 * @since 7.20
	 */
	public static final String CMD_SCHEME = HCURL.CMD_PROTOCAL;
	
	/**
	 * the form scheme.
	 * @see #MenuItem(String, String, String, BufferedImage, String)
	 * @since 7.20
	 */
	public static final String FORM_SCHEME = HCURL.FORM_PROTOCAL;
	
	/**
	 * the controller scheme.
	 * @since 7.20
	 */
	public static final String CONTROLLER_SCHEME = HCURL.CONTROLLER_PROTOCAL;
	
	/**
	 * the screen scheme,
	 * @since 7.20
	 */
	public static final String SCREEN_SCHEME = HCURL.SCREEN_PROTOCAL;
	
	/**
	 * the configuration scheme,
	 * @since 7.20
	 */
	private static final String CFG_SCHEME = HCURL.CFG_PROTOCAL;//注意：此cfg不是cmd://config
	
	MenuItem(final String name, final int type, final String image, final String url, 
			final I18NStoreableHashMapWithModifyFlag i18nName, final String listener, final String extendMap){
		this.itemName = name;
		this.itemType = type;
		this.itemImage = image;
		this.itemURL = url;
		this.i18nName = i18nName;
		setScripts(listener);
		this.extendMap = extendMap;
	}
	
	/**
	 * change the scripts for current menu item.
     * <BR><BR>
     * <STRONG>Important :</STRONG><BR>
     * the scripts of menu item works in session level, no matter which is added to set of project level or session level.
	 * @param scripts
	 * @see ProjectContext#insertMenuItemToProjectLevel(MenuItem, int)
	 * @see ProjectContext#insertMenuItemToSessionLevel(MenuItem, int)
	 * @since 7.20
	 */
	public void setScripts(final String scripts){
		itemListenerOri = scripts;
		synchronized (itemListener) {
			itemListener.value = scripts;
		}
	}
	
	/**
	 * return the scripts for current item.
	 * @return
	 * @since 7.20
	 */
	public String getScripts(){
		return itemListenerOri;
	}
	
	/**
	 * creates a <code>MenuItem</code> with the specified parameters.
	 * <BR><BR>
	 * to set internationalization texts, see {@link #setText(String[], String[])}
	 * @param text the text of the item.
	 * @param scheme one of {@link #CMD_SCHEME}, {@link #FORM_SCHEME}.
	 * @param elementID for example, a menu item for Mlet is "<code>form://MyForm</code>", then the elementID is "<code>MyForm</code>".
	 * @param icon the best image size is 128 X 128 for current version.
	 * @param scripts the response scripts for the menu item.
	 * @since 7.20
	 */
	public MenuItem(final String text, final String scheme, final String elementID, final BufferedImage icon, final String scripts){
		this(text, getTypeFromScheme(scheme), "", checkAndBuild(scheme, elementID), 
				new I18NStoreableHashMapWithModifyFlag(), scripts, "");
		setIcon(icon);
	}
	
	/**
	 * if the MenuItem is set to disable, then the <code>icon</code> will be gray on mobile immediately.
	 * <BR><BR>
	 * if the MenuItem is added in project level, then the change will apply to all mobile clients.
	 * <BR><BR>
	 * actions on a disabled MenuItem will be invalid.
	 * @param b
	 * @see ProjectContext#insertMenuItemToProjectLevel(MenuItem, int)
	 * @since 7.20
	 */
	public void setEnabled(final boolean b) {
		if(isEnable != b){
			this.isEnable = b;
			
			if(belongToMenu != null){
				belongToMenu.notifyModify(this);
			}
		}
	}
	
	/**
	 * true means the MenuItem is enable; false means disable.
	 * @return
	 * @since 7.20
	 */
	public boolean isEnabled() {
		return this.isEnable;
	}
	
	private static int getTypeFromScheme(final String scheme){
		if(CMD_SCHEME.equals(scheme)){
			return HPNode.TYPE_MENU_ITEM_CMD;
		}else if(FORM_SCHEME.equals(scheme)){
			return HPNode.TYPE_MENU_ITEM_FORM;
		}
		return 0;
//		public static final int TYPE_MENU_ITEM_CMD = 1 | MASK_MENU_ITEM;
//		public static final int TYPE_MENU_ITEM_SCREEN = 2 | MASK_MENU_ITEM;
//		public static final int TYPE_MENU_ITEM_CONTROLLER = 3 | MASK_MENU_ITEM;
//		public static final int TYPE_MENU_ITEM_FORM = 4 | MASK_MENU_ITEM;
	}
	
	private static String checkAndBuild(final String scheme, final String elementID){
		if(CMD_SCHEME.equals(scheme) || FORM_SCHEME.equals(scheme)){
		}else{
			throw new IllegalArgumentException("scheme must be one of [" + CMD_SCHEME + "], [" + FORM_SCHEME + "].");
		}
		
		return HCURL.buildStandardURL(scheme, elementID);
	}
	
	/**
	 * 
	 * if the MenuItem is displayed on mobile, then it will be refreshed.
	 * <BR><BR>
	 * if the MenuItem is added in project level, then the change will apply to all mobile clients.
	 * <BR><BR>
	 * <STRONG>Important</STRONG> : <BR>
	 * if there is no match in map, which is put by {@link #setText(String[], String[])}, {@link #setText(Map)} or designer, 
	 * then the <code>text</code> is used for menu item.
	 * @param text
	 * @see #setText(String[], String[])
	 * @since 7.20
	 */
	public void setText(final String text){
		if(text == null || text.equals(itemName)){
			return;
		}
		
		itemName = text;
		
		if(belongToMenu != null){
			belongToMenu.notifyModify(this);
		}
	}
	
	/**
	 * return the text which is set.
	 * @return
	 * @see #setText(String)
	 */
	public String getText(){
		return itemName;
	}
	
	/**
	 * if the MenuItem is displayed on mobile, then it will be refreshed.
	 * <BR><BR>
	 * if the MenuItem is added in project level, then the change will apply to all mobile clients.
	 * <BR><BR>
	 * for example, <code>locales</code> is {"en-US", "fr-FR"}, <code>texts</code> is {"Hello", "Bonjour"}.
	 * @param locales the array for locale
	 * @param texts the array for text
	 * @return false means <code>locales</code> is null, <code>texts</code> is null or array lengths are not equal.
	 * @see #setText(Map)
	 * @see #setText(String)
	 * @since 7.20
	 */
	public boolean setText(final String[] locales, final String[] texts){
		if(locales == null || texts == null){
			return false;
		}
		
		final int locSize = locales.length;
		final int textSize = texts.length;
		
		if(locSize != textSize){
			return false;
		}
		
		for (int i = 0; i < locSize; i++) {
			i18nName.put(locales[i], texts[i]);
		}
		
		if(belongToMenu != null){
			belongToMenu.notifyModify(this);
		}
		
		return true;
	}
	
	/**
	 * if the MenuItem is displayed on mobile, then it will be refreshed.
	 * <BR><BR>
	 * if the MenuItem is added in project level, then the change will apply to all mobile clients.
	 * <BR><BR>
	 * for example, <code>map</code> is {"en-US" -&gt; "Hello", "fr-FR" -&gt; "Bonjour"}.
	 * @param map the map text for key locales.
	 * @return false means <code>map</code> is null
	 * @see #setText(String[], String[])
	 * @since 7.46
	 */
	public boolean setText(final Map map){
		if(map == null){
			return false;
		}
		
		i18nName.putAll(map);
		
		if(belongToMenu != null){
			belongToMenu.notifyModify(this);
		}
		
		return true;
	}
	
	/**
	 * set icon of MenuItem.<BR><BR>
	 * if the MenuItem is displayed on mobile, then it will be refreshed immediately.
	 * <BR><BR>
	 * if the MenuItem is added in project level, then the change will apply to all mobile clients.
	 * <BR><BR>
	 * if the MenuItem is set to disable, then the <code>icon</code> will be gray on mobile immediately.
	 * @param icon the best image size is 128 X 128 for current version.
	 * @since 7.20
	 */
	public void setIcon(BufferedImage icon){
		if(icon == null){
			throw new IllegalArgumentException("icon of MenuItem is null.");//会导致后续异常发生，所以必须
		}
		
		if(icon == settedImg){
			return;
		}
		
		settedImg = icon;
		cacheOriImage = null;
		icon = ResourceUtil.standardMenuIconForAllPlatform(icon, UIUtil.ICON_MAX, false);
		
		final HCByteArrayOutputStream byteArrayOutputStream = new HCByteArrayOutputStream();
		byteArrayOutputStream.reset(ByteUtil.byteArrayCacher.getFree(icon.getWidth() * 2 * icon.getHeight() * 2), 0);
		itemImage = ServerUIUtil.imageToBase64(icon, byteArrayOutputStream);
		ByteUtil.byteArrayCacher.cycle(byteArrayOutputStream.buf);
		
		if(belongToMenu != null){
			belongToMenu.notifyChangeIcon(this);
		}
	}
	
	/**
	 * the instance of BufferedImage which is set.
	 * @return
	 * @since 7.20
	 */
	public BufferedImage getIcon(){
		return settedImg;
	}
	
}
