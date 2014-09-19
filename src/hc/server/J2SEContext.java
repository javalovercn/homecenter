package hc.server;

import hc.App;
import hc.JPTrayIcon;
import hc.core.ClientInitor;
import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.EventCenter;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.IStatusListen;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.Message;
import hc.core.MsgBuilder;
import hc.core.ReceiveServer;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.data.ServerConfig;
import hc.core.sip.SIPManager;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.HCURLUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.IMsgNotifier;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.data.KeyComperPanel;
import hc.server.relay.RelayManager;
import hc.server.relay.RelayShutdownWatch;
import hc.server.ui.ClientDesc;
import hc.server.ui.JcipManager;
import hc.server.ui.LogViewer;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.video.CapControlFrame;
import hc.server.ui.video.CapManager;
import hc.server.ui.video.CapPreviewPane;
import hc.server.ui.video.CapStream;
import hc.server.ui.video.CaptureConfig;
import hc.server.util.ServerCUtil;
import hc.util.ExitManager;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.TrayIcon.MessageType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class J2SEContext extends CommJ2SEContext implements IStatusListen{
	public static JPTrayIcon ti;
    public static JMenuItem transNewCertKey;
    private static CapNotify capNotify = new CapNotify();
    static{
    	CapManager.addListener(capNotify);
    }
    
	public J2SEContext() {
		ToolTipManager.sharedInstance().setDismissDelay(1000 * 20);

		SIPManager.setSIPContext(new J2SESIPContext(){

			@Override
			public Object buildSocket(int localPort, String targetServer, int targetPort){
				Object s = super.buildSocket(localPort, targetServer, targetPort);
				if(s == null){
					//要进行重连，不同于其它如Root，
					SIPManager.notifyRelineon(false);
				}
				return s;
			}
		});
		
		super.init(new ReceiveServer(), new J2SEUDPReceiveServer());

		if(IConstant.serverSide){
//			RelayManager.start(HttpUtil.getLocalIP(), SIPManager.relayPort, null);
			
			showTray();
			KeepaliveManager.keepalive.doNowAsynchronous();
			KeepaliveManager.keepalive.setEnable(true);
		}
		
	}
	
	public void interrupt(Thread thread){
		thread.interrupt();
	}
	
	public void exit(){
		ExitManager.exit();				
	}
	
	public void notifyShutdown(){
		//获得全部通讯，并通知下线。
		L.V = L.O ? false : LogManager.log("Shut down");
		
		//关闭用户工程组
		ServerUIUtil.stop();
		
		if(IConstant.serverSide == false){
			ContextManager.shutDown();
		}else{
	    	DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null){
				public void doBiz() {
					if(ti != null){
						ti.remove();
						ti.exit();
						ti = null;
					}
	
					if(RelayManager.startMoveNewRelay(
							new RelayShutdownWatch() {
								public void extShutdown() {
									DelayServer.getInstance().shutDown();
								}
							})){
					}else{
						DelayServer.getInstance().shutDown();
					}
				}});
		}
    }
	
//	private static long lastCanvasMainAction = 0;
	private static void doCanvasMain(String url){
		//删除时间过滤，由于版本普遍更新后，该过滤将失去作用
		//为了防服务器推送后,旧版本的客户端的请求再次到来,加时间过滤.
//		final long currentTimeMillis = System.currentTimeMillis();
//		if(currentTimeMillis - lastCanvasMainAction < 10000){
//			return;
//		}
//		lastCanvasMainAction = currentTimeMillis;
		
		//检查UDP通道的可达性
		if(ContextManager.getContextInstance().isDoneUDPChannelCheck == false){
			L.V = L.O ? false : LogManager.log("Ready check UDP channel usable");
			if(ContextManager.getContextInstance().isBuildedUPDChannel){
				L.V = L.O ? false : LogManager.log("Auto Disable UDP Channel");
				//关闭不可通达的UDP
				ContextManager.getContextInstance().isBuildedUPDChannel = false;
			}else{
				L.V = L.O ? false : LogManager.log("UDP Channel is Disabled by NO_MSG_RECEIVE");
			}
			ContextManager.getContextInstance().isDoneUDPChannelCheck = true;
		}
		
//		L.V = L.O ? false : LogManager.log("Receive Req:" + url);
		DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
			@Override
			public void doBiz() {
				ServerUIUtil.getResponsor().onEvent(ProjectContext.EVENT_SYS_MOBILE_LOGIN);
			}
		});
		
		HCURLUtil.process(url, ContextManager.getContextInstance().getHCURLAction());
	}
	
	public void run() {
		super.run();
		
		ClientInitor.doNothing();
		
		EventCenter.addListener(new IEventHCListener(){
			public boolean action(final byte[] bs) {
//				if(SIPManager.isSameUUID(event.data_bs)){
					if(IConstant.serverSide){
						//客户端主动下线
						String token = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
						if(token.equals(RootServerConnector.getHideToken())){					
							notifyExitByMobi();
						}else{
							hc.core.L.V=hc.core.L.O?false:LogManager.log("Error Token at client shutdown");
						}
					}else{
						//TODO j2se客户机
					}
					return true;
//				}
//				return true;
			}

			public byte getEventTag() {
				return MsgBuilder.E_TAG_SHUT_DOWN_BETWEEN_CS;
			}});
		
		startAllServers();
		
//		KeepaliveManager.keepalive.setEnable(true);
//		KeepaliveManager.keepalive.doNowAsynchronous();
		
		if(IConstant.serverSide){
			//服务器端增加各种MobiUI应答逻辑
			
			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					String jcip = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
					JcipManager.responseFormSubmit(jcip);
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_JCIP_FORM_SUBMIT;
				}});

			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					String formID = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
					JcipManager.removeAutoResonseTimer(formID);
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_JCIP_FORM_EXIT;
				}});

			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					String url = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);

					doCanvasMain(url);

					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_CANVAS_MAIN;
				}});
			
			EventCenter.addListener(new IEventHCListener(){
				public boolean action(final byte[] bs) {
					if(ScreenServer.isServing() == false){
						ClientDesc.refreshClientInfo(Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA));

						//如果手机版本过低，产生通知
						String pcReqMobiVer = (String)doExtBiz(BIZ_GET_REQ_MOBI_VER_FROM_PC, null);
						if(StringUtil.higer(pcReqMobiVer, ClientDesc.clientVer)){
							send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS));
							LogManager.err("Min required mobile version : [" + pcReqMobiVer + "], current mobile version : [" + ClientDesc.clientVer + "]");
							L.V = L.O ? false : LogManager.log("Cancel mobile login process");
							sleepAfterError();
							SIPManager.notifyRelineon(false);
							return true;
						}
						
						//服务器产生一个随机数，用CertKey和密码处理后待用，
						byte[] randomBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
						CCoreUtil.generateRandomKey(randomBS, MsgBuilder.INDEX_MSG_DATA, CUtil.TRANS_CERT_KEY_LEN);
						CUtil.resetCheck();
						CUtil.SERVER_READY_TO_CHECK = randomBS;
						
						byte[] randomEvent = ContextManager.cloneDatagram(randomBS);
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Send random data to client for check CK and password");
//						try{
//							Thread.sleep(50);
//						}catch (Exception e) {
//						}
						send(MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD, randomEvent, CUtil.TRANS_CERT_KEY_LEN);

					}else{
						hc.core.L.V=hc.core.L.O?false:LogManager.log("In Serving, Skip other client desc");
					}
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_CLIENT_INFO;
				}});
		}
		
		if(IConstant.serverSide){
			//加载其它启动逻辑
			CaptureConfig cc = CaptureConfig.getInstance();
			if(cc.get(CaptureConfig.USE_VIDEO, IConstant.FALSE).equals(IConstant.TRUE)
					&& cc.getAutoRecord().equals(IConstant.TRUE)){
				CapStream.getInstance(false).startRecord(false);
			}
		}
		
		LinkMenuManager.startAutoUpgradeBiz();
	}
	
    static ImageIcon dl_certkey, disable_dl_certkey;
    Image hc_Enable, hc_Disable, hc_mobi;

    public void displayMessage(String caption, String text, int type, Object imageData, int timeOut){
    	MessageType mtype = null;
    	if(type == IContext.ERROR){
    		mtype = MessageType.ERROR;
    	}else if(type == IContext.INFO){
    		mtype = MessageType.INFO;
    	}else if(type == IContext.WARN){
    		mtype = MessageType.WARNING;
    	}else{
    		mtype = MessageType.INFO;
    	}
    	if(ti != null){
    		ti.displayMessage(caption, text, mtype);
    	}
    }
    
    public JPopupMenu popupTi = new JPopupMenu() ;//弹出菜单
	
    public static long lastCheckMS = System.currentTimeMillis();
    
    /**
     * 
     * @param checkTime true表示仅当时间超过3分钟后，才进行验证；false表示只要是多模式，则必需验证
     * @param opName
     * @return
     */
    public static boolean checkPassword(boolean checkTime, String opName){
    	if(PropertiesManager.isTrue(PropertiesManager.p_isMultiUserMode)
    			&&
    				(	(checkTime == false) 
						|| (System.currentTimeMillis() - lastCheckMS > 1000 * 60 * 3))){
    		PWDDialog pd = new PWDDialog();
    		
    		String pwd = App.getFromBASE64(PropertiesManager.getValue(PropertiesManager.p_password));
    		if(pd.pwd == null){
    			//取消操作
    			return false;
    		}
    		
    		if(pwd.equals(pd.pwd)){
    			refreshActionMS(true);
    			return true;
    		}
    		
    		if(opName != null){
    			L.V = L.O ? false : LogManager.log("Desktop Menu [" + opName + "] password error!");
    		}
    		Object[] options={(String)ResourceUtil.get(1010)};
    		JOptionPane.showOptionDialog(null, (String)ResourceUtil.get(1019), "HomeCenter", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
    		return false;
    	}else{
    		refreshActionMS(false);
    		return true;
    	}
    }

	public static void refreshActionMS(boolean isForce) {
		if(isForce || (System.currentTimeMillis() - lastCheckMS < 1000 * 60 * 3)){
			lastCheckMS = System.currentTimeMillis();
		}
	}
    
	final static String transOnTip = (String)ResourceUtil.get(9063);
	final static String transOffTip = (String)ResourceUtil.get(9064);

	private void buildCertMenu(final JPopupMenu popupMenu){
		final String str_certification = (String)ResourceUtil.get(9060);
		final JMenu certMenu = new JMenu(str_certification);
        BufferedImage newKey = null;
		try {
			newKey = ImageIO.read(ImageSrc.NEW_CERTKEY_ICON);
		} catch (IOException e2) {
		}
		certMenu.setIcon(new ImageIcon(newKey));
		
        //生成新证书
        final JMenuItem buildNewCertKey = new JMenuItem((String)ResourceUtil.get(9001));
		buildNewCertKey.setToolTipText("<html>" +
				"certification encrypt transmission of data and prevent hacker try to login." +
				"<BR>certification is stored locally, not on HomeCenter." +
				"<BR>after creating new certification, the old in mobile side is invalid.</html>");
        buildNewCertKey.setIcon(new ImageIcon(newKey));
        buildNewCertKey.addActionListener(new ActionListener() {
        	String opName = buildNewCertKey.getText();
			public void actionPerformed(ActionEvent e) {
				if(checkPassword(false, opName)){
            		ConditionWatcher.addWatcher(new LineonAndServingExecWatcher(buildNewCertKey.getText()){
						@Override
						public void doBiz() {
							createNewCertification();
						}});
				}
			}
        });
        certMenu.add(buildNewCertKey);
        
        //传输新证书开关
		transNewCertKey = new JMenuItem("");//菜单项
		transNewCertKey.setToolTipText("<html>'Transmit:on', " +
				transOnTip +
				";" +
				"<BR>'Transmit:off', " +
				transOffTip +
				".</html>");
		initEnableTransNewCert();
		if(isEnableTransNewCertNow()){
			transNewCertKey.setIcon(dl_certkey);
			transNewCertKey.setText((String)ResourceUtil.get(1021));
		}else{
			transNewCertKey.setIcon(disable_dl_certkey);
			transNewCertKey.setText((String)ResourceUtil.get(1020));			
		}
		if(PropertiesManager.isTrue(PropertiesManager.p_NewCertIsNotTransed) 
            			&& (isEnableTransNewCertNow())){
			transNewCertKey.setEnabled(false);
		}
		
		transNewCertKey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	refreshActionMS(false);
            	if(checkPassword(true, transNewCertKey.getText())){
            		ConditionWatcher.addWatcher(new LineonAndServingExecWatcher(transNewCertKey.getText()){
						@Override
						public void doBiz() {
			            	flipTransable(!isEnableTransNewCertNow(), false);
						}});
            	}
            }
        });
		certMenu.add(transNewCertKey);
        
