package hc.server;

import hc.App;
import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.EventCenter;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.Message;
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
import hc.res.ImageSrc;
import hc.server.ui.JcipManager;
import hc.server.ui.SingleMessageNotify;
import hc.server.util.ServerCUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

public class ServerInitor {
	public static void doNothing(){
		
	}
	
	private static void doLineOffProcess(boolean isClientReq) {
		ContextManager.setStatus(ContextManager.STATUS_NEED_NAT);
		
		//如果不调用此步,可能导致Root提供给手机客户端以错误的状态信息.
		RootServerConnector.delLineInfo(TokenManager.getToken(), false);
		
		//可能更新p_Encrypt_Factor，故刷新(不论正常断线或手机请求)
		RootConfig.reset();

		CUtil.resetCheck();
		
		KeepaliveManager.keepalive.setEnable(false);
		KeepaliveManager.keepalive.setIntervalMS(KeepaliveManager.KEEPALIVE_MS);
		KeepaliveManager.ConnBuilderWatcher.setEnable(false);
		
		if(KeepaliveManager.dServer != null){
			//因为Relay模式，可能为null
			KeepaliveManager.dServer.shutdown();
		}

		ConditionWatcher.cancelAllWatch();
		JcipManager.clearAllTimer();
		SIPManager.getSIPContext().resetNearDeployTime();
		
		if(isClientReq == false || SIPManager.isOnRelay()){
			SIPManager.close();
		}
		try{
			//setClient(null)之前，稍等，以响应当前客户可能存在的包
			Thread.sleep(300);
		}catch (Exception e) {
			
		}
		
		SIPManager.getSIPContext().resender.reset();
		
		KeepaliveManager.keepalive.doNowAsynchronous();
		KeepaliveManager.keepalive.setEnable(true);
	}
		
