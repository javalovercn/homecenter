package hc.server;

import hc.core.IConstant;
import hc.core.util.ByteUtil;
import hc.core.util.CUtil;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;

public class J2SEConstant extends IConstant {

	public int getInt(String p) {
		if(p == IConstant.RelayMax){
			return 1024;
		}else if(p == IConstant.IS_J2ME){
			return 0;
		}
		return 20;
	}

	public String getAjax(String url) {
		return HttpUtil.getAjax(url);
	}

	public String getAjaxForSimu(String url, boolean isTCP) {
		return HttpUtil.getAjaxForSimu(url, false);
	}

	public Object getObject(String p) {
		if(p == IConstant.CertKey){
			byte[] keys = null;
			String ck = PropertiesManager.getValue(PropertiesManager.p_CertKey);
			if(ck == null){
				keys = CUtil.INI_CERTKEY.getBytes();
			}else{
				keys = ByteUtil.decodeBase64(ck);
			}
			return keys;
		}else{
			return PropertiesManager.getValue(p);
		}
	}

	public void setObject(String key, Object value) {
		if(key == IConstant.CertKey){
			PropertiesManager.setValue(PropertiesManager.p_CertKey, ByteUtil.encodeBase64((byte[])value));
			PropertiesManager.saveFile();
			return;
		}
	}


}