//        {
//        	//QR
//            final JMenuItem qrInput = new JMenuItem("QR " + str_certification);
//            ImageIcon qrImg = new ImageIcon(ResourceUtil.loadImage("qr_22.png"));
//    		qrInput.setToolTipText("<html>display certification string, i will input it directly to mobile config panel," +
//    				"<BR>instead of transmit it on network.</html>");
//            qrInput.setIcon(qrImg);
//            qrInput.addActionListener(new ActionListener() {
//            	String opName = qrInput.getText();
//    			public void actionPerformed(ActionEvent e) {
//    				if(checkPassword(false, opName)){
//    					QRTool.qrCertification();
//					}
//    			}
//            });
//            certMenu.add(qrInput);    
//        }		

        popupMenu.add(certMenu);
	}
	
	public void buildMenu(Locale locale){

		popupTi = new JPopupMenu();
    	
    	popupTi.applyComponentOrientation(ComponentOrientation.getOrientation(locale));
    	
        final JMenu hcMenu = new JMenu("HomeCenter");
        ImageIcon hcIcon = null;
        try{
        	hcIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/hc_22.png")));
        }catch (Exception e) {
			
		}
        try{
        	hcMenu.setIcon(hcIcon);
        }catch (Exception e) {
		}

        ActionListener aboutAction;	
        
        {
        	final String title = (String)ResourceUtil.get(9040);
			final JMenuItem option = new JMenuItem(title);
            ImageIcon optionIco = null;
            try{
            	optionIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/option_22.png")));
            	option.setIcon(optionIco);
            }catch (Exception e) {
    		}
            option.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SingleJFrame.showJFrame(ConfigPane.class);
				}
			});
            hcMenu.add(option);
            
			buildAutoUpgradMenuItem(hcMenu);
	        
			{
		        //Multi user mode
		        final JCheckBoxMenuItem multiUserMenu = new JCheckBoxMenuItem((String)ResourceUtil.get(9058));
		        try{
		        	multiUserMenu.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/group_22.png"))));
		        }catch (Exception e) {
				}
		        multiUserMenu.setSelected(PropertiesManager.isTrue(PropertiesManager.p_isMultiUserMode));
		        final String tipMsg = "it prevent your children (or other) to set this program when you are outside." +
		        		"<BR>if the computer is only used by you, you don't need multi-user mode.";
		        multiUserMenu.setToolTipText("<html>" + tipMsg + "</html>");
		        multiUserMenu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						final boolean isMultMode = multiUserMenu.isSelected();
						if(isMultMode){
							JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html><body style=\"width:500\"><STRONG>Multi User Mode</STRONG> : ON" +
									"<BR><BR><STRONG>What is Multi User Mode?</STRONG><BR>" + tipMsg + ". " +
									"password is required on many menu in mutli-user mode.<br>" +
									"if you click the same menu or other within few minutes, system treade you as a valid user." +
									"<BR><BR>" +
									"<strong>forget password</strong>" +
									"<br>1.shutdown HomeCenter," +
									"<br>2.open '<strong>hc_config.properties</strong>', find key : <strong>password</strong>," +
									"<br>3.change to <strong>password=MTIzNDU2Nzg\\=</strong>" +
									"<br>4.new password is <strong>12345678</strong></body></html>",
									App.getSysIcon(App.SYS_INFO_ICON),SwingConstants.LEADING)
									);
							App.showCenterPanel(panel, 0, 0, "Multi User Mode");
						}else{
							if(checkPassword(false, multiUserMenu.getText()) == false){
								multiUserMenu.setSelected(true);
								return;
							}
							
							JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html><STRONG>Multi User Mode</STRONG> : off" +
									"</html>",
									App.getSysIcon(App.SYS_INFO_ICON),SwingConstants.LEADING)
									);
							App.showCenterPanel(panel, 0, 0, "Multi User Mode");
						}
						PropertiesManager.setValue(PropertiesManager.p_isMultiUserMode, 
								isMultMode?IConstant.TRUE:IConstant.FALSE);
						PropertiesManager.saveFile();					}
				});
		        hcMenu.add(multiUserMenu);
			}
			
			hcMenu.addSeparator();
	        //浏览当天当前的日志
	        final JMenuItem browseCurrLog = new JMenuItem((String)ResourceUtil.get(9002));
	        ImageIcon currLog = null;
			try {
				currLog = new ImageIcon(ImageIO.read(ImageSrc.LOG_ICON));
			} catch (IOException e2) {
			}
			browseCurrLog.setIcon(currLog);
			browseCurrLog.setToolTipText("view current log of starting up HomeCenter server");
			ActionListener currLogAction = new ActionListener() {
				String opName = browseCurrLog.getText();
				LogViewer lv;
				public void actionPerformed(ActionEvent e) {
					if(checkPassword(true, opName)){
						if(lv == null || lv.isShowing() == false){
							String pwd = PropertiesManager.getValue(PropertiesManager.p_LogPassword1);
			
							byte[] pwdBS;
							try {
								pwdBS = App.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
								lv = viewLog(ImageSrc.HC_LOG, pwdBS, (String)ResourceUtil.get(9002));
							} catch (UnsupportedEncodingException e1) {
								e1.printStackTrace();
							}
						}else{
							lv.setVisible(true);
						}
						
					}
				}
	        };
			browseCurrLog.addActionListener(currLogAction);
			hcMenu.add(browseCurrLog);
	        
	        //浏览前次的日志
	        {
	        	File file = new File(ImageSrc.HC_LOG_BAK);
	            if(file.exists()){
			        final JMenuItem browseLogBak = new JMenuItem((String)ResourceUtil.get(9003));
			        ImageIcon currbakLog = null;
			        try {
						currbakLog = new ImageIcon(ImageIO.read(ImageSrc.LOG_BAK_ICON));
					} catch (IOException e2) {
					}
					browseLogBak.setIcon(currbakLog);
					browseLogBak.setToolTipText("view log of last shutdown HomeCenter server");
					browseLogBak.addActionListener(new ActionListener() {
						String opName = browseLogBak.getText();
						LogViewer lv;
						public void actionPerformed(ActionEvent e) {
							if(checkPassword(true, opName)){
								if(lv == null || lv.isShowing() == false){
									String pwd = PropertiesManager.getValue(PropertiesManager.p_LogPassword2);
									if(pwd != null){
										byte[] pwdBS;
										try {
											pwdBS = App.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
											lv = viewLog(ImageSrc.HC_LOG_BAK, pwdBS, (String)ResourceUtil.get(9003));
										} catch (UnsupportedEncodingException e1) {
											e1.printStackTrace();
										}
									}else{
										runBrowser(ImageSrc.HC_LOG_BAK);
									}
								}else{
									lv.setVisible(true);
								}
							}
						}
			        });
					hcMenu.add(browseLogBak);
	            }
	        }
	        hcMenu.addSeparator();

