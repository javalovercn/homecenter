package hc.server.ui.design.hpj;

import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.StoreableHashMap;
import hc.core.util.UIUtil;
import hc.util.StoreableHashMapWithModifyFlag;

public class HPMenuItem extends HPNode {
	public String url = "";
	public String imageData = UIUtil.SYS_DEFAULT_ICON;
	public String listener = "";
	public StoreableHashMap extendMap = new StoreableHashMap();
	
	public HPMenuItem(final int type, final String name){
		this(type, name, new StoreableHashMapWithModifyFlag(), HCURL.URL_CMD_EXIT, UIUtil.SYS_DEFAULT_ICON);
	}
	
	public HPMenuItem(final int type, final String name, final StoreableHashMapWithModifyFlag i18nMap, final String url, final String imageData) {
		super(type, name);
		this.i18nMap = i18nMap;
		this.url = url;
		this.imageData = imageData;
	}
	
	@Override
	public String toString(){
		return name + " [" + (url==null?"":url) + "]";
	}
	
	@Override
	public boolean equals(final Object obj){
		if(obj instanceof HPMenuItem){
			final HPMenuItem cp = (HPMenuItem)obj;
			final HCURL hcurl1 = HCURLUtil.extract(url);
			final HCURL hcurl2 = HCURLUtil.extract(cp.url);
			if(name.equals(cp.name) || hcurl1.elementID.equals(hcurl2.elementID)){
				return true;
			}
		}
		return false;
	}
}
