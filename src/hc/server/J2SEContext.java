package hc.server;

import hc.App;
import hc.PlatformTrayIcon;
import hc.core.ClientInitor;
import hc.core.ConditionWatcher;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.EventCenter;
import hc.core.HCMessage;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.IStatusListen;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.ReceiveServer;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.cache.CacheManager;
import hc.core.cache.PendStore;
import hc.core.data.ServerConfig;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionJSON;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.res.ImageSrc;
import hc.server.data.KeyComperPanel;
import hc.server.msb.WiFiHelper;
import hc.server.ui.ClientDesc;
import hc.server.ui.JcipManager;
import hc.server.ui.LogViewer;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCEventQueue;
import hc.server.util.HCJDialog;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.ServerCUtil;
import hc.util.BaseResponsor;
import hc.util.ExitManager;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class J2SEContext extends CommJ2SEContext implements IStatusListen{
	private static PlatformTrayIcon ti;
    private static JMenuItem transNewCertKey;
	private final HCEventQueue hcEventQueue = HCLimitSecurityManager.getHCSecurityManager().getHCEventQueue();
	private final Thread eventDispatchThread = HCLimitSecurityManager.getHCSecurityManager().getEventDispatchThread();
    protected final ThreadGroup threadPoolToken = App.getThreadPoolToken();
    
    @Override
    public final boolean isInLimitThread(){
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			return true;
		}
		return false;
    }
    
	public J2SEContext() {
		super(false);
		
		ToolTipManager.sharedInstance().setDismissDelay(1000 * 20);

		SIPManager.setSIPContext(new J2SESIPContext(){

			@Override
			public Object buildSocket(final int localPort, final String targetServer, final int targetPort){
				final Object s = super.buildSocket(localPort, targetServer, targetPort);
				if(s == null){
					//要进行重连，不同于其它如Root，
					SIPManager.notifyRelineon(false);
				}
				return s;
			}
		});
		
		ContextManager.setContextInstance(this);

		super.init(new ReceiveServer(), new J2SEUDPReceiveServer());

		if(IConstant.serverSide){
//			RelayManager.start(HttpUtil.getLocalIP(), SIPManager.relayPort, null);
			
			showTray();
			
			{
				//提前初始相应线程
				final Object obj = KeepaliveManager.keepalive;
			}
			
			ServerUIUtil.restartResponsorServer(null, null);
			
			KeepaliveManager.keepalive.doNowAsynchronous();
			KeepaliveManager.keepalive.setEnable(true);
		}
		
	}
	
	@Override
	public void interrupt(final Thread thread){
		thread.interrupt();
	}
	
	@Override
	public void exit(){
		ExitManager.exit();				
	}
	
	@Override
	public void notifyShutdown(){
		//获得全部通讯，并通知下线。
		L.V = L.O ? false : LogManager.log("Shut down");
		
		//关闭用户工程组
		ServerUIUtil.stop();
		
    	ContextManager.getThreadPool().run(new Runnable(){
			@Override
			public void run() {
				if(ti != null){
					ti.remove();
					ti.exit();
					ti = null;
				}
			}}, threadPoolToken);
    	
    	ContextManager.shutDown();
    }
	
//	private static long lastCanvasMainAction = 0;
	private static void doCanvasMain(final String url){
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
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				final BaseResponsor responsor = ServerUIUtil.getResponsor();
				responsor.onEvent(ProjectContext.EVENT_SYS_MOBILE_LOGIN);
				
				ClientDesc.getAgent().set(ConfigManager.UI_IS_BACKGROUND, IConstant.FALSE);
				responsor.onEvent(ProjectContext.EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND);
			}
		});
		
		HCURLUtil.process(url, ContextManager.getContextInstance().getHCURLAction());
	}
	
	@Override
	public void run() {
		super.run();
		
		ClientInitor.doNothing();
		
		EventCenter.addListener(new IEventHCListener() {
			@Override
			public byte getEventTag() {
				return MsgBuilder.E_CLASS;
			}
			
			@Override
			public final boolean action(final byte[] bs) {
				//classNameLen(4) + classBS + paraLen(4) + paraBS
				
				int nextReadIdx = MsgBuilder.INDEX_MSG_DATA;
				final int classBSLen = (int)ByteUtil.fourBytesToLong(bs, nextReadIdx);
				nextReadIdx += 4;
				final String className = ByteUtil.buildString(bs, nextReadIdx, classBSLen, IConstant.UTF_8);
				nextReadIdx += classBSLen;
				final int paraBSLen = (int)ByteUtil.fourBytesToLong(bs, nextReadIdx);
				nextReadIdx += 4;
				
				J2SEEClassHelper.dispatch(className, bs, nextReadIdx, paraBSLen);
				return true;
			}
		});
		
		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final boolean action(final byte[] bs) {
//				if(SIPManager.isSameUUID(event.data_bs)){
					if(IConstant.serverSide){
						//客户端主动下线
						final String token = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
						if(token.equals(RootServerConnector.getHideToken())){					
							notifyExitByMobi();
						}else{
							L.V = L.O ? false : LogManager.log("Error Token at client shutdown");
						}
					}else{
						//TODO j2se客户机
					}
					return true;
//				}
//				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_SHUT_DOWN_BETWEEN_CS;
			}});
		
		startAllServers();
		
//		KeepaliveManager.keepalive.setEnable(true);
//		KeepaliveManager.keepalive.doNowAsynchronous();
		
		if(IConstant.serverSide){
			//服务器端增加各种MobiUI应答逻辑
			
			EventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs) {
					final String jcip = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
					JcipManager.responseCtrlSubmit(jcip);
					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_CTRL_SUBMIT;
				}});

			EventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs) {
					final String url = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);

					doCanvasMain(url);

					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_CANVAS_MAIN;
				}});
			
			EventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs) {
					if(ScreenServer.isServing() == false){
						ClientDesc.refreshClientInfo(HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA));

						//TODO YYH 旧版本的通知，已被新版本替代，未来需要关闭。如果手机版本过低，产生通知
						final String pcReqMobiVer = (String)doExtBiz(BIZ_GET_REQ_MOBI_VER_FROM_PC, null);
						if(StringUtil.higer(pcReqMobiVer, ClientDesc.getHCClientVer())){
							send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS));
							LogManager.err("Min required mobile version : [" + pcReqMobiVer + "], current mobile version : [" + ClientDesc.getHCClientVer() + "]");
							L.V = L.O ? false : LogManager.log("Cancel mobile login process");
							sleepAfterError();
							SIPManager.notifyRelineon(false);
							return true;
						}
						
						CUtil.setUserExtFactor(ClientDesc.getAgent().getEncryptionStrength());
						
						//服务器产生一个随机数，用CertKey和密码处理后待用，
						final byte[] randomBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
						CCoreUtil.generateRandomKey(App.getStartMS(), randomBS, MsgBuilder.INDEX_MSG_DATA, CUtil.TRANS_CERT_KEY_LEN);
						CUtil.resetCheck();
						CUtil.SERVER_READY_TO_CHECK = randomBS;
						
						final byte[] randomEvent = ContextManager.cloneDatagram(randomBS);
						L.V = L.O ? false : LogManager.log("Send random data to client for check CK and password");