//			hcMenu.addSeparator();
//			
//	        //Tutorial
//	        final JMenuItem turoItem = new JMenuItem((String)ResourceUtil.get(9029));
//	        try{
//	        	turoItem.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/tuto_22.png"))));
//	        }catch (Exception e) {
//				
//			}
//	        turoItem.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					App.showTuto();
//				}
//			});
//	        hcMenu.add(turoItem);
	        
	        //测试锁屏
//	        {
//	        	final JMenuItem lockScreenTestItem = new JMenuItem("锁屏测试");//(String)ResourceUtil.get(9029)
//		        try{
//		        	lockScreenTestItem.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/tuto_22.png"))));
//		        }catch (Exception e) {
//					
//				}
//		        lockScreenTestItem.addActionListener(new ActionListener() {
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						LockTester.startLockTest();
//					}
//				});
//		        hcMenu.add(lockScreenTestItem);
//	        }
	        
			//FAQ
			final JMenuItem faqItem = new JMenuItem((String)ResourceUtil.get(9013));//菜单项
	        try {
	        	faqItem.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/faq22.png"))));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	        faqItem.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	refreshActionMS(false);
	    			String targetURL;
					try {
						targetURL = HttpUtil.buildLangURL("pc/faq.htm", null);
		            	HttpUtil.browseLangURL(targetURL);
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}

	            }
	        });
	        hcMenu.add(faqItem);
	        
//	        hcMenu.addSeparator();
	        
	        //由于续费，所以关闭isDonateToken条件
//			if(TokenManager.isDonateToken() == false){
	        	final JMenuItem vip = new JMenuItem("VIP Register");
	        	try {
					vip.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/vip_22.png"))));
				} catch (IOException e4) {
				}
	        	vip.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						App.showUnlock();
					}
				});
	        	hcMenu.add(vip);
