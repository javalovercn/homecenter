package hc.server;

import hc.App;
import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.EventCenter;
import hc.core.HCMessage;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.HCURL;
import hc.core.util.HCURLCacher;
import hc.core.util.HCURLUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.io.StreamBuilder;
import hc.res.ImageSrc;
import hc.server.ui.SingleMessageNotify;
import hc.server.util.ServerCUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

public class ServerInitor {
	static boolean isWillCheckServer;

	public static void doNothing(){
		
	}
	
	private static void doLineOffProcess(final boolean isClientReq) {
		StreamBuilder.notifyExceptionForReleaseStreamResources(new IOException("System Line Off Exception"));
		
		ContextManager.setStatus(ContextManager.STATUS_NEED_NAT);
		
		//如果不调用此步,可能导致Root提供给手机客户端以错误的状态信息.
		RootServerConnector.delLineInfo(TokenManager.getToken(), false);
		
		//可能更新p_Encrypt_Factor，故刷新(不论正常断线或手机请求)
		RootConfig.reset(true);

		CUtil.resetCheck();
		
		KeepaliveManager.keepalive.setEnable(false);
		KeepaliveManager.keepalive.setIntervalMS(KeepaliveManager.KEEPALIVE_MS);
		KeepaliveManager.ConnBuilderWatcher.setEnable(false);
		
		if(KeepaliveManager.dServer != null){
			//因为Relay模式，可能为null
			KeepaliveManager.dServer.shutdown();
		}

		SIPManager.getSIPContext().resetNearDeployTime();
		
//		if(isClientReq == false || SIPManager.isOnRelay()){
		//旧连接要关闭，否则会导致新注入的连接会产生连接关闭异常
			SIPManager.close();
//		}
		try{
			//setClient(null)之前，稍等，以响应当前客户可能存在的包
			Thread.sleep(CCoreUtil.WAIT_MS_FOR_NEW_CONN);
		}catch (final Exception e) {
			
		}
		
		SIPManager.getSIPContext().resender.reset();
		ConditionWatcher.cancelAllWatch();//释放在途EventBack事件，须在sleep之后
		
		KeepaliveManager.keepalive.doNowAsynchronous();
		KeepaliveManager.keepalive.setEnable(true);
	}
		
