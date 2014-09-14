package hc.server.ui.design;

import hc.core.IConstant;
import hc.core.util.StoreableHashMap;
import hc.server.ui.design.hpj.HCjar;

import java.util.Map;

public class LinkProjectStore extends StoreableHashMap{
	public static final String NO_DEPLOY_TMP_DIR = "";
	
	private final String FIELD_PROJ_ID = "id";
	private final String FIELD_PROJ_REMARK = "Comment";
	private final String FIELD_PROJ_LINK_NAME = "linkName";
	private final String FIELD_PROJ_IS_ACTIVE = "isActive";
	private final String FIELD_PROJ_UPGRADE_URL = "url";
	
	@Deprecated
	private final String FIELD_PROJ_IMPORT_FILE_PATH = "filePath";
	@Deprecated
	private final String FIELD_PROJ_STATUS = "status";
	@Deprecated
	private final String FIELD_PROJ_OP = "op";
	
	private final String FIELD_HAR_FILE = "harFile";
	private final String FIELD_HAR_FILE_PARENT = "harParent";
	private final String FIELD_DEPLOY_TMP_DIR = "dDir";
	private final String FIELD_VERSION = "ver";
	private final String FIELD_DOWNLOADING_VERSION = "download_ver";
	private final String FIELD_DOWNLOADING_POSITION = "download_position";
	private final String FIELD_DOWNLOADING_ERR = "download_err";
	private final String FIELD_PROJ_IS_ROOT = "isRoot";
	private final String FIELD_MENU_NAME = "menuName";
	
	public static final String DEFAULT_UNKOWN_VER = "0.0.1";


	public String getMenuName(){
		return getValueDefault(FIELD_MENU_NAME, "Menu");
	}
	
	/**
	 * 如果没有用户指定自定义链接名，则采用被链接工程的主菜单名
	 * @param mName
	 */
	public void setMenuName(String mName){
		put(FIELD_MENU_NAME, mName);
	}
	
	public String getDownloadingErr(){
		return getValueDefault(FIELD_DOWNLOADING_ERR, "");
	}
	
	public void resetDownloading(){
		setDownloadingErr("");
		setDownloadingPosition(0);
		setDownloadingVersion(DEFAULT_UNKOWN_VER);
	}
	
	public void setDownloadingErr(final String err){
		put(FIELD_DOWNLOADING_ERR, err);
	}
	
	public int getDownloadingPosition(){
		return Integer.parseInt(getValueDefault(FIELD_DOWNLOADING_POSITION, "0"));
	}
	
	public void setDownloadingPosition(int position){
		put(FIELD_DOWNLOADING_POSITION, String.valueOf(position));
	}
	
	public String getDownloadingVer(){
		return getValueDefault(FIELD_DOWNLOADING_VERSION, DEFAULT_UNKOWN_VER);
	}
	
	public void setDownloadingVersion(String version){
		put(FIELD_DOWNLOADING_VERSION, version);
	}
	
	public String getVersion(){
		return getValueDefault(FIELD_VERSION, DEFAULT_UNKOWN_VER);
	}
	
	public void setVersion(String version){
		put(FIELD_VERSION, version);
	}
	
	public String getHarParentDir(){
		return getValueDefault(FIELD_HAR_FILE_PARENT, LinkProjectManager.CURRENT_DIR);
	}
	
	public void setHarParentDir(String parentDir){
		put(FIELD_HAR_FILE_PARENT, parentDir);
	}
	
	public String getDeployTmpDir(){
		return getValueDefault(FIELD_DEPLOY_TMP_DIR, NO_DEPLOY_TMP_DIR);
	}
	
	/**
	 * 存放供JRuby使用的rb文件和jar文件的数字型随机目录
	 * @param dirName
	 */
	public void setDeployTmpDir(String dirName){
		put(FIELD_DEPLOY_TMP_DIR, dirName);
	}
	
	public String getHarFile(){
		return getValueDefault(FIELD_HAR_FILE, "");
	}
	
	public void setHarFile(String harFile){
		put(FIELD_HAR_FILE, harFile);
	}
	
	public String getProjectUpgradeURL(){
		return getValueDefault(FIELD_PROJ_UPGRADE_URL, "");
	}
	
	public void setProjectUpgradeURL(String url){
		put(FIELD_PROJ_UPGRADE_URL, url);
	}
	
	public String getProjectID(){
		return getValueDefault(FIELD_PROJ_ID, "");
	}
	
	public void setProjectID(String id){
		put(FIELD_PROJ_ID, id);
	}
	
	public String getProjectRemark(){
		return getValueDefault(FIELD_PROJ_REMARK, "");
	}
	
	public void setProjectRemark(String remark){
		put(FIELD_PROJ_REMARK, remark);
	}
	
	public String getLinkName(){
		return getValueDefault(FIELD_PROJ_LINK_NAME, "");
	}
	
	public void setLinkName(String name){
		put(FIELD_PROJ_LINK_NAME, name);
	}
	
	public boolean isRoot(){
		return isTrue(FIELD_PROJ_IS_ROOT);
	}

	public void setRoot(boolean main){
		put(FIELD_PROJ_IS_ROOT, (main?IConstant.TRUE:IConstant.FALSE));
	}

	public boolean isActive(){
		return isTrue(FIELD_PROJ_IS_ACTIVE);
	}

	public void setActive(boolean active){
		put(FIELD_PROJ_IS_ACTIVE, (active?IConstant.TRUE:IConstant.FALSE));
	}
	
	public String getLinkScriptName() {
		String link_Name = getLinkName();
		if(link_Name.length() == 0){
			link_Name = getMenuName();
		}
		return link_Name;
	}

	public void copyFrom(Map<String, Object> map) {
		setVersion((String)map.get(HCjar.PROJ_VER));
		setProjectUpgradeURL((String)map.get(HCjar.PROJ_UPGRADE_URL));
		setMenuName(HCjar.getMenuName(map, 0));
	}
}
