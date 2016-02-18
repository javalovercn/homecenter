package hc.server.msb;

import hc.App;
import hc.UIActionListener;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.res.ImageSrc;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.AddHarHTMLMlet;
import hc.server.ui.design.MobiUIResponsor;
import hc.server.ui.design.ProjResponser;
import hc.util.BaseResponsor;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class WiFiHelper {
	public static final String WIFI_SSID = "SSID";
	public static final String WIFI_SECURITY_OPTION = "Security Option";
	public static final String SECURITY_WIFI_NONE = "NONE";
	public final static WiFiDeviceManager remoteWrapper = new WiFiManagerRemoteWrapper();
	
	public static void startAPIfExists() {
		CCoreUtil.checkAccess();
		
		final String isAutoCreated = PropertiesManager.getValue(PropertiesManager.p_WiFi_currIsAutoCreated);
		if(isAutoCreated != null && isAutoCreated.equals(IConstant.TRUE)){//启动自带WiFi模块的AP，而非router
			startAPOnServer();
		}
	}
	
	/**
	 * 本方法是Device到J2SE和Android Server的中转方法
	 * @return
	 */
	static WiFiAccount getWiFiAccount(final ProjectContext ctx, final ThreadGroup token){
		synchronized(WiFiHelper.class){//有可能被多个Device同时调用，所以加锁
			String currWiFiSSID = PropertiesManager.getValue(PropertiesManager.p_WiFi_currSSID);
			if(currWiFiSSID == null){
				final WiFiDeviceManager instance = WiFiDeviceManager.getInstance();
				if((instance.hasWiFiModule() && instance.canCreateWiFiAccount())){
//					|| 
//				(ContextManager.isMobileLogin() && ClientDesc.agent.ctrlWiFi()
					createAccountAuto(ctx, token);
				}else{
					return null;
				}
			}
			
			currWiFiSSID = PropertiesManager.getValue(PropertiesManager.p_WiFi_currSSID);
			final String currWiFiPWD = PropertiesManager.getValue(PropertiesManager.p_WiFi_currPassword);
			final String currWiFiSecurityOption = PropertiesManager.getValue(PropertiesManager.p_WiFi_currSecurityOption);
			
			final WiFiAccount account = new WiFiAccount(currWiFiSSID, currWiFiPWD, currWiFiSecurityOption);
			
			return account;
		}
	}

	private static void createAccountAuto(final ProjectContext projCtx, final ThreadGroup token) {
		final WiFiDeviceManager wifiDeviceManager = WiFiDeviceManager.getInstance();
		
		final String hasWiFiModuleKey = PropertiesManager.getValue(PropertiesManager.p_WiFi_hasWiFiModule);
		boolean hasWiFiModule;
		if(hasWiFiModuleKey == null){
			hasWiFiModule = wifiDeviceManager.hasWiFiModule();
			final String value = hasWiFiModule?IConstant.TRUE:IConstant.FALSE;
			PropertiesManager.setValue(PropertiesManager.p_WiFi_hasWiFiModule, value);
		}else{
			hasWiFiModule = hasWiFiModuleKey.equals(IConstant.TRUE);
		}
		
		boolean canCreateAP = false;
		if(hasWiFiModule){
			final String canCreateAPKey = PropertiesManager.getValue(PropertiesManager.p_WiFi_canCreateAP);
			if(canCreateAPKey == null){
				canCreateAP = wifiDeviceManager.canCreateWiFiAccount();
				final String value = canCreateAP?IConstant.TRUE:IConstant.FALSE;
				PropertiesManager.setValue(PropertiesManager.p_WiFi_canCreateAP, value);
			}else{
				canCreateAP = canCreateAPKey.equals(IConstant.TRUE);
			}
		}
		
		final String isAutoCreated = PropertiesManager.getValue(PropertiesManager.p_WiFi_currIsAutoCreated);
		if(canCreateAP){
			if(isAutoCreated == null){
				createAPOnServer();
			}
			startAPOnServer();
		}else{
			if(isAutoCreated == null){
				if(ContextManager.isMobileLogin()){
					final AddHarHTMLMlet currMlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet();
					if(currMlet != null){
						currMlet.waitForInputWiFiPassword(token);
					}else{
						projCtx.sendMessage((String)ResourceUtil.get(IContext.INFO), "unknow status of Mlet", ProjectContext.MESSAGE_INFO, null, 0);
					}
				}else{
					showInputWiFiPassword(true);
				}
				
				while( PropertiesManager.getValue(PropertiesManager.p_WiFi_currIsAutoCreated) == null){
					try{
						Thread.sleep(500);
					}catch (final Exception e) {
					}
				}
			}
		}
	}
	
	public static void showInputWiFiPassword(final boolean isRequired){
		CCoreUtil.checkAccess();
		
		final String passwdStr = getPasswordDesc();

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		final Insets insets = new Insets(5, 5, 5, 5);

		final JLabel jluuid = new JLabel();
		jluuid.setIcon(getWiFiIcon());
		final JPanel uuidPanelflow = new JPanel();
		uuidPanelflow.setLayout(new FlowLayout());
		uuidPanelflow.add(jluuid);
		uuidPanelflow.add(new JLabel(WIFI_SSID));
		uuidPanelflow.add(new JLabel(":"));
		panel.add(uuidPanelflow, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));

		final int columns = 15;
		final JTextField jtfuuid = new JTextField("", columns);
		jtfuuid.setForeground(Color.BLUE);
		jtfuuid.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(jtfuuid, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));

		final JLabel jlPassword = new JLabel(passwdStr);
		jlPassword.setIcon(getPasswordIcon());
		
		final JPasswordField passwd1, passwd2;
		passwd1 = new JPasswordField("", columns);
		passwd1.setEchoChar('*');
		passwd1.enableInputMethods(true);
		passwd1.setHorizontalAlignment(SwingUtilities.RIGHT);
		passwd2 = new JPasswordField("", columns);
		passwd2.setEchoChar('*');
		passwd2.enableInputMethods(true);
		passwd2.setHorizontalAlignment(SwingUtilities.RIGHT);

		final JPanel pwJpanel = new JPanel();
		pwJpanel.setLayout(new FlowLayout());
		pwJpanel.add(jlPassword);
		pwJpanel.add(new JLabel(":"));
		panel.add(pwJpanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		Component subItem = passwd1;
		panel.add(subItem, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));

		final JPanel doublepw = new JPanel();
		doublepw.setLayout(new FlowLayout());
		final JLabel jlPassword2 = new JLabel(passwdStr);
		jlPassword2.setIcon(getPasswordIcon());
		doublepw.add(jlPassword2);

		doublepw.add(new JLabel(":"));

		panel.add(doublepw, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		
		subItem = passwd2;
		panel.add(subItem, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));

		panel.add(new JLabel(), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		final JPanel securityOptioncb = new JPanel(new FlowLayout());
		final ButtonGroup bgSecurityOption=new ButtonGroup();
		final JRadioButton rb_nopass = new JRadioButton(SECURITY_WIFI_NONE);
		final JRadioButton rb_wep = new JRadioButton(WiFiAccount.SECURITY_OPTION_WEP);
		final JRadioButton rb_wpa = new JRadioButton(WiFiAccount.SECURITY_OPTION_WPA_WPA2_PSK);

		bgSecurityOption.add(rb_nopass);
		bgSecurityOption.add(rb_wep);
		bgSecurityOption.add(rb_wpa);
		
		securityOptioncb.add(rb_nopass);
		securityOptioncb.add(rb_wep);
		securityOptioncb.add(rb_wpa);
		
		rb_nopass.setSelected(true);
		panel.add(securityOptioncb, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));
		
		panel.add(new JSeparator(SwingConstants.HORIZONTAL), new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));
		
		final JButton jbOK = new JButton(
					(String) ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));
		final UIActionListener jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				final String ssid = jtfuuid.getText();
				if(ssid.length() == 0){
					App.showMessageDialog(window, "WiFi SSID is empty!",
							(String)ResourceUtil.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (passwd2.getText().equals(passwd1.getText())) {
					final WiFiAccount wifiAccount = updateWiFiAccount(ssid, passwd1, rb_nopass, rb_wep, rb_wpa);

					window.dispose();
					
					if(isRequired == false){
						//通知在线Device.notifyNewWiFiAccount
						final BaseResponsor resp = ServerUIUtil.getResponsor();
						if(resp != null && resp instanceof MobiUIResponsor){
							final MobiUIResponsor mresp = (MobiUIResponsor)resp;
							final HashMap<ProjResponser, Device[]> projs = mresp.getAllDevices();
							final Iterator<ProjResponser> it = projs.keySet().iterator();
							while(it.hasNext()){
								final ProjResponser proj = it.next();
								final Device[] devs = projs.get(proj);
								final int size = devs.length;
								if(size > 0){
									proj.threadPool.run(new Runnable() {
										@Override
										public void run() {
											for (int i = 0; i < size; i++) {
												final Device device = devs[i];
												L.V = L.O ? false : LogManager.log("notifyNewWiFiAccount to [" + device.project_id + "/" + device.getName() + "].");
												if(HCURL.isUsingWiFiWPS){
//													device.notifyNewWiFiAccount(wifiAccount);
												}
											}
										}
									});
								}
							}
						}
					}
				} else {
					App.showMessageDialog(window, StringUtil.replace((String)ResourceUtil.get(9077), "{min}", "" + 0),
							(String)ResourceUtil.get(IContext.ERROR), JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		UIActionListener cancelAction;
		if(isRequired){
			cancelAction = new UIActionListener() {
				@Override
				public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
					window.dispose();
					showInputWiFiPassword(isRequired);
				}
			};
		}else{
			cancelAction = new UIActionListener() {
				@Override
				public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
					window.dispose();
				}
			};
		}
		
		final JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(panel, BorderLayout.CENTER);
		final String withCancel = StringUtil.replace((String)ResourceUtil.get(9125), "{cancel}", (String)ResourceUtil.get(IContext.CANCEL));
		centerPanel.add(new JLabel("<html><STRONG>"+(String)ResourceUtil.get(9095)+"</STRONG><BR>" + withCancel + "</html>"), BorderLayout.SOUTH);
		centerPanel.setBorder(new TitledBorder(""));
		
		App.showCenterPanelButtons(centerPanel, 0, 0, getInputWiFiAccountStr(), isRequired?false:true, jbOK, isRequired?null:App.buildDefaultCancelButton(),
				jbOKAction, isRequired?null:cancelAction, null, false, false, null, false, false);
		jtfuuid.requestFocus();
	}

	public static String getInputWiFiAccountStr() {
		return (String)ResourceUtil.get(9123);
	}

	public static ImageIcon getPasswordIcon() {
		return new ImageIcon(ImageSrc.PASSWORD_ICON);
	}

	public static String getPasswordDesc() {
		return (String) ResourceUtil.get(1007);
	}

	public static ImageIcon getWiFiIcon() {
		return new ImageIcon(ResourceUtil
				.getResource("hc/res/wifi_22.png"));
	}
	
	static final void startAPOnServer(){
		final String ssid = PropertiesManager.getValue(PropertiesManager.p_WiFi_currSSID);
		final String pwd = PropertiesManager.getValue(PropertiesManager.p_WiFi_currPassword);
		final String option = PropertiesManager.getValue(PropertiesManager.p_WiFi_currSecurityOption);
		
		try{
			WiFiDeviceManager.getInstance().startWiFiAP(ssid, pwd, option);
		}catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	static final void createAPOnServer(){
//		CCoreUtil.checkAccess();
		
		final String pwd = ResourceUtil.createRandomVariable(63, 0);
//		try{
//			Thread.sleep(Math.abs(pwd.hashCode()) % 300);
//		}catch (final Exception e) {
//		}
		final String password = PropertiesManager.getValue(PropertiesManager.p_password, "");
		final String ssid = "hc_" + ResourceUtil.createRandomVariable(32 - 3, password.hashCode());//SSID最长为32
		final String securityOption = ConfigManager.WIFI_SECURITY_OPTION_WPA_WPA2_PSK;
		
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currSSID, ssid);
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currPassword, pwd);
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currSecurityOption, securityOption);
		
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currIsAutoCreated, IConstant.TRUE);
		
		PropertiesManager.saveFile();
	}

	public static WiFiAccount updateWiFiAccount(final String ssid,
			final JPasswordField passwd1, final JRadioButton rb_nopass,
			final JRadioButton rb_wep, final JRadioButton rb_wpa) {
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currSSID, ssid);
		final String pwd = passwd1.getText();
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currPassword, pwd);
		String securityOp = "";
		if(rb_nopass.isSelected()){
			securityOp = WiFiAccount.SECURITY_OPTION_NO_PASSWORD;
		}else if(rb_wep.isSelected()){
			securityOp = WiFiAccount.SECURITY_OPTION_WEP;
		}else if(rb_wpa.isSelected()){
			securityOp = WiFiAccount.SECURITY_OPTION_WPA_WPA2_PSK;
		}
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currSecurityOption, securityOp);
		
		PropertiesManager.setValue(PropertiesManager.p_WiFi_currIsAutoCreated, IConstant.FALSE);
		PropertiesManager.saveFile();
		
		return new WiFiAccount(ssid, pwd, securityOp);
	}
}
