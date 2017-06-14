package hc.server;

import hc.core.IConstant;
import hc.core.cache.CacheManager;
import hc.core.util.ByteUtil;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;


public class J2SEEClassHelper {
	protected static void dispatch(final J2SESession coreSS, final String className, final byte[] bs, final int offset, final int len){
		final String classPara = ByteUtil.buildString(bs, offset, len, IConstant.UTF_8);
		
		if(className.equals(HCURLUtil.CLASS_ERR_ON_CACHE)){
			//注意：以下要提前，以减少因网络导致不一致。
			HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_ERR_ON_CACHE, classPara);

			//通知客户端可以删除cache，客户端不能自行删除。
			final String softUID = UserThreadResourceUtil.getMobileSoftUID(coreSS);
			LogManager.log("receive ERR_ON_CACHE on project [" + classPara + "].");
			if(CacheManager.removeUIDFrom(classPara, softUID)){
				LogManager.log("remove cache data for [" + classPara + "/" + softUID + "].");
			}
		}else if(className.equals(HCURLUtil.CLASS_MOV_NEW_SERVER)){
			final String softUID = UserThreadResourceUtil.getMobileSoftUID(coreSS);
			CacheManager.removeUIDFrom(softUID);
		}
//		if(className.equals("testClass")){
//			System.out.println("testClass ====>" + ByteUtil.buildString(bs, offset, len, IConstant.UTF_8));
//		}
	}
}