//        	}
			
			//aboutus
			final JMenuItem aboutusItem = new JMenuItem("About HomeCenter");
			try{
				aboutusItem.setIcon(hcIcon);
			}catch (Exception e) {
				
			}
			aboutAction = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					refreshActionMS(false);
					
					final JDialog dialog = new JDialog();
					dialog.setTitle("About HomeCenter");
					dialog.setIconImage(App.SYS_LOGO);

					final ActionListener disposeAction = new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							dialog.dispose();
						}
					};

					dialog.getRootPane().registerKeyboardAction(disposeAction, 
						KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
						JComponent.WHEN_IN_FOCUSED_WINDOW);

					JPanel panel = new JPanel();
					panel.setLayout(new GridLayout(0,1,3,3));
					try {
						JLabel icon = new JLabel(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/hc_32.png"))));
						panel.add(icon);
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					JLabel productName = new JLabel("HomeCenter - connect PC and IoT mobile platform", null, JLabel.CENTER);
					try {
						productName.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/verify_22.png"))));
					} catch (IOException e3) {
					}
					
					panel.add(productName);
					
					JLabel ver = new JLabel("HomeCenter version : " + StarterManager.getHCVersion(), null, JLabel.CENTER);
					panel.add(ver);
					
					//今年的年数
					Calendar cal = Calendar.getInstance();
					int today_year = cal.get(Calendar.YEAR);
					String copyright = "Copyright © 2011 - " + today_year + " HomeCenter.MOBI";
					panel.add(new JLabel(copyright, null, JLabel.CENTER));
					panel.add(new JLabel("Mozilla Public License Version 1.1", null, JLabel.CENTER));

					JButton jbOK = null;
					try {
						jbOK = new JButton("O K", new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					{
						JButton iconPower = null;
						try {
							Font defaultBtnFont = jbOK.getFont();
							iconPower = new JButton("Icons(some) by:LazyCrazy  http://www.artdesigner.lv", new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/art.png"))));
							HashMap<TextAttribute, Object> hm = new HashMap<TextAttribute, Object>(); 
							hm.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);  // 定义是否有下划线 
							hm.put(TextAttribute.SIZE, defaultBtnFont.getSize());    // 定义字号 
							hm.put(TextAttribute.FAMILY, defaultBtnFont.getFamily());    // 定义字体名 
							Font font = new Font(hm);    // 生成字号为12，字体为宋体，字形带有下划线的字体 
							iconPower.setFont(font);
//							iconPower.setBorderPainted(true);
							iconPower.setBorder(BorderFactory.createEmptyBorder());
						} catch (IOException e2) {
						}
						iconPower.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								HttpUtil.browse("http://www.artdesigner.lv");
							}
						});
						iconPower.setFocusable(false);
						panel.add(iconPower);
					}
					JButton jbMail = null;
					try {
						Font defaultBtnFont = jbOK.getFont();
						jbMail = new JButton("help@homecenter.mobi", new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/email_22.png"))));
						HashMap<TextAttribute, Object> hm = new HashMap<TextAttribute, Object>(); 
						hm.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);  // 定义是否有下划线 
						hm.put(TextAttribute.SIZE, defaultBtnFont.getSize());    // 定义字号 
						hm.put(TextAttribute.FAMILY, defaultBtnFont.getFamily());    // 定义字体名 
						Font font = new Font(hm);    // 生成字号为12，字体为宋体，字形带有下划线的字体 
						jbMail.setFont(font);
						jbMail.setBorderPainted(true);
						jbMail.setBorder(BorderFactory.createEmptyBorder());
					} catch (IOException e2) {
						e2.printStackTrace();
					}
					jbMail.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
				            Desktop desktop = Desktop.getDesktop();  
				            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.MAIL)) {  
				                try {
									desktop.mail(new java.net.URI("mailto:help@homecenter.mobi?subject=[HomeCenter.MOBI]%20Help%20:%20"));
								} catch (IOException e1) {
									e1.printStackTrace();
								} catch (URISyntaxException e1) {
									e1.printStackTrace();
								}  
				            }else{
				            	JOptionPane.showConfirmDialog(dialog, "Unable call operating system sending mail function!", "Error", JOptionPane.ERROR_MESSAGE);
				            }
	
						}
					});
					jbMail.setFocusable(false);
					panel.add(jbMail);
	
					jbOK.addActionListener(disposeAction);
					panel.add(jbOK);
					
					
					dialog.add(panel);
					panel.setBorder(new EmptyBorder(10, 10, 10, 10));
					
					dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					jbOK.setFocusable(true);
					dialog.getRootPane().setDefaultButton(jbOK);
					jbOK.requestFocus();
					
					dialog.pack();
					dialog.setResizable(false);
					
					App.showCenter(dialog);
				}
			};
			aboutusItem.addActionListener(aboutAction);
				
			hcMenu.add(aboutusItem);
			
        }
        popupTi.add(hcMenu);
        
        popupTi.addSeparator();
        
        //选择语言
        final JMenu langSubItem = new JMenu("Language");
        try{
        	langSubItem.setIcon(new ImageIcon(ImageIO.read(ImageSrc.LANG_ICON)));
        }catch (Exception e) {
			
		}
        {
        	boolean isEn = PropertiesManager.isTrue(PropertiesManager.p_ForceEn);
        	
	        ButtonGroup group = new ButtonGroup();
	        final JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem("Auto Detect");
	        try {
				rbMenuItem.setIcon(new ImageIcon(ImageIO.read(ImageSrc.LANG_ICON)));
			} catch (IOException e2) {
			}
	        rbMenuItem.setSelected(!isEn);
	        group.add(rbMenuItem);
	        langSubItem.add(rbMenuItem);
	        rbMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					refreshActionMS(false);
					
					UILang.setLocale(null);
					PropertiesManager.setValue(PropertiesManager.p_ForceEn, IConstant.FALSE);
					PropertiesManager.saveFile();
					buildMenu(UILang.getUsedLocale());
				}
			});
	
	        final JRadioButtonMenuItem enMenuItem = new JRadioButtonMenuItem("English");
	        try {
				enMenuItem.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/en_22.png"))));
			} catch (IOException e1) {
			}
	        enMenuItem.setSelected(isEn);
	        group.add(enMenuItem);
	        langSubItem.add(enMenuItem);
	        enMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					refreshActionMS(false);
					
					final Locale locale = UILang.EN_LOCALE;
					UILang.setLocale(locale);
					PropertiesManager.setValue(PropertiesManager.p_ForceEn, IConstant.TRUE);
					PropertiesManager.saveFile();
					buildMenu(UILang.getUsedLocale());
				}
			});
        }
        popupTi.add(langSubItem);
        
		//登录改为密码
		final JMenuItem loginItem = new JMenuItem((String)ResourceUtil.get(1007));//菜单项
        try {
			loginItem.setIcon(new ImageIcon(ImageIO.read(ImageSrc.PASSWORD_ICON)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        loginItem.setToolTipText("<html>change mobile login password.<BR>password is saved locally.</html>");
        loginItem.addActionListener(new ActionListener() {
        	String opName = loginItem.getText();
            public void actionPerformed(ActionEvent e) {
            	if(checkPassword(false, opName)){
            		App.showInputPWDDialog(IConstant.uuid, "", "", false);
//            		new LoginDialog(loginItem);
            	}
            }
        });
        popupTi.add(loginItem);
        
        {
        	Class checkJMFClass = null;
    		try{
    			checkJMFClass = Class.forName("javax.media.CaptureDeviceManager");
    		}catch (Throwable e) {
    		}
    		//关闭Capture
    		if(checkJMFClass != null){
	        	if((ResourceUtil.isWindowsOS() == false)
	        			|| ResourceUtil.isWindowsXP()){
	        		//仅限非Windows或XP
	        		buildCaptureMenu();
	        	}
    		}
        }
        
        popupTi.addSeparator();

        {
        	//建菜单
        	buildCertMenu(popupTi);
        }
                
        popupTi.addSeparator();
        
        {
//    		if((RootConfig.getInstance().isTrue(RootConfig.p_ShowDesinger))){
//    	        if(RootConfig.getInstance().isTrue(RootConfig.p_ShowDesingerToAll) 
//    	        		|| TokenManager.isDonateToken()){
//    		        popupTi.addSeparator();
        	{
    		        final JMenuItem designer = new JMenuItem((String)ResourceUtil.get(9034));//菜单项
    		        ImageIcon designIco = null;
    		        
    				//检查是否有新版本
    				final String lastSampleVer = PropertiesManager.getValue(PropertiesManager.p_LastSampleVer, "1.0");
    				if(StringUtil.higer(RootConfig.getInstance().getProperty(RootConfig.p_Sample_Ver), lastSampleVer)){
    					try {
        					designIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/designernew_22.png")));
        				} catch (IOException e2) {
        					e2.printStackTrace();
        				}
    				}else{
        				try {
        					designIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/designer_22.png")));
        				} catch (IOException e2) {
        					e2.printStackTrace();
        				}    					
    				}

    				designer.setToolTipText("<html>powerful mobile menus and UI designer. " +
    						"<BR>Load HAR project of smart devices for mobile accessing." +
    						//"<BR>for example, import jar libs, program complex UI to control home smart devices and PC." +
    						//"<BR>install JRuby engine online is required." +
    						"</html>");
	            	designer.setIcon(designIco);
    	            designer.addActionListener(new ActionListener() {
    	                public void actionPerformed(ActionEvent e) {
    	                	refreshActionMS(false);
    	                	
//        	                	IDArrayGroup.showMsg(IDArrayGroup.MSG_ID_DESIGNER, "<html><body style=\"width:700\">Mobile Designer is platform to design mobile menu and UI forms (for smart devices, DVD, TV...). " +
//    							"<BR>It is more than access PC desktop screen." +
//    							"<BR>On this platform, you can just click and move to build controller for IoT, import jar libraries, design powerful mobile UI to control and view these devices, load HAR project for smart devices." +
//    							"<BR><BR>click '" + (String)ResourceUtil.get(IContext.OK) + "' to install JRuby engine online.</body></html>",
//        	                			(String) ResourceUtil.get(IContext.INFO));
//        	                	JPanel panel = new JPanel();
//        	            		panel.add(new JLabel("<html>click '" + (String)ResourceUtil.get(IContext.OK) + "' to download JRuby online.</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.CENTER));
//        	            		App.showCenterPanel(panel, 0, 0, (String) ResourceUtil.get(IContext.INFO), false, null, null, null, null, null, true, false);

//									agreeBiz.start();
//            					}//安装前提示
        					runDesigner();
    	                }
    	            });
    	            popupTi.add(designer);
        	}
//    	        }
//            }

        	{
        		final JMenuItem linkItem = new JMenuItem((String)ResourceUtil.get(9059));//菜单项
            	try {
            		linkItem.setIcon(new ImageIcon(ImageIO.read(
    						ResourceUtil.getResource("hc/res/menu_22.png"))));
    			} catch (IOException e1) {
    			}
    			linkItem.setToolTipText("<html>add, delete, config project(s).<BR>select project to edit." +
    					"</html>");
            	linkItem.addActionListener(new ActionListener() {
    	            public void actionPerformed(ActionEvent e) {
	            		LinkMenuManager.showLinkPanel(null);
    	            }
    	        });
    	        
    	        popupTi.add(linkItem);
        	}
        	
			final JMenuItem mapItem = new JMenuItem((String)ResourceUtil.get(9035));//菜单项
        	try {
        		mapItem.setIcon(new ImageIcon(ImageIO.read(
						ResourceUtil.getResource("hc/res/map_22.png"))));
			} catch (IOException e1) {
			}
			mapItem.setToolTipText("<html>when access desktop screen from mobile, " +
					"<BR>click icon (mapping shortcut key) to execute shortcut key(s) on PC.</html>");
        	mapItem.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	refreshActionMS(false);
	            	
	            	try{
	            		KeyComperPanel.showKeyComperPanel();
	            	}catch (Exception ee) {
						JOptionPane.showConfirmDialog(null, "Cant load Key", 
								"Error", JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
					}
	            }
	        });
	        
	        popupTi.add(mapItem);
        }

        
        if(StarterManager.hadUpgradeError){
	        popupTi.addSeparator();
	        final String downloadMe = "download me";
			final JMenuItem verifyItem = new JMenuItem(downloadMe);
	        try {
	        	verifyItem.setIcon(new ImageIcon(ImageIO.read(
	        			ResourceUtil.getResource("hc/res/verify_22.png"))));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	        
	        verifyItem.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					JPanel panel = new JPanel();
					panel.add(new JLabel("<html><body style='width:400'>" +
							"System try upgrade starter.jar, but it fails, for more information see log.<br>Please do as following by hand.<BR><BR>" +
							"1. click 'O K' to download zip from http://homecenter.mobi<BR>" +
							"2. shutdown this HomeCenter App Server<BR>" +
							"3. unzip and override older HomeCenter App Server<BR>" +
							"4. run HomeCenter App Server, this '<strong>"+downloadMe+"</strong>' menu will disappear.<BR>" +
							"</body></html>"));
					App.showCenterPanel(panel, 0, 0, "Fail on upgrade starter!", false, null, null, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							HttpUtil.browse("http://homecenter.mobi/download/HC_Server.zip?verifyme");
						}
					}, null, null, false, false, null, false, false);
				}});
	        
	        popupTi.add(verifyItem);
        }

        popupTi.addSeparator();
        
		//退出
        final int indexOfAT = IConstant.uuid.indexOf("@");
        final String uuid_for_email = (indexOfAT>0)?IConstant.uuid.substring(0, indexOfAT):IConstant.uuid;
        final JMenuItem exitItem = new JMenuItem(
        		(String)ResourceUtil.get(IContext.EXIT) + "   (" + uuid_for_email + ")");//菜单项
        try {
			exitItem.setIcon(new ImageIcon(ImageIO.read(ImageSrc.EXIT_ICON)));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        exitItem.addActionListener(new ActionListener() {
        	String opName = exitItem.getText();
            public void actionPerformed(ActionEvent e) {
            	if(checkPassword(true, opName)){
            		
            		//直接采用主线程，会导致退出提示信息会延时显示，效果较差
            		DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
						@Override
						public void doBiz() {
		            		App.showCenterMessage((String)ResourceUtil.get(9067));
		            		
		            		HttpUtil.notifyStopServer(false, null);
		            		//以上逻辑不能置于notifyShutdown中，因为这些方法有可能被其它外部事件，如手机下线，中继下线触发。
		            		
		            		RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);

		            		ExitManager.startForceExitThread();
		            		ContextManager.notifyShutdown();
						}
					});
            	}
            }
        });
        popupTi.add(exitItem);
        
		// 切换皮肤时，会导致异常，所以不复用对象。
        String oldTip = null;//保留ToolTip
		if (ti != null) {
			oldTip = ti.getToolTip();
			ti.remove();
		}
		try {
			ContextManager.statusListen = this;
			ti = new JPTrayIcon((ti != null ? ti.getImage() : hc_Disable),
					(String) ResourceUtil.get(UILang.PRODUCT_NAME), popupTi);
			if(oldTip != null){
				ti.setToolTip(oldTip);
			}
//			关闭缺省的菜单功能
//			if(aboutAction != null){
//				ti.setDefaultActionListener(aboutAction);
//			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}// 图标，标题，右键弹出菜单
    }
	
	private void buildCaptureMenu() {
		popupTi.addSeparator();

		final JMenu cameraMenu = new JMenu((String)ResourceUtil.get(9048));
		ImageIcon cameraIcon = null;
		try{
			cameraIcon = new ImageIcon(ImageSrc.BUILD_SMALL_ICON);
		}catch (Exception e) {
		}
		cameraMenu.setToolTipText("<html>record video by USB camera, moving object will be smart detected and snapshot." +
				"<BR>require Java Media Framework (jmf) installed or install online</html>");
		
		//camera
		final JMenuItem cameraItem = new JMenuItem((String)ResourceUtil.get(9040), cameraIcon);
		try{
			cameraMenu.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/camera_22.png"))));
		}catch (Exception e) {
		}
		cameraMenu.add(cameraItem);
		
		Class checkJMFClass = null;
		try{
			checkJMFClass = Class.forName("javax.media.CaptureDeviceManager");
		}catch (Throwable e) {
		}
		if(checkJMFClass == null){
			cameraItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					CapManager.installJMF();
				}
			});
			
			popupTi.add(cameraMenu);
			//未安装JMF
			return ;
		}else{
			cameraItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SingleJFrame.showJFrame(CapControlFrame.class);
				}
			});
		}
		final JMenuItem cameraStart = new JMenuItem();
		final JMenuItem cameraStop = new JMenuItem();
		cameraStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CapPreviewPane.doStart();
			}
		});
		cameraStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CapPreviewPane.doStop();
			}
		});
		cameraMenu.add(cameraStart);
		
		cameraStop.setText((String)ResourceUtil.get(9052));
		try{
			cameraStop.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/stop_22.png"))));
		}catch (Exception e) {
		}
		cameraMenu.add(cameraStop);

		capNotify.setMenuItem(cameraMenu, cameraStart, cameraStop);
		int capEnable = CapStream.CAP_DISABLE;
		try{
			capEnable = CaptureConfig.isCapEnable()?CapStream.CAP_ENABLE:CapStream.CAP_DISABLE;
		    capNotify.notifyNewMsg(String.valueOf(capEnable));
		    final CapStream instance = CapStream.getInstance(false);
		    if(instance != null){
		    	capNotify.notifyNewMsg(String.valueOf(instance.getCapStatus()));
		    }
		}catch (Throwable e) {
			//有可能未安装，没有相关类。
		}
		
		popupTi.add(cameraMenu);
	}

	/**
     * 暂停使用，因为中继端可能升级，导致不可预知的问题
     * @param hcMenu
     */
	private void buildAutoUpgradMenuItem(final JMenu hcMenu) {
		final JCheckBoxMenuItem upgradeItem = new JCheckBoxMenuItem((String)ResourceUtil.get(9031));
		upgradeItem.setSelected(!PropertiesManager.isTrue(PropertiesManager.p_isNotAutoUpgrade));
		try{
			upgradeItem.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/upgrade_22.png"))));
		}catch (Exception e) {
		}
		upgradeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final boolean isAutoAfterClick = upgradeItem.isSelected();
				if(isAutoAfterClick == false){
					JPanel panel = new JPanel(new BorderLayout());
					panel.add(new JLabel("<html><body style=\"width:700\">" +
							"Important :" +
							"<BR>" +
							"<BR>1. if upgrade mobile side, please keep this App server to be the newest." +
							"<BR>2. if upgrade App Server, please keep mobile side to be the newest." +
							"<BR>3. in insecure networks, please do NOT upgrade." +
							"<BR>4. if exception, try keep both to be the newest, maybe the relay server was upgraded." +
							"<BR><BR>click '" + (String) ResourceUtil.get(IContext.OK) + "' to continue." +
							"</body></html>"), BorderLayout.CENTER);
					App.showCenterPanel(panel, 0, 0, "Before Disable Auto Upgrade...", true, null, null, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							flipAutoUpgrade(upgradeItem, isAutoAfterClick);
						}
					}, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							upgradeItem.setSelected(!isAutoAfterClick);
						}
					}, null, false, false, null, false, false);
				}else{				
					flipAutoUpgrade(upgradeItem, isAutoAfterClick);
				}
			}
		});
		hcMenu.add(upgradeItem);
	}
    
	public void showTray() {
		try {
			disable_dl_certkey = new ImageIcon(ImageIO.read(ImageSrc.DISABLE_DL_CERTKEY_ICON));
			dl_certkey = new ImageIcon(ImageIO.read(ImageSrc.DL_CERTKEY_ICON));
			hc_Enable = ImageIO.read(ResourceUtil.getResource("hc/res/hc_48.jpg"));
			hc_Disable = ImageIO.read(ResourceUtil.getResource("hc/res/hc_dis_48.jpg"));
			hc_mobi = ImageIO.read(ResourceUtil.getResource("hc/res/hc_mobi_48.jpg"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		buildMenu(UILang.getUsedLocale());
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
        	public void run(){
        		//因为系统调用System.exit时，会激活此处。
        		if(ContextManager.cmStatus != ContextManager.STATUS_EXIT){
	        		L.V = L.O ? false : LogManager.log("User Power Off");
	        		ContextManager.notifyShutdown();
        		}
        	}
        });
        
        if(IConstant.serverSide){
        	String msg = (String)ResourceUtil.get(9009);
			displayMessage((String)ResourceUtil.get(IContext.INFO), msg, 
        			IContext.INFO, null, 0);
        	ti.setToolTip(msg);
        }
	}
	
	public static String[][] splitFileAndVer(String files, boolean isTempMode){
		String[] fs = files.split(";");
		
		int size = fs.length;
		String[][] out = new String[size][2];
		
		for (int i = 0; i < size; i++) {
			String[] tmp = fs[i].split(":");
			if(tmp.length < 2){
				return null;
			}
			out[i][0] = tmp[0];
			out[i][1] = tmp[1];
		}
		
		return out;
	}
	
	public static LogViewer viewLog(String fileName, byte[] pwdBS, String title) { 
        try {  
            return LogViewer.loadFile(fileName, pwdBS, title);
        } catch (Exception ex) {  
            ex.printStackTrace();  
            JOptionPane.showMessageDialog(null, ex.toString());
        }  
        return null;
    }  
	
	public static void runBrowser(String fileName) {  
        try {  
            File file = new File(fileName);
            if(file.exists() == false){
            	ContextManager.getContextInstance().displayMessage((String) ResourceUtil.get(IContext.ERROR), 
						(String) ResourceUtil.get(9004), IContext.ERROR, null, 0);
            	return;
            }
            Desktop desktop = Desktop.getDesktop();  
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.OPEN)) {  
                desktop.open(file);  
            }else{
            	ContextManager.getContextInstance().displayMessage((String) ResourceUtil.get(IContext.ERROR), 
						(String) ResourceUtil.get(9005), IContext.ERROR, null, 0);
            }
        } catch (IOException ex) {  
            ex.printStackTrace();  
        }  
    }  
	
	public Object getSysImg() {
		return App.SYS_LOGO;
	}

	public boolean isSoundOnMode() {
		//TODO isSoundOff
		return false;
	}
	
