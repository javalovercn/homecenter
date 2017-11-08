package hc.server;

import hc.App;
import hc.PlatformTrayIcon;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.GlobalConditionWatcher;
import hc.core.HCConnection;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.SessionManager;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.res.ImageSrc;
import hc.server.data.KeyComperPanel;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.WiFiHelper;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.LogViewer;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.design.J2SESession;
import hc.server.util.HCJDialog;
import hc.server.util.ServerCUtil;
import hc.server.util.VerifyEmailManager;
import hc.util.ExitManager;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
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
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TrayMenuUtil {
	final static ThreadGroup threadPoolToken = App.getThreadPoolToken();
    private static JMenuItem transNewCertKey, hideIDForErrCert;
    static ImageIcon dl_certkey, disable_dl_certkey;
    static Image hc_Enable, hc_Disable, hc_mobi;
	static PlatformTrayIcon ti;
	final static String transOnTip = (String)ResourceUtil.get(9063);
	final static String transOffTip = (String)ResourceUtil.get(9064);

    public static JPopupMenu popupTi = new JPopupMenu() ;//弹出菜单
	
    public static long lastCheckMS = System.currentTimeMillis();

    public static void refreshActionMS(final boolean isForce) {
		if(isForce || (System.currentTimeMillis() - lastCheckMS < 1000 * 60 * 3)){
			lastCheckMS = System.currentTimeMillis();
		}
	}
    
	private static void doAfterCertIsNotTransed() {
		App.setNoTransCert();
		
		if(!DefaultManager.isEnableTransNewCertNow()){
			TrayMenuUtil.flipTransable(!DefaultManager.isEnableTransNewCertNow(), false);
		}
		transNewCertKey.setEnabled(false);
	}

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
    		return checkPasswordDirect(opName);
    	}else{
    		refreshActionMS(false);
    		return true;
    	}
    }

	private static boolean checkPasswordDirect(final String opName) {
		final PWDDialog pd = new PWDDialog();
		
		final String pwd = PropertiesManager.getPasswordAsInput();
		if(pd.pwd == null){
			//取消操作
			return false;
		}
		
		if(pwd.equals(pd.pwd)){
			refreshActionMS(true);
			return true;
		}
		
		if(opName != null){
			LogManager.log("Desktop Menu [" + opName + "] password error!");
		}
		final Object[] options={(String)ResourceUtil.get(1010)};
		App.showOptionDialog(null, ResourceUtil.get(1019), ResourceUtil.getProductName(), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
		return false;
	}
    
	final static Window showSuccCreateNewCertDialog() {
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
		
		final ThreadGroup token = App.getThreadPoolToken();
		final ActionListener listener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final CoreSession[] coreSSS = SessionManager.getAllSocketSessions();
				boolean isTransOK = false;
				for (int i = 0; i < coreSSS.length; i++) {
					final J2SESession coreSS = (J2SESession)coreSSS[i];
					if(coreSS.isTransedCertToMobile){
						isTransOK = true;
						break;
					}
				}
				
				if(isTransOK){
					TrayMenuUtil.showTransQues(token);
				}
			}
		}, token);
		return App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getInfoI18N(), 
				false, null, null, listener, null, null, false, false, null, false, false);
	}

	private static void transNewCertification(final J2SESession coreSS, final HCConnection hcConnection, final Window window) {
		final J2SEContext j2seContext = (J2SEContext)coreSS.context;
		
		if(UserThreadResourceUtil.isInServing(j2seContext)){
			//确保送达
			
			coreSS.isTransedCertToMobile = false;
			
			transNewCertKey(coreSS, hcConnection);
			
			coreSS.eventCenterDriver.addWatcher(new IWatcher() {
				long curr = System.currentTimeMillis();
				@Override
				public boolean watch() {
					if(coreSS.isTransedCertToMobile){
						final J2SESession[] coreSSS = {coreSS};
						ServerUIAPIAgent.sendMessageViaCoreSS(coreSSS, ResourceUtil.getInfoI18N(coreSS), (String) ResourceUtil.get(coreSS, 9033), IContext.INFO, null, 0);
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
			coreSS.eventCenterDriver.addWatcher(new IWatcher(){
				long curr = System.currentTimeMillis();
				
				@Override
				public boolean watch() {
					if(window.isShowing()){
						return false;
					}
					
					if(System.currentTimeMillis() - curr > 6000){
						if(coreSS.isTransedCertToMobile){
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
		if(transNewCertKey != null){//isDemoServer时，为null
			transNewCertKey.setEnabled(true);
		}
	}

	public static void appendTitleJRubyVer(final JFrame frame) {
		final String ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		if(ver != null){
			frame.setTitle(frame.getTitle() + " - {JRuby:" + ver + "}");
		}
	}
	
	private static void enableTransCertKey(final boolean enable){
		if(enable){
			PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.TRUE);
		}else{
			PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.FALSE);
		}
		PropertiesManager.saveFile();
		
		ResourceUtil.refreshRootAlive();//可能转为显示或隐身
	}

	public static void flipTransable(final boolean isEnable, final boolean isCallAfterTrans) {
		enableTransCertKey(isEnable);

//		if(isEnable){
//			transNewCertKey.setIcon(dl_certkey);
//			transNewCertKey.setText((String)ResourceUtil.get(1021));	
//		}else{
//			transNewCertKey.setIcon(disable_dl_certkey);
//			transNewCertKey.setText((String)ResourceUtil.get(1020));			
//		}
		refreshTransNewCertTip();
		
		ResourceUtil.refreshHideCheckBox(null, hideIDForErrCert);
		
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
			if(isEnable == false){
				final JCheckBox hideCheck = new JCheckBox();
				final JCheckBox checkBox = new JCheckBox();
				
				hideCheck.setSelected(ResourceUtil.refreshHideCheckBox(hideCheck, hideIDForErrCert));
				checkBox.setSelected(hideCheck.isSelected());
				
				hideCheck.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final boolean newSelected = hideCheck.isSelected();
						setHideForErrCert(newSelected, hideCheck);
						checkBox.setSelected(newSelected);
					}
				}, App.getThreadPoolToken()));
				
				checkBox.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final boolean newSelected = checkBox.isSelected();
						setHideForErrCert(newSelected, hideCheck);
						hideCheck.setSelected(newSelected);
					}
				}, App.getThreadPoolToken()));
				
				final JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
				if(ResourceUtil.isJ2SELimitFunction()){
					checkPanel.add(checkBox);//Android实现会出现两个选项框
				}
				checkPanel.add(hideCheck);
				
				msgPanel.add(checkPanel, BorderLayout.SOUTH);
			}
			tipPanle.add(msgPanel, BorderLayout.SOUTH);
		}

		App.showCenterPanelMain(tipPanle, 0, 0, (isEnable?(String)ResourceUtil.get(1020):(String)ResourceUtil.get(1021)), false, null, null, null, null, null, false, false, null, false, false);
	}

	private static void setHideForErrCert(final boolean newSelected,
			final JCheckBox hideCheck) {
		ResourceUtil.setHideIDForErrCertAndSave(newSelected);

		ResourceUtil.refreshHideCheckBox(hideCheck, hideIDForErrCert);
	}
	
	/**
	 * 
	 * @param b
	 * @return false : 保留旧的高阶状态
	 */
	static boolean setTrayEnable(final boolean b){
		if(L.isInWorkshop){
			LogManager.log("TrayEnable:" + b);
		}
		
		if(b){
			if(ti != null){
				final Image oldImg = ti.getImage();
				
				if(oldImg == hc_mobi){
					if(L.isInWorkshop){
						LogManager.log("old Image is hc_mobi.");
					}
					
					//检查是否还有keepConnection
					if(SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_SERVER_SELF)){
						ti.setToolTip(buildMobileConnectionTip());
						return false;
					}
				}
				
				ti.setImage(hc_Enable);
				ti.setToolTip(buildLineOnTrayTip());
				return true;
			}
		}else{
			if(ti != null){
				final Image oldImg = ti.getImage();
				
				if(oldImg == hc_mobi){
					L.V = L.WShop ? false : LogManager.log("old Image is hc_mobi.");
					
					//检查是否还有keepConnection
					if(SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_SERVER_SELF)){
						ti.setToolTip(buildMobileConnectionTip());
						return false;
					}
					
					if(SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_READY_TO_LINE_ON)){
						ti.setImage(hc_Enable);
						ti.setToolTip(buildLineOnTrayTip());
						return false;
					}
				}
				
				if(oldImg == hc_Enable){
					L.V = L.WShop ? false : LogManager.log("old Image is hc_enable.");
					
					//检查是否有联root
					if(SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_READY_TO_LINE_ON)
							|| SessionManager.checkAtLeastOneMeet(ContextManager.STATUS_READY_MTU)){
						ti.setToolTip(buildLineOnTrayTip());
						return false;
					}
					
					displayMessage(ResourceUtil.getInfoI18N(), 
							(String)ResourceUtil.get(9009), 
							IContext.INFO, null, 0);
				}
				
				ti.setImage(hc_Disable);
				ti.setToolTip(ResourceUtil.getProductName());
				return true;
			}
		}
		
		return false;
	}

	public static void notifyMobileLineOn(){
		if(ti != null){//isDemoServer时，为null
			ti.setToolTip(TrayMenuUtil.buildMobileConnectionTip());
			ti.setImage(hc_mobi);
		}
	}

	private static void buildCertMenu(final JPopupMenu popupMenu){
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
				refreshActionMS(false);
				final int result = App.showConfirmDialog(null, "<html>" + (String)ResourceUtil.get(9227) + "</html>", (String)ResourceUtil.get(9001), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, App.getSysIcon(App.SYS_QUES_ICON));
				if(result == JOptionPane.YES_OPTION){
					if(checkPasswordDirect(opName)){//强制输入密码，以防止删除
						App.generateCert();
						
						final Window window = showSuccCreateNewCertDialog();

						final J2SESession[] coreSSS = J2SESessionManager.getAllOnlineSocketSessions();//不考虑性能
						if(coreSSS != null && coreSSS.length > 0){
							for (int i = 0; i < coreSSS.length; i++) {
								final J2SESession j2seCoreSS = coreSSS[i];
								j2seCoreSS.eventCenterDriver.addWatcher(new LineonAndServingExecWatcher(j2seCoreSS, buildNewCertKey.getText()){
									@Override
									public final void doBiz() {
										transNewCertification(j2seCoreSS, j2seCoreSS.getHCConnection(), window);
									}});
							}
						}else{
							doAfterCertIsNotTransed();//一般此逻辑不会执行，仅维持理论上的完整性
						}
					}
				}
			}
		}, threadPoolToken));
        certMenu.add(buildNewCertKey);
        
        //传输新证书开关
		transNewCertKey = new JMenuItem("");//菜单项
		refreshTransNewCertTip();
		if(PropertiesManager.isTrue(PropertiesManager.p_NewCertIsNotTransed) 
            			&& (DefaultManager.isEnableTransNewCertNow())){
			transNewCertKey.setEnabled(false);
		}
		
		transNewCertKey.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
            	refreshActionMS(false);
            	if(checkPassword(true, transNewCertKey.getText())){
            		GlobalConditionWatcher.addWatcher(new LineonAndServingExecWatcher(J2SESession.NULL_J2SESESSION_FOR_PROJECT, transNewCertKey.getText()){//注意：不参与具体coreSS
						@Override
						public final void doBiz() {
			            	flipTransable(!DefaultManager.isEnableTransNewCertNow(), false);
						}
						
						@Override
	            		public boolean watch() {
	            			doBiz();
	            			return true;
	            		}
					});
            	}
			}
		}, threadPoolToken));
		certMenu.add(transNewCertKey);
        
		//控制显示或隐身
		{
			hideIDForErrCert = new JMenuItem("");//菜单项
			
			ResourceUtil.refreshHideCheckBox(null, hideIDForErrCert);

			hideIDForErrCert.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
	            	refreshActionMS(false);
	            	if(checkPassword(true, hideIDForErrCert.getText())){
						final boolean toStatus = !DefaultManager.isHideIDForErrCert();
						ResourceUtil.setHideIDForErrCertAndSave(toStatus);
						ResourceUtil.refreshHideCheckBox(null, hideIDForErrCert);
						if(toStatus){
							App.showMessageDialog(null, ResourceUtil.getHideTip(), ResourceUtil.getHideText(), App.INFORMATION_MESSAGE, App.getSysIcon(App.SYS_INFO_ICON));
						}else{
							App.showMessageDialog(null, ResourceUtil.getShowTip(), ResourceUtil.getShowText(), App.INFORMATION_MESSAGE, App.getSysIcon(App.SYS_INFO_ICON));
						}
	            	}
				}
			}, threadPoolToken));
			certMenu.add(hideIDForErrCert);
		}
		
		
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

	private static void refreshTransNewCertTip() {
		final String transmitCert = (String)ResourceUtil.get(9117);

		final String tipOn = (String)ResourceUtil.get(9118);
		final String tipOff = (String)ResourceUtil.get(9119);
		
		final boolean isEnable = DefaultManager.isEnableTransNewCertNow();

		transNewCertKey.setToolTipText("<html>" + 
				(String)ResourceUtil.get(9236) + (isEnable?tipOn:tipOff) + "<BR><BR>" +
				transmitCert + ":<STRONG>" + tipOn + "</STRONG>, " +
				transOnTip +
				";" +
				"<BR>" + transmitCert + ":<STRONG>" + tipOff + "</STRONG>, " +
				transOffTip +
				".</html>");
		
		if(isEnable){
			transNewCertKey.setIcon(dl_certkey);
			transNewCertKey.setText((String)ResourceUtil.get(1021));
		}else{
			transNewCertKey.setIcon(disable_dl_certkey);
			transNewCertKey.setText((String)ResourceUtil.get(1020));			
		}
	}
	
	private final static void addSysPwdMenuItem(final JMenu subMenu) {
		{
			final JMenuItem verifyEmailItem = new JMenuItem(VerifyEmailManager.getVerifyEmailButtonText());
			final ImageIcon verifyIcon = new ImageIcon(ImageSrc.ACCOUNT_LOCK_ICON);
			verifyEmailItem.setIcon(verifyIcon);
//			verifyItem.setToolTipText("verify email");
			verifyEmailItem.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					VerifyEmailManager.startVerifyProcess();
				}
			}, threadPoolToken));
			subMenu.add(verifyEmailItem);
			