	static {
		if(IConstant.serverSide){
			EventCenter.addListener(new IEventHCListener(){
				IHCURLAction urlAction;
				public boolean action(final byte[] bs) {
					String cmd = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
//					L.V = L.O ? false : LogManager.log("Receive URL:" + cmd);
					if(urlAction == null){
						urlAction = ContextManager.getContextInstance().getHCURLAction();
					}
					HCURLUtil.process(cmd, urlAction);
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_GOTO_URL;
				}});

			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					String url = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
					
					HCURL hu = HCURLUtil.extract(url);

					final String protocal = hu.protocal;
					final String elementID = hu.elementID;

					if(protocal == HCURL.CMD_PROTOCAL){
						if(elementID.equals(HCURL.DATA_CMD_SendPara)){
							String para1= hu.getParaAtIdx(0);
							if(para1 != null && para1.equals(HCURL.DATA_PARA_CERT_RECEIVED)){
								final String value1 = hu.getValueofPara(para1);
								if(value1.equals(CCoreUtil.RECEIVE_CERT_OK)){
									if(ContextManager.cmStatus != ContextManager.STATUS_READY_TO_LINE_ON){
//										if(PropertiesManager.isTrue(PropertiesManager.p_NewCertIsNotTransed)){
											PropertiesManager.setValue(PropertiesManager.p_NewCertIsNotTransed, IConstant.FALSE);
											PropertiesManager.saveFile();

											J2SEContext.transNewCertKey.setEnabled(true);
											
											if(SingleMessageNotify.isShowMessage(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF) == false){
											new Thread(){
												public void run(){
													SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, true);
													JPanel askPanle = new JPanel();
													askPanle.setLayout(new BoxLayout(askPanle, BoxLayout.X_AXIS));
													
													askPanle.setBorder(new TitledBorder(((String)ResourceUtil.get(1021)) + "?"));
													
													askPanle.add(Box.createHorizontalGlue());
													askPanle.add(new JLabel(J2SEContext.dl_certkey));
													askPanle.add(Box.createHorizontalGlue());
													try {
														askPanle.add(new JLabel(new ImageIcon(ImageIO.read(ImageSrc.MOVE_TO_ICON))));
													} catch (IOException e1) {
														e1.printStackTrace();
													}
													askPanle.add(Box.createHorizontalGlue());
													askPanle.add(new JLabel(J2SEContext.disable_dl_certkey));
													askPanle.add(Box.createHorizontalGlue());
													
													JPanel panel = new JPanel(new BorderLayout());
													{
														JPanel subPanel = new JPanel(new BorderLayout());
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
								
													App.showCenterPanel(panel, 0, 0, "HomeCenter", true, null, null, new ActionListener() {
														@Override
														public void actionPerformed(ActionEvent e) {
															SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, false);
															if(J2SEContext.isEnableTransNewCertNow()){
																J2SEContext.flipTransable(false, true);																
															}
														}
													}, new ActionListener() {
														@Override
														public void actionPerformed(ActionEvent e) {
															SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, false);
														}
													}, null, false, false, null, false, false);													
												}
											}.start();
											}else{
												ContextManager.getContextInstance().displayMessage(
														(String) ResourceUtil.get(IContext.INFO), (String)ResourceUtil.get(9061), IContext.INFO, null, 0);
											}
										}
//									}
								}else if(value1.equals(CCoreUtil.RECEIVE_CERT_FORBID)){
									if(ContextManager.cmStatus != ContextManager.STATUS_READY_TO_LINE_ON){
										LogManager.err("mobile side:" + CCoreUtil.FORBID_UPDATE_CERT);
										SIPManager.notifyRelineon(true);
										RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_forbidCert_STR);
										SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_FORBID_CERT, 
												StringUtil.replace((String)ResourceUtil.get(9078), "{forbid}", CCoreUtil.FORBID_UPDATE_CERT), 
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

				public byte getEventTag() {
					return MsgBuilder.E_GOTO_URL_UN_XOR;
				}});

			
			EventCenter.addListener(new IEventHCListener(){
				public byte getEventTag(){
					return MsgBuilder.E_LINE_OFF_EXCEPTION;
				}
				
				public boolean action(byte[] bs) {					
					Boolean isClientReq = Boolean.parseBoolean(Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA));

					doLineOffProcess(isClientReq);
						
					return true;
				}});
			

			EventCenter.addListener(new IEventHCListener(){
				short pwdErrTry;
				long lastErrMS;
				public boolean action(final byte[] bs) {
					hc.core.L.V=hc.core.L.O?false:LogManager.log("Receive the back data which to check CK and password");
					
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
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Server Reconnected, Skip the back data");
						return true;
					}
					
					//由于每次检验，所以保留备份
					byte[] oldbs = new byte[CUtil.SERVER_READY_TO_CHECK.length];
					for (int i = 0; i < oldbs.length; i++) {
						oldbs[i] = CUtil.SERVER_READY_TO_CHECK[i];
					}
					
					short b = ServerCUtil.checkCertKeyAndPwd(bs, MsgBuilder.INDEX_MSG_DATA, 
							IConstant.passwordBS, CUtil.CertKey, oldbs);
					if(b == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
						ContextManager.getContextInstance().doExtBiz(b, null);
						lastErrMS = System.currentTimeMillis();
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");
					}else if(b == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR && CUtil.isSecondCertKeyError == true){
						lastErrMS = System.currentTimeMillis();
						
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");

						ContextManager.getContextInstance().send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR));
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Second Cert Key Error, send Err Password status");						
						SIPManager.notifyRelineon(true);
					}else{
						ContextManager.getContextInstance().doExtBiz(b, null);
						
						if(b == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR && CUtil.isSecondCertKeyError == false){
							hc.core.L.V=hc.core.L.O?false:LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");
							CUtil.isSecondCertKeyError = true;
						}else{
							CUtil.resetCheck();
							resetErrInfo();
						}
					}
					return true;
				}

				private void resetErrInfo() {
					pwdErrTry = 0;
					lastErrMS = 0;
				}
	
				public byte getEventTag() {
					return MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD;
				}});
			
			EventCenter.addListener(new IEventHCListener() {
				
				@Override
				public byte getEventTag() {
					return MsgBuilder.E_TAG_SERVER_RELAY_START;
				}
				
				@Override
				public boolean action(byte[] bs) {
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
