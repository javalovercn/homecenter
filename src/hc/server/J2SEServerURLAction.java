package hc.server;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.data.DataClientAgent;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.RecycleRes;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.core.util.WiFiDeviceManager;
import hc.server.data.screen.ScreenCapturer;
import hc.server.msb.AirCmdsUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.MenuItem;
import hc.server.ui.ProjectContext;
import hc.server.ui.QuestionParameter;
import hc.server.ui.ResParameter;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.design.AddHarHTMLMlet;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.util.VoiceCommand;
import hc.util.BaseResponsor;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Robot;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class J2SEServerURLAction implements IHCURLAction {
	
	@Override
	public boolean doBiz(final CoreSession coreSS, final HCURL url) {
		
		final String protocal = url.protocal;
		
//		LogManager.log("goto " + url.url);
		
		final J2SESession j2seCoreSS = (J2SESession)coreSS;
		if(protocal == HCURL.SCREEN_PROTOCAL){
			if(HCURL.REMOTE_HOME_SCREEN.equals(url.getElementIDLower())){
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
					coreSS.context.send(MsgBuilder.E_SCREEN_REMOTE_SIZE, homeBS, ss.getLength());
				}
				
				final int mobileWidth = UserThreadResourceUtil.getMobileWidthFrom(j2seCoreSS);
				final int mobileHeight = UserThreadResourceUtil.getMobileHeightFrom(j2seCoreSS);
				
				final String screenID = ServerUIAPIAgent.buildScreenID(MultiUsingManager.NULL_PROJECT_ID, HCURL.URL_HOME_SCREEN);
				MultiUsingManager.enter(j2seCoreSS, screenID, HCURL.URL_HOME_SCREEN);
				
				j2seCoreSS.cap = new ScreenCapturer(j2seCoreSS, mobileWidth,
						mobileHeight, screenWidth, screenHeight);
				
				ScreenServer.pushScreen(j2seCoreSS, j2seCoreSS.cap);
				final int offY = (screenHeight > mobileHeight)?(screenHeight - mobileHeight):0;
				j2seCoreSS.cap.refreshRectange(0, offY);//优先发送手机能显示内容
				
				return true;
			}
		}else if(protocal == HCURL.CMD_PROTOCAL){
			final String elementID = url.elementID;
			
			if(HCURL.DATA_CMD_EXIT.equals(url.getElementIDLower())){
				if(ScreenServer.popScreen(j2seCoreSS) == false){
//					System.out.println("Receiv Exit , and Notify exit by mobile");
					J2SEContext.notifyExitByMobi(j2seCoreSS);
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

							LogManager.log(ScreenCapturer.OP_STR + "Ctrl + mouse left click at (" + x + ", " + y + ")");
							
							doClickAt(url, ResourceUtil.getAbstractCtrlKeyCode(), x, y);
							return true;
						}else if(value1.equals(HCURL.DATA_PARA_VALUE_SHIFT)){
							final int x = Integer.parseInt(url.getValueofPara("x"));
							final int y = Integer.parseInt(url.getValueofPara("y"));

							LogManager.log(ScreenCapturer.OP_STR + "Shift + mouse left click at (" + x + ", " + y + ")");
							
							doClickAt(url, KeyEvent.VK_SHIFT, x, y);
							return true;
						}
					}
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_QUESTION_ID)){
					ServerUIAPIAgent.execQuestionResult(
							j2seCoreSS, 
							url.getValueofPara(HCURL.DATA_PARA_QUESTION_ID), url.getValueofPara(HCURL.DATA_PARA_QUESTION_RESULT));

					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_DISMISS_QUES_DIALOG_BY_BACK)){
					final int questionID = Integer.parseInt(url.getValueofPara(HCURL.DATA_PARA_DISMISS_QUES_DIALOG_BY_BACK));
					
					final ResParameter parameter = ServerUIAPIAgent.removeQuestionDialogFromMap(j2seCoreSS, questionID, true);
					if(parameter != null){
						if(parameter instanceof QuestionParameter){
							ServerUIAPIAgent.execQuestionResult(j2seCoreSS, (QuestionParameter)parameter, 
									questionID, ServerUIAPIAgent.QUESTION_CANCEL);
						}
						if(L.isInWorkshop){
							LogManager.log("cancel question/dialog [" + questionID + "] by back key.");
						}
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_CLASS)){
//					final String v = url.getValueofPara(HCURL.DATA_PARA_CLASS);
//					final String className = v;
					HCURL.setToContext(coreSS, url);
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_PUBLISH_STATUS_ID)){
					final String publishID = url.getValueofPara(HCURL.DATA_PARA_PUBLISH_STATUS_ID);
					UserThreadResourceUtil.getMobileAgent(j2seCoreSS).set(
							publishID, 
							url.getValueofPara(HCURL.DATA_PARA_PUBLISH_STATUS_VALUE));
					if(publishID.equals(ConfigManager.UI_IS_BACKGROUND)){
						final BaseResponsor responsor = ServerUIUtil.getResponsor();
						if(responsor != null){//exit时，会出现null
							responsor.onEvent(j2seCoreSS, ProjectContext.EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND);
						}
//								if(isIOSForBackgroundCond()){
//									flipIOSBackground(ServerUIAPIAgent.getMobileAgent(j2seCoreSS).isBackground());
//								}
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_WIFI_MANAGER)){
//					final String methodName = url.getValueofPara(HCURL.DATA_PARA_WIFI_MANAGER);
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_PROC_ADD_HAR_URL)){
					final String urlHexStr = url.getValueofPara(HCURL.DATA_PARA_PROC_ADD_HAR_URL);
					final byte[] bs = ByteUtil.toBytesFromHexStr(urlHexStr);
					final String urlStr = StringUtil.bytesToString(bs, 0, bs.length);
					
					AddHarHTMLMlet.startAddHTMLHarUI(j2seCoreSS, urlStr);
					return true;
				}else if(HCURL.DATA_PARA_VOICE_COMMANDS.equals(para1)){
					final String voiceCommands = VoiceCommand.format(url.getValueofPara(HCURL.DATA_PARA_VOICE_COMMANDS));
					final VoiceCommand vc = new VoiceCommand(voiceCommands);
					final MenuItem out = j2seCoreSS.searchMenuItemByVoiceCommand(vc);
					if(out != null){
						final String itemURL = ServerUIAPIAgent.getMobiMenuItem_URL(out);
						LogManager.log("execute [" + itemURL + "] by voice command [" + voiceCommands + "].");
						ServerUIAPIAgent.goInSysThread(j2seCoreSS, out.belongToMenu.resp.context, itemURL);
					}else{
						ServerUIAPIAgent.sendOneMovingMsg(coreSS, StringUtil.replace((String)ResourceUtil.get(coreSS, 9245), "{voice}", voiceCommands));
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_CERT_RECEIVED)){
					final String value1 = url.getValueofPara(para1);
					if(value1.equals(CCoreUtil.RECEIVE_CERT_OK)){
						j2seCoreSS.isTransedCertToMobile = true;
					}

					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_REPORT_JUMP_EXCEPTION)){
					final String value1 = url.getValueofPara(para1);
					final byte[] bytes = ByteUtil.toBytesFromHexStr(value1);
					try {
						final String jump_url = new String(bytes, IConstant.UTF_8);
						LogManager.log("receive mobile jump exception at url : [" + jump_url + "], maybe out of memory.");
						ScreenServer.popScreen(j2seCoreSS);
//						if(ScreenServer.popScreen() == false){
//							J2SEContext.notifyExitByMobi();
//						}
					} catch (final UnsupportedEncodingException e) {
						ExceptionReporter.printStackTrace(e);
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_RELOAD_THUMBNAIL)){
					final ScreenCapturer sc = j2seCoreSS.cap;
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
								public void run(){//显示手机端开启阻击证书更新的通知
									if(ResourceUtil.isNonUIServer() || ResourceUtil.isDisableUIForTest()){//为Demo时，不显示UI界面
										return;
									}
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
									final String forbid_update_cert = "[" + (String)coreSS.context.doExtBiz(IContext.BIZ_GET_FORBID_UPDATE_CERT_I18N, null) + "]";
									msgpanel.add(new JLabel("<html><body style=\"width:500\"><strong>" + (String)ResourceUtil.get(IContext.TIP) + "</strong>" +
											"" + StringUtil.replace((String)ResourceUtil.get(9062), "{forbid}", forbid_update_cert)+
											"</body></html>",
											App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEFT), BorderLayout.CENTER);
									App.showCenterPanelMain(msgpanel, 0, 0, (String)ResourceUtil.get(IContext.TIP), false, null, null, al, al, null, false, true, null, false, true);
//							}
								}
							}.start();
						}
					}

					return true;
				}
			}
		}else if(protocal == HCURL.CFG_PROTOCAL){
			final String elementID = url.elementID;
			
			if(elementID.equals(HCURL.ADD_HAR_WIFI)){//注意：此处为WiFi添加模式
				AddHarHTMLMlet.startAddHTMLHarUI(j2seCoreSS, null);
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						final AddHarHTMLMlet mlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet(j2seCoreSS);
						boolean isNotifyPressStart = false;
						int totalSleep = 0;
						final int sleepMS = 1000;
						final int maxSleepMS = (PropertiesManager.isSimu()?5:30) * sleepMS;
						String url;
						do{
//							final String[] commands = {"hello_1", "hello_world"};
//							WiFiDeviceManager.getInstance().broadcastWiFiAccountAsSSID(commands, "group");
//							WiFiDeviceManager.getInstance().clearWiFiAccountGroup("group");
							url = AirCmdsUtil.getAirCmdsHarUrl(WiFiDeviceManager.getInstance(coreSS).getSSIDListOnAir());
							
							if(url != null){
								break;
							}
							
							if(isNotifyPressStart == false){
								mlet.appendMessage((String)ResourceUtil.get(j2seCoreSS, 9134));
								isNotifyPressStart = true;
							}
							try{
								Thread.sleep(sleepMS);
								totalSleep += sleepMS;
							}catch (final Exception e) {
							}
						}while(url == null && totalSleep < maxSleepMS);
						
						if(url == null && totalSleep >= maxSleepMS){
							mlet.appendMessage((String)ResourceUtil.get(j2seCoreSS, 9133));
							mlet.exitButton.setEnabled(true);
							return;
						}
						final String downloadURL = url;
						
						{
							final ThreadGroup token = App.getThreadPoolToken();
							final ThreadPool pools = ContextManager.getThreadPool();
							final Runnable yesRunnable = new Runnable() {
								@Override
								public void run() {
	//								System.out.println("----------start download test : " + downloadURL);
									pools.run(new Runnable() {//转系统级
										@Override
										public void run() {
											mlet.startAddHarProcessInSysThread(j2seCoreSS, downloadURL);			
										}
									}, token);
								}
							};
							final ProjectContext projectContext = mlet.getProjectContext();
							ServerUIAPIAgent.runInSessionThreadPool(j2seCoreSS, ServerUIAPIAgent.getProjResponserMaybeNull(projectContext), new Runnable() {
								@Override
								public void run() {
									projectContext.sendQuestion((String)ResourceUtil.get(j2seCoreSS, IContext.CONFIRMATION), downloadURL, null, yesRunnable, null, null);
								}
							});
						}
					}
				});
				return true;
			}
		}
		return ServerUIUtil.getResponsor().doBiz(coreSS, url);
	}

	final static Object[] SYS_JRUBY_ENGINE = new Object[2];
	
	/**
	 * 注意：切勿关闭或释放本对象，因为可能被再次或其它系统级使用
	 * @return object_0:HCJRubyEngine; object_1:ProjectContext
	 */
	public static final synchronized Object[] getProjectContextForSystemLevelNoTerminate(){
		CCoreUtil.checkAccess();
		
		if(SYS_JRUBY_ENGINE[0] == null){
			final HCJRubyEngine hcje = new HCJRubyEngine(null, ResourceUtil.getJRubyClassLoader(false), true);
			final RecycleRes recycleRes = new RecycleRes("JRubyEngine", ContextManager.getThreadPool(), RecycleRes.getSequenceTempWatcher());
			final ProjectContext context = ServerUIUtil.buildProjectContext("", "", recycleRes, null, null);
			SYS_JRUBY_ENGINE[0] = hcje;
			SYS_JRUBY_ENGINE[1] = context;
		}
		return SYS_JRUBY_ENGINE;
	}
	
	private void doClickAt(final HCURL url, final int mode, final int x, final int y) {
		final Robot robot = ScreenCapturer.robot;
		synchronized (robot) {
			robot.mouseMove(x, y);
			
			robot.keyPress(mode);
	
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
	
			robot.keyRelease(mode);
		}
	}

}
