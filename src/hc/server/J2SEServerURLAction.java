package hc.server;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.cache.CacheManager;
import hc.core.data.DataClientAgent;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.RecycleRes;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.core.util.WiFiDeviceManager;
import hc.server.data.screen.ScreenCapturer;
import hc.server.msb.AirCmdsUtil;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.MenuItem;
import hc.server.ui.ProjectContext;
import hc.server.ui.QuestionParameter;
import hc.server.ui.ResParameter;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.design.AddHarHTMLMlet;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.Location;
import hc.server.ui.design.MobiUIResponsor;
import hc.server.ui.design.ProjMgrDialog;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.util.ServerUtil;
import hc.server.util.VoiceParameter;
import hc.server.util.ai.ProjectTargetForAI;
import hc.util.BaseResponsor;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class J2SEServerURLAction implements IHCURLAction {
	
	@Override
	public boolean doBiz(final CoreSession coreSS, final HCURL url) {
		
		final String protocal = url.protocal;
		
		L.V = L.WShop ? false : LogManager.log("J2SEServerURLAction processing : " + url.url);
		
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
				
				final int mobileWidth = UserThreadResourceUtil.getDeviceWidthFrom(j2seCoreSS);
				final int mobileHeight = UserThreadResourceUtil.getDeviceHeightFrom(j2seCoreSS);
				
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
				L.V = L.WShop ? false : LogManager.log("client send Exit command.");
				if(ScreenServer.popScreen(j2seCoreSS) == false){
//					System.out.println("Receiv Exit , and Notify exit by mobile");
					J2SEContext.notifyExitByMobi(j2seCoreSS);
				}
				
				return true;
			}else if(elementID.equals(HCURL.MGR_PROJS_COMMAND)){
				j2seCoreSS.notifyCanvasMenuResponse();

				final ThreadGroup token = App.getThreadPoolToken();
				final ProjResponser resp = ServerUIAPIAgent.getCurrentProjResponser(j2seCoreSS);
				ServerUIAPIAgent.runInSessionThreadPool(j2seCoreSS, resp, new Runnable() {
					@Override
					public void run() {
						final boolean tryEnter = (Boolean)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable(){
							@Override
							public Object run() {
								return LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_VIA_MOBILE);
							}
							
						}, token);
						
						if(tryEnter == false){
							final int status = LinkProjectStatus.getStatus();
							
							int msgID = 0;
							if(status == LinkProjectStatus.MANAGER_VIA_MOBILE){
								msgID = 9262;
							}else if (status == LinkProjectStatus.MANAGER_UPGRADE_DOWNLOADING){
								//system is downloading and upgrading project(s), please wait for a moment.
								msgID = 9161;
							}else{
								msgID = 9263;//server is managing/locking projects!
							}
							
							ProjectContext.getProjectContext().sendMessage(ResourceUtil.getErrorI18N(j2seCoreSS), ResourceUtil.get(j2seCoreSS, msgID), ProjectContext.MESSAGE_ERROR, null, 0);
							return;
						}
						
						ProjectContext.getProjectContext().sendDialogWhenInSession(new ProjMgrDialog(j2seCoreSS, token));
					}
				});
				return true;
			}else if(elementID.equals(HCURL.DATA_CMD_SendPara)){
				final String para1 = url.getParaAtIdx(0);
				if(HCURL.DATA_PARA_RIGHT_CLICK.equals(para1)){
					final String value1 = url.getValueofPara(para1);
					if(value1 != null){
						if(value1.equals(HCURL.DATA_PARA_VALUE_CTRL)){
							final int x = Integer.parseInt(url.getValueofPara("x"));
							final int y = Integer.parseInt(url.getValueofPara("y"));

							LogManager.log(ILog.OP_STR + "Ctrl + mouse left click at (" + x + ", " + y + ")");
							
							ScreenCapturer.doClickAt(url, ResourceUtil.getAbstractCtrlKeyCode(), x, y);
							return true;
						}else if(value1.equals(HCURL.DATA_PARA_VALUE_SHIFT)){
							final int x = Integer.parseInt(url.getValueofPara("x"));
							final int y = Integer.parseInt(url.getValueofPara("y"));

							LogManager.log(ILog.OP_STR + "Shift + mouse left click at (" + x + ", " + y + ")");
							
							ScreenCapturer.doClickAt(url, KeyEvent.VK_SHIFT, x, y);
							return true;
						}
					}
				}else if(HCURL.DATA_PARA_PUBLISH_LOCATION.equals(para1)){
					try{
						final String latitude = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_LATITUDE);
						final String longitude = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_LONGITUDE);
						final String altitude = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_ALTITUDE);
						final String course = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_COURSE);
						final String speed = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_SPEED);
						final String isGPS = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_IS_GPS);
						final String isFresh = url.getValueofPara(HCURL.DATA_PARA_LOCATION_STR_IS_FRESH);
						
						final StringBuilder sb = StringBuilderCacher.getFree();
						sb.append("mobile location latitude : ").append(latitude).append(", longitude : ").append(longitude).
							append(", altitude : ").append(altitude).append(", course : ").append(course).append(", speed : ").append(speed).
							append(", isGPS : ").append(isGPS).append(", isFresh : ").append(isFresh);

						final String log = sb.toString();
						LogManager.log(log);
						StringBuilderCacher.cycle(sb);
						
						final Location location = new Location(Double.parseDouble(latitude), Double.parseDouble(longitude),
								Double.parseDouble(altitude), Double.parseDouble(course), Double.parseDouble(speed),
								IConstant.TRUE.equals(isGPS), IConstant.TRUE.equals(isFresh));
						
						ServerUIUtil.getMobiResponsor().dispatchLocation(j2seCoreSS, location);
					}catch (final Throwable e) {
						e.printStackTrace();
					}
					return true;
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
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_ERR_ON_CACHE)){
					final String projID = url.getValueofPara(HCURL.DATA_PARA_ERR_ON_CACHE);
					//注意：以下要提前，以减少因网络导致不一致。
					HCURLUtil.sendCmdSuperLevel(j2seCoreSS, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_ERR_ON_CACHE, projID);

					//通知客户端可以删除cache，客户端不能自行删除。
					final String softUID = UserThreadResourceUtil.getMobileSoftUID(j2seCoreSS);
					LogManager.log("receive ERR_ON_CACHE on project [" + projID + "].");
					if(CacheManager.removeUIDFrom(projID, softUID)){
						LogManager.log("remove cache data for [" + projID + "/" + softUID + "].");
					}
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_SCAN_QR_CODE)){
					final String urlHexStr = url.getValueofPara(HCURL.DATA_PARA_SCAN_QR_CODE);
					final byte[] bs = ByteUtil.toBytesFromHexStr(urlHexStr);
					final String result = StringUtil.bytesToString(bs, 0, bs.length);
					
					ServerUIAPIAgent.notifyClientSessionQRResult(j2seCoreSS.clientSession, result);
					return true;
				}else if(para1 != null && para1.equals(HCURL.DATA_PARA_PROC_ADD_HAR_URL)){
					j2seCoreSS.notifyCanvasMenuResponse();//置于startAddHTMLHarUI之后，mouse busy界面不能刷新
					final String urlHexStr = url.getValueofPara(HCURL.DATA_PARA_PROC_ADD_HAR_URL);
					final byte[] bs = ByteUtil.toBytesFromHexStr(urlHexStr);
					final String urlStr = StringUtil.bytesToString(bs, 0, bs.length);
					
					AddHarHTMLMlet.startAddHTMLHarUI(j2seCoreSS, urlStr, true);
					return true;
				}else if(HCURL.DATA_PARA_VOICE_COMMANDS.equals(para1)){
					final String voiceText = VoiceParameter.format(url.getValueofPara(HCURL.DATA_PARA_VOICE_COMMANDS));
					ContextManager.getThreadPool().run(new Runnable() {//可能较长时间，
						@Override
						public void run() {
							processVoice(j2seCoreSS, voiceText);
						}
					});
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
									msgpanel.add(new JLabel("<html><body style=\"width:500\"><strong>" + ResourceUtil.get(IContext.TIP) + "</strong>" +
											"" + StringUtil.replace(ResourceUtil.get(9062), "{forbid}", forbid_update_cert)+
											"</body></html>",
											App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEFT), BorderLayout.CENTER);
									App.showCenterPanelMain(msgpanel, 0, 0, ResourceUtil.get(IContext.TIP), false, null, null, al, al, null, false, true, null, false, true);
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
				j2seCoreSS.notifyCanvasMenuResponse();//因为手机端请求时，进入等待状态
				
				final boolean isInstallFromClient = true;
				AddHarHTMLMlet.startAddHTMLHarUI(j2seCoreSS, null, isInstallFromClient);
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
								mlet.appendMessage(ResourceUtil.get(j2seCoreSS, 9134));
								isNotifyPressStart = true;
							}
							try{
								Thread.sleep(sleepMS);
								totalSleep += sleepMS;
							}catch (final Exception e) {
							}
						}while(url == null && totalSleep < maxSleepMS);
						
						if(url == null && totalSleep >= maxSleepMS){
							mlet.appendMessage(ResourceUtil.get(j2seCoreSS, 9133));
							mlet.setButtonEnable(mlet.exitButton, true);
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
											mlet.startAddHarProcessInSysThread(j2seCoreSS, downloadURL, isInstallFromClient);			
										}
									}, token);
								}
							};
							final ProjectContext projectContext = mlet.getProjectContext();
							ServerUIAPIAgent.runInSessionThreadPool(j2seCoreSS, ServerUIAPIAgent.getProjResponserMaybeNull(projectContext), new Runnable() {
								@Override
								public void run() {
									projectContext.sendQuestion(ResourceUtil.get(j2seCoreSS, IContext.CONFIRMATION), downloadURL, null, yesRunnable, null, null);
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

	private final void processVoice(final J2SESession j2seCoreSS, final String voiceText) {
		final VoiceParameter vp = new VoiceParameter(voiceText);
		final MenuItem out = j2seCoreSS.searchMenuItemByVoiceCommand(vp);
		if(out != null){
			if(out.isEnabled()){
				final String itemURL = ServerUIAPIAgent.getMobiMenuItem_URL(out);
				LogManager.log(ILog.OP_STR + "execute [" + itemURL + "] by voice command [" + voiceText + "].");
				ServerUIAPIAgent.goInSysThread(j2seCoreSS, ServerUIAPIAgent.getBelongMobiMenu(out).resp.context, itemURL);
			}else{
				final String msg = "[" + voiceText + "] : " + ResourceUtil.get(9247);
				ServerUIAPIAgent.sendOneMovingMsg(j2seCoreSS, msg);
				LogManager.log(ILog.OP_STR + "voice command " + msg);
			}
		}else{
			
			final MobiUIResponsor resp = (MobiUIResponsor)ServerUIUtil.getResponsor();
			if(resp.dispatchVoiceCommand(j2seCoreSS, voiceText) == false){
				//没有工程级assistant响应，则HCAI进行处理
				final String locale = UserThreadResourceUtil.getMobileLocaleFrom(j2seCoreSS);
				
				final ProjectTargetForAI target = resp.query(j2seCoreSS, locale, voiceText);
				
				if(target == null){
					ServerUIAPIAgent.sendOneMovingMsg(j2seCoreSS, StringUtil.replace(ResourceUtil.get(j2seCoreSS, 9245), "{voice}", voiceText));
				}else{
					LogManager.log(ILog.OP_STR + "find target [" + target + "] for voice [" + voiceText + "].");
					final ProjectContext ctx = resp.getProjResponser(target.projectID).context;
					ServerUIAPIAgent.goInSysThread(j2seCoreSS, ctx, target.target);
					
					final String vcTip = StringUtil.replace(ResourceUtil.get(9257), "{cmd}", voiceText);
					ServerUIAPIAgent.sendOneMovingMsg(j2seCoreSS, vcTip);
				}
			}
		}
	}

	final static Object[] SYS_JRUBY_ENGINE = new Object[2];
	
	/**
	 * 注意：切勿关闭或释放本对象，因为可能被再次或其它系统级使用
	 * @return object_0:HCJRubyEngine; object_1:ProjectContext
	 */
	public static final synchronized Object[] getProjectContextForSystemLevelNoTerminate(){
		CCoreUtil.checkAccess();
		
		if(SYS_JRUBY_ENGINE[0] == null){
			final HCJRubyEngine hcje = new HCJRubyEngine(null, null, ServerUtil.getJRubyClassLoader(false), true, HCJRubyEngine.IDE_LEVEL_ENGINE + "SYS_JRUBY_ENGINE");
			final RecycleRes recycleRes = new RecycleRes("JRubyEngine", ContextManager.getThreadPool(), RecycleRes.getSequenceTempWatcher());
			final ProjectContext context = ServerUIUtil.buildProjectContext("", "", recycleRes, null, null);
			SYS_JRUBY_ENGINE[0] = hcje;
			SYS_JRUBY_ENGINE[1] = context;
		}
		return SYS_JRUBY_ENGINE;
	}
	
}
