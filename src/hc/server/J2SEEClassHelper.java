package hc.server;

import hc.core.IConstant;
import hc.core.util.ByteUtil;
import hc.core.util.HCURLUtil;
import hc.server.ui.design.J2SESession;


public class J2SEEClassHelper {
	protected static void dispatch(final J2SESession coreSS, final String className, final byte[] bs, final int offset, final int len){
		final String classPara = ByteUtil.buildString(bs, offset, len, IConstant.UTF_8);
		
		if(className.equals(HCURLUtil.CLASS_MOV_NEW_SERVER)){
			coreSS.isNeedRemoveCacheLater = true;
		}
//		if(className.equals("testClass")){
//			System.out.println("testClass ====>" + ByteUtil.buildString(bs, offset, len, IConstant.UTF_8));
//		}
	}
}