//						try{
//							Thread.sleep(50);
//						}catch (Exception e) {
//						}
						send(MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD, randomEvent, CUtil.TRANS_CERT_KEY_LEN);

					}else{
						L.V = L.O ? false : LogManager.log("In Serving, Skip other client desc");
					}
					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_CLIENT_INFO;
				}});
			
			EventCenter.addListener(new IEventHCListener() {
				@Override
				public final boolean action(final byte[] bs) {
					final Vector<PendStore> vector = (Vector<PendStore>)ServerUIAPIAgent.getMobileAttribute(ServerUIAPIAgent.ATTRIBUTE_PEND_CACHE);
					savePendStore(vector, bs, MsgBuilder.INDEX_MSG_DATA, HCMessage.getMsgLen(bs));
					return true;
				}
				
				private final void savePendStore(final Vector<PendStore> pendStoreVector, final byte[] code, final int offset, final int len){
					synchronized(pendStoreVector){
						final int size = pendStoreVector.size();
						for (int i = 0; i < size; i++) {
							final PendStore ps = pendStoreVector.elementAt(i);
							final byte[] psCode = ps.codeBS;
							boolean match = (len == psCode.length);
							if(match){
								for (int j = 0; j < len; j++) {
									if(psCode[j] != code[j + offset]){
										match = false;
										break;
									}
								}
							}
							
							if(match){
								CacheManager.storeCache(ps.projID, ps.uuid, ps.urlID, 
										ps.projIDbs, 0, ps.projIDbs.length, 
										ps.uuidBS, 0, ps.uuidBS.length, 
										ps.urlIDBS, 0, ps.urlIDBS.length, 
										ps.codeBS, 0, ps.codeBS.length, 
										ps.scriptBS, 0, ps.scriptBS.length, true);
								pendStoreVector.removeElementAt(i);
								return;
							}
						}
					}
					LogManager.errToLog("[cache] pend store is not matched.");
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_RESP_CACHE_OK;
				}
			});
			
			EventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs) {
					ServerUIUtil.getResponsor().activeNewOneTimeKeys();//服务器端收到确认，由于是在EventCenter进程，所以不需加锁
//					LogManager.info("successful E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL");

					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL;
				}});
		}
		
		PlatformManager.getService().startCapture();
		
		LinkMenuManager.startAutoUpgradeBiz();
	}
	
    static ImageIcon dl_certkey, disable_dl_certkey;
    Image hc_Enable, hc_Disable, hc_mobi;

    @Override
	public void displayMessage(final String caption, final String text, final int type, final Object imageData, final int timeOut){
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
     * 重要，请勿在Event线程中调用，
     * @param checkTime true表示仅当时间超过3分钟后，才进行验证；false表示只要是多模式，则必需验证
     * @param opName
     * @return
     */
    public static boolean checkPassword(final boolean checkTime, final String opName){
    	if(PropertiesManager.isTrue(PropertiesManager.p_isMultiUserMode)
    			&&
    				(	(checkTime == false) 
						|| (System.currentTimeMillis() - lastCheckMS > 1000 * 60 * 3))){
    		final PWDDialog pd = new PWDDialog();
    		
    		final String pwd = App.getFromBASE64(PropertiesManager.getValue(PropertiesManager.p_password));
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
    		final Object[] options={(String)ResourceUtil.get(1010)};
    		App.showOptionDialog(null, ResourceUtil.get(1019), "HomeCenter", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
    		return false;
    	}else{
    		refreshActionMS(false);
    		return true;
    	}
    }

	public static void refreshActionMS(final boolean isForce) {
		if(isForce || (System.currentTimeMillis() - lastCheckMS < 1000 * 60 * 3)){
			lastCheckMS = System.currentTimeMillis();
		}
	}
    
	final static String transOnTip = (String)ResourceUtil.get(9063);
	final static String transOffTip = (String)ResourceUtil.get(9064);

	private void buildCertMenu(final JPopupMenu popupMenu){
		final String str_certification = (String)ResourceUtil.get(9060);
		final JMenu certMenu = new JMenu(str_certification);
		certMenu.setIcon(new ImageIcon(ImageSrc.NEW_CERTKEY_ICON));
		
        //生成新证书
        final JMenuItem buildNewCertKey = new JMenuItem((String)ResourceUtil.get(9001));
		buildNewCertKey.setToolTipText("<html>" + (String)ResourceUtil.get(9120) + "</html>");//注意：9120被其它处使用
        buildNewCertKey.setIcon(new ImageIcon(ImageSrc.NEW_CERTKEY_ICON));
        buildNewCertKey.addActionListener(new HCActionListener(new Runnable() {
        	String opName = buildNewCertKey.getText();
			@Override
			public void run() {
				if(checkPassword(false, opName)){
            		ConditionWatcher.addWatcher(new LineonAndServingExecWatcher(buildNewCertKey.getText()){
						@Override
						public final void doBiz() {
							createNewCertification();
						}});
				}
			}
		}, threadPoolToken));
        certMenu.add(buildNewCertKey);
        
        //传输新证书开关
		transNewCertKey = new JMenuItem("");//菜单项
		final String transmitCert = (String)ResourceUtil.get(9117);
		final String tipOn = (String)ResourceUtil.get(9118);
		final String tipOff = (String)ResourceUtil.get(9119);
		transNewCertKey.setToolTipText("<html>" + transmitCert + ":<STRONG>" + tipOn + "</STRONG>, " +
				transOnTip +
				";" +
				"<BR>" + transmitCert + ":<STRONG>" + tipOff + "</STRONG>, " +
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
		
		transNewCertKey.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
            	refreshActionMS(false);
            	if(checkPassword(true, transNewCertKey.getText())){
            		ConditionWatcher.addWatcher(new LineonAndServingExecWatcher(transNewCertKey.getText()){
						@Override
						public final void doBiz() {
			            	flipTransable(!isEnableTransNewCertNow(), false);
						}});
            	}
			}
		}, threadPoolToken));
		certMenu.add(transNewCertKey);
        