	static {
		final ThreadGroup threadPoolToken = App.getThreadPoolToken();
		if(IConstant.serverSide){
			EventCenter.addListener(new IEventHCListener(){
				IHCURLAction urlAction;
				@Override
				public final boolean action(final byte[] bs) {
					final String cmd = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
//					L.V = L.O ? false : LogManager.log("Receive URL:" + cmd);
					if(urlAction == null){
						urlAction = ContextManager.getContextInstance().getHCURLAction();
					}
					HCURLUtil.process(cmd, urlAction);
					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_GOTO_URL;
				}});

			EventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs) {
					final String url = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
					
					final HCURL hu = HCURLUtil.extract(url);

					final String protocal = hu.protocal;
					final String elementID = hu.elementID;

					if(protocal == HCURL.CMD_PROTOCAL){
						if(elementID.equals(HCURL.DATA_CMD_SendPara)){
							final String para1= hu.getParaAtIdx(0);
							if(para1 != null && para1.equals(HCURL.DATA_PARA_CERT_RECEIVED)){
								final String value1 = hu.getValueofPara(para1);
								if(value1.equals(CCoreUtil.RECEIVE_CERT_OK)){
									if(ContextManager.cmStatus != ContextManager.STATUS_READY_TO_LINE_ON){
//										if(PropertiesManager.isTrue(PropertiesManager.p_NewCertIsNotTransed)){
											PropertiesManager.setValue(PropertiesManager.p_NewCertIsNotTransed, IConstant.FALSE);
											PropertiesManager.saveFile();

											J2SEContext.enableTransNewCertMenuItem();
											
											if(SingleMessageNotify.isShowMessage(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF) == false){
											new Thread(){
												@Override
												public void run(){
													SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, true);
													final JPanel askPanle = new JPanel();
													askPanle.setLayout(new BoxLayout(askPanle, BoxLayout.X_AXIS));
													
													askPanle.setBorder(new TitledBorder(((String)ResourceUtil.get(1021)) + "?"));
													
													askPanle.add(Box.createHorizontalGlue());
													askPanle.add(new JLabel(J2SEContext.dl_certkey));
													askPanle.add(Box.createHorizontalGlue());
													askPanle.add(new JLabel(new ImageIcon(ImageSrc.MOVE_TO_ICON)));
													askPanle.add(Box.createHorizontalGlue());
													askPanle.add(new JLabel(J2SEContext.disable_dl_certkey));
													askPanle.add(Box.createHorizontalGlue());
													
													final JPanel panel = new JPanel(new BorderLayout());
													{
														final JPanel subPanel = new JPanel(new BorderLayout());
														subPanel.add(new JLabel("<html>" + (String)ResourceUtil.get(9061) +
															"</html>", new ImageIcon(ResourceUtil.loadImage("ok_22.png")), SwingConstants.LEFT), BorderLayout.NORTH);
														subPanel.add(new JLabel("<html><BR></html>"), BorderLayout.SOUTH);

														panel.add(subPanel, BorderLayout.NORTH);
													}
													panel.add(askPanle, BorderLayout.CENTER);
													final String str_prevent = StringUtil.replace((String)ResourceUtil.get(9069), "{ok}",(String) ResourceUtil.get(IContext.OK));
													panel.add(new JLabel("<html><body style=\"500\">" +
															str_prevent +
															"</body></html>"), BorderLayout.SOUTH);
								
													App.showCenterPanelMain(panel, 0, 0, "HomeCenter", true, null, null, new HCActionListener(new Runnable() {
														@Override
														public void run() {
															SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, false);
															if(DefaultManager.isEnableTransNewCertNow()){
																J2SEContext.flipTransable(false, true);																
															}
														}
													}, threadPoolToken), new HCActionListener(new Runnable() {
														@Override
														public void run() {
															SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, false);
														}
													}, threadPoolToken), null, false, false, null, false, false);													
												}
											}.start();
											}else{
												ContextManager.getContextInstance().displayMessage(
														ResourceUtil.getInfoI18N(), (String)ResourceUtil.get(9061), IContext.INFO, null, 0);
											}
										}
//									}
								}else if(value1.equals(CCoreUtil.RECEIVE_CERT_FORBID)){
									if(ContextManager.cmStatus != ContextManager.STATUS_READY_TO_LINE_ON){
										final String forbid_update_cert = (String)ContextManager.getContextInstance().doExtBiz(IContext.BIZ_GET_FORBID_UPDATE_CERT_I18N, null);
										
										LogManager.err("mobile side:" + forbid_update_cert);
										SIPManager.notifyRelineon(true);
										RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_forbidCert_STR);
										SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_FORBID_CERT, 
												StringUtil.replace((String)ResourceUtil.get(9078), "{forbid}", forbid_update_cert), 
												(String)ResourceUtil.get(IContext.ERROR),
												60000 * 5, App.getSysIcon(App.SYS_ERROR_ICON));
										
									}
								}

							}
						}
					}
					HCURLCacher.getInstance().cycle(hu);
					
					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_GOTO_URL_UN_XOR;
				}});

			
			EventCenter.addListener(new IEventHCListener(){
				@Override
				public final byte getEventTag(){
					return MsgBuilder.E_LINE_OFF_EXCEPTION;
				}
				
				@Override
				public final boolean action(final byte[] bs) {					
					final Boolean isClientReq = Boolean.parseBoolean(HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA));

					doLineOffProcess(isClientReq);
						
					return true;
				}});
			
			EventCenter.addListener(new IEventHCListener() {
				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_RANDOM_FOR_CHECK_SERVER;
				}
				int count = 0;
				@Override
				public final boolean action(final byte[] bs) {
					if(isWillCheckServer){
						isWillCheckServer = false;
						count = 0;
						final int len = HCMessage.getBigMsgLen(bs);
						//收到客户端发来的随机信息，验证后，发回客户端
						final byte[] certKey = CUtil.getCertKey();
						final byte[] passwordBS = IConstant.getPasswordBS();
						CUtil.checkServer(bs, MsgBuilder.INDEX_MSG_DATA, len, certKey, 0, certKey.length, passwordBS, 0, passwordBS.length);
						ContextManager.getContextInstance().sendWrap(MsgBuilder.E_RANDOM_FOR_CHECK_SERVER, 
								bs, MsgBuilder.INDEX_MSG_DATA, len);
						
						//必须先发回服务器签名，才能发送正常的内容。
						//客户端认可服务器失败后，会拒收以下的数据
						
						ContextManager.getThreadPool().run(new Runnable() {
							@Override
							public void run() {
								final J2SEContext j2seCtx = (J2SEContext)ContextManager.getContextInstance();
								j2seCtx.startTransMobileContent();
							}
						});
					}else{
						if(count++ > 0){//忽略第一次
							LogManager.errToLog("ignore random data for identify server.");
						}
					}
					return true;
				}
			});
			
			EventCenter.addListener(new IEventHCListener(){
				short pwdErrTry;
				long lastErrMS;
				@Override
				public final boolean action(final byte[] bs) {
					L.V = L.O ? false : LogManager.log("Receive the back data which to check CK and password");
					
//					System.out.println("pwdErrTry : " + pwdErrTry + ",  MAXTimers : " + LockManager.MAXTIMES);
					if(pwdErrTry < SystemLockManager.MAXTIMES){
					}else{
						if(System.currentTimeMillis() - lastErrMS < SystemLockManager.LOCK_MS){
							SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_LOCK_CERT, 
									"System is locking now!!!<BR><BR>Err Password or certification more than "+SystemLockManager.MAXTIMES+" times.", 
									"Lock System now!!", 1000 * 60 * 1, App.getSysIcon(App.SYS_WARN_ICON));
							LogManager.errToLog("Err Password or certification more than "+SystemLockManager.MAXTIMES+" times.");
							ContextManager.getContextInstance().send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_UNKNOW_STATUS));
							J2SEContext.sleepAfterError();
							SIPManager.notifyRelineon(true);
							
							return true;
						}else{
							resetErrInfo();
						}
					}

					//并将该随机数发送给客户机，客户机用同法处理后回转给服务器
					//服务器据此判断客户机的CertKey和密码状态
					
					if(CUtil.SERVER_READY_TO_CHECK == null){
						L.V = L.O ? false : LogManager.log("Server Reconnected, Skip the back data");
						return true;
					}
					
					//由于每次检验，所以保留备份
					final byte[] oldbs = new byte[CUtil.SERVER_READY_TO_CHECK.length];
					for (int i = 0; i < oldbs.length; i++) {
						oldbs[i] = CUtil.SERVER_READY_TO_CHECK[i];
					}
					
					final short b = ServerCUtil.checkCertKeyAndPwd(bs, MsgBuilder.INDEX_MSG_DATA, 
							IConstant.getPasswordBS(), CUtil.getCertKey(), oldbs);
					if(b == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
						ContextManager.getContextInstance().doExtBiz(b, null);
						lastErrMS = System.currentTimeMillis();
						L.V = L.O ? false : LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");
					}else if(b == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR && CUtil.isSecondCertKeyError == true){
						lastErrMS = System.currentTimeMillis();
						
						L.V = L.O ? false : LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");

						ContextManager.getContextInstance().send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR));
						L.V = L.O ? false : LogManager.log("Second Cert Key Error, send Err Password status");		
						
						ServerCUtil.notifyErrPWDDialog();
						
						SIPManager.notifyRelineon(true);
					}else{
						ContextManager.getContextInstance().doExtBiz(b, null);
						
						if(b == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR && CUtil.isSecondCertKeyError == false){
							L.V = L.O ? false : LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");
							CUtil.isSecondCertKeyError = true;
						}else{
							CUtil.resetCheck();
							resetErrInfo();
							isWillCheckServer = true;//开启只接收一次的状态，接收后置false，以防止被滥用。
						}
					}
					return true;
				}

				private void resetErrInfo() {
					pwdErrTry = 0;
					lastErrMS = 0;
				}
	
				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD;
				}});
			
			EventCenter.addListener(new IEventHCListener() {
				
				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_TAG_SERVER_RELAY_START;
				}
				
				@Override
				public final boolean action(final byte[] bs) {
					ContextManager.setConnectionModeStatus(ContextManager.MODE_CONNECTION_RELAY);
					
					if(SIPManager.isOnRelay()){
						ContextManager.setStatus(ContextManager.STATUS_READY_MTU);
					}
					return true;
				}
			});
			
		}
	}
}
