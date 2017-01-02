package hc.server.util;

import hc.core.util.CCoreUtil;
import hc.util.ResourceUtil;

import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class BCProvider {
	private static Provider bcProvider;
	static boolean isAdded = false;

	public static final String BC_PROVIDER = "BC";
	
	public final static Provider getBCProvider() {
		ResourceUtil.checkHCStackTraceInclude(null, null);
		synchronized (BCProvider.class) {
			if(bcProvider == null){
				bcProvider = new BouncyCastleProvider();
			}
		}
		return bcProvider;
	}

	/**
	 * 注意：不推荐将bc加入到Security的provider中，建议直接将provider作为参数传入
	 */
	public static void addBCProvider(){
		if(BCProvider.isAdded == false){
			BCProvider.isAdded = true;
			CCoreUtil.checkAccess();
			Security.addProvider(getBCProvider());
		}
	}

}