//        {
//        	//QR
//            final JMenuItem qrInput = new JMenuItem("QR " + str_certification);
//            ImageIcon qrImg = new ImageIcon(ResourceUtil.loadImage("qr_22.png"));
//    		qrInput.setToolTipText("<html>display certification string, i will input it directly to mobile config panel," +
//    				"<BR>instead of transmit it on network.</html>");
//            qrInput.setIcon(qrImg);
//            qrInput.addActionListener(new HCActionListener(new Runnable() {
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
	
	private final void showLicense(final String title, final String license_url) {
		final JDialog dialog = new HCJDialog();
		dialog.setModal(true);
		dialog.setTitle(title);
		dialog.setIconImage(App.SYS_LOGO);

		final Container main = dialog.getContentPane();

		final JPanel c = new JPanel();
		c.setLayout(new BorderLayout(5, 5));

		main.setLayout(new GridBagLayout());
		main.add(c, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
						10, 10, 10, 10), 0, 0));

		final JTextArea area = new JTextArea(30, 30);
		try {
			final URL oracle = new URL(HttpUtil.replaceSimuURL(license_url, PropertiesManager.isTrue(PropertiesManager.p_IsSimu)));
			BufferedReader in;
			in = new BufferedReader(new InputStreamReader(oracle.openStream()));
			area.read(in, null);

			area.setEditable(false);
			area.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(final MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // 鼠标进入Text区后变为文本输入指针
				}

				@Override
				public void mouseExited(final MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // 鼠标离开Text区后恢复默认形态
				}
			});
			area.getCaret().addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					area.getCaret().setVisible(true); // 使Text区的文本光标显示
				}
			});
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					final String[] options = { "O K" };
					App.showOptionDialog(null,
							"Cant connect server, please try late!", "HomeCenter",
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
							null, options, options[0]);
				}
			});
			dialog.dispose();
			return;
		}
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				dialog.dispose();
			}
		});
		dialog.getRootPane().registerKeyboardAction(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					dialog.dispose();
				}
			},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		final JButton ok = App.buildDefaultOKButton();

		ok.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
			}
		}, threadPoolToken));

		final JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(ok);

		final JPanel botton = new JPanel();
		botton.setLayout(new BorderLayout(5, 5));
		botton.add(btnPanel, "East");

		final JScrollPane jsp = new JScrollPane(area);
		c.add(jsp, "Center");
		c.add(botton, "South");

		dialog.setSize(700, 600);
		dialog.setResizable(false);
		App.showCenter(dialog);
	}
	
	public void buildMenu(final Locale locale){

		popupTi = new JPopupMenu();
    	
    	popupTi.applyComponentOrientation(ComponentOrientation.getOrientation(locale));
    	
        final JMenu hcMenu = new JMenu("HomeCenter");
        ImageIcon hcIcon = null;
        try{
        	hcIcon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/hc_22.png")));
        }catch (final Exception e) {
			
		}
        try{
        	hcMenu.setIcon(hcIcon);
        }catch (final Exception e) {
		}

        ActionListener aboutAction;	
        
        {
        	final String title = (String)ResourceUtil.get(9040);
			final JMenuItem option = new JMenuItem(title);
            ImageIcon optionIco = null;
            try{
            	optionIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/option_22.png")));
            	option.setIcon(optionIco);
            }catch (final Exception e) {
    		}
            option.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					SingleJFrame.showJFrame(ConfigPane.class);							
				}
			}, threadPoolToken));
            hcMenu.add(option);
            
			buildAutoUpgradMenuItem(hcMenu);
	        
			{
		        //Multi user mode
		        final JCheckBoxMenuItem multiUserMenu = new JCheckBoxMenuItem((String)ResourceUtil.get(9058));
		        try{
		        	multiUserMenu.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/group_22.png"))));
		        }catch (final Exception e) {
				}
		        multiUserMenu.setSelected(PropertiesManager.isTrue(PropertiesManager.p_isMultiUserMode));
		        final String tipMsg = (String)ResourceUtil.get(9099);
		        multiUserMenu.setToolTipText("<html>" + tipMsg + "</html>");
		        multiUserMenu.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final boolean isMultMode = multiUserMenu.isSelected();
						if(isMultMode){
							final JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html><body style=\"width:500\">" + (String)ResourceUtil.get(9098) +
									"<BR><BR><STRONG>" + (String)ResourceUtil.get(9100) + "</STRONG><BR>" + tipMsg +
									"<BR>" + (String)ResourceUtil.get(9101) + "<br>" +
//											"if you click the same menu or other within few minutes, system treade you as a valid user." +
//											"<BR>" +
//											"<BR>" +
//											"<strong>forget password</strong>" +
//											"<br>1.shutdown HomeCenter," +
//											"<br>2.open '<strong>hc_config.properties</strong>', find key : <strong>password</strong>," +
//											"<br>3.change to <strong>password=MTIzNDU2Nzg\\=</strong>" +
//											"<br>4.new password is <strong>12345678</strong>" +
									"</body></html>",
									App.getSysIcon(App.SYS_INFO_ICON),SwingConstants.LEADING)
									);
							App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(9096));
						}else{
							if(checkPassword(false, multiUserMenu.getText()) == false){
								multiUserMenu.setSelected(true);
								return;
							}
							
							final JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html>" + (String)ResourceUtil.get(9097) + "</html>",
									App.getSysIcon(App.SYS_INFO_ICON),SwingConstants.LEADING)
									);
							App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(9096));
						}
						PropertiesManager.setValue(PropertiesManager.p_isMultiUserMode, 
								isMultMode?IConstant.TRUE:IConstant.FALSE);
						PropertiesManager.saveFile();
					}
				}, threadPoolToken));
		        hcMenu.add(multiUserMenu);
			}
			
			if(LogManager.INI_DEBUG_ON == false){
				hcMenu.addSeparator();
		        //浏览当天当前的日志
		        final JMenuItem browseCurrLog = new JMenuItem((String)ResourceUtil.get(9002));
				browseCurrLog.setIcon(new ImageIcon(ImageSrc.LOG_ICON));
				browseCurrLog.setToolTipText("view current log of starting up HomeCenter server");
				final ActionListener currLogAction = new HCActionListener(new Runnable() {
					String opName = browseCurrLog.getText();
					LogViewer lv;
					@Override
					public void run() {
						if(checkPassword(true, opName)){
							if(lv == null || lv.isShowing() == false){
								final String pwd = PropertiesManager.getValue(PropertiesManager.p_LogPassword1);
								final String ca1 = PropertiesManager.getValue(PropertiesManager.p_LogCipherAlgorithm1);
								
								byte[] pwdBS;
								try {
									pwdBS = App.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
									lv = viewLog(ImageSrc.HC_LOG, pwdBS, ca1, (String)ResourceUtil.get(9002));
								} catch (final UnsupportedEncodingException e) {
									ExceptionReporter.printStackTrace(e);
								}
							}else{
								lv.setVisible(true);
							}
						}
					}
				}, threadPoolToken);
				browseCurrLog.addActionListener(currLogAction);
				hcMenu.add(browseCurrLog);
				
				//浏览前次的日志
				final File file = new File(App.getBaseDir(), ImageSrc.HC_LOG_BAK);
	            if(file.exists()){
			        final JMenuItem browseLogBak = new JMenuItem((String)ResourceUtil.get(9003));
					browseLogBak.setIcon(new ImageIcon(ImageSrc.LOG_BAK_ICON));
					browseLogBak.setToolTipText("view log of last shutdown HomeCenter server");
					browseLogBak.addActionListener(new HCActionListener(new Runnable() {
						String opName = browseLogBak.getText();
						LogViewer lv;
						@Override
						public void run() {
							if(checkPassword(true, opName)){
								if(lv == null || lv.isShowing() == false){
									final String pwd = PropertiesManager.getValue(PropertiesManager.p_LogPassword2);
									final String ca2 = PropertiesManager.getValue(PropertiesManager.p_LogCipherAlgorithm2);
									
									if(pwd != null){
										byte[] pwdBS;
										try {
											pwdBS = App.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
											lv = viewLog(ImageSrc.HC_LOG_BAK, pwdBS, ca2, (String)ResourceUtil.get(9003));
										} catch (final UnsupportedEncodingException e) {
											ExceptionReporter.printStackTrace(e);
										}
									}else{
										runBrowser(ImageSrc.HC_LOG_BAK);
									}
								}else{
									lv.setVisible(true);
								}
							}
						}
					}, threadPoolToken));
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
//	        turoItem.addActionListener(new HCActionListener(new Runnable() {
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
//		        lockScreenTestItem.addActionListener(new HCActionListener(new Runnable() {
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
			} catch (final IOException e1) {
			}
	        faqItem.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					refreshActionMS(false);
	    			String targetURL;
					try {
						targetURL = HttpUtil.buildLangURL("pc/faq.htm", null);
		            	HttpUtil.browseLangURL(targetURL);
					} catch (final UnsupportedEncodingException e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
			}, threadPoolToken));
	        hcMenu.add(faqItem);
	        