//	public final void send(byte type_request, InetAddress address, int port) {
//		DatagramPacket p = (DatagramPacket)dpCacher.getFree();
//		
//		byte[] bs = p.getData();
//		
//		Message.setSendUUID(bs, selfUUID, selfUUID.length);
//		
//		bs[MsgBuilder.INDEX_CTRL_TAG] = type_request;
//		bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;
//		p.setLength(MsgBuilder.MIN_LEN_MSG);
////		p.setData(bs, 0, MsgBuilder.MIN_LEN_MSG);
//		
//		p.setAddress(address);
//		p.setPort(port);
//		
//		sServer.pushIn(p);
//	}
	
//	/**
//	 * 该逻辑被切换中继，j2se客户端，服务器进入接入等调用
//	 * 注意：各调用环境的条件状态
//	 */
//	public final void setTargetPeer(String ip, String port, Object datagram) throws Exception {
//		if(datagram == null){
//			int status = ContextManager.cmStatus;
//			
//			try {
//				if(status == ContextManager.STATUS_SERVER_SELF || status == ContextManager.STATUS_CLIENT_SELF){
//					//Relay切换到另一个Relay
//				}else{
//					J2SESIPContext.endPunchHoleProcess();
//				}
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("setTargetPeer, IP:" + ip + ":" + port);
//				TargetPeer tp = new TargetPeer();
//				tp.clientInet = InetAddress.getByName(ip);
//				tp.clientPort = Integer.parseInt(port);
//				KeepaliveManager.setClient(tp);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}else{
//			DatagramPacket packet = (DatagramPacket)datagram;
//			try {
//				packet.setAddress(InetAddress.getByName(ip));
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			}
//			packet.setPort(Integer.parseInt(port));
//		}
//	}

	public Object doExtBiz(short bizNo, Object newParam) {
		if(bizNo == IContext.BIZ_LOAD_SERVER_CONFIG){
			if(IConstant.serverSide){
//				if(SIPManager.isRelayServerNATType(NatHcTimer.LocalNATType)){
//					//具备中继型的服务器，对客户机进行最优发现，上限不超过均值
//					sc.setTryMaxMTU(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);
//				}else if(SIPManager.isOnRelay()){
//					//依赖于中继的服务器，上限不能高于均值
//					sc.setTryMaxMTU(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);					
//				}else{
//					//TODO 从用户配置中，获得尝试的最大传送块
//					sc.setTryMaxMTU(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);
//				}
//
//				//TODO 从用户配置中，获得是否要求进行最优发现，缺省为0
//				sc.setMTU(1448);//MTU设置为0，表示通知客户端，要进行最优发现
//				sc.setTryMaxMTU(Integer.parseInt(
//						RootConfig.getInstance().getProperty(RootConfig.p_MaxUDPSize)));//仅在MTU设置为0下，最优化时，尝试的上限(含)
				
				//abc###efg
				ServerConfig sc = new ServerConfig("");
				sc.setProperty(ServerConfig.p_HC_VERSION, StarterManager.getHCVersion());
				return sc.toTransString();
			}else{
				return null;
			}
//		}else if(bizNo == IContext.BIZ_IS_ON_SERVICE){
//			if(IConstant.serverSide){
//				KeepaliveManager.keepalive.setEnable(true);
//			}
		}else if(bizNo == IContext.BIZ_UPLOAD_LINE_ON){
//			String hostAddress = SIPManager.LocalNATIPAddress;
//
//			if(hostAddress.equals(SIPManager.getExternalUPnPIPAddress())){
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("UPnP external IP == Public IP");
//				SIPManager.LocalNATPort = SIPManager.getExternalUPnPPort();
//				SIPManager.LocalNATType = EnumNAT.OPEN_INTERNET;
//			}
			
//			if(IConstant.serverSide && SIPManager.isRelayFullOpenMode(SIPManager.LocalNATType)){
//				RelayManager.notifyIsRelayServer();
//			}

//			ti.putTip(JPTrayIcon.NAT_DESC, EnumNAT.getNatDesc(UDPChannel.nattype));
//			ti.putTip(JPTrayIcon.PUBLIC_IP, hostAddress + ":" + String.valueOf(UDPChannel.publicPort));
			
			String out = RootServerConnector.lineOn(
					IConstant.uuid, KeepaliveManager.homeWirelessIpPort.ip, 
					KeepaliveManager.homeWirelessIpPort.port, 0, 1, 
					KeepaliveManager.relayServerUPnPIP, KeepaliveManager.relayServerUPnPPort,
					SIPManager.relayIpPort.ip, SIPManager.relayIpPort.port, TokenManager.getToken(), 
					!isEnableTransNewCertNow(), RootServerConnector.getHideToken());
			
			if(out == null){
				LogManager.err("unable to connect root server");
				SIPManager.notifyRelineon(true);
				String[] ret = {"false"};
				return ret;
			}else if(out.equals("e")){
				String msg = "Same ID is using now, try another ID!";
				LogManager.err(msg);
				
				JOptionPane.showMessageDialog(null,
					msg, (String) ResourceUtil
							.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE);
				
				notifyShutdown();
				String[] ret = {"false"};
				return ret;
			}else if(out.equals("d")){
				RootConfig.getInstance().setProperty(RootConfig.p_Color_On_Relay, "5");
				RootConfig.getInstance().setProperty(RootConfig.p_MS_On_Relay, 100);
			}
			
			setTrayEnable(true);
			String msg = (String) ResourceUtil.get(9008);
			ContextManager.displayMessage((String) ResourceUtil.get(IContext.INFO), 
				msg, IContext.INFO, 0);

			//将启动置于成功上线提示之后，因为启用应用层时，可能防会产生提示消息，而被盖掉
			if(ServerUIUtil.getResponsor() == null){
				//启动应用服务器必须紧跟上传，以防手机先入后，导致应用服务器尚未初始化
				ServerUIUtil.restartResponsorServer();
			}
			
			try{
				J2SEContext.ti.setToolTip(msg + " (ID:" + IConstant.uuid + ")");
			}catch (Exception e) {
				//出现ShutDown时的空情形，所以要加异常
			}

			SingleMessageNotify.closeDialog(SingleMessageNotify.TYPE_ERROR_CONNECTION);
			
//			本地无线信息，不需在日志中出现。
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Success line on " 
//						+ ServerUIUtil.replaceIPWithHC(KeepaliveManager.homeWirelessIpPort.ip) + ", port : " 
//						+ KeepaliveManager.homeWirelessIpPort.port + " for client connection");
			
			//上传上线信息
			if(SIPManager.relayIpPort.port > 0){	
				hc.core.L.V=hc.core.L.O?false:LogManager.log("Success line on " 
						+ HttpUtil.replaceIPWithHC(SIPManager.relayIpPort.ip)  + ", port : " 
						+ SIPManager.relayIpPort.port + " for client connection");
			}
			
			return null;
		}else if(bizNo == IContext.BIZ_AFTER_HOLE){
			if(IConstant.serverSide){
//				KeepaliveManager.keepalive.setEnable(true);
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Active Keepalive Watcher");
				
				//开启断线侦测
//				KeepaliveManager.ConnBuilderWatcher.setEnable(true);
				
				//关闭连接异常侦测
				//注意：与keepAliveWatcher.setEnable状态互斥
//				NatHcTimer.timerPunchHole.setEnable(false);
				
				//服务器端
				String sc = (String)ContextManager.getContextInstance().doExtBiz(IContext.BIZ_LOAD_SERVER_CONFIG, null);
				ContextManager.getContextInstance().send(
						MsgBuilder.E_TRANS_SERVER_CONFIG, sc != null?sc:"");//必须发送，因为手机端会返回
				L.V = L.O ? false : LogManager.log("Transed Server Config");
				
				//传输完Server_Config后，进入MTU设置或最优发现侦听
//				if(ServerConfig.getInstance().getMTU() != 0){
//					SIPManager.notifyUDPSize(ServerConfig.getInstance().getMTU());
//				}else{
//					//初始化MTU于服务器
////					MsgBuilder.refreshUDPByteLen(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);
//					
//					//加载MTU优化事件侦听器
//					if(ServerMTUTimer != null){
//						ServerRecMaxMTU = 0;
//						ServerMTUTimer.resetTimerCount();
//						ServerMTUTimer.setEnable(true);
//					}else{
//						ServerMTUTimer = new HCTimer("MTU Best", 4000, true){
//							public void doBiz() {
//								if(ServerRecMaxMTU > 0){
//									SIPManager.notifyUDPSize(ServerRecMaxMTU);
//									setEnable(false);
//									
//									//没有加2，由客户端以补充存储两位空间
//									ContextManager.getContextInstance().send(MsgBuilder.E_FIND_UDP_MTU_SIZE_BACK, String.valueOf(ServerRecMaxMTU));
//
//									ServerRecMaxMTU = 0;
//								}else{
//									hc.core.L.V=hc.core.L.O?false:LogManager.log("No receive MTU UDP");
//								}
//							}};
//						EventCenter.addListener(new IEventHCListener(){
//							final DataMTUTest dmt = new DataMTUTest();
//							public boolean action(final byte[] bs) {
//								byte[] bs = event.data_bs;
//								dmt.setBytes(bs);
//								if(dmt.passData()){
//									int tmpMaxMTU = dmt.getLength() + MsgBuilder.INDEX_MSG_DATA;
//									if(tmpMaxMTU > ServerRecMaxMTU){
//										ServerRecMaxMTU = tmpMaxMTU;
//										if(ServerMTUTimer != null){
//											ServerMTUTimer.resetTimerCount();
//										}
//									}
//								}
//								return true;
//							}
//
//							public byte getEventTag() {
//								return MsgBuilder.E_FIND_UDP_MTU_SIZE;
//							}});
//					}
//				}

				//等待，让ServerConfig先于后面的包到达目标

//				DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
//					@Override
//					public void doBiz() {
//						try{
//							Thread.sleep(1000);
//						}catch (Exception e) {
//							
//						}
//					}
//				});
				//并将该随机数发送给客户机，客户机用同法处理后回转给服务器
				//服务器据此判断客户机的CertKey和密码状态				

//				ContextManager.setStatus(ContextManager.STATUS_READY_FOR_CLIENT);
			}
			
			return null;
		}
		
//		if(bizNo == IContext.BIZ_OPEN_REQ_BUILD_CONN_LISTEN){
//			EventCenter.addListener(new IEventHCListener(){
//				public boolean action(final byte[] bs) {
//					if(ContextManager.isNotWorkingStatus()){
//						send(MsgBuilder.E_TAG_REQUEST_BUILD_CONNECTION);
//						hc.core.L.V=hc.core.L.O?false:LogManager.log("Successful connect to the target peer");
//						ContextManager.setStatus(ContextManager.STATUS_READY_FOR_CLIENT);
//						if(IConstant.serverSide){
//							//将askForService移出，因为有可能密码验证不通过
//							NatHcTimer.LISTENER_FROM_HTTP.setEnable(false);
//						}
//					}
//					return true;
//				}
//	
//				public byte getEventTag() {
//					return MsgBuilder.E_TAG_REQUEST_BUILD_CONNECTION;
//				}});
//			return null;
//		}
		if(bizNo == IContext.BIZ_CHANGE_RELAY){
			String[] ips = (String[])newParam;
			RootServerConnector.changeRelay(IConstant.uuid, ips[0], ips[1], TokenManager.getToken());
			return null;
		}else if(bizNo == IContext.BIZ_GET_TOKEN){
			return TokenManager.getTokenBS();
		}else if(bizNo == IContext.BIZ_SET_TRAY_ENABLE){
			if(newParam instanceof Boolean){
				boolean b = (Boolean)newParam;
				setTrayEnable(b);
			}
			return null;
		}
		
		if(IConstant.serverSide){
			if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS){
				doAfterCertKeyAndPwdPass();
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR){
				doAfterCertKeyError();
				RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_CertErr_STR);
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
				doAfterPwdError();
				RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_pwdErr_STR);
			}
		}else{
			if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_SERVICE_IS_FULL){
				
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_UNKNOW_STATUS){
				
			}
		}
		if(bizNo == IContext.BIZ_VERSION_MID_OR_PC){
			return StarterManager.getHCVersion();
		}else if(bizNo == IContext.BIZ_GET_REQ_MOBI_VER_FROM_PC){
			return "6.67";
		}
		return null;
	}

	private void doAfterCertKeyAndPwdPass() {
		if(ScreenServer.askForService()){
			ServerCUtil.transOneTimeCertKey();
			
			hc.core.L.V=hc.core.L.O?false:LogManager.log("Pass Certification Key and password");
			
			//由于会传送OneTimeKey，所以不需要下步
			//instance.send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS));

			try{
				Thread.sleep(1000);
			}catch (Exception e) {
			}
			
			ContextManager.setStatus(ContextManager.STATUS_SERVER_SELF);
			
			//由原来的客户端请求,改为服务器推送
			doCanvasMain("menu://root");
			
//			final TargetPeer target = KeepaliveManager.target;

			String msg = (String)ResourceUtil.get(9012);
			displayMessage((String) ResourceUtil.get(IContext.INFO), msg, 
					IContext.INFO, null, 0);
			ti.setToolTip(msg);
			ti.setImage(hc_mobi);

			//关闭以防被再次接入
			RootServerConnector.delLineInfo(TokenManager.getToken(), true);
		}else{
			hc.core.L.V=hc.core.L.O?false:LogManager.log("Service is full, error for client connection");
			send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_SERVICE_IS_FULL));
			sleepAfterError();
			SIPManager.notifyRelineon(true);
		}
	}

	private static void doAfterPwdError() {
		hc.core.L.V=hc.core.L.O?false:LogManager.log("Send Error password status to client");
		ContextManager.getContextInstance().send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR));
		sleepAfterError();
		
		SIPManager.notifyRelineon(true);
	}
	
	public static void sleepAfterError(){
		try{
			Thread.sleep(1000);
		}catch (Exception e) {
		}
	}

	private static void doAfterCertKeyError() {
		if(isEnableTransNewCertNow()){
			//传输证书
			transNewCertKey();
//			try{
//				//增加时间，确保transOneTimeCertKey后于NewCertKey
//				Thread.sleep(300);
//			}catch (Exception e) {
//				
//			}
			hc.core.L.V=hc.core.L.O?false:LogManager.log(RootServerConnector.unObfuscate("rtnapsro teCtrK yet  olceitn."));
		}else{
			hc.core.L.V=hc.core.L.O?false:LogManager.log("reject a mobile login with invalid certification.");
			ContextManager.getContextInstance().send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR));
			SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_ERROR_CERT, 
					"a mobile try login with ERROR certification<BR><BR>If you had created new certification, please enable transmit, " +
					"<BR>then the new certification will be transmitted to mobile.", "Error Certification", 1000 * 60,
					App.getSysIcon(App.SYS_ERROR_ICON));
