package hc.server.ui.design.hpj;

import hc.core.util.HCURL;
import hc.server.ui.design.LinkProjectManager;
import hc.server.util.ContextSecurityConfig;

public class HPProject extends HPNode{
	public static final String DEFAULT_VER = "1.0";
	public static final String HAR_EXT = "har";
	public static final String HAD_EXT = "had";
	
	public String id, ver, upgradeURL, contact, copyright, desc, license, styles;
	public ContextSecurityConfig csc;
	
	public static String convertProjectIDFromName(String name){
		return LinkProjectManager.buildSysProjID();
	}

	public HPProject(int type, String name, String id, String ver, String url,
			String contact, String copyright, String desc, String license,
			ContextSecurityConfig csc, String styles) {
		super(type, name);
		
		this.id = id;
		this.ver = ver;
		this.upgradeURL = url;
		this.contact = contact;
		this.copyright = copyright;
		this.desc = desc;
		this.license = license;
		
		this.csc = csc;
		this.styles = styles;
	}
	
	public String toString(){
		return name + ", ver:" + ver;//, ID : " + id + "
	}
	
	public boolean equals(Object obj){
		if(obj instanceof HPProject){
			HPProject cp = (HPProject)obj;
			return cp.id.equals(id);
		}
		return false;
	}
	
	@Override
	public String validate(){
		if(id.equals(HCURL.ROOT_MENU)){
			return "<strong>root</strong> is system reserved ID.";
		}
		
		upgradeURL = upgradeURL.trim();
		
		if(upgradeURL.length() > 0){
			if(!upgradeURL.endsWith(HAD_EXT)){
				return "upgrade url must end with <strong>" + HAD_EXT +  "</strong>, not "+HAR_EXT+" file or other.";
			}else if(!upgradeURL.startsWith("http")){//支持https
				return "upgrade url must start with <strong>http</strong>.";
			}
		}
		return null;
	}
}