//	        hcMenu.addSeparator();
	        
	        //由于续费，所以关闭isDonateToken条件
			if(ResourceUtil.isJ2SELimitFunction()){//TokenManager.isDonateToken() == false
	        	final JMenuItem vip = new JMenuItem("VIP Register");
	        	try {
					vip.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/vip_22.png"))));
				} catch (final IOException e4) {
				}
	        	vip.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						App.showUnlock();
					}
				}, threadPoolToken));
	        	hcMenu.add(vip);
        	}
			
			//aboutus
			final JMenuItem aboutusItem = new JMenuItem("About HomeCenter");
			try{
				aboutusItem.setIcon(hcIcon);
			}catch (final Exception e) {
				
			}
			aboutAction = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					refreshActionMS(false);
					
					final JDialog dialog = new HCJDialog();
					dialog.setTitle("About HomeCenter");
					dialog.setIconImage(App.SYS_LOGO);

					final ActionListener disposeAction = new HCActionListener(new Runnable() {
						@Override
						public void run() {
							dialog.dispose();
						}
					}, threadPoolToken);

					dialog.getRootPane().registerKeyboardAction(disposeAction, 
						KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
						JComponent.WHEN_IN_FOCUSED_WINDOW);

					final JPanel panel = new JPanel();
					panel.setLayout(new GridLayout(0,1,3,3));
					try {
						final JLabel icon = new JLabel(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/hc_32.png"))));
						panel.add(icon);
						
					} catch (final IOException e) {
						ExceptionReporter.printStackTrace(e);
					}
					
					final JLabel productName = new JLabel("HomeCenter - connect PC and IoT mobile platform", null, JLabel.CENTER);
					try {
						productName.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/verify_22.png"))));
					} catch (final IOException e3) {
					}
					
					panel.add(productName);
					
					final JLabel ver = new JLabel("HomeCenter version : " + StarterManager.getHCVersion(), null, JLabel.CENTER);
					panel.add(ver);
					
					//今年的年数
					final Calendar cal = Calendar.getInstance();
					final int today_year = cal.get(Calendar.YEAR);
					final String copyright = "Copyright © 2011 - " + today_year + " HomeCenter.MOBI";
					panel.add(new JLabel(copyright, null, JLabel.CENTER));

					final JButton jbOK = new JButton("O K", new ImageIcon(ImageSrc.OK_ICON));
					final Font defaultBtnFont = jbOK.getFont();

					{
						JButton licenseBtn = null;
						licenseBtn = new JButton("License");
						final HashMap<TextAttribute, Object> hm = new HashMap<TextAttribute, Object>(); 
						hm.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
						hm.put(TextAttribute.SIZE, defaultBtnFont.getSize());
						hm.put(TextAttribute.FAMILY, defaultBtnFont.getFamily());
						final Font font = new Font(hm);    // 生成字号为12，字体为宋体，字形带有下划线的字体 
						licenseBtn.setFont(font);
						licenseBtn.setBorder(BorderFactory.createEmptyBorder());
						licenseBtn.addActionListener(new HCActionListener(new Runnable() {
							@Override
							public void run() {
								showLicense("HomeCenter : License Agreement", "http://homecenter.mobi/bcl.txt");
							}
						}, threadPoolToken));
						panel.add(licenseBtn);
					}
					
					{
						JButton iconPower = null;
						try {
							iconPower = new JButton("Icons(some) by:LazyCrazy  http://www.artdesigner.lv", new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/art.png"))));
							final HashMap<TextAttribute, Object> hm = new HashMap<TextAttribute, Object>(); 
							hm.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);  // 定义是否有下划线 
							hm.put(TextAttribute.SIZE, defaultBtnFont.getSize());    // 定义字号 
							hm.put(TextAttribute.FAMILY, defaultBtnFont.getFamily());    // 定义字体名 
							final Font font = new Font(hm);    // 生成字号为12，字体为宋体，字形带有下划线的字体 
							iconPower.setFont(font);
//									iconPower.setBorderPainted(true);
							iconPower.setBorder(BorderFactory.createEmptyBorder());
							iconPower.addActionListener(new HCActionListener(new Runnable() {
								@Override
								public void run() {
									HttpUtil.browse("http://www.artdesigner.lv");
								}
							}, threadPoolToken));
							iconPower.setFocusable(false);
							panel.add(iconPower);
						} catch (final IOException e2) {
						}
					}
					JButton jbMail = null;
					try {
						jbMail = new JButton("help@homecenter.mobi", new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/email_22.png"))));
						final HashMap<TextAttribute, Object> hm = new HashMap<TextAttribute, Object>(); 
						hm.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);  // 定义是否有下划线 
						hm.put(TextAttribute.SIZE, defaultBtnFont.getSize());    // 定义字号 
						hm.put(TextAttribute.FAMILY, defaultBtnFont.getFamily());    // 定义字体名 
						final Font font = new Font(hm);    // 生成字号为12，字体为宋体，字形带有下划线的字体 
						jbMail.setFont(font);
						jbMail.setBorderPainted(true);
						jbMail.setBorder(BorderFactory.createEmptyBorder());
						jbMail.addActionListener(new HCActionListener(new Runnable() {
							@Override
							public void run() {
					            final Desktop desktop = Desktop.getDesktop();  
					            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.MAIL)) {  
					                try {
										desktop.mail(new java.net.URI("mailto:help@homecenter.mobi?subject=[HomeCenter.MOBI]%20Help%20:%20"));
									} catch (final Exception e1) {
						            	App.showConfirmDialog(dialog, "Unable call operating system sending mail function!", "Error", JOptionPane.ERROR_MESSAGE);
									}  
					            }else{
					            	App.showConfirmDialog(dialog, "Unable call operating system sending mail function!", "Error", JOptionPane.ERROR_MESSAGE);
					            }
		
							}
						}, threadPoolToken));
						jbMail.setFocusable(false);
						panel.add(jbMail);
					} catch (final IOException e) {
						ExceptionReporter.printStackTrace(e);
					}
	
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
			}, threadPoolToken);
			aboutusItem.addActionListener(aboutAction);
				
			hcMenu.add(aboutusItem);
			
        }
        popupTi.add(hcMenu);
        
        popupTi.addSeparator();
        
        //选择语言
        final JMenu langSubItem = new JMenu((String)ResourceUtil.get(9165));
    	langSubItem.setIcon(new ImageIcon(ImageSrc.LANG_ICON));
        {
        	final boolean isEn = PropertiesManager.isTrue(PropertiesManager.p_ForceEn);
        	
	        final ButtonGroup group = new ButtonGroup();
	        final JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem((String)ResourceUtil.get(9166));
			rbMenuItem.setIcon(new ImageIcon(ImageSrc.LANG_ICON));
	        rbMenuItem.setSelected(!isEn);
	        group.add(rbMenuItem);
	        langSubItem.add(rbMenuItem);
	        rbMenuItem.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					refreshActionMS(false);
					
					UILang.setLocale(null);
					PropertiesManager.setValue(PropertiesManager.p_ForceEn, IConstant.FALSE);
					PropertiesManager.saveFile();
					buildMenu(UILang.getUsedLocale());
				}
			}, threadPoolToken));
	
	        final JRadioButtonMenuItem enMenuItem = new JRadioButtonMenuItem("English");
	        try {
				enMenuItem.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/en_22.png"))));
			} catch (final IOException e1) {
			}
	        enMenuItem.setSelected(isEn);
	        group.add(enMenuItem);
	        langSubItem.add(enMenuItem);
	        enMenuItem.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					refreshActionMS(false);
					
					final Locale locale = UILang.EN_LOCALE;
					UILang.setLocale(locale);
					PropertiesManager.setValue(PropertiesManager.p_ForceEn, IConstant.TRUE);
					PropertiesManager.saveFile();
					buildMenu(UILang.getUsedLocale());
				}
			}, threadPoolToken));
        }
        popupTi.add(langSubItem);
        
        //注意：通过false，强制关闭WiFi密码配置界面
    	final boolean hasWiFiAccount = HCURL.isUsingWiFiWPS && (PropertiesManager.isTrue(PropertiesManager.p_WiFi_isMobileViaWiFi) || WiFiDeviceManager.getInstance().canCreateWiFiAccount());
    	if(hasWiFiAccount){
    		addPwdSubMenu(hasWiFiAccount);//管理系统和WiFi的密码子菜单
    	}else{
    		addSysPwdMenuItem(null, popupTi);//只添加系统密码菜单项到PopupMenu
    	}
        
       if(ResourceUtil.isJ2SELimitFunction()) {
        	Class checkJMFClass = null;
    		try{
    			checkJMFClass = Class.forName("javax.media.CaptureDeviceManager");
    		}catch (final Throwable e) {
    		}
    		//由于安装配置较为繁琐，故暂关闭Capture
    		if(checkJMFClass != null){
	        	if((ResourceUtil.isWindowsOS() == false)
	        			|| ResourceUtil.isWindowsXP()){
	        		//旧用户可能已使用中，仅限非Windows或XP
	        		PlatformManager.getService().buildCaptureMenu(popupTi, threadPoolToken);
	        	}
    		}
        }
        
        popupTi.addSeparator();

        {
        	//建菜单
        	buildCertMenu(popupTi);
        }
                
        popupTi.addSeparator();
        
    	
		final JMenuItem mapItem = new JMenuItem((String)ResourceUtil.get(9035));//菜单项
    	try {
    		mapItem.setIcon(new ImageIcon(ImageIO.read(
					ResourceUtil.getResource("hc/res/map_22.png"))));
		} catch (final IOException e1) {
		}
		mapItem.setToolTipText("<html>" + (String)ResourceUtil.get(9082) + "</html>");
    	mapItem.addActionListener(new HCActionListener(new Runnable() {
            @Override
			public void run() {
            	refreshActionMS(false);
            	
            	try{
            		KeyComperPanel.showKeyComperPanel();
            	}catch (final Exception ee) {
					App.showConfirmDialog(null, "Cant load Key", 
							"Error", JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
				}
            }
        }, threadPoolToken));
        
        popupTi.add(mapItem);
        
//        popupTi.addSeparator();
        
        {
//    		if((RootConfig.getInstance().isTrue(RootConfig.p_ShowDesinger))){
//    	        if(RootConfig.getInstance().isTrue(RootConfig.p_ShowDesingerToAll) 
//    	        		|| TokenManager.isDonateToken()){
//    		        popupTi.addSeparator();
        	if(ResourceUtil.isEnableDesigner()){
    		        final JMenuItem designer = new JMenuItem((String)ResourceUtil.get(9034));//菜单项
    		        ImageIcon designIco = null;
    		        
//    				//检查是否有新版本
    				final String lastSampleVer = PropertiesManager.getValue(PropertiesManager.p_LastSampleVer, "1.0");
    				if(StringUtil.higer(J2SEContext.getSampleHarVersion(), lastSampleVer)){
    					try {
        					designIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/designernew_22.png")));
        				} catch (final IOException e) {
        					ExceptionReporter.printStackTrace(e);
        				}
    				}else{
        				try {
        					designIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/designer_22.png")));
        				} catch (final IOException e) {
        					ExceptionReporter.printStackTrace(e);
        				}    					
    				}

    				designer.setToolTipText((String)ResourceUtil.get(9080));
	            	designer.setIcon(designIco);
    	            designer.addActionListener(new HCActionListener(new Runnable() {
    	                @Override
						public void run() {
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
    	                	LinkMenuManager.startDesigner(true);
    	                }
    	            }, threadPoolToken));
    	            popupTi.add(designer);
        	}
//    	        }
//            }

        	{
        		final JMenuItem linkItem = new JMenuItem((String)ResourceUtil.get(9059));//菜单项
            	try {
            		linkItem.setIcon(new ImageIcon(ImageIO.read(
    						ResourceUtil.getResource("hc/res/menu_22.png"))));
    			} catch (final IOException e1) {
    			}
    			linkItem.setToolTipText((String)ResourceUtil.get(9081));
            	linkItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
	            		LinkMenuManager.showLinkPanel(null);
					}
				}, threadPoolToken));
    	        
    	        popupTi.add(linkItem);
        	}
        }

        
        if(StarterManager.hadUpgradeError){
	        popupTi.addSeparator();
	        final String downloadMe = "download me";
			final JMenuItem verifyItem = new JMenuItem(downloadMe);
	        try {
	        	verifyItem.setIcon(new ImageIcon(ImageIO.read(
	        			ResourceUtil.getResource("hc/res/verify_22.png"))));
			} catch (final IOException e) {
				ExceptionReporter.printStackTrace(e);
			}
	        
	        verifyItem.addActionListener(new HCActionListener(new Runnable() {
	            @Override
				public void run() {
					final JPanel panel = new JPanel();
					panel.add(new JLabel("<html><body style='width:400'>" +
							"System try upgrade starter.jar, but it fails, for more information see log.<br>Please do as following by hand.<BR><BR>" +
							"1. click 'O K' to download zip from http://homecenter.mobi<BR>" +
							"2. shutdown this HomeCenter App Server<BR>" +
							"3. unzip and override older HomeCenter App Server<BR>" +
							"4. run HomeCenter App Server, this '<strong>"+downloadMe+"</strong>' menu will disappear.<BR>" +
							"</body></html>"));
					App.showCenterPanel(panel, 0, 0, "Fail on upgrade starter!", false, null, null, new HCActionListener(new Runnable() {
						@Override
						public void run() {
							HttpUtil.browse("http://homecenter.mobi/download/HC_Server.zip?verifyme");
						}
					}, threadPoolToken), null, null, false, false, null, false, false);
				}
	        }, threadPoolToken));
	        
	        popupTi.add(verifyItem);
        }

        popupTi.addSeparator();
        
		//退出
        final int indexOfAT = IConstant.getUUID().indexOf("@");
        final String uuid_for_email = (indexOfAT>0)?IConstant.getUUID().substring(0, indexOfAT):IConstant.getUUID();
        final JMenuItem exitItem = new JMenuItem(
        		(String)ResourceUtil.get(IContext.EXIT) + "   (" + uuid_for_email + ")");//菜单项
		exitItem.setIcon(new ImageIcon(ImageSrc.EXIT_ICON));
        exitItem.addActionListener(new HCActionListener(new Runnable() {
        	String opName = exitItem.getText();
            @Override
			public void run() {
            	if(checkPassword(true, opName)){
            		//在J2SE环境下，直接调用ExitManager.startExitSystem()；Android服务器模式下，增加后台运行选项
            		PlatformManager.getService().startExitSystem();					
            	}
            }
        }, threadPoolToken));
        popupTi.add(exitItem);
        
		// 切换皮肤时，会导致异常，所以不复用对象。
        String oldTip = null;//保留ToolTip
		if (ti != null) {
			oldTip = ti.getToolTip();
			ti.remove();
		}
		try {
			ContextManager.statusListen = this;
			ti = PlatformManager.getService().buildPlatformTrayIcon((ti != null ? ti.getImage() : hc_Disable),
					(String) ResourceUtil.get(UILang.PRODUCT_NAME), popupTi);
			if(oldTip != null){
				ti.setToolTip(oldTip);
			}
//			关闭缺省的菜单功能
//			if(aboutAction != null){
//				ti.setDefaultActionListener(aboutAction);
//			}
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}// 图标，标题，右键弹出菜单
    }

	private final void addPwdSubMenu(final boolean hasWiFiAccount) {
		final JMenu accountItem = new JMenu((String)ResourceUtil.get(9124));
        final ImageIcon passwordIcon = new ImageIcon(ImageSrc.PASSWORD_ICON);
		accountItem.setIcon(passwordIcon);
        {
    		addSysPwdMenuItem(accountItem, null);
            
			if(hasWiFiAccount){
        		final JMenuItem wifiItem = new JMenuItem("WiFi");//菜单项
        		wifiItem.setIcon(WiFiHelper.getWiFiIcon());
                wifiItem.setToolTipText("<html>" + WiFiHelper.getInputWiFiAccountStr() + "</html>");
                wifiItem.addActionListener(new HCActionListener(new Runnable() {
        			@Override
        			public void run() {
        				WiFiHelper.showInputWiFiPassword(false);
        			}
        		}, threadPoolToken));
                accountItem.add(wifiItem);
        	}
        }
        popupTi.add(accountItem);
	}

	private final void addSysPwdMenuItem(final JMenu subMenu, final JPopupMenu popMenu) {
		//登录改为密码
		{
			final JMenuItem loginItem = new JMenuItem((String)ResourceUtil.get(1007));//菜单项
			final ImageIcon pwdIcon = new ImageIcon(ImageSrc.PASSWORD_ICON);
			loginItem.setIcon(pwdIcon);
		    loginItem.setToolTipText("<html>" + (String)ResourceUtil.get(9116) + "<BR> " + App.getPasswordLocalStoreTip() + "</html>");
		    loginItem.addActionListener(new HCActionListener(new Runnable() {
		    	String opName = loginItem.getText();
				@Override
				public void run() {
		        	if(checkPassword(false, opName)){
		        		App.showInputPWDDialog(IConstant.getUUID(), "", "", false);
//    		            		new LoginDialog(loginItem);
		        	}
				}
			}, threadPoolToken));
		    if(subMenu != null){
		    	subMenu.add(loginItem);
		    }else{
		    	popMenu.add(loginItem);
		    }
		}
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
		}catch (final Exception e) {
		}
		upgradeItem.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final boolean isAutoAfterClick = upgradeItem.isSelected();
				if(isAutoAfterClick == false){
					final JPanel panel = new JPanel(new BorderLayout());
					panel.add(new JLabel("<html><body style=\"width:500\">" +
							(String)ResourceUtil.get(9103) +
							"<BR><BR>" + StringUtil.replace((String)ResourceUtil.get(9104), "{ok}", (String)ResourceUtil.get(IContext.OK)) +
							"</body></html>"), BorderLayout.CENTER);
					App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(9102), true, null, null, new HCActionListener(new Runnable() {
						@Override
						public void run() {
							flipAutoUpgrade(upgradeItem, isAutoAfterClick);
							final JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html>" + (String)ResourceUtil.get(9105) + "</html>", App.getSysIcon(App.SYS_INFO_ICON), JLabel.LEADING), BorderLayout.CENTER);
							App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.INFO));
						}
					}, threadPoolToken), new HCActionListener(new Runnable() {
						@Override
						public void run() {
							upgradeItem.setSelected(!isAutoAfterClick);
						}
					}, threadPoolToken), null, false, false, null, false, false);
				}else{				
					flipAutoUpgrade(upgradeItem, isAutoAfterClick);
				}
			}
		}, threadPoolToken));
		hcMenu.add(upgradeItem);
	}
    
	public void showTray() {
		try {
			disable_dl_certkey = new ImageIcon(ImageSrc.DISABLE_DL_CERTKEY_ICON);
			dl_certkey = new ImageIcon(ImageSrc.DL_CERTKEY_ICON);
			final ClassLoader appClassLoader = App.class.getClassLoader();
			if(ResourceUtil.isMacOSX() || ResourceUtil.isAndroidServerPlatform()){
				hc_Enable = ImageIO.read(appClassLoader.getResource("hc/res/hc_48.png"));
				hc_Disable = ImageIO.read(appClassLoader.getResource("hc/res/hc_dis_48.png"));
				hc_mobi = ImageIO.read(appClassLoader.getResource("hc/res/hc_mobi_48.png"));
			}else{
				hc_Enable = ImageIO.read(appClassLoader.getResource("hc/res/hc_48.jpg"));
				hc_Disable = ImageIO.read(appClassLoader.getResource("hc/res/hc_dis_48.jpg"));
				hc_mobi = ImageIO.read(appClassLoader.getResource("hc/res/hc_mobi_48.jpg"));
			}
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}

		buildMenu(UILang.getUsedLocale());
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
        	@Override
			public void run(){
        		PropertiesManager.notifyShutdownHook();
        		//因为系统调用System.exit时，会激活此处。
        		if(ContextManager.cmStatus != ContextManager.STATUS_EXIT){
	        		L.V = L.O ? false : LogManager.log("User Power Off");
	        		ContextManager.notifyShutdown();
        		}
        	}
        });
        
        if(IConstant.serverSide){
        	final String msg = (String)ResourceUtil.get(9009);
			displayMessage((String)ResourceUtil.get(IContext.INFO), msg, 
        			IContext.INFO, null, 0);
        	ti.setToolTip(msg);
        }
	}
	
	public static String[][] splitFileAndVer(final String files, final boolean isTempMode){
		final String[] fs = files.split(";");
		
		final int size = fs.length;
		final String[][] out = new String[size][2];
		
		for (int i = 0; i < size; i++) {
			final String[] tmp = fs[i].split(":");
			if(tmp.length < 2){
				return null;
			}
			out[i][0] = tmp[0];
			out[i][1] = tmp[1];
		}
		
		return out;
	}
	
	public static LogViewer viewLog(final String fileName, final byte[] pwdBS, final String cipherAlgorithm, final String title) { 
        try {  
            return LogViewer.loadFile(fileName, pwdBS, cipherAlgorithm, title);
        } catch (final Exception e) {  
            ExceptionReporter.printStackTrace(e);  
            App.showMessageDialog(null, e.toString());
        }  
        return null;
    }  
	
	public static void runBrowser(final String fileName) {  
        try {  
            final File file = new File(App.getBaseDir(), fileName);
            if(file.exists() == false){
            	ContextManager.getContextInstance().displayMessage((String) ResourceUtil.get(IContext.ERROR), 
						(String) ResourceUtil.get(9004), IContext.ERROR, null, 0);
            	return;
            }
            final Desktop desktop = Desktop.getDesktop();  
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.OPEN)) {  
                desktop.open(file);  
            }else{
            	ContextManager.getContextInstance().displayMessage((String) ResourceUtil.get(IContext.ERROR), 
						(String) ResourceUtil.get(9005), IContext.ERROR, null, 0);
            }
        } catch (final IOException e) {  
            ExceptionReporter.printStackTrace(e);  
        }  
    }  
	
	@Override
	public Object getSysImg() {
		return App.SYS_LOGO;
	}

	@Override
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
//				L.V = L.O ? false : LogManager.log("setTargetPeer, IP:" + ip + ":" + port);
//				TargetPeer tp = new TargetPeer();
//				tp.clientInet = InetAddress.getByName(ip);
//				tp.clientPort = Integer.parseInt(port);
//				KeepaliveManager.setClient(tp);
//			} catch (Exception e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//		}else{
//			DatagramPacket packet = (DatagramPacket)datagram;
//			try {
//				packet.setAddress(InetAddress.getByName(ip));
//			} catch (UnknownHostException e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//			packet.setPort(Integer.parseInt(port));
//		}
//	}

	@Override
	public Object doExtBiz(final short bizNo, final Object newParam) {
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
				
				final int[] screenSize = ResourceUtil.getSimuScreenSize();

				//abc###efg
				final ServerConfig sc = new ServerConfig("");
				sc.setProperty(ServerConfig.p_HC_VERSION, StarterManager.getHCVersion());
				sc.setProperty(ServerConfig.p_MIN_MOBI_VER_REQUIRED_BY_PC, minMobiVerRequiredByServer);
				sc.setProperty(ServerConfig.P_SERVER_COLOR_ON_RELAY, RootConfig.getInstance().getProperty(RootConfig.p_Color_On_Relay));
				
				sc.setProperty(ServerConfig.P_SERVER_WIDTH, String.valueOf(screenSize[0]));
				sc.setProperty(ServerConfig.P_SERVER_HEIGHT, String.valueOf(screenSize[1]));
//				sc.setProperty(ServerConfig.P_SERVER_WIDTH, 360);
//				sc.setProperty(ServerConfig.P_SERVER_HEIGHT, 360);				
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
//				L.V = L.O ? false : LogManager.log("UPnP external IP == Public IP");
//				SIPManager.LocalNATPort = SIPManager.getExternalUPnPPort();
//				SIPManager.LocalNATType = EnumNAT.OPEN_INTERNET;
//			}
			
//			if(IConstant.serverSide && SIPManager.isRelayFullOpenMode(SIPManager.LocalNATType)){
//				RelayManager.notifyIsRelayServer();
//			}

//			ti.putTip(JPTrayIcon.NAT_DESC, EnumNAT.getNatDesc(UDPChannel.nattype));
//			ti.putTip(JPTrayIcon.PUBLIC_IP, hostAddress + ":" + String.valueOf(UDPChannel.publicPort));
			
			final String out = RootServerConnector.lineOn(
					IConstant.getUUID(), KeepaliveManager.homeWirelessIpPort.ip, 
					KeepaliveManager.homeWirelessIpPort.port, 0, 1, 
					KeepaliveManager.relayServerUPnPIP, KeepaliveManager.relayServerUPnPPort,
					SIPManager.relayIpPort.ip, SIPManager.relayIpPort.port, TokenManager.getToken(), 
					!isEnableTransNewCertNow(), RootServerConnector.getHideToken());
			
			if(out == null){
				LogManager.err("unable to connect root server");
				SIPManager.notifyRelineon(true);
				final String[] ret = {"false"};
				return ret;
			}else if(out.equals("e")){
				String msg = (String)ResourceUtil.get(9113);
				msg = StringUtil.replace(msg, "{uuid}", IConstant.getUUID());
//				LogManager.err(msg);
				
				App.showMessageDialog(null,
					msg, (String) ResourceUtil
							.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE);
				
				notifyShutdown();
				final String[] ret = {"false"};
				return ret;
			}else if(out.equals("d")){
				RootConfig.getInstance().setProperty(RootConfig.p_Color_On_Relay, "5");
				RootConfig.getInstance().setProperty(RootConfig.p_MS_On_Relay, 100);
			}
			
			setTrayEnable(true);
			final String msg = (String) ResourceUtil.get(9008);
			ContextManager.displayMessage((String) ResourceUtil.get(IContext.INFO), 
				msg, IContext.INFO, 0);
			
			try{
				J2SEContext.ti.setToolTip(msg + " (ID:" + IConstant.getUUID() + ")");
			}catch (final Exception e) {
				//出现ShutDown时的空情形，所以要加异常
			}

			SingleMessageNotify.closeDialog(SingleMessageNotify.TYPE_ERROR_CONNECTION);
			
//			本地无线信息，不需在日志中出现。
//				L.V = L.O ? false : LogManager.log("Success line on " 
//						+ ServerUIUtil.replaceIPWithHC(KeepaliveManager.homeWirelessIpPort.ip) + ", port : " 
//						+ KeepaliveManager.homeWirelessIpPort.port + " for client connection");
			
			//上传上线信息
			if(SIPManager.relayIpPort.port > 0){	
				L.V = L.O ? false : LogManager.log("Success line on " 
						+ HttpUtil.replaceIPWithHC(SIPManager.relayIpPort.ip)  + ", port : " 
						+ SIPManager.relayIpPort.port + " for client connection");
			}
			
			return null;
		}else if(bizNo == IContext.BIZ_GET_FORBID_UPDATE_CERT_I18N){
			return ResourceUtil.get(9121);
		}else if(bizNo == IContext.BIZ_AFTER_HOLE){
			if(IConstant.serverSide){
				//服务器端
				final String sc = (String)ContextManager.getContextInstance().doExtBiz(IContext.BIZ_LOAD_SERVER_CONFIG, null);
				ContextManager.getContextInstance().send(
						MsgBuilder.E_TRANS_SERVER_CONFIG, sc != null?sc:"");//必须发送，因为手机端会返回
				L.V = L.O ? false : LogManager.log("Transed Server Config");
				

				//等待，让ServerConfig先于后面的包到达目标

				//并将该随机数发送给客户机，客户机用同法处理后回转给服务器
				//服务器据此判断客户机的CertKey和密码状态				
			}
			
			return null;
		}
		