//			LogManager.errToLog("Mobile login with ERROR CertKey");
			sleepAfterError();
			SIPManager.notifyRelineon(true);
		}
	}

	private static void transNewCertKey() {
//		LogManager.logInTest("send Cert : " + CUtil.toHexString(CUtil.CertKey));
		ServerCUtil.transCertKey(CUtil.CertKey, MsgBuilder.E_TRANS_NEW_CERT_KEY, false);
	}
	
	private static boolean enableTransCertKey;
	
	public static void enableTransCertKey(boolean enable){
		if(enable){
			PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.TRUE);
		}else{
			PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.FALSE);
		}
		PropertiesManager.saveFile();
		enableTransCertKey = enable;

		RootServerConnector.refreshRootAlive(TokenManager.getToken(),
				!isEnableTransNewCertNow(), RootServerConnector.getHideToken());
	}
	
	public static boolean isEnableTransNewCertNow(){
		return enableTransCertKey;
	}
	
	public static void initEnableTransNewCert(){
		String out = PropertiesManager.getValue(PropertiesManager.p_EnableTransNewCertKeyNow);
		if(out == null || out.equals(IConstant.TRUE)){
			enableTransCertKey = true;
		}else{
			enableTransCertKey = false;
		}
	}

	public Object getProperty(Object propertyID) {
		return PropertiesManager.getValue((String)propertyID);
	}

//	public void copyDatagramAddress(Object from, Object to) {
//		DatagramPacket dpTo = (DatagramPacket)to;
//		DatagramPacket dpFrom = (DatagramPacket)from;
//		
//		dpTo.setSocketAddress(dpFrom.getSocketAddress());
//	}

	final IHCURLAction urlAction = new J2SEServerURLAction();
	
	public static final String jrubyjarname = "jruby.jar";
	public IHCURLAction getHCURLAction() {
		return urlAction;
	}

	@Override
	public void notify(short statusFrom, short statusTo) {
		if(statusTo == ContextManager.STATUS_LINEOFF){
			
			if(statusFrom == ContextManager.STATUS_SERVER_SELF){
				ScreenServer.emptyScreen();
				ServerUIUtil.getResponsor().onEvent(ProjectContext.EVENT_SYS_MOBILE_LOGOUT);
			}
		}
		
		if(statusTo == ContextManager.STATUS_NEED_NAT){
			setTrayEnable(false);
			if(ti != null){
				ti.setDefaultToolTip();
			}
		}else if(statusFrom == ContextManager.STATUS_NEED_NAT){
		}

		if(statusTo == ContextManager.STATUS_READY_TO_LINE_ON){
		}

		if(statusTo == ContextManager.STATUS_READY_MTU){
			//直联或中继初始接入，但未进入验证参数传送
			KeepaliveManager.ConnBuilderWatcher.resetTimerCount();
			KeepaliveManager.ConnBuilderWatcher.setEnable(true);				
		}
		
		if(statusTo == ContextManager.STATUS_SERVER_SELF){
//			L.V = L.O ? false : LogManager.log("set setIntervalMS to " + KeepaliveManager.KEEPALIVE_MS);
			KeepaliveManager.keepalive.setIntervalMS(KeepaliveManager.KEEPALIVE_MS);
		}
	}
	
	private void setTrayEnable(boolean b){
//		L.V = L.O ? false : LogManager.log("TrayEnable:" + b);
		if(b){
			if(ti != null){
				ti.setImage(hc_Enable);
			}
		}else{
			if(ti != null){
				if(ti.getImage() == hc_Enable){
					displayMessage((String) ResourceUtil.get(IContext.INFO), 
							(String)ResourceUtil.get(9009), 
							IContext.INFO, null, 0);
				}
				ti.setImage(hc_Disable);
			}
		}
	}


	private void runDesigner() {
		LinkMenuManager.startDesigner();
	}
	
	public static boolean isTransedToMobileSize = false;
	
	private void doAfterCertIsNotTransed() {
		App.setNoTransCert();
		
		if(!isEnableTransNewCertNow()){
			flipTransable(!isEnableTransNewCertNow(), false);
		}
		transNewCertKey.setEnabled(false);
		
		JPanel panel = new JPanel(new BorderLayout());
		try {
			panel.add(new JLabel((String) ResourceUtil.get(9007), new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)), SwingConstants.LEFT), BorderLayout.NORTH);
			panel.add(new JLabel("<html><body style=\"width:600\"><BR><STRONG>" + (String) ResourceUtil.get(IContext.TIP) + "</STRONG>" +
					"<BR>1. new certification is stored locally, <strong>not</strong> on HomeCenter.MOBI" +
					"<BR>2. if '<strong>Transmit Certification : on</strong>', mobile phone will download and save it" +
					"<BR>3. if '<strong>Transmit Certification : off</strong>', server will hide to mobile phone with wrong certification, to defend harmful attacks" +
					"</body></html>"), BorderLayout.CENTER);
		} catch (IOException e2) {
		}
		App.showCenterPanel(panel, 0, 0, (String) ResourceUtil.get(IContext.INFO), 
				false, null, null, null, null, null, false, false, null, false, false);
	}

	private void doAfterMobileReceivedCert() {
		JPanel nextpanel = new JPanel();
		try {
			nextpanel.add(new JLabel((String) ResourceUtil.get(9032), new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)), SwingConstants.LEFT));
		} catch (IOException e2) {
		}
		App.showCenterPanel(nextpanel, 0, 0, (String) ResourceUtil.get(IContext.INFO), false, null, null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			}
		}, null, null, false, false, null, false, false);
	}

	public static final String MAX_HC_VER = "9999999";//注意与Starter.NO_UPGRADE_VER保持同步
	
	private void flipAutoUpgrade(final JCheckBoxMenuItem upgradeItem,
			final boolean isAutoAfterClick) {
		
		//更新到Properties文件中
		Properties start = new Properties();
		try{
			start.load(new FileInputStream(StarterManager._STARTER_PROP_FILE_NAME));
		}catch (Throwable ex) {
			JOptionPane.showConfirmDialog(null, "Unable open file starter.properties.", "Error", JOptionPane.ERROR_MESSAGE);
			upgradeItem.setSelected(!isAutoAfterClick);					
			return;
		}
		if(isAutoAfterClick){
			start.setProperty("ver", 
					PropertiesManager.getValue(PropertiesManager.p_LasterAutoUpgradeVer));
		}else{
			PropertiesManager.setValue(PropertiesManager.p_LasterAutoUpgradeVer,
					start.getProperty("ver"));
			start.setProperty("ver", MAX_HC_VER);
		}
		try {
			start.store(new FileOutputStream(StarterManager._STARTER_PROP_FILE_NAME), "");
		} catch (Exception e1) {
			JOptionPane.showConfirmDialog(null, "Unable write file " +StarterManager. _STARTER_PROP_FILE_NAME + ".", "Error", JOptionPane.ERROR_MESSAGE);
			upgradeItem.setSelected(!isAutoAfterClick);
			return;
		}

//			upgradeItem.setSelected(isOldAuto);
		PropertiesManager.setValue(PropertiesManager.p_isNotAutoUpgrade, 
				(isAutoAfterClick == false)?IConstant.TRUE:IConstant.FALSE);
		PropertiesManager.saveFile();
		
		if(isAutoAfterClick){
			ImageIcon icon = null;
			try {
				icon = new ImageIcon(ImageIO.read(ImageSrc.OK_ICON));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			JPanel panel = new JPanel();
			panel.add(new JLabel("<html><body style=\"width:500\">Upgrading will check and work when this app server restart." +
					"<BR><BR>" +
					"Note : this is NO effect to mobile side.</body></html>", icon, SwingConstants.LEFT));
			App.showCenterPanel(panel, 0, 0, "Success Enable Auto Upgrade!", false, null, null, null, null, null, true, false, null, false, false);
		}
	}

	private void createNewCertification() {
		App.generateCert();
		
		if(ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
			//确保送达
			
			isTransedToMobileSize = false;
			
			transNewCertKey();
			
			ConditionWatcher.addWatcher(new IWatcher() {
				long curr = System.currentTimeMillis();
				@Override
				public boolean watch() {
					if(System.currentTimeMillis() - curr > 2000){
						ProjectContext.sendStaticMessage((String) ResourceUtil.get(IContext.INFO), (String) ResourceUtil.get(9033), IContext.INFO, null, 0);
						return true;
					}
					return false;
				}
				
				@Override
				public void setPara(Object p) {
				}
				
				@Override
				public boolean isNotCancelable() {
					return false;
				}
				
				@Override
				public void cancel() {
				}
			});
			final JPanel panel = new JPanel();
			try {
				panel.add(new JLabel((String) ResourceUtil.get(9007), new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)), SwingConstants.LEFT));
			} catch (IOException e2) {
			}
			final Window window = App.showCenterPanel(panel, 0, 0, (String) ResourceUtil.get(IContext.INFO), false, null, null, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
				}
			}, null, null, false, false, null, false, false);
			ConditionWatcher.addWatcher(new IWatcher(){
				long curr = System.currentTimeMillis();
				
				@Override
				public boolean watch() {
					if(window.isShowing()){
						return false;
					}
					
					if(System.currentTimeMillis() - curr > 6000){
						if(isTransedToMobileSize){
							doAfterMobileReceivedCert();
						}else{
							doAfterCertIsNotTransed();
						}
					}

					
					return true;
				}

				@Override
				public void setPara(Object p) {
				}

				@Override
				public void cancel() {
				}

				@Override
				public boolean isNotCancelable() {
					return false;
				}});
			return ;
			//END 在线传送证书
		}else{
			doAfterCertIsNotTransed();
		}
	}

	public static void notifyExitByMobi() {
		hc.core.L.V=hc.core.L.O?false:LogManager.log("Client/Relay request lineoff!");

		ContextManager.getContextInstance().displayMessage(
				(String)ResourceUtil.get(IContext.INFO), 
				(String)ResourceUtil.get(9006), IContext.INFO, null, 0);

		RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_MobReqExitToPC_STR);

		SIPManager.notifyRelineon(true);
	}

	public static void appendTitleJRubyVer(JFrame frame) {
		String ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		if(ver != null){
			frame.setTitle(frame.getTitle() + " - [JRuby:" + ver + "]");
		}
	}

	public static void flipTransable(final boolean isEnable, final boolean isCallAfterTrans) {
		enableTransCertKey(isEnable);
		if(isEnable){
			transNewCertKey.setIcon(dl_certkey);
			transNewCertKey.setText((String)ResourceUtil.get(1021));	
		}else{
			transNewCertKey.setIcon(disable_dl_certkey);
			transNewCertKey.setText((String)ResourceUtil.get(1020));			
		}
		
		JPanel iconPanle = new JPanel();
		iconPanle.setLayout(new BoxLayout(iconPanle, BoxLayout.X_AXIS));
		iconPanle.add(Box.createHorizontalGlue());
		iconPanle.add(new JLabel(isEnable?disable_dl_certkey:dl_certkey));
		iconPanle.add(Box.createHorizontalGlue());
		try {
			iconPanle.add(new JLabel(new ImageIcon(ImageIO.read(ImageSrc.MOVE_TO_G_ICON))));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		iconPanle.add(Box.createHorizontalGlue());
		iconPanle.add(new JLabel(isEnable?dl_certkey:disable_dl_certkey));
		iconPanle.add(Box.createHorizontalGlue());
		
		JPanel tipPanle = new JPanel(new BorderLayout(0, 20));
		
		tipPanle.add(iconPanle, BorderLayout.CENTER);
		{
			JPanel msgPanel = new JPanel(new BorderLayout());
			msgPanel.add(new JLabel("<html><body style='width:450' align='left'><strong>"+(String)ResourceUtil.get(IContext.TIP)+"</strong> : <BR>"+(isEnable?transOnTip:transOffTip)+"</body></html>"), 
					BorderLayout.CENTER);
			tipPanle.add(msgPanel, BorderLayout.SOUTH);
		}

		App.showCenterPanel(tipPanle, 0, 0, (isEnable?(String)ResourceUtil.get(1020):(String)ResourceUtil.get(1021)), false, null, null, null, null, null, false, false, null, false, false);
	}
}

class PWDDialog extends JDialog {
	JPanel pwdPanel = new JPanel();
	JPanel btnPanel = new JPanel();
	Border border1;
	TitledBorder titledBorder1;
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JButton jbOK = null;
	JButton jbExit = null;
	JPasswordField jPasswordField1 = new JPasswordField(15);// 20个字符宽度

	public PWDDialog() {
		setModal(true);
		
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init() throws Exception {

		setTitle("HomeCenter");
		this.setIconImage(App.SYS_LOGO);//new File("hc/res/hc_16.png")
		
		java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jbExit_actionPerformed(e);
			}
		};

		this.getRootPane().registerKeyboardAction(exitActionListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		//必须有此行代码，作为窗口右上的关闭响应
//		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				jbExit_actionPerformed(null);
			}

		});
		
		jbOK = new JButton("", new ImageIcon(ImageSrc.OK_ICON));
		jbExit = new JButton("", new ImageIcon(ImageSrc.CANCEL_ICON));

		// new LineBorder(Color.LIGHT_GRAY, 1, true)
		titledBorder1 = new TitledBorder((String)ResourceUtil.get(1007));// BorderFactory.createEtchedBorder()
		
		JPanel root = new JPanel();
		
		App.addBorderGap(this.getContentPane(), root);
		
		root.setLayout(gridBagLayout2);
		pwdPanel.setLayout(new FlowLayout());
		pwdPanel.setBorder(titledBorder1);

		jbOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				System.out.println(e);
				if(e.getSource() == jbOK){ 
					jbOK_actionPerformed(e);
				}
			}
		});

//		jlPassword.setHorizontalAlignment(SwingConstants.RIGHT);
		jbOK.setNextFocusableComponent(jbExit);
		jbOK.setSelected(true);
		jbOK.setText((String) ResourceUtil.get(IContext.OK));

		jbExit.setText((String) ResourceUtil.get(1018));
		jbExit.addActionListener(exitActionListener);

		jPasswordField1.setEchoChar('*');
		jPasswordField1.setHorizontalAlignment(SwingUtilities.RIGHT);
		jPasswordField1.enableInputMethods(true);
		
		pwdPanel.add(new JLabel("", new ImageIcon(ImageSrc.PASSWORD_ICON), SwingConstants.CENTER));
		pwdPanel.add(jPasswordField1);
		root.add(
				pwdPanel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(jbOK);
		btnPanel.add(jbExit);
		root.add(
				btnPanel,
				new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		this.getRootPane().setDefaultButton(jbOK);

		pack();
		// int width = 400, height = 270;
		int width = getWidth(), height = getHeight();
		setSize(width, height);

		App.showCenter(this);
		
	}
	
	void jbOK_actionPerformed(ActionEvent e) {
		pwd = jPasswordField1.getText();
		endDialog();
	}

	void jbExit_actionPerformed(ActionEvent e) {
		endDialog();
	}

	private void endDialog() {
		super.dispose();
	}
	
	public String pwd;
}
class CapNotify implements IMsgNotifier {
	ImageIcon readyIcon, recordingCameraIcon, recordIcon, pauseIcon, resumeIcon;
	private final String CMD_RECORD = (String)ResourceUtil.get(9049);
	private final String CMD_PAUSE = (String)ResourceUtil.get(9050);
	private final String CMD_RESUME = (String)ResourceUtil.get(9051);

	@Override
	public void notifyNewMsg(String msg) {
		final int currStatus = Integer.parseInt(msg);
		
		if(readyIcon == null){
			try{
		    	readyIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/camera_22.png")));
		    }catch (Exception e) {
			}
			try{
				recordingCameraIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/camera_on_22.png")));
		    }catch (Exception e) {
				
			}
			try{
				recordIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/record_22.png")));
				pauseIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/pause_22.png")));
				resumeIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/play_22.png")));
			}catch (Exception e) {
			}
		}
		if(currStatus == CapStream.CAP_RECORDING
				|| currStatus == CapStream.CAP_PAUSEING){
			capItem.setIcon(recordingCameraIcon);
		}else{
			capItem.setIcon(readyIcon);
		}
		
		if(currStatus == CapStream.CAP_ENABLE){
			startItem.setVisible(true);
		} else {
			CapStream instance = null;
			try{
				instance = CapStream.getInstance(false);
			}catch (Throwable e) {
			}
			if((currStatus == CapStream.CAP_DISABLE)
					&& (instance == null 
						|| instance.getCapStatus() == CapStream.CAP_NO_WORKING)){
				startItem.setVisible(false);
				stopItem.setVisible(false);
			}
		}
		
		if(currStatus == CapStream.CAP_NO_WORKING){
			startItem.setText(CMD_RECORD);
			startItem.setIcon(recordIcon);
			stopItem.setVisible(false);
			if(CaptureConfig.isCapEnable() == false){
				startItem.setVisible(false);
			}
		}else if(currStatus == CapStream.CAP_RECORDING){
			startItem.setText(CMD_PAUSE);
			startItem.setIcon(pauseIcon);
			stopItem.setVisible(true);
		}else if(currStatus == CapStream.CAP_PAUSEING){
			startItem.setText(CMD_RESUME);
			startItem.setIcon(resumeIcon);
		}
	}

	@Override
	public String getNextMsg() {
		return null;
	}
	private JMenuItem startItem, stopItem;
	private JMenu capItem;
	
	public void setMenuItem(JMenu item, JMenuItem startItem, JMenuItem stopItem){
		this.capItem = item;
		this.startItem = startItem;
		this.stopItem = stopItem;
	}
	
}
abstract class LineonAndServingExecWatcher implements IWatcher{
	long currMS = System.currentTimeMillis();
	final String opName;
	
	LineonAndServingExecWatcher(String opName){
		this.opName = opName;
	}
	
	public abstract void doBiz();
	
	@Override
	public boolean watch() {
		//防止在连接中途发生关闭传输，从而导致证书不予传输的情形
		if(ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON
				|| ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
        	doBiz();
        	return true;
		}else{
			if(System.currentTimeMillis() - currMS > 1000 * 10){
				L.V = L.O ? false : LogManager.log("Unknow status, skip execute op [" + opName + "]");
				return true;
			}
			return false;
		}
	}

	@Override
	public void setPara(Object p) {
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isNotCancelable() {
		return false;
	}}