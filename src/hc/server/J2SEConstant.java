package hc.server;

import hc.core.IConstant;
import hc.core.util.ByteUtil;
import hc.core.util.CUtil;
import hc.util.PropertiesManager;

public class J2SEConstant extends IConstant {

	@Override
	public int getInt(final String p) {
		if(p.equals(IConstant.RelayMax)){
			return 1024;
		}else if(p.equals(IConstant.IS_J2ME)){
			return 0;
		}
		return 20;
	}

	@Override
	public Object getObject(final String p) {
		if(p.equals(IConstant.CertKey)){
			byte[] keys = null;
			final String ck = PropertiesManager.getValue(PropertiesManager.p_CertKey);
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

	@Override
	public void setObject(final String key, final Object value) {
		if(key.equals(IConstant.CertKey)){
			PropertiesManager.updateCertKey((byte[])value);
			PropertiesManager.saveFile();
			return;
		}
	}


}
