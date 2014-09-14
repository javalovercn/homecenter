package hc.server.ui.design.hpj;

import hc.core.data.ServerConfig;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.StoreableHashMap;

public class HPMenuItem extends HPNode {
	public String url = "";
	public String imageData = ServerConfig.SYS_DEFAULT_ICON;
	public String listener = "";
	public StoreableHashMap extendMap = new StoreableHashMap();
	
	public HPMenuItem(int type, String name){
		this(type, name, HCURL.URL_CMD_EXIT, ServerConfig.SYS_DEFAULT_ICON);
	}
	
	public HPMenuItem(int type, String name, String url, String imageData) {
		super(type, name);
		
		this.url = url;
		this.imageData = imageData;
	}
	
	public String toString(){
		return name + " [" + (url==null?"":url) + "]";
	}
	
	public boolean equals(Object obj){
		if(obj instanceof HPMenuItem){
			HPMenuItem cp = (HPMenuItem)obj;
			HCURL hcurl1 = HCURLUtil.extract(url);
			HCURL hcurl2 = HCURLUtil.extract(cp.url);
			if(name.equals(cp.name) || hcurl1.elementID.equals(hcurl2.elementID)){
				return true;
			}
		}
		return false;
	}
}
