package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.server.DebugThreadPool;
import hc.server.J2SEConstant;

public class TestHelper {

	public static void initForTester() {
		L.setInWorkshop(true);
		System.setProperty(Constant.DESIGNER_IN_TEST, "true");
		
		IConstant.propertiesFileName = "hc_config.properties";
		IConstant.setInstance(new J2SEConstant());
		ContextManager.setThreadPool(new DebugThreadPool(), App.getRootThreadGroup());
	}

}
