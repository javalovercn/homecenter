package hc.server.ui.design;

import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.J2SEServerURLAction;
import hc.server.ScreenServer;
import hc.server.msb.ConverterInfo;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.RealDeviceInfo;
import hc.server.msb.WiFiAccount;
import hc.server.msb.WiFiHelper;
import hc.server.ui.ClientDesc;
import hc.server.ui.HTMLMlet;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class AddHarHTMLMlet extends HTMLMlet {
	final JPanel addProcessingPanel = new JPanel(new BorderLayout());
	final String processingMsg = (String)ResourceUtil.get(9130);
	final JLabel processingLabel = new JLabel(processingMsg, null, SwingConstants.CENTER);
	public final JButton exitButton = new JButton((String)ResourceUtil.get(9131));
	final String css = "errorStatus {color:red}";
	final String css_error = "errorStatus";
	final BufferedImage okIcon, cancelIcon;
	
	public AddHarHTMLMlet(final String url){
		this();
		startAddHarProcess(url);
	}
	
	public AddHarHTMLMlet(){
		loadCSS(css);
		final int dpi = ClientDesc.getDPI();
		if(dpi < 300){
			okIcon = ImageSrc.OK_ICON;
			cancelIcon = ImageSrc.CANCEL_ICON;
		}else{
			okIcon = ImageSrc.OK_44_ICON;
			cancelIcon = ImageSrc.CANCEL_44_ICON;
		}
		final int fontSizePX = okIcon.getHeight();
//		int fontSizePX = (dpi < 200)?20:(dpi / 10);
//		if(fontSizePX > 40){
//			fontSizePX = 40;
//		}
		setLayout(new BorderLayout());
		final String fontSizeCSS = "font-size:" + fontSizePX + "px";
		setCSS(this, null, fontSizeCSS);
		addProcessingPanel.add(processingLabel, BorderLayout.CENTER);
		exitButton.setIcon(new ImageIcon(okIcon));
		exitButton.setEnabled(false);
		setCSS(exitButton, null, "text-align:center;vertical-align:middle;width:100%;height:100%" + ";" + fontSizeCSS);
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				go(URL_EXIT);
			}
		});
		addProcessingPanel.add(exitButton, BorderLayout.SOUTH);
		
		this.add(addProcessingPanel, BorderLayout.CENTER);
	}
	
	public static AddHarHTMLMlet getCurrAddHarHTMLMlet(){
		CCoreUtil.checkAccess();
		
		int count = 0;
		do{
			final Mlet currMlet = ScreenServer.getCurrMlet();
			if(currMlet != null && currMlet instanceof AddHarHTMLMlet){
				return (AddHarHTMLMlet)currMlet;
			}
			try{
				Thread.currentThread().sleep(200);
			}catch (final Exception e) {
			}
		}while(++count < 20);
		
		return null;
	}

	public void startAddHarProcess(final String url) {
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				startAddHar(url);
			}
		});
	}
	
	public void notifyBroadcastWifiAccout(final String projID, final String device){
		final String isBroadcast = (String)ResourceUtil.get(9132);
		processingLabel.setText(StringUtil.replace(StringUtil.replace(isBroadcast, "{projID}", projID), "{devName}", device));
	}
	
	public final void setProcessingMessage(final String msg){
		processingLabel.setText(msg);
	}
	
	private void startAddHar(final String url){
		L.V = L.O ? false : LogManager.log("try ready to download HAR [" + url.toString() + "]...");
		
		//关闭可能资源锁窗口，比如Designer或LinkProjectPanel
		if(LinkProjectStatus.isIdle() == false){
			final String designMenuItem = (String)ResourceUtil.get(9034);
			final String linkProjectMenuItem = (String)ResourceUtil.get(9059);
			final String closeStr = (String)ResourceUtil.get(9129);
			
			processingLabel.setText(StringUtil.replace(StringUtil.replace(closeStr, "{designer}", designMenuItem), "{panel}", linkProjectMenuItem));
			exitButton.setEnabled(true);
			return;
		}
		
		if(LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_ADD_HAR_VIA_MOBILE)){
			try{
				final File fileHar = ResourceUtil.getTempFileName(Designer.HAR_EXT);
				final Properties had = new Properties();
				String strharurl = null;
				//支持har和had下载
				final String lowerCaseURL = url.toLowerCase();
				if(lowerCaseURL.indexOf("http") != 0){
					throw new Exception("it must be download file");
				}
				if(lowerCaseURL.endsWith(Designer.HAR_EXT)){
					strharurl = url;
				}else if(lowerCaseURL.endsWith(Designer.HAD_EXT)){
					LinkProjectManager.loadHAD(url, had);
					strharurl = had.getProperty(HCjad.HAD_HAR_URL, HCjad.convertToExtHar(url));
				}else{
					throw new Exception("it must be har or had file.");
				}
				PropertiesManager.addDelFile(fileHar);
				
				final String hadmd5 = had.getProperty(HCjad.HAD_HAR_MD5, "");
				final boolean succ = HttpUtil.download(fileHar, new URL(strharurl));
				if(succ == false){
					final String httpErr = "http connection error";
					throw new Exception(httpErr);
				}
				
		        if((hadmd5.length() > 0 && ResourceUtil.getMD5(fileHar).toLowerCase().equals(hadmd5.toLowerCase()))
		        		|| hadmd5.length() == 0){
		        	final Map<String, Object> map = getMap(fileHar);
					if(map.isEmpty()){
						final String errMsg = "HAR project file is corrupted or incomplete.";
						throw new Exception(errMsg);
					}
//					final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
//					if(licenseURL.length() > 0){
//					}
					
					final String proj_id = (String)map.get(HCjar.PROJ_ID);
					final boolean isUpgrade = (LinkProjectManager.getProjByID(proj_id) != null);
					final File oldBackEditFile = null;
					if(isUpgrade){
						final String errMsg = "project [" + proj_id + "] is added or using now!";
						throw new Exception(errMsg);
					}
					
					final ArrayList<LinkProjectStore> appendLPS = appendMapToSavedLPS(fileHar, map, false, isUpgrade, oldBackEditFile);
					LinkProjectManager.reloadLinkProjects();//十分重要
					
					final MobiUIResponsor mobiResp = (MobiUIResponsor)ServerUIUtil.getResponsor();
					
					final ProjResponser[] appendResp = mobiResp.appendNewHarProject();//仅扩展新工程，不启动或不运行
					BindRobotSource bindSource = mobiResp.bindRobotSource;
					if(bindSource == null){
						bindSource = new BindRobotSource(mobiResp);
					}
					
					if(BindManager.findNewUnbind(bindSource)){//进行自动绑定
						//TODO mobile bind 
						final String errMsg = "please bind on Server";
						throw new Exception(errMsg);
					}
					
					mobiResp.fireSystemEventListenerOnAppendProject(appendResp, appendLPS);//进入运行状态
					
					final ProjResponser resp = mobiResp.findContext(mobiResp.getCurrentContext());
					final JarMainMenu linkMenu = resp.menu[resp.mainMenuIdx];
					linkMenu.rebuildMenuItemArray();
					ContextManager.getContextInstance().send(MsgBuilder.E_MENU_REFRESH, linkMenu.buildJcip());
					
					L.V = L.O ? false : LogManager.log("successful apply added project to ACTIVE status.");
		        }else{
		        	final String errMsg = "md5 error, try after a minute";
					throw new Exception(errMsg);
		        }
		        processingLabel.setText((String)ResourceUtil.get(9128));
			}catch (final Throwable e) {
				e.printStackTrace();
				processingLabel.setIcon(new ImageIcon(cancelIcon));
				processingLabel.setText(e.getMessage());
			}finally{
				LinkProjectStatus.exitStatus();
				exitButton.setEnabled(true);
			}
		}
	}

	/**
	 * 添加一个har到lps并存储。
	 * 注意：本过程被初始加载及发布MyFirst.har使用
	 * @param fileHar
	 * @param map
	 * @return
	 */
	static final ArrayList<LinkProjectStore> appendMapToSavedLPS(final File fileHar, final Map<String, Object> map, 
			final boolean isRoot, final boolean isUpgrade, final File oldBackEditFile) {
		final LinkEditData led = buildAddHarDesc(fileHar, map, "", "");
		AddHarHTMLMlet.addHarToDeployArea(led, led.lps, isRoot, isUpgrade, oldBackEditFile);
		
		final Iterator<LinkProjectStore> lpsIT = LinkProjectManager.getLinkProjsIterator(true);
		final Vector<LinkProjectStore> lpsVector = new Vector<LinkProjectStore>();
		while(lpsIT.hasNext()){
			lpsVector.add(lpsIT.next());
		}

		//添加新工程到存储集中
		final ArrayList<LinkProjectStore> appendLPS = new ArrayList<LinkProjectStore>();
		appendLPS.add(led.lps);//可能有多个工程
		L.V = L.O ? false : LogManager.log("successful add HAR project [" + led.lps.getProjectID() + "]");
		
		{
			final int size = appendLPS.size();
			for (int i = 0; i < size; i++) {
				lpsVector.add(appendLPS.get(i));
			}
		}
		
		LinkProjectStore[] lpss = {};
		lpss = lpsVector.toArray(lpss);
		AddHarHTMLMlet.saveLinkStore(lpss, AddHarHTMLMlet.getLinkProjSet());//更新到LPS
		return appendLPS;
	}
	
	/**
	 * 
	 * @param bar
	 * @param totalDevice
	 * @param descLabel
	 * @param isCancel true:use click 'Cancel' to connect all devices
	 * @return
	 */
	public JProgressBar showProgressBar(final JProgressBar bar, final int totalDevice, final int finishedDevice, final JLabel descLabel, final boolean[] isCancel){
		final JPanel progressPanel = new JPanel(new GridLayout(3, 1));
		progressPanel.add(bar);
		progressPanel.add(descLabel);
		final JButton cancel = new JButton((String) ResourceUtil.get(IContext.CANCEL));
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				isCancel[0] = true;
			}
		});
		progressPanel.add(cancel);
		showPanelAtNorth(progressPanel);
		return bar;
	}
	
	public void waitForInputWiFiPassword(final ThreadGroup token){
		
		final JPanel inputPanel = new JPanel(new GridLayout(11, 1));
		
		inputPanel.add(new JLabel(WiFiHelper.WIFI_SSID, WiFiHelper.getWiFiIcon(), SwingConstants.LEFT));
		final JTextField ssidField = new JTextField("");
		inputPanel.add(ssidField);
		
		inputPanel.add(new JLabel(WiFiHelper.getPasswordDesc(), WiFiHelper.getWiFiIcon(), SwingConstants.LEFT));
		final JPasswordField pass1 = new JPasswordField("");
		pass1.setEchoChar('*');
		inputPanel.add(pass1);
		
		final JPasswordField pass2 = new JPasswordField("");
		pass2.setEchoChar('*');
		inputPanel.add(pass2);
		
		inputPanel.add(new JLabel(WiFiHelper.WIFI_SECURITY_OPTION, SwingConstants.LEFT));
		final ButtonGroup bgSecurityOption=new ButtonGroup();
		final JRadioButton rb_nopass = new JRadioButton(WiFiHelper.SECURITY_WIFI_NONE);
		final JRadioButton rb_wep = new JRadioButton(WiFiAccount.SECURITY_OPTION_WEP);
		final JRadioButton rb_wpa = new JRadioButton(WiFiAccount.SECURITY_OPTION_WPA_WPA2_PSK);

		bgSecurityOption.add(rb_nopass);
		bgSecurityOption.add(rb_wep);
		bgSecurityOption.add(rb_wpa);
		rb_nopass.setSelected(true);
		
		inputPanel.add(rb_nopass);
		inputPanel.add(rb_wep);
		inputPanel.add(rb_wpa);
		
		final JLabel statusReport = new JLabel("", SwingConstants.LEFT);
		inputPanel.add(statusReport);
		
		final JButton submit = new JButton((String) ResourceUtil.get(IContext.OK));
		inputPanel.add(submit);
		
		submit.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final AddHarHTMLMlet addHarHTMLMlet = AddHarHTMLMlet.this;
				if(ssidField.getText().length() == 0){
					statusReport.setText((String)ResourceUtil.get(9126));
					ssidField.requestFocusInWindow();
					addHarHTMLMlet.setCSS(statusReport, null, css_error);
					return;
				}
				
				if(pass1.getText().equals(pass2.getText()) == false){
					statusReport.setText((String)ResourceUtil.get(9127));
					addHarHTMLMlet.setCSS(statusReport, null, css_error);
					return;
				}
				
				WiFiHelper.updateWiFiAccount(ssidField.getText(), pass1, rb_nopass, rb_wep, rb_wpa);
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						addHarHTMLMlet.removeAll();
						addHarHTMLMlet.add(addProcessingPanel, BorderLayout.CENTER);
						
						revalidateMlet(addHarHTMLMlet);
					}
				});
				
				synchronized (addHarHTMLMlet) {
					addHarHTMLMlet.notify();
				}
			}
		}, token));
		
		showPanelAtNorth(inputPanel);
		
		synchronized (this) {
			try {
				wait();
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void showPanelAtNorth(final JPanel inputPanel) {
		this.removeAll();
		this.add(inputPanel, BorderLayout.NORTH);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				revalidateMlet(AddHarHTMLMlet.this);				
			}
		});
	}
	
	private void revalidateMlet(final AddHarHTMLMlet addHarHTMLMlet) {
		addHarHTMLMlet.invalidate();
		addHarHTMLMlet.validate();
		addHarHTMLMlet.revalidate();
	}

	public static void startAddHTMLHarUI(final String urlStr) {
		CCoreUtil.checkAccess();
	
		try{
			AddHarHTMLMlet.class.getName();
			final Object[] para = J2SEServerURLAction.getProjectContextForSystemLevelNoTerminate();
			final HCJRubyEngine hcje = (HCJRubyEngine)para[0];
			final String scripts = "" +
					"#encoding:utf-8\n" +
					"import Java::" + AddHarHTMLMlet.class.getName() + "\n" +
					"return " + AddHarHTMLMlet.class.getSimpleName() + ".new(" + (urlStr==null?"":("\"" + urlStr + "\"")) + ")\n";
			ProjResponser.startMlet(scripts, null, "SYS_AddHarMlet", "add HAR", hcje, (ProjectContext)para[1]);
//						hcje.terminate();//注意：切勿关闭，因为可能被或再次使用
		}catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将一个工程全部绑定，更新到LPS中。但不保存
	 * @param projID
	 * @param bdns 每一个Robot可能含有多个BindDeviceNode，所以为数组。此为工程下全部Robots的，故为数组的Vector
	 */
	static void updateProjectBindsToLPS(final String projID, final Vector<BindDeviceNode[]> bdns){
		final int size = bdns.size();
		if(size > 0){
			final Vector<String> dev_bind_ids = new Vector<String>();
			final Vector<String> conv_bind_ids = new Vector<String>();
			final Vector<BindDeviceNode> dev_binds = new Vector<BindDeviceNode>();
			
			for (int i = 0; i < size; i++) { 
	        	toArrayReferDevice(projID, bdns.get(i), dev_bind_ids, conv_bind_ids, dev_binds);
			}
			
			//更新到相应project
			updateOneProjBindToLPS(projID, dev_bind_ids, conv_bind_ids, dev_binds);
		}
	}

	private static void toArrayReferDevice(final String projID, final BindDeviceNode[] bdns,
			final Vector<String> dev_bind_ids, final Vector<String> cov_bind_ids, 
			final Vector<BindDeviceNode> dev_binds){
		final int size = bdns.length;
		if(size > 0){
			for (int i = 0; i < size; i++) { 
	        	final BindDeviceNode referBDN = bdns[i];
	        	final String robotName = referBDN.lever2Name;
	        	
	        	final String bindID = DeviceBindInfo.buildStandardBindID(projID, robotName, referBDN.ref_dev_ID);
	        	
	        	if(referBDN.isBindedRealDevice()){
	            	dev_bind_ids.add(bindID);
	            	dev_binds.add(referBDN);
	        	}
	        	
	        	if(referBDN.isBindedConverter()){
	        		cov_bind_ids.add(bindID);
	        	}
			}
		}
	}

	static void updateOneProjBindToLPS(final String projID,
			final Vector<String> dev_bind_ids,
			final Vector<String> conv_bind_ids,
			final Vector<BindDeviceNode> dev_binds) {
//		CCoreUtil.checkAccess();//friend方式，关闭checkAccess
		
		final LinkProjectStore lps = LinkProjectManager.getProjByID(projID);
		if(lps != null){
			lps.clearBindMap();
	
			{
				final int dev_bind_size = dev_bind_ids.size();
				if(dev_bind_size > 0){
					final String[] ids = new String[dev_bind_size];
					final RealDeviceInfo[] binds = new RealDeviceInfo[dev_bind_size];
					
					for (int i = 0; i < dev_bind_size; i++) {
						ids[i] = dev_bind_ids.elementAt(i);
						binds[i] = dev_binds.elementAt(i).realDevBind;
					}
					
					lps.setDevBindMap(ids, binds, ids.length);
				}
			}
			
			{
				final int conv_bind_size = conv_bind_ids.size();
				if(conv_bind_size > 0){
					final String[] ids = new String[conv_bind_size];
					final ConverterInfo[] binds = new ConverterInfo[conv_bind_size];
					
					final int size = dev_bind_ids.size();
					int index = 0;
					for (int i = 0; i < size; i++) {
						final ConverterInfo convBind = dev_binds.elementAt(i).convBind;
						if(convBind != null){
							ids[index] = dev_bind_ids.elementAt(i);
							binds[index++] = convBind;
						}
					}
					
					lps.setConvBindMap(ids, binds, ids.length);
				}
			}
			
			lps.setDoneBind(true);
		}
	}

	public static Map<String, Object> getMap(final File file){
		return HCjar.loadHar(file, false);
	}

	/**
	 * 
	 * @param file
	 * @param map
	 * @param linkName 如果不是覆盖旧的，则为空串
	 * @param linkRemark 如果不是覆盖旧的，则为空串
	 * @return
	 */
	static LinkEditData buildAddHarDesc(final File file,
			final Map<String, Object> map, final String linkName, final String linkRemark) {
		final LinkEditData led = new LinkEditData();
		final LinkProjectStore lps = new LinkProjectStore();
		led.lps = lps;
		led.filePath = (file.getAbsolutePath());
		led.op = (LinkProjectManager.STATUS_NEW);
		led.status = (LinkProjectManager.STATUS_NEW);
		
		lps.setActive(true);
		lps.setLinkName(linkName);
		lps.setProjectRemark(linkRemark);
		lps.setProjectID((String)map.get(HCjar.PROJ_ID));
		lps.copyFrom(map);
		return led;
	}

	static boolean addHarToDeployArea(final LinkEditData led, final LinkProjectStore lps, 
			final boolean isRoot, final boolean isUpgrade, final File oldBackFile) {
		final File source = new File(led.filePath);//不能加App.getBaseDir(), 它是绝对路径。适合J2SE, Android
		final boolean result = LinkProjectManager.importLinkProject(lps, source, isUpgrade, oldBackFile);
		led.status = LinkProjectManager.STATUS_DEPLOYED;
		if(isRoot){
			lps.setRoot(true);
		}
		return result;
	}

	static PropertiesSet getLinkProjSet() {
		return new PropertiesSet(PropertiesManager.S_LINK_PROJECTS);
	}

	static void saveLinkStore(final LinkProjectStore[] lpss, final PropertiesSet projIDSet) {
		final Object[] objs = new Object[lpss.length];
		for (int i = 0; i < lpss.length; i++) {
			final LinkProjectStore lps = lpss[i];
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("save link store for project [" + lps.getProjectID() + "] {" + lps + "}");
			}
			objs[i] = lps.toSerial();
		}
		
		projIDSet.refill(objs);
		projIDSet.save();
		
//		有可能只需要保存，而不需要拉新
//		LinkProjectManager.reloadLinkProjects();
	}
	
}
