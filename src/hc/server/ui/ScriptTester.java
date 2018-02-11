package hc.server.ui;

import hc.App;

public class ScriptTester {
	private static ThreadGroup threadToken = App.getThreadPoolToken();
	
	public static void doNothing(){
	}
	
//	public static void jumpToBindHTMLMlet(final Mlet fromMlet){
//		final BufferedImage[] okImage = new BufferedImage[1];
//		final BufferedImage[] cancelImage = new BufferedImage[1];
//		
//		final BindRobotSource source = (BindRobotSource)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
//			@Override
//			public Object run() throws Throwable {
//				okImage[0] = ImageSrc.OK_44_ICON;
//				cancelImage[0] = ImageSrc.CANCEL_44_ICON;
//				final BaseResponsor resp = ServerUIUtil.getResponsor();
//				final MobiUIResponsor mobiResp = (MobiUIResponsor)resp;
//				return mobiResp.bindRobotSource;
//			}
//		}, threadToken);
//		
//		
//		final Boolean[] waitLock = {false};
//		final BindHTMLMlet mlet = new BindHTMLMlet(source, threadToken, okImage[0], cancelImage[0], "OK", "Cancel", false, 
//				"Robots", "Converter", "Devices", waitLock, "empty");
//		fromMlet.goMlet(mlet, BindHTMLMlet.class.getName(), false);
//	}
}