//		if(bizNo == IContext.BIZ_OPEN_REQ_BUILD_CONN_LISTEN){
//			EventCenter.addListener(new IEventHCListener(){
//				public final boolean action(final byte[] bs) {
//					if(ContextManager.isNotWorkingStatus()){
//						send(MsgBuilder.E_TAG_REQUEST_BUILD_CONNECTION);
//						L.V = L.O ? false : LogManager.log("Successful connect to the target peer");
//						ContextManager.setStatus(ContextManager.STATUS_READY_FOR_CLIENT);
//						if(IConstant.serverSide){
//							//将askForService移出，因为有可能密码验证不通过
//							NatHcTimer.LISTENER_FROM_HTTP.setEnable(false);
//						}
//					}
//					return true;
//				}
//	
//				public final byte getEventTag() {
//					return MsgBuilder.E_TAG_REQUEST_BUILD_CONNECTION;
//				}});
//			return null;
//		}
		if(bizNo == IContext.BIZ_CHANGE_RELAY){
			final String[] ips = (String[])newParam;
			RootServerConnector.changeRelay(IConstant.getUUID(), ips[0], ips[1], TokenManager.getToken());
			return null;
		}else if(bizNo == IContext.BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF){
			if (KeepaliveManager.keepalive.isEnable() == false){
				if(L.isInWorkshop){
					L.V = L.O ? false : LogManager.log("BIZ_START_WATCH_KEEPALIVE keepalive : false");
				}
				ConditionWatcher.addWatcher(new IWatcher() {
					final long startMS = System.currentTimeMillis();
					@Override
					public boolean watch() {
						if(System.currentTimeMillis() - startMS > 5000){
							if(KeepaliveManager.keepalive.isEnable() == false){
								if(L.isInWorkshop){
									L.V = L.O ? false : LogManager.log("BIZ_START_WATCH_KEEPALIVE set keepalive : true");
								}
								KeepaliveManager.keepalive.setEnable(true);
							}
							return true;
						}
						return false;
					}
					@Override
					public void setPara(final Object p) {
					}
					@Override
					public boolean isCancelable() {
						return false;
					}
					@Override
					public void cancel() {
					}
				});
			}else{
				if(L.isInWorkshop){
					L.V = L.O ? false : LogManager.log("BIZ_START_WATCH_KEEPALIVE keepalive still : true");
				}
			}
		}else if(bizNo == IContext.BIZ_GET_TOKEN){
			return TokenManager.getTokenBS();
		}else if(bizNo == IContext.BIZ_SET_TRAY_ENABLE){
			if(newParam instanceof Boolean){
				final boolean b = (Boolean)newParam;
				setTrayEnable(b);
			}
			return null;
		}
		
		if(IConstant.serverSide){
			if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS){
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
			return minMobiVerRequiredByServer;
		}
		if(bizNo == IContext.BIZ_REPORT_EXCEPTION){
			reportException((ExceptionJSON)newParam);
			return null;
		}
		return null;
	}
	
	private final void reportException(final ExceptionJSON json){
		final boolean forTest = json.isForTest;
		HttpURLConnection connection = null;
		DataOutputStream out = null;
		try {
			String urlStr = json.getToURL();
			urlStr = HttpUtil.replaceSimuURL(urlStr, App.isSimu());
			
			final String email = json.getAttToEmail();
			
			if(forTest){
				System.out.println("[test] report exception to : " + (email!=null?email:urlStr));
			}
			// 创建连接
			final URL url = new URL(urlStr);
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoOutput(true);
			connection.setConnectTimeout(15000);
			if(forTest){
				connection.setReadTimeout(15000);
				connection.setDoInput(true);//for test only
			}
			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);

			connection.setRequestProperty("Content-Type", ExceptionJSON.APPLICATION_JSON_CHARSET_UTF_8);
			connection.connect();

			// POST请求
			out = new DataOutputStream(connection.getOutputStream());
			out.write(json.getJSONBytesCache());
			out.flush();

			//--------------------------以下接收响应--------------------------
			final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String lines;
			final StringBuffer sb = new StringBuffer(1024);
			
			sb.append("[test] response of report exception (the response will be ignore if NOT test) :\n");
			while ((lines = reader.readLine()) != null) {
				lines = new String(lines.getBytes(), "utf-8");
				sb.append(lines);
				sb.append("\n");
			}
			reader.close();//必须接收，否则发送不成功!
			
			if (forTest && email == null) {
				L.V = L.O ? false : LogManager.log(sb.toString());
			}
		} catch (final Throwable e) {
			// 不处理异常
			if(forTest){
				e.printStackTrace();
			}
		}finally{
			try{
				out.close();
			}catch (final Throwable e) {
			}
			try{
				connection.disconnect();
			}catch (final Throwable e) {
			}
		}
	}

	public final void startTransMobileContent() {
		if(ScreenServer.askForService()){
			ServerCUtil.transOneTimeCertKey();
			
			L.V = L.O ? false : LogManager.log("Pass Certification Key and password");
			
			//由于会传送OneTimeKey，所以不需要下步
			//instance.send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS));

			//必须要提前到sleep(1000)之前，以便接收相应状态的消息。否则Invalid statue tag received
			ContextManager.setStatus(ContextManager.STATUS_SERVER_SELF);

			if(ConfigManager.isTCPOnly == false){//UDP情形下，等待OneTimeCertKey数据到达
				try{
					Thread.sleep(1000);
				}catch (final Exception e) {
				}
			}
			
			//由原来的客户端请求,改为服务器推送
			doCanvasMain("menu://root");
			
//			final TargetPeer target = KeepaliveManager.target;

			final String msg = (String)ResourceUtil.get(9012);
			displayMessage((String) ResourceUtil.get(IContext.INFO), msg, 
					IContext.INFO, null, 0);
			ti.setToolTip(msg);
			ti.setImage(hc_mobi);

			//关闭以防被再次接入
			RootServerConnector.delLineInfo(TokenManager.getToken(), true);
		}else{
			L.V = L.O ? false : LogManager.log("Service is full, error for client connection");
			send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_SERVICE_IS_FULL));
			sleepAfterError();
			SIPManager.notifyRelineon(true);
		}
	}

	private static void doAfterPwdError() {
		L.V = L.O ? false : LogManager.log("Send Error password status to client");
		ContextManager.getContextInstance().send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR));
		sleepAfterError();
		
		SIPManager.notifyRelineon(true);
	}
	
	public static void sleepAfterError(){
		try{
			Thread.sleep(1000);
		}catch (final Exception e) {
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
			L.V = L.O ? false : LogManager.log(RootServerConnector.unObfuscate("rtnapsro teCtrK yet  olceitn."));
		}else{
			L.V = L.O ? false : LogManager.log("reject a mobile login with invalid certification.");
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
//		LogManager.log("send Cert : " + CUtil.toHexString(CUtil.getCertKey()));
		if(ContextManager.cmStatus != ContextManager.STATUS_SERVER_SELF){
			ServerCUtil.transCertKey(CUtil.getCertKey(), MsgBuilder.E_TRANS_NEW_CERT_KEY, false);
		}else{
			ServerCUtil.transCertKey(CUtil.getCertKey(), MsgBuilder.E_TRANS_NEW_CERT_KEY_IN_SECU_CHANNEL, false);
		}
	}
	
	private static boolean enableTransCertKey;
	
	public static void enableTransCertKey(final boolean enable){
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
		final String out = PropertiesManager.getValue(PropertiesManager.p_EnableTransNewCertKeyNow);
		if(out == null || out.equals(IConstant.TRUE)){
			enableTransCertKey = true;
		}else{
			enableTransCertKey = false;
		}
	}

	@Override
	public Object getProperty(final Object propertyID) {
		return PropertiesManager.getValue((String)propertyID);
	}

//	public void copyDatagramAddress(Object from, Object to) {
//		DatagramPacket dpTo = (DatagramPacket)to;
//		DatagramPacket dpFrom = (DatagramPacket)from;
//		
//		dpTo.setSocketAddress(dpFrom.getSocketAddress());
//	}

	final IHCURLAction urlAction = new J2SEServerURLAction();
	
	public static final String jrubyjarname = ResourceUtil.getLibNameForAllPlatforms("jruby.jar");

	@Override
	public IHCURLAction getHCURLAction() {
		return urlAction;
	}

	@Override
	public void notify(final short statusFrom, final short statusTo) {
		if(statusTo == ContextManager.STATUS_LINEOFF){
			
			if(statusFrom == ContextManager.STATUS_SERVER_SELF){
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						ScreenServer.emptyScreen();
						final BaseResponsor responsor = ServerUIUtil.getResponsor();
						
//						ClientDesc.agent.set(ConfigManager.UI_IS_BACKGROUND, IConstant.TRUE);
						if(responsor != null){//退出时，多进程可能导致已关闭为null
//							responsor.onEvent(ProjectContext.EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND);
							responsor.onEvent(ProjectContext.EVENT_SYS_MOBILE_LOGOUT);
						}
					}
				});
			}
		}
		
		if(statusTo == ContextManager.STATUS_NEED_NAT){
			setTrayEnable(false);
			if(ti != null){
				ti.setToolTip("HomeCenter");
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
			
			//由于接收菜单大数据可能需要消耗较多时间，故resetTimerCount变相增加时间量。
			//注意：要置于上行之后，以使用新的时间间隔来重算
			KeepaliveManager.keepalive.resetTimerCount();
			
			KeepaliveManager.resetSendData();//为接收Menu增加时间
		}
	}
	
	private void setTrayEnable(final boolean b){
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

	public static boolean isTransedToMobileSize = false;
	
	private void doAfterCertIsNotTransed() {
		App.setNoTransCert();
		
		if(!isEnableTransNewCertNow()){
			flipTransable(!isEnableTransNewCertNow(), false);
		}
		transNewCertKey.setEnabled(false);
		
		showSuccCreateNewCertDialog();
	}

	private Window showSuccCreateNewCertDialog() {
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel((String) ResourceUtil.get(9007), new ImageIcon(ImageSrc.OK_ICON), SwingConstants.LEFT), BorderLayout.NORTH);
		
		final String transmitCert = (String)ResourceUtil.get(9117);
		final String tipOn = (String)ResourceUtil.get(9118);
		final String tipOff = (String)ResourceUtil.get(9119);
		final String certTip = (String)ResourceUtil.get(9120);
		panel.add(new JLabel("<html><body style=\"width:600\"><BR><STRONG>" + (String) ResourceUtil.get(IContext.TIP) + "</STRONG>" +
				"<BR>" + certTip +
				"<BR>4. " + transmitCert + ":<STRONG>" + tipOn + "</STRONG>, " + transOnTip +
				"<BR>5. " + transmitCert + ":<STRONG>" + tipOff + "</STRONG>, " + transOffTip +
				"</body></html>"), BorderLayout.CENTER);
		
		return App.showCenterPanel(panel, 0, 0, (String) ResourceUtil.get(IContext.INFO), 
				false, null, null, null, null, null, false, false, null, false, false);
	}

	private void doAfterMobileReceivedCert() {
		final JPanel nextpanel = new JPanel();
		nextpanel.add(new JLabel((String) ResourceUtil.get(9032), new ImageIcon(ImageSrc.OK_ICON), SwingConstants.LEFT));
		App.showCenterPanelOKDispose(nextpanel, 0, 0, (String) ResourceUtil.get(IContext.INFO), false, (JButton)null, (String)null, new HCActionListener(new Runnable() {
			@Override
			public void run() {
			}
		}, threadPoolToken), null, true, null, false, true, null, false, true);
		
	}

	public static final String MAX_HC_VER = "9999999";//注意与Starter.NO_UPGRADE_VER保持同步
	
	private final String minMobiVerRequiredByServer = "7.2";//(含)，
	//你可能 还 需要修改服务器版本，StarterManager HCVertion = "6.97";
	
	public static final String getSampleHarVersion(){
		return "7.2";
	}
	
	private void flipAutoUpgrade(final JCheckBoxMenuItem upgradeItem,
			final boolean isAutoAfterClick) {
		
		//更新到Properties文件中
		final Properties start = new Properties();
		try{
			start.load(new FileInputStream(StarterManager._STARTER_PROP_FILE_NAME));
		}catch (final Throwable ex) {
			App.showConfirmDialog(null, "Unable open file starter.properties.", "Error", JOptionPane.ERROR_MESSAGE);
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
		} catch (final Exception e1) {
			App.showConfirmDialog(null, "Unable write file " +StarterManager. _STARTER_PROP_FILE_NAME + ".", "Error", JOptionPane.ERROR_MESSAGE);
			upgradeItem.setSelected(!isAutoAfterClick);
			return;
		}

//			upgradeItem.setSelected(isOldAuto);
		PropertiesManager.setValue(PropertiesManager.p_isNotAutoUpgrade, 
				(isAutoAfterClick == false)?IConstant.TRUE:IConstant.FALSE);
		PropertiesManager.saveFile();
		
		if(isAutoAfterClick){
			final JPanel panel = new JPanel();
			panel.add(new JLabel("<html><body style=\"width:500\">" + (String)ResourceUtil.get(9107) +
					"</body></html>", new ImageIcon(ImageSrc.OK_ICON), SwingConstants.LEFT));
			App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(9106), false, null, null, null, null, null, true, false, null, false, false);
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
					if(isTransedToMobileSize){
						ServerUIAPIAgent.__sendStaticMessage((String) ResourceUtil.get(IContext.INFO), (String) ResourceUtil.get(9033), IContext.INFO, null, 0);
						return true;
					}
					if(System.currentTimeMillis() - curr > 3000){
						return true;
					}
					return false;
				}
				
				@Override
				public void setPara(final Object p) {
				}
				
				@Override
				public boolean isCancelable() {
					return false;
				}
				
				@Override
				public void cancel() {
				}
			});
			final Window window = showSuccCreateNewCertDialog();
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
						return true;
					}else{
						return false;
					}
				}

				@Override
				public void setPara(final Object p) {
				}

				@Override
				public void cancel() {
				}

				@Override
				public boolean isCancelable() {
					return false;
				}});
			return ;
			//END 在线传送证书
		}else{
			doAfterCertIsNotTransed();
		}
	}

	public static void enableTransNewCertMenuItem() {
		transNewCertKey.setEnabled(true);
	}

	public static void notifyExitByMobi() {
		L.V = L.O ? false : LogManager.log("Client/Relay request lineoff!");

		ContextManager.getContextInstance().displayMessage(
				(String)ResourceUtil.get(IContext.INFO), 
				(String)ResourceUtil.get(9006), IContext.INFO, null, 0);

		RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_MobReqExitToPC_STR);

		SIPManager.notifyRelineon(true);
	}

	public static void appendTitleJRubyVer(final JFrame frame) {
		final String ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		if(ver != null){
			frame.setTitle(frame.getTitle() + " - {JRuby:" + ver + "}");
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
		
		final JPanel iconPanle = new JPanel();
		iconPanle.setLayout(new BoxLayout(iconPanle, BoxLayout.X_AXIS));
		iconPanle.add(Box.createHorizontalGlue());
		iconPanle.add(new JLabel(isEnable?disable_dl_certkey:dl_certkey));
		iconPanle.add(Box.createHorizontalGlue());
		iconPanle.add(new JLabel(new ImageIcon(ImageSrc.MOVE_TO_G_ICON)));
		iconPanle.add(Box.createHorizontalGlue());
		iconPanle.add(new JLabel(isEnable?dl_certkey:disable_dl_certkey));
		iconPanle.add(Box.createHorizontalGlue());
		
		final JPanel tipPanle = new JPanel(new BorderLayout(0, 20));
		
		tipPanle.add(iconPanle, BorderLayout.CENTER);
		{
			final JPanel msgPanel = new JPanel(new BorderLayout());
			msgPanel.add(new JLabel("<html><body style='width:450' align='left'><strong>"+(String)ResourceUtil.get(IContext.TIP)+"</strong> : <BR>"+(isEnable?transOnTip:transOffTip)+"</body></html>"), 
					BorderLayout.CENTER);
			tipPanle.add(msgPanel, BorderLayout.SOUTH);
		}

		App.showCenterPanel(tipPanle, 0, 0, (isEnable?(String)ResourceUtil.get(1020):(String)ResourceUtil.get(1021)), false, null, null, null, null, null, false, false, null, false, false);
	}

	@Override
	public final WiFiDeviceManager getWiFiDeviceManager() {
		final WiFiDeviceManager platManager = PlatformManager.getService().getWiFiManager();
		if(platManager.hasWiFiModule() && platManager.canCreateWiFiAccount()){
			//优先使用服务器自带WiFi
			return platManager;
		}else if(ContextManager.isMobileLogin() && ClientDesc.getAgent().ctrlWiFi()){
			//如果手机上线，借用手机WiFi功能
			return WiFiHelper.remoteWrapper;
		}else{
			//无WiFi环境
			return platManager;
		}
	}

	@Override
	public void notifyStreamReceiverBuilder(final boolean isInputStream, final String className,
			final int streamID, final byte[] bs, final int offset, final int len) {
	}
}

