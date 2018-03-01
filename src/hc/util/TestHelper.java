package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.RootBuilder;
import hc.server.DebugThreadPool;
import hc.server.J2SEConstant;
import hc.server.util.J2SERootBuilder;

public class TestHelper {

	public static void initForTester() {
		L.setInWorkshop(true);
		System.setProperty(Constant.DESIGNER_IN_TEST, "true");

		IConstant.propertiesFileName = "hc_config.properties";
		IConstant.serverSide = true;
		RootBuilder.setInstance(new J2SERootBuilder(null));
		IConstant.setInstance(new J2SEConstant());
		ContextManager.setThreadPool(new DebugThreadPool(), App.getRootThreadGroup());

		final Thread t = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(20000);
					} catch (final Exception e) {
					}

					ClassUtil.printThreadStack(null);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

}
