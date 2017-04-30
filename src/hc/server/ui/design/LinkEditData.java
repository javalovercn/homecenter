package hc.server.ui.design;


public class LinkEditData {
	LinkProjectStore lps;
	String filePath;
	
	/**
	 * 用户手工安装，用新版本替换旧版本
	 */
	boolean isUpgrade = false;
	int status;
	int op;
}