//			if(VerifyEmailManager.isVerifiedEmail()){//有可能更换邮箱
//				verifyEmailItem.setEnabled(false);
//			}
		}
		
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
		        	}
				}
			}, threadPoolToken));
	    	subMenu.add(loginItem);
	    }
	}

	private final static void addPwdSubMenu(final boolean hasWiFiAccount, final JPopupMenu popMenu) {
		final JMenu accountItem = buildAccountMenu();
        {
    		addSysPwdMenuItem(accountItem);
            
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
        popMenu.add(accountItem);
	}

	private final static JMenu buildAccountMenu() {
		final JMenu accountItem = new JMenu((String)ResourceUtil.get(9124));
        final ImageIcon passwordIcon = new ImageIcon(ImageSrc.ACCOUNT_ICON);
		accountItem.setIcon(passwordIcon);
		return accountItem;
	}

	/**
     * 暂停使用，因为中继端可能升级，导致不可预知的问题
     * @param hcMenu
     */
	private static void buildAutoUpgradMenuItem(final JMenu hcMenu) {
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
					App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9102), true, null, null, new HCActionListener(new Runnable() {
						@Override
						public void run() {
							flipAutoUpgrade(upgradeItem, isAutoAfterClick);
							final JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html>" + (String)ResourceUtil.get(9105) + "</html>", App.getSysIcon(App.SYS_INFO_ICON), JLabel.LEADING), BorderLayout.CENTER);
							App.showCenterPanel(panel, 0, 0, ResourceUtil.getInfoI18N());
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
    
	public static void buildMenu(final Locale locale){
		if(ResourceUtil.isNonUIServer()){
			return;
		}
		
		CCoreUtil.checkAccess();
		
		popupTi = new JPopupMenu();
    	
    	popupTi.applyComponentOrientation(ComponentOrientation.getOrientation(locale));
    	
        final JMenu hcMenu = new JMenu(ResourceUtil.getProductName());
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
            
            if(ResourceUtil.isJ2SELimitFunction()){
            	buildAutoUpgradMenuItem(hcMenu);
            }
            
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
				browseCurrLog.setToolTipText((String)ResourceUtil.get(9264));
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
									pwdBS = ResourceUtil.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
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
				final File file = new File(ResourceUtil.getBaseDir(), ImageSrc.HC_LOG_BAK);
	            if(file.exists()){
			        final JMenuItem browseLogBak = new JMenuItem((String)ResourceUtil.get(9003));
					browseLogBak.setIcon(new ImageIcon(ImageSrc.LOG_BAK_ICON));
					browseLogBak.setToolTipText((String)ResourceUtil.get(9265));
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
											pwdBS = ResourceUtil.getFromBASE64(pwd).getBytes(IConstant.UTF_8);
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
	        if(ResourceUtil.isJ2SELimitFunction()){
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
	        }
	        
//	        hcMenu.addSeparator();
	        
	        //由于续费，所以关闭isDonateToken条件
			if(ResourceUtil.isJ2SELimitFunction()
					&& (PropertiesManager.isTrue(PropertiesManager.p_isDonateOrVIPNowOrEver, false)
							||
							RootConfig.getInstance().isTrue(RootConfig.p_isDisplayVIPMenu))){//TokenManager.isDonateToken() == false
	        	final JMenuItem vip = new JMenuItem("VIP Register");
	        	try {
					vip.setIcon(new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/vip_22.png"))));
				} catch (final IOException e4) {
				}
	        	vip.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						App.showVIP();
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
					
					final JLabel productName = new JLabel("HomeCenter - connect home cloud anywhere", null, JLabel.CENTER);
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
								showLicense("HomeCenter : License Agreement");
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
					ResourceUtil.buildMenu();
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
					ResourceUtil.buildMenu();
				}
			}, threadPoolToken));
        }
        popupTi.add(langSubItem);
        
        {
	        //注意：通过false，强制关闭WiFi密码配置界面
    		final WiFiDeviceManager platManager = PlatformManager.getService().getWiFiManager();
    		final boolean platEnableWiFi = ResourceUtil.canCreateWiFiAccountOnPlatform(platManager);
	    	final boolean hasWiFiAccount = HCURL.isUsingWiFiWPS && (PropertiesManager.isTrue(PropertiesManager.p_WiFi_isMobileViaWiFi) || platEnableWiFi);
	    	if(hasWiFiAccount){
	    		addPwdSubMenu(hasWiFiAccount, popupTi);//管理系统和WiFi的密码子菜单
	    	}else{
	        	final JMenu accountMenu = buildAccountMenu();
	    		addSysPwdMenuItem(accountMenu);//只添加系统密码菜单项到PopupMenu
	    		popupTi.add(accountMenu);
	    	}
        }
        
        {
        	//建菜单
        	buildCertMenu(popupTi);
        }
                
        if(ResourceUtil.isJ2SELimitFunction()) {
        	final Class checkJMFClass = ResourceUtil.loadClass("javax.media.CaptureDeviceManager", false);//注：关闭printNotFound
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
        
    	if(ResourceUtil.isJ2SELimitFunction()){//由于Android服务器版本布局不美观，故暂关闭
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
    	}
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
    				if(StringUtil.higher(J2SEContext.getSampleHarVersion(), lastSampleVer)){
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

//    				designer.setToolTipText((String)ResourceUtil.get(9080));
	            	designer.setIcon(designIco);
    	            designer.addActionListener(new HCActionListener(new Runnable() {
    	                @Override
						public void run() {
    	                	refreshActionMS(false);
    	                	
//        	                	IDArrayGroup.showMsg(IDArrayGroup.MSG_ID_DESIGNER, "<html><body style=\"width:700\">Mobile Designer is platform to design mobile menu and UI forms (for smart devices, DVD, TV...). " +
//    							"<BR>It is more than access PC desktop screen." +
//    							"<BR>On this platform, you can just click and move to build controller for IoT, import jar libraries, design powerful mobile UI to control and view these devices, load HAR project for smart devices." +
//    							"<BR><BR>click '" + (String)ResourceUtil.get(IContext.OK) + "' to install JRuby engine online.</body></html>",
//        	                			(String) ResourceUtil.getInfoI18N());
//        	                	JPanel panel = new JPanel();
//        	            		panel.add(new JLabel("<html>click '" + (String)ResourceUtil.get(IContext.OK) + "' to download JRuby online.</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.CENTER));
//        	            		App.showCenterPanel(panel, 0, 0, (String) ResourceUtil.getInfoI18N(), false, null, null, null, null, null, true, false);

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

        
        if(StarterManager.hadUpgradeError && ResourceUtil.isStandardJ2SEServer()){
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
					App.showCenterPanelMain(panel, 0, 0, "Fail on upgrade starter!", false, null, null, new HCActionListener(new Runnable() {
						@Override
						public void run() {
							String os = "Win";
							if(ResourceUtil.isLinux()){
								os = "Linux";
							}else if(ResourceUtil.isMacOSX()){
								os = "Mac";
							}
							HttpUtil.browse("http://homecenter.mobi/download/HC_Server_For_" + os + ".zip");
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
        Image oldImag = null;
		if (ti != null) {
			oldTip = ti.getToolTip();
			oldImag = ti.getImage();
			ti.remove();
		}
		try {
			ti = PlatformManager.getService().buildPlatformTrayIcon((ti != null ? ti.getImage() : hc_Disable),
					ResourceUtil.getProductName(), popupTi);
			if(oldTip != null){
				ti.setToolTip(oldTip);
			}
			if(oldImag != null){
				ti.setImage(oldImag);
			}
//			关闭缺省的菜单功能
//			if(aboutAction != null){
//				ti.setDefaultActionListener(aboutAction);
//			}
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}// 图标，标题，右键弹出菜单
    }

	private static void doAfterMobileReceivedCert() {
		displayMessage(ResourceUtil.getInfoI18N(), (String) ResourceUtil.get(9032), IContext.INFO, null, 0);
	}

	private static void flipAutoUpgrade(final JCheckBoxMenuItem upgradeItem,
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
				start.setProperty("ver", J2SEContext.MAX_HC_VER);
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
				App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9106), false, null, null, null, null, null, true, false, null, false, false);
			}
		}

	private static final void showLicense(final String title) {
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
			final URL oracle = ResourceUtil.getBCLURL();
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
							"Cant connect server, please try late!", ResourceUtil.getProductName(),
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
		dialog.getRootPane().registerKeyboardAction(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					dialog.dispose();
				}
			}, threadPoolToken),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);
	
		final JButton ok = App.buildDefaultOKButton();
	
		ok.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
			}
		}, threadPoolToken));
	
		final JPanel botton = new JPanel();
		botton.setLayout(new BorderLayout(10, 10));
		botton.add(ok, BorderLayout.EAST);
	
		final JScrollPane jsp = new JScrollPane(area);
		c.add(jsp, BorderLayout.CENTER);
		c.add(botton, BorderLayout.SOUTH);
	
		dialog.setSize(700, 600);
		dialog.setResizable(false);
		App.showCenter(dialog);
	}

	public static void runBrowser(final String fileName) {  
	    try {  
	        final File file = new File(ResourceUtil.getBaseDir(), fileName);
	        if(file.exists() == false){
	        	displayMessage((String) ResourceUtil.get(IContext.ERROR), 
						(String) ResourceUtil.get(9004), IContext.ERROR, null, 0);
	        	return;
	        }
	        final Desktop desktop = Desktop.getDesktop();  
	        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.OPEN)) {  
	            desktop.open(file);  
	        }else{
	        	displayMessage((String) ResourceUtil.get(IContext.ERROR), 
						(String) ResourceUtil.get(9005), IContext.ERROR, null, 0);
	        }
	    } catch (final IOException e) {  
	        ExceptionReporter.printStackTrace(e);  
	    }  
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

	static void transNewCertKey(final CoreSession coreSS, final HCConnection hcConnection) {
	//		LogManager.log("send Cert : " + CUtil.toHexString(CUtil.getCertKey()));
		if(coreSS.context.cmStatus != ContextManager.STATUS_SERVER_SELF){
			ServerCUtil.transCertKey(coreSS, hcConnection, CUtil.getCertKey(), MsgBuilder.E_TRANS_NEW_CERT_KEY, false);
		}else{
			ServerCUtil.transCertKey(coreSS, hcConnection, CUtil.getCertKey(), MsgBuilder.E_TRANS_NEW_CERT_KEY_IN_SECU_CHANNEL, false);
		}
	}

	public static void removeTray(final ThreadGroup threadPoolToken){
    	ContextManager.getThreadPool().run(new Runnable(){
			@Override
			public void run() {
				if(ti != null){
					ti.remove();
					ti.exit();
					ti = null;
				}
			}}, threadPoolToken);
	}
	
	public static final void doBefore(){
		ToolTipManager.sharedInstance().setDismissDelay(1000 * 1000);
		showTray();
		
		ServerUIUtil.restartResponsorServer(null, null);
	}
	
	public static void showTray() {
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

		ResourceUtil.buildMenu();
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
        	@Override
			public void run(){
        		ExitManager.startExitSystem();
        	}
        });
        
    	final String msg = (String)ResourceUtil.get(9009);//初始化中...
		displayMessage(ResourceUtil.getInfoI18N(), msg, 
    			IContext.INFO, null, 0);
		if(ti != null){//isDemoServer时，为null
			ti.setToolTip(msg);
		}
	}
	
	public static void displayMessage(final String caption, final String text, final int type, final Object imageData, final int timeOut){
	  	if(ResourceUtil.isNonUIServer()){
    		LogManager.log("this is demo server, skip displayMessage.");
    		return;
    	}
    	
    	if(text.startsWith("<html>")){
    		LogManager.errToLog("HTML tag can NOT be in TrayIcon displayMessage method.");
    	}
    	
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

	public static String buildLineOnTrayTip() {
		return (String) ResourceUtil.get(9008) + " (ID:" + IConstant.getUUID() + ")";
	}

	public static String buildMobileConnectionTip() {
		return (String)ResourceUtil.get(9012);
	}

	public static void showTransQues(final ThreadGroup threadPoolToken) {
		if(SingleMessageNotify.isShowMessage(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF) == false){
			new Thread(){
				@Override
				public void run(){//显示是否关闭trans证书的对话框
					if(ResourceUtil.isNonUIServer() || ResourceUtil.isDisableUIForTest()){//Demo时，不显示UI界面
						return;
					}
					SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, true);
					final JPanel askPanle = new JPanel();
					askPanle.setLayout(new BoxLayout(askPanle, BoxLayout.X_AXIS));
					
					askPanle.setBorder(new TitledBorder(((String)ResourceUtil.get(1021)) + "?"));
					
					askPanle.add(Box.createHorizontalGlue());
					askPanle.add(new JLabel(dl_certkey));
					askPanle.add(Box.createHorizontalGlue());
					askPanle.add(new JLabel(new ImageIcon(ImageSrc.MOVE_TO_ICON)));
					askPanle.add(Box.createHorizontalGlue());
					askPanle.add(new JLabel(disable_dl_certkey));
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
	
					App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getProductName(), true, null, null, new HCActionListener(new Runnable() {
						@Override
						public void run() {
							SingleMessageNotify.setShowToType(SingleMessageNotify.TYPE_DIALOG_TRANS_OFF, false);
							if(DefaultManager.isEnableTransNewCertNow()){
								flipTransable(false, true);																
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
			displayMessage(
					ResourceUtil.getInfoI18N(), (String)ResourceUtil.get(9061), IContext.INFO, null, 0);
		}
	}
}