class PWDDialog extends HCJDialog {
	JPanel pwdPanel = new JPanel();
	JPanel btnPanel = new JPanel();
	Border border1;
	TitledBorder titledBorder1;
	JButton jbOK = null;
	JButton jbExit = null;
	JPasswordField jPasswordField1 = new JPasswordField(15);// 20个字符宽度

	public PWDDialog() {
		setModal(true);
		
		try {
			init();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	private void init() throws Exception {

		setTitle("HomeCenter");
		this.setIconImage(App.SYS_LOGO);
		
		final java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				jbExit_actionPerformed(e);
			}
		};

		this.getRootPane().registerKeyboardAction(exitActionListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		//必须有此行代码，作为窗口右上的关闭响应
//		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new HCWindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				jbExit_actionPerformed(null);
			}
		});
		
		jbOK = new JButton("", new ImageIcon(ImageSrc.OK_ICON));
		jbExit = new JButton("", new ImageIcon(ImageSrc.CANCEL_ICON));

		// new LineBorder(Color.LIGHT_GRAY, 1, true)
		titledBorder1 = new TitledBorder((String)ResourceUtil.get(1007));// BorderFactory.createEtchedBorder()
		
		final JPanel root = new JPanel();
		
		App.addBorderGap(this.getContentPane(), root);
		
		root.setLayout(new BorderLayout());
		pwdPanel.setLayout(new FlowLayout());
		pwdPanel.setBorder(titledBorder1);

		jbOK.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
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
				BorderLayout.CENTER);

		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(jbOK);
		btnPanel.add(jbExit);
		root.add(
				btnPanel,
				BorderLayout.SOUTH);

		this.getRootPane().setDefaultButton(jbOK);

		pack();
		// int width = 400, height = 270;
		final int width = getWidth(), height = getHeight();
		setSize(width, height);

		App.showCenter(this);
		
	}
	
	void jbOK_actionPerformed(final ActionEvent e) {
		pwd = jPasswordField1.getText();
		endDialog();
	}

	void jbExit_actionPerformed(final ActionEvent e) {
		endDialog();
	}

	private void endDialog() {
		super.dispose();
	}
	
	public String pwd;
}

abstract class LineonAndServingExecWatcher implements IWatcher{
	long currMS = System.currentTimeMillis();
	final String opName;
	
	LineonAndServingExecWatcher(final String opName){
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
	public void setPara(final Object p) {
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isCancelable() {
		return false;
	}
}