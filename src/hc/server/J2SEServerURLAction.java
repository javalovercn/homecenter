package hc.server;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootServerConnector;
import hc.core.data.DataClientAgent;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.server.data.screen.ScreenCapturer;
import hc.server.msb.AirCmdsUtil;
import hc.server.ui.ClientDesc;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.design.AddHarHTMLMlet;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.util.BaseResponsor;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class J2SEServerURLAction implements IHCURLAction {
	private static HCTimer closeIOSLongConnection;
	
	private final static HCTimer buildCloseIOSLongConnection(){
		final int iosMaxBGMinutes = ClientDesc.getAgent().getIOSMaxBGMinutes();
		return new HCTimer("iOSLongConnection", 1000 * 60 * iosMaxBGMinutes, true) {
			@Override
			public void doBiz() {
				L.V = L.O ? false : LogManager.log("force close connection when iOS keep in background for max minutes!");
				RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);
				SIPManager.notifyRelineon(false);
				setEnable(false);
			}
		};
	}

	private final static void clearIOSLongConnectionTimer() {
		if(closeIOSLongConnection != null){
			L.V = L.O ? false : LogManager.log("remove iOS long connection watch timer.");
			HCTimer.remove(closeIOSLongConnection);
		}
	}
	
	private static boolean isKeepaliveEnableOld;
	
	private static void flipIOSBackground(final boolean isBackground){
		L.V = L.O ? false : LogManager.log("client iOS background : [" + isBackground + "]");
		
		if(isBackground){
			if(closeIOSLongConnection != null){
				clearIOSLongConnectionTimer();
			}
			closeIOSLongConnection = buildCloseIOSLongConnection();
		}else{
			clearIOSLongConnectionTimer();
		}

		if(isBackground){
			LogManager.warning("iOS will do nothing when in background!!!");
			
			isKeepaliveEnableOld = KeepaliveManager.keepalive.isEnable();
			if(isKeepaliveEnableOld){
				L.V = L.O ? false : LogManager.log("disable keepalive when iOS in background!");
				KeepaliveManager.keepalive.setEnable(false);
			}
		}else{
			if(isKeepaliveEnableOld){
				L.V = L.O ? false : LogManager.log("enable keepalive when iOS resume from background!");
				KeepaliveManager.resetSendData();
				KeepaliveManager.keepalive.resetTimerCount();
				KeepaliveManager.keepalive.setEnable(true);
			}
		}
	}
	
	@Override
	public boolean doBiz(final HCURL url) {
		
		final String protocal = url.protocal;
		final String elementID = url.elementID;
		
//		L.V = L.O ? false : LogManager.log("goto " + url.url);
		
		if(protocal == HCURL.SCREEN_PROTOCAL){
			if(elementID.equals(HCURL.REMOTE_HOME_SCREEN)){			
				//主菜单模式下，再次进入时，如果不增加如下时间，会导致块上白
//				try{
//					Thread.sleep(2000);
//				}catch (Exception e) {
//				}

				//送回本地的
				final int[] screenSize = ResourceUtil.getSimuScreenSize();
				final int screenWidth = screenSize[0];
				final int screenHeight = screenSize[1];
				{
					//保留旧的传送方式，另增加新的通道，参见E_TRANS_SERVER_CONFIG
					final byte[] homeBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
					final DataClientAgent ss = new DataClientAgent();
					ss.setBytes(homeBS);
					
					ss.setWidth(screenWidth);
					ss.setHeight(screenHeight);
					ContextManager.getContextInstance().send(MsgBuilder.E_SCREEN_REMOTE_SIZE, homeBS, ss.getLength());
				}
				
				ScreenServer.cap = new ScreenCapturer(ClientDesc.getClientWidth(), ClientDesc.getClientHeight(),
						screenWidth, screenHeight);
				
				ScreenServer.pushScreen(ScreenServer.cap);
				final int offY = (screenHeight > ClientDesc.getClientHeight())?(screenHeight - ClientDesc.getClientHeight()):0;
				ScreenServer.cap.refreshRectange(0, offY);//优先发送手机能显示内容
				
				return true;
			}
		}else if(protocal == HCURL.CMD_PROTOCAL){
			if(elementID.equals(HCURL.DATA_CMD_EXIT)){
				if(ScreenServer.popScreen() == false){
//					System.out.println("Receiv Exit , and Notify exit by mobile");
					J2SEContext.notifyExitByMobi();
				}
				
				return true;
			}else if(elementID.equals(HCURL.DATA_CMD_SendPara)){
				final String para1= url.getParaAtIdx(0);
				if(para1 != null && para1.equals(HCURL.DATA_PARA_RIGHT_CLICK)){
					final String value1 = url.getValueofPara(para1);
					if(value1 != null){
						if(value1.equals(HCURL.DATA_PARA_VALUE_CTRL)){
							final int x = Integer.parseInt(url.getValueofPara("x"));
							final int y = Integer.parseInt(url.getValueofPara("y"));

							L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "Ctrl + mouse left click at (" + x + ", " + y + ")");
							
							doClickAt(url, ResourceUtil.getAbstractCtrlKeyCode(), x, y);
							return true;
						}else if(value1.equals(HCURL.DATA_PARA_VALUE_SHIFT)){
							final int x = Integer.parseInt(url.getValueofPara("x"));
							final int y = Integer.parseInt(url.getValueofPara("y"));

							L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "Shift + mouse left click at (" + x + ", " + y + ")");
							
							doClickAt(url, KeyEvent.VK_SHIFT, x, y);
							return true;
						}
					}
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_QUESTION_ID)){
					ServerUIAPIAgent.execQuestionResult(
							url.getValueofPara(HCURL.DATA_PARA_QUESTION_ID), 
							url.getValueofPara(HCURL.DATA_PARA_QUESTION_RESULT));

					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_CLASS)){
					final String v = url.getValueofPara(HCURL.DATA_PARA_CLASS);
					final String className = v;
					HCURL.setToContext(url);
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_PUBLISH_STATUS_ID)){
					final String publishID = url.getValueofPara(HCURL.DATA_PARA_PUBLISH_STATUS_ID);
					ClientDesc.getAgent().set(
							publishID, 
							url.getValueofPara(HCURL.DATA_PARA_PUBLISH_STATUS_VALUE));
					if(publishID.equals(ConfigManager.UI_IS_BACKGROUND)){
						ContextManager.getThreadPool().run(new Runnable() {
							@Override
							public void run() {
								final BaseResponsor responsor = ServerUIUtil.getResponsor();
								responsor.onEvent(ProjectContext.EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND);
								
								if(isIOSForBackgroundCond()){
									flipIOSBackground(ClientDesc.getAgent().isBackground());
								}
							}
						});
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_WIFI_MANAGER)){
//					final String methodName = url.getValueofPara(HCURL.DATA_PARA_WIFI_MANAGER);
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_PROC_ADD_HAR_URL)){
					final String urlHexStr = url.getValueofPara(HCURL.DATA_PARA_PROC_ADD_HAR_URL);
					final byte[] bs = ByteUtil.toBytesFromHexStr(urlHexStr);
					final String urlStr = StringUtil.bytesToString(bs, 0, bs.length);
					
					AddHarHTMLMlet.startAddHTMLHarUI(urlStr);
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_CERT_RECEIVED)){
					final String value1 = url.getValueofPara(para1);
					if(value1.equals(CCoreUtil.RECEIVE_CERT_OK)){
						J2SEContext.isTransedToMobileSize = true;
					}

					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_REPORT_JUMP_EXCEPTION)){
					final String value1 = url.getValueofPara(para1);
					final byte[] bytes = ByteUtil.toBytesFromHexStr(value1);
					try {
						final String jump_url = new String(bytes, IConstant.UTF_8);
						L.V = L.O ? false : LogManager.log("receive mobile jump exception at url : [" + jump_url + "], maybe out of memory.");
						ScreenServer.popScreen();
//						if(ScreenServer.popScreen() == false){
//							J2SEContext.notifyExitByMobi();
//						}
					} catch (final UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_RELOAD_THUMBNAIL)){
					final ScreenCapturer sc = ScreenServer.cap;
					if(sc != null){
						sc.clearCacheThumbnail();
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_FORBID_CERT_UPDATE)){
					final String value1 = url.getValueofPara(para1);
					if(value1.equals(IConstant.TRUE)){
						if(SingleMessageNotify.isShowMessage(SingleMessageNotify.TYPE_DIALOG_CERT) == false){
							new Thread(){
								@Override
								public void run(){
									SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_CERT, true);
									
									//UDP可能导致数据后于disable对话框
									try{
										Thread.sleep(2000);
									}catch (final Exception e) {
									}
	//							if(IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_FORBID_UPDATE_CERT)){
									final ActionListener al = new HCActionListener(new Runnable() {
										@Override
										public void run() {
											SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_CERT, false);
										}
									}, App.getThreadPoolToken());
									final JPanel msgpanel = new JPanel(new BorderLayout());
									final String forbid_update_cert = "[" + (String)ContextManager.getContextInstance().doExtBiz(IContext.BIZ_GET_FORBID_UPDATE_CERT_I18N, null) + "]";
									msgpanel.add(new JLabel("<html><body style=\"width:500\"><strong>" + (String)ResourceUtil.get(IContext.TIP) + "</strong>" +
											"" + StringUtil.replace((String)ResourceUtil.get(9062), "{forbid}", forbid_update_cert)+
											"</body></html>",
											App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEFT), BorderLayout.CENTER);
									App.showCenterPanel(msgpanel, 0, 0, (String)ResourceUtil.get(IContext.TIP), false, null, null, al, al, null, false, true, null, false, true);
//							}
								}
							}.start();
						}
					}

					return true;
				}
			}
		}else if(protocal == HCURL.CFG_PROTOCAL){
			if(elementID.equals(HCURL.ADD_HAR_WIFI)){			
				AddHarHTMLMlet.startAddHTMLHarUI(null);
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						final AddHarHTMLMlet mlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet();
						boolean isNotifyPressStart = false;
						int totalSleep = 0;
						final int sleepMS = 1000;
						final int maxSleepMS = (App.isSimu()?5:30) * sleepMS;
						String url;
						do{
//							final String[] commands = {"hello_1", "hello_world"};
//							WiFiDeviceManager.getInstance().broadcastWiFiAccountAsSSID(commands, "group");
//							WiFiDeviceManager.getInstance().clearWiFiAccountGroup("group");
							url = AirCmdsUtil.getAirCmdsHarUrl(WiFiDeviceManager.getInstance().getSSIDListOnAir());
							
							if(url != null){
								break;
							}
							
							if(isNotifyPressStart == false){
								mlet.setProcessingMessage((String)ResourceUtil.get(9134));
								isNotifyPressStart = true;
							}
							try{
								Thread.sleep(sleepMS);
								totalSleep += sleepMS;
							}catch (final Exception e) {
							}
						}while(url == null && totalSleep < maxSleepMS);
						
						if(url == null && totalSleep >= maxSleepMS){
							mlet.setProcessingMessage((String)ResourceUtil.get(9133));
							mlet.exitButton.setEnabled(true);
							return;
						}
						final String downloadURL = url;
						mlet.getProjectContext().sendQuestion((String)ResourceUtil.get(IContext.CONFIRMATION), downloadURL, null, new Runnable() {
							@Override
							public void run() {
//								System.out.println("----------start download test : " + downloadURL);
								mlet.startAddHarProcess(downloadURL);								
							}
						}, null, null);
					}
				});
				return true;
			}
		}
		return ServerUIUtil.getResponsor().doBiz(url);
	}

	final static Object[] SYS_JRUBY_ENGINE = new Object[2];
	
	/**
	 * 注意：切勿关闭或释放本对象，因为可能被再次或其它系统级使用
	 * @return object_0:HCJRubyEngine; object_1:ProjectContext
	 */
	public static final synchronized Object[] getProjectContextForSystemLevelNoTerminate(){
		if(SYS_JRUBY_ENGINE[0] == null){
			final HCJRubyEngine hcje = new HCJRubyEngine(null, ResourceUtil.getJRubyClassLoader(false));
			final ProjectContext context = new ProjectContext("", "", ContextManager.getThreadPool(), null, null);
			SYS_JRUBY_ENGINE[0] = hcje;
			SYS_JRUBY_ENGINE[1] = context;
		}
		return SYS_JRUBY_ENGINE;
	}
	
	private void doClickAt(final HCURL url, final int mode, final int x, final int y) {
		ScreenCapturer.robot.mouseMove(x, y);
		
		ScreenCapturer.robot.keyPress(mode);

		ScreenCapturer.robot.mousePress(InputEvent.BUTTON1_MASK);
		ScreenCapturer.robot.mouseRelease(InputEvent.BUTTON1_MASK);

		ScreenCapturer.robot.keyRelease(mode);
	}

	public static boolean isIOSForBackgroundCond() {
		return ClientDesc.getAgent().getOS().equals(ProjectContext.OS_IOS);
	}

}
