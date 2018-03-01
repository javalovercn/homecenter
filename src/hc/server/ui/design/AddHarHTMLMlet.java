package hc.server.ui.design;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.core.util.StringValue;
import hc.core.util.ThreadPriorityManager;
import hc.server.HCActionListener;
import hc.server.PlatformManager;
import hc.server.ScreenServer;
import hc.server.TrayMenuUtil;
import hc.server.msb.ConverterInfo;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.RealDeviceInfo;
import hc.server.msb.WiFiAccount;
import hc.server.msb.WiFiHelper;
import hc.server.ui.ClientSessionForSys;
import hc.server.ui.HTMLMlet;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.DelDeployedProjManager;
import hc.server.util.SafeDataManager;
import hc.server.util.SignHelper;
import hc.util.ClassUtil;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class AddHarHTMLMlet extends SystemHTMLMlet {
	final JTextArea msgArea = new JTextArea();
	final String css = "errorStatus {color:red}";
	final String css_error = "errorStatus";
	public final JButton exitButton;
	final ProjectContext ctx;

	final BufferedImage okImage, cancelImage;// 该值同时被LicenseHTMLMlet引用
	String iagreeStr, acceptStr, cancelStr, acceptAllAndNeverDisplay;// 该值被LicenseHTMLMlet引用
	String licenseText;

	// need system level resource.
	String processingMsg, exitButtonStr;

	public final synchronized void appendMessage(final String msg) {
		msgArea.append(msg + "\n");
	}

	static ThreadGroup token;

	public static void initToken() {
		if (token == null) {
			token = App.getThreadPoolToken();
		}
	}

	@Override
	public void onExit() {
		super.onExit();
		runingAddHar = null;
		J2SESessionManager.notifySessionEvent(SessionEventManager.EVENT_ADD_HAR_BUSY, token);
	}

	public AddHarHTMLMlet() {
		ctx = getProjectContext();

		ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				exitButtonStr = ResourceUtil.get(localCoreSS, 9131);
				processingMsg = ResourceUtil.get(localCoreSS, 9130);

				iagreeStr = ResourceUtil.get(localCoreSS, 9115);
				acceptStr = ResourceUtil.get(localCoreSS, 9114);
				acceptAllAndNeverDisplay = ResourceUtil.get(localCoreSS, 9241);
				acceptStr = StringUtil.replace(acceptStr, "{iagree}", iagreeStr);
				cancelStr = ResourceUtil.get(localCoreSS, IContext.CANCEL);

				// HCLimitSecurityManager.getHCSecurityManager().setAllowAccessSystemImageResource(true);
				return null;
			}
		}, token);

		// final int dpi = UserThreadResourceUtil.getMobileDPIFrom(localCoreSS);
		// if(dpi < 300){
		// //因为Android服务器会对自动缩放图片（ImageSrc.OK_ICON），所以必须重新提取。
		// //并如上，调用setAllowAccessSystemResource，及如下关闭allow
		// okImage = ImageSrc.loadImageFromPath(ImageSrc.HC_RES_OK_22_PNG_PATH);
		// cancelImage =
		// ImageSrc.loadImageFromPath(ImageSrc.HC_RES_CANCEL_22_PNG_PATH);
		// }else{
		// okImage = ImageSrc.loadImageFromPath(ImageSrc.HC_RES_OK_44_PNG_PATH);
		// cancelImage =
		// ImageSrc.loadImageFromPath(ImageSrc.HC_RES_CANCEL_44_PNG_PATH);
		// }

		// ContextManager.getThreadPool().run(new Runnable() {
		// @Override
		// public void run() {
		// HCLimitSecurityManager.getHCSecurityManager().setAllowAccessSystemImageResource(false);
		// }
		// }, token);

		{
			final ProjResponser pr = ServerUIAPIAgent.getProjResponserMaybeNull(ctx);
			okImage = (BufferedImage) ServerUIAPIAgent.getClientSessionAttributeForSys(localCoreSS,
					pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_OK_ICON);
			cancelImage = (BufferedImage) ServerUIAPIAgent.getClientSessionAttributeForSys(
					localCoreSS, pr, ClientSessionForSys.STR_CLIENT_SESSION_ATTRIBUTE_CANCEL_ICON);
		}

		final int fontSizePX = okImage.getHeight();

		loadCSS(SystemHTMLMlet.buildCSS(getButtonHeight(), getFontSizeForButton(),
				getColorForFontByIntValue(), getColorForBodyByIntValue()) + css, false);

		exitButton = new JButton(exitButtonStr);
		setButtonStyle(exitButton);

		setLayout(new BorderLayout());
		setCSS(msgArea, null,
				"width:100%;height:100%;border:1px solid #" + getColorForBodyByHexString() + ";"
						+ "background-color:#" + getColorForBodyByHexString() + ";color:#"
						+ HTMLMlet.getColorForFontByHexString() + ";");

		appendMessage(processingMsg);

		exitButton.setIcon(new ImageIcon(okImage));
		setButtonEnable(exitButton, false);
		exitButton.setPreferredSize(new Dimension(getMobileWidth(), SystemHTMLMlet
				.getButtonHeight(fontSizePX + getFontSizeForButton(), getButtonHeight())));
		// setCSS(exitButton, null, "width:100%;height:100%;border-radius: " +
		// getButtonHeight() + "px;display: block;transition: all 0.15s
		// ease;border: 0.1em solid #" + HTMLMlet.getColorForFontByHexString() +
		// ";" + fontSizeCSS);//系统Mlet, //不考虑in user thread
		// setCSSForDiv(exitButton, null, BUTTON_DIV);
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				go(URL_EXIT);
			}
		});

		// final String fontSizeCSS = "font-size:" + fontSizePX + "px;";
		// setCSS(this, null, fontSizeCSS);//系统Mlet, //不考虑in user thread

		add(msgArea, BorderLayout.CENTER);
		add(exitButton, BorderLayout.SOUTH);
	}

	public static J2SESession getCurrAddHarHTMLMletCoreSession() {
		final AddHarHTMLMlet addHar = runingAddHar;
		if (addHar != null) {
			return addHar.localCoreSS;
		}
		return null;
	}

	public static AddHarHTMLMlet getCurrAddHarHTMLMlet() {
		return runingAddHar;// 最多只有一个运行实例
	}

	public static AddHarHTMLMlet getCurrAddHarHTMLMlet(final J2SESession coreSS) {
		int count = 0;
		do {
			final Mlet currMlet = ScreenServer.getCurrMlet(coreSS);
			if (currMlet != null && currMlet instanceof AddHarHTMLMlet) {
				return (AddHarHTMLMlet) currMlet;
			}
			try {
				Thread.sleep(200);
			} catch (final Exception e) {
			}
		} while (++count < 20);

		return null;
	}

	public final void startAddHarProcessInSysThread(final J2SESession coreSS, final String url,
			final boolean isInstallFromClient) {
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				startAddHar(coreSS, url, isInstallFromClient);
			}
		});
	}

	public void notifyBroadcastWifiAccout(final J2SESession coreSS, final String projID,
			final String device) {
		final String isBroadcast = ResourceUtil.get(coreSS, 9132);
		appendMessage(StringUtil.replace(StringUtil.replace(isBroadcast, "{projID}", projID),
				"{devName}", device));
	}

	static void acceptAllHARLicenseInUT(final boolean isAccept) {
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				PropertiesManager.setValue(PropertiesManager.p_isAcceptAllHARLicenses, isAccept);
				PropertiesManager.saveFile();
			}
		}, token);
	}

	/**
	 * 注意：本方法内部能拦截异常，并显示到手机
	 * 
	 * @param coreSS
	 * @param url
	 */
	private final void startAddHar(final J2SESession coreSS, final String url,
			final boolean isInstallFromClient) {
		LogManager.log("try ready to download HAR [" + url.toString() + "]...");

		// 关闭可能资源锁窗口，比如Designer或LinkProjectPanel
		if (LinkProjectStatus.isIdle() == false) {
			final String designMenuItem = ResourceUtil.get(coreSS, 9034);
			final String linkProjectMenuItem = ResourceUtil.get(coreSS, 9059);
			final String closeStr = ResourceUtil.get(coreSS, 9129);

			appendMessage(
					StringUtil.replace(StringUtil.replace(closeStr, "{designer}", designMenuItem),
							"{panel}", linkProjectMenuItem));
			setButtonEnable(exitButton, true);
			return;
		}

		if (LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_ADD_HAR_VIA_MOBILE)) {
			SafeDataManager.disableSafeBackup();

			try {
				final File fileHar = ResourceUtil.getTempFileName(Designer.HAR_EXT);
				final Properties had = new Properties();
				String strharurl = null;
				// 支持har和had下载
				final String lowerCaseURL = url.toLowerCase();
				if (lowerCaseURL.indexOf("http") != 0) {
					throw new Exception("it must be download file");
				}
				if (lowerCaseURL.endsWith(Designer.HAR_EXT)) {
					strharurl = url;
				} else if (lowerCaseURL.endsWith(Designer.HAD_EXT)) {
					LinkProjectManager.loadHAD(url, had);
					final String projID = had.getProperty(HCjad.HAD_ID, "");
					if (DelDeployedProjManager.isDeledDeployed(projID)) {
						throw new Exception(ResourceUtil.getErrProjIsDeledNeedRestart(coreSS));
					}
					strharurl = had.getProperty(HCjad.HAD_HAR_URL, HCjad.convertToExtHar(url));
				} else {
					throw new Exception("it must be har or had file.");
				}
				PropertiesManager.addDelFile(fileHar);

				final String hadmd5 = had.getProperty(HCjad.HAD_HAR_MD5, "");
				final boolean succ = HttpUtil.download(fileHar, new URL(strharurl), 1,
						ResourceUtil.getUserAgentForHAD());
				if (succ == false) {
					final String httpErr = ResourceUtil.get(coreSS, 9269);// connection
																			// error
																			// or
																			// timeout
					throw new Exception(httpErr);
				}

				if (ResourceUtil.checkSysPackageNameInJar(fileHar)) {
					throw new Exception(ResourceUtil.RESERVED_PACKAGE_NAME_IS_IN_HAR);
				}

				if ((hadmd5.length() > 0
						&& ResourceUtil.getMD5(fileHar).toLowerCase().equals(hadmd5.toLowerCase()))
						|| hadmd5.length() == 0) {
					final Map<String, Object> map = getMap(fileHar);
					if (map.isEmpty()) {
						throw new Exception(ResourceUtil.HAR_PROJECT_FILE_IS_CORRUPTED);
					}
					// final String licenseURL =
					// ((String)map.get(HCjar.PROJ_LICENSE)).trim();
					// if(licenseURL.length() > 0){
					// }

					final String proj_id = (String) map.get(HCjar.PROJ_ID);
					if (DelDeployedProjManager.isDeledDeployed(proj_id)) {
						throw new Exception(ResourceUtil.getErrProjIsDeledNeedRestart(coreSS));
					}

					if (SignHelper.verifyJar(fileHar,
							LinkProjectManager.getCertificatesByID(proj_id)) == null) {// 完整性检查进行前置
						throw new Exception(ResourceUtil.FILE_IS_MODIFIED_AFTER_SIGNED);
					}

					final boolean isUpgrade = (LinkProjectManager.getProjByID(proj_id) != null);
					final File oldBackEditFile = null;
					if (isUpgrade) {
						// is added, can NOT be added again.
						throw new Exception(StringUtil.replace(ResourceUtil.get(coreSS, 9192),
								"{projID}", proj_id));// 是否覆盖为resID=9157
					}

					final String licenseURL = ((String) map.get(HCjar.PROJ_LICENSE)).trim();

					final Boolean[] isDone = new Boolean[1];
					isDone[0] = false;
					final Boolean[] isOvertime = new Boolean[1];
					isOvertime[0] = false;
					final Exception[] runException = new Exception[1];

					final ThreadGroup token = App.getThreadPoolToken();
					final IWatcher yesInUserThread = new IWatcher() {
						HTMLMlet mlet;// 可能为licenseHTMMlet或addHar

						@Override
						public boolean watch() {
							ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
								@Override
								public Object run() throws Throwable {
									synchronized (isOvertime) {
										if (isOvertime[0]) {
											return null;
										}
										try {
											startAddAfterAgreeInQuestionThread(mlet, coreSS,
													fileHar, map, isUpgrade, oldBackEditFile,
													isInstallFromClient);
										} catch (final Exception e) {
											runException[0] = e;
										}

										isDone[0] = true;
									}
									return null;
								}
							}, token);// 转主线程

							if (mlet instanceof LicenseHTMLMlet) {
								((LicenseHTMLMlet) mlet).back();
							}

							return true;
						}

						@Override
						public void setPara(final Object p) {
							mlet = (HTMLMlet) p;
						}

						@Override
						public void cancel() {
						}

						@Override
						public boolean isCancelable() {
							return false;
						}
					};
					yesInUserThread.setPara(this);

					if (ResourceUtil.needAccepLicense(licenseURL)) {
						final String opIsCanceled = ResourceUtil.get(coreSS, 9193);// 需要系统特权。不能入Runnable
						final IWatcher noInUserThread = new IWatcher() {
							LicenseHTMLMlet mlet;

							@Override
							public boolean watch() {
								L.V = L.WShop ? false
										: LogManager.log("[AddHarHTMLMlet] cancel license.");
								mlet.back();

								appendMessage(opIsCanceled);
								isDone[0] = true;
								return true;
							}

							@Override
							public void setPara(final Object p) {
								mlet = (LicenseHTMLMlet) p;
							}

							@Override
							public void cancel() {
							}

							@Override
							public boolean isCancelable() {
								return false;
							}
						};

						try {
							licenseText = ResourceUtil.getStringFromURL(licenseURL, true);

							if (licenseText != null && licenseText.length() == 0) {
								licenseText = licenseURL;
							}
						} catch (final Throwable e) {
							licenseText = licenseURL;
						}

						final LicenseHTMLMlet licenseHtmlMlet = (LicenseHTMLMlet) ServerUIAPIAgent
								.runAndWaitInSessionThreadPool(coreSS,
										ServerUIAPIAgent.getProjResponserMaybeNull(ctx),
										new ReturnableRunnable() {
											@Override
											public Object run() throws Throwable {
												return new LicenseHTMLMlet(licenseText,
														acceptAllAndNeverDisplay, yesInUserThread,
														noInUserThread, okImage, cancelImage,
														iagreeStr, acceptStr, cancelStr);
											}
										});

						goMlet(licenseHtmlMlet,
								CCoreUtil.SYS_PREFIX + LicenseHTMLMlet.class.getSimpleName(),
								false);
					} else {
						// 没有license的情形
						yesInUserThread.watch();
					}

					// final String strLicense =
					// (String)ResourceUtil.get(9194);//License
					// getProjectContext().sendQuestion(strLicense, iAgree,
					// null, yesRunnable, noRunnable, null);

					final long startMS = System.currentTimeMillis();
					while (isDone[0] == false && getStatus() != Mlet.STATUS_EXIT) {
						if (System.currentTimeMillis() - startMS > 1000 * 60 * 30) {// 30分钟
							synchronized (isOvertime) {
								isOvertime[0] = true;
							}
							break;
						}

						try {
							Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
						} catch (final Exception e) {
						}
					}

					if (runException[0] != null) {
						throw runException[0];
					}
				} else {
					throw new Exception(ResourceUtil.get(coreSS, 9270));// file
																		// verification
																		// error,
																		// try
																		// again
																		// later
				}
			} catch (final Throwable e) {
				// ExceptionReporter.printStackTrace(e);可能网络不正常，所以无需
				appendMessage(ResourceUtil.getErrorI18N(coreSS) + " : " + e.getMessage());
			} finally {
				SafeDataManager.enableSafeBackup(true, true);
				LinkProjectStatus.exitStatus();
				setButtonEnable(exitButton, true);
			}
		}
	}

	private final boolean enterMobileBindInSysThread(final BindRobotSource source,
			final J2SESession coreSS, final HTMLMlet mletFrom) {
		final Boolean[] waitLock = { false };

		enterBindInSysThread(waitLock, mletFrom, source, coreSS);

		synchronized (waitLock) {
			L.V = L.WShop ? false : LogManager.log("wait user bind in mobile...");
			try {
				waitLock.wait();
			} catch (final InterruptedException e) {
			}
			L.V = L.WShop ? false : LogManager.log("done user bind in mobile!");
		}

		return waitLock[0];
	}

	public final void enterBindInSysThread(final Boolean[] waitLock, final HTMLMlet mletFrom,
			final BindRobotSource source, final J2SESession coreSS) {
		final String robotsDesc = ResourceUtil.get(coreSS, 8024);
		final String convDesc = ResourceUtil.get(coreSS, 8004);
		final String devDesc = ResourceUtil.get(coreSS, 8007);

		final String cancelDesc = ResourceUtil.get(coreSS, 1018);
		final String okDesc = ResourceUtil.getOKI18N(coreSS);
		final String emptyDesc = ResourceUtil.get(coreSS, 9242);

		final String nextOne = ResourceUtil.get(coreSS, 8025);

		final boolean isReleaseCurr = mletFrom instanceof LicenseHTMLMlet;

		ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS,
				ServerUIAPIAgent.getProjResponserMaybeNull(ctx), new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final BindHTMLMlet bindMlet = new BindHTMLMlet(source, token, nextOne,
								okImage, cancelImage, okDesc, cancelDesc, true, robotsDesc,
								convDesc, devDesc, waitLock, emptyDesc);
						mletFrom.goMlet(bindMlet,
								CCoreUtil.SYS_PREFIX + BindHTMLMlet.class.getSimpleName(),
								isReleaseCurr);
						return bindMlet;
					}
				});
	}

	/**
	 * 本方法内异常会被拦截，并转显到手机
	 * 
	 * @param coreSS
	 * @return true isNeedCloseLicenseMlet
	 */
	private final void startAddAfterAgreeInQuestionThread(final HTMLMlet mlet,
			final J2SESession coreSS, final File fileHar, final Map<String, Object> map,
			final boolean isUpgrade, final File oldBackEditFile, final boolean isInstallFromClient)
			throws Exception {
		final ArrayList<LinkProjectStore> appendLPS = appendMapToSavedLPS(coreSS, fileHar, map,
				false, isUpgrade, oldBackEditFile);
		LinkProjectManager.reloadLinkProjects();// 十分重要

		final MobiUIResponsor mobiResp = (MobiUIResponsor) ServerUIUtil.getResponsor();

		final ProjResponser[] appendRespNotAll = mobiResp.appendNewHarProject(isInstallFromClient);// 仅扩展新工程，不启动或不运行

		{
			for (int i = 0; i < appendRespNotAll.length; i++) {
				final ProjResponser pr = appendRespNotAll[i];

				pr.initSessionContext(coreSS);// 供device input token
			}
		}

		showMsgForAddHar(IConstant.INFO, ResourceUtil.get(coreSS, 9092));

		BindRobotSource bindSource = mobiResp.bindRobotSource;
		if (bindSource == null) {
			bindSource = new BindRobotSource(mobiResp);
		}

		if (BindManager.findNewUnbind(bindSource)) {// 进行自动绑定
			// 提前释放可能的异常
			bindSource.getRealDeviceInAllProject();
			bindSource.getConverterInAllProject();

			if (enterMobileBindInSysThread(bindSource, coreSS, mlet) == false) {
				final String errMsg = "NOT binded for robots for project ["
						+ (String) map.get(HCjar.PROJ_ID) + "].";
				throw new Exception(errMsg);
			}
		}

		mobiResp.fireSystemEventListenerOnAppendProject(appendRespNotAll, appendLPS);// 进入运行状态

		final ProjResponser resp = mobiResp.getCurrentProjResponser(coreSS);
		final JarMainMenu linkMenu = resp.jarMainMenu;
		linkMenu.appendProjToMenuItemArray(mobiResp.maps, mobiResp.responserSize, appendLPS);
		// final String menuData = linkMenu.buildJcip(coreSS);
		//
		// {
		// final String projID = resp.context.getProjectID();
		// final byte[] projIDbs = StringUtil.getBytes(projID);
		// final String softUID =
		// UserThreadResourceUtil.getMobileSoftUID(coreSS);
		// final byte[] softUidBS = ByteUtil.getBytes(softUID, IConstant.UTF_8);
		// final String urlID = CacheManager.ELE_URL_ID_MENU;
		// final byte[] urlIDbs = CacheManager.ELE_URL_ID_MENU_BS;
		//
		// final CacheComparator menuRefreshComp = new CacheComparator(projID,
		// softUID, urlID, projIDbs, softUidBS, urlIDbs) {
		// @Override
		// public void sendData(final Object[] paras) {
		// coreSS.context.send(MsgBuilder.E_MENU_REFRESH, menuData);
		//// HCURLUtil.sendEClass(HCURLUtil.CLASS_BODY_TO_MOBI, bodyBS, 0,
		// bodyBS.length);
		// }
		// };
		// final byte[] data = StringUtil.getBytes(menuData);
		//
		// //注意：不能走cache，因为历史安装过，可能存在相同cache，导致生成一个新Menu
		// menuRefreshComp.encodeGetCompare(coreSS, false, data, 0, data.length,
		// null);
		// }

		LogManager.log("successful apply added project to ACTIVE status.");
		appendMessage(ResourceUtil.get(coreSS, 9128));
	}

	/**
	 * 添加一个har到lps并存储。 注意：本过程被初始加载及发布MyFirst.har使用
	 * 
	 * @param fileHar
	 * @param map
	 * @return
	 */
	static final ArrayList<LinkProjectStore> appendMapToSavedLPS(final J2SESession coreSS,
			final File fileHar, final Map<String, Object> map, final boolean isRoot,
			final boolean isUpgrade, final File oldBackEditFile) throws Exception {// 本异常会被拦截，并转显到手机
		final LinkEditData led = buildAddHarDesc(fileHar, map, "", "");
		AddHarHTMLMlet.addHarToDeployArea(coreSS, led, led.lps, isRoot, isUpgrade, oldBackEditFile);

		final Iterator<LinkProjectStore> lpsIT = LinkProjectManager
				.getLinkProjsIteratorInUserSysThread(true);
		final Vector<LinkProjectStore> lpsVectorNewStore = new Vector<LinkProjectStore>();
		while (lpsIT.hasNext()) {
			lpsVectorNewStore.add(lpsIT.next());
		}

		// 添加新工程到存储集中
		final ArrayList<LinkProjectStore> appendLPS = new ArrayList<LinkProjectStore>();
		appendLPS.add(led.lps);// 可能有多个工程
		LogManager.log("successful add HAR project [" + led.lps.getProjectID() + "]");

		{
			final int size = appendLPS.size();
			for (int i = 0; i < size; i++) {
				lpsVectorNewStore.add(appendLPS.get(i));
			}
		}

		LinkProjectStore[] lpss = {};
		lpss = lpsVectorNewStore.toArray(lpss);
		LinkProjectManager.saveLinkStore(lpss, LinkProjectManager.newLinkProjSetInstance());// 更新到LPS
		return appendLPS;
	}

	/**
	 * 
	 * @param bar
	 * @param totalDevice
	 * @param descLabel
	 * @param isCancel
	 *            true:use click 'Cancel' to connect all devices
	 * @return
	 */
	public JProgressBar showProgressBar(final JProgressBar bar, final int totalDevice,
			final int finishedDevice, final JLabel descLabel, final boolean[] isCancel) {
		final JPanel progressPanel = new JPanel(new GridLayout(3, 1));
		progressPanel.add(bar);
		progressPanel.add(descLabel);
		final JButton cancel = new JButton(ResourceUtil.get(IContext.CANCEL));
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

	public void waitForInputWiFiPassword(final ThreadGroup token) {

		final JPanel inputPanel = new JPanel(new GridLayout(11, 1));

		inputPanel.add(
				new JLabel(WiFiHelper.WIFI_SSID, WiFiHelper.getWiFiIcon(), SwingConstants.LEFT));
		final JTextField ssidField = new JTextField("");
		inputPanel.add(ssidField);

		inputPanel.add(new JLabel(WiFiHelper.getPasswordDesc(), WiFiHelper.getWiFiIcon(),
				SwingConstants.LEFT));
		final JPasswordField pass1 = new JPasswordField("");
		pass1.setEchoChar('*');
		inputPanel.add(pass1);

		final JPasswordField pass2 = new JPasswordField("");
		pass2.setEchoChar('*');
		inputPanel.add(pass2);

		inputPanel.add(new JLabel(WiFiHelper.WIFI_SECURITY_OPTION, SwingConstants.LEFT));
		final ButtonGroup bgSecurityOption = new ButtonGroup();
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

		final JButton submit = new JButton(ResourceUtil.get(localCoreSS, IContext.OK));
		inputPanel.add(submit);

		submit.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final AddHarHTMLMlet addHarHTMLMlet = AddHarHTMLMlet.this;
				if (ssidField.getText().length() == 0) {
					statusReport.setText(ResourceUtil.get(localCoreSS, 9126));
					ssidField.requestFocusInWindow();
					addHarHTMLMlet.setCSS(statusReport, null, css_error);// 系统Mlet,
																			// //不考虑in
																			// user
																			// thread
					return;
				}

				if (pass1.getText().equals(pass2.getText()) == false) {
					statusReport.setText(ResourceUtil.get(localCoreSS, 9127));
					addHarHTMLMlet.setCSS(statusReport, null, css_error);// 系统Mlet,
																			// //不考虑in
																			// user
																			// thread
					return;
				}

				WiFiHelper.updateWiFiAccount(ssidField.getText(), pass1, rb_nopass, rb_wep, rb_wpa);

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						addHarHTMLMlet.removeAll();

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
			} catch (final InterruptedException e) {
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
		ClassUtil.revalidate(addHarHTMLMlet);
	}

	final static Object startAddHarLock = new Object();
	static AddHarHTMLMlet runingAddHar;

	public static void startAddHTMLHarUI(final J2SESession coreSS, final String urlStr,
			final boolean isInstallFromClient) {
		initToken();// 暂时申请特权

		synchronized (startAddHarLock) {
			if (runingAddHar == null) {
				runingAddHar = buildAndDispMlet(coreSS, AddHarHTMLMlet.class);
				if (runingAddHar != null) {
					runingAddHar.startAddHarProcessInSysThread(coreSS, urlStr, isInstallFromClient);
				}
			} else {
				buildAndDispMlet(coreSS, AddHarIsBusy.class);
				coreSS.sessionEventManager.addListener(SessionEventManager.EVENT_ADD_HAR_BUSY,
						new Runnable() {
							@Override
							public void run() {
								final String notifyNotBusy = ResourceUtil.get(coreSS, 9235);
								final J2SESession[] coreSSS = { coreSS };
								ServerUIAPIAgent.sendMessageViaCoreSSInUserOrSys(coreSSS,
										ResourceUtil.getInfoI18N(), notifyNotBusy,
										ProjectContext.MESSAGE_INFO, null, 0);
							}
						});
			}
		}

		// clearToken();//注意：由于token被后续逻辑使用，故不clear
	}

	private static AddHarHTMLMlet buildAndDispMlet(final J2SESession coreSS, final Class claz) {
		final String scripts = "" + "#encoding:utf-8\n" + "import Java::" + claz.getName() + "\n"
				+ "return " + claz.getSimpleName() + ".new()\n";

		try {
			final ProjResponser pr = ServerUIAPIAgent.getCurrentProjResponser(coreSS);

			final boolean isSynchronized = true;
			final ProjectContext context = pr.context;
			final String elementID = CCoreUtil.SYS_PREFIX + claz.getSimpleName();
			final String targetURL = HCURL.buildStandardURL(HCURL.FORM_PROTOCAL, elementID);
			final AddHarHTMLMlet addHar = (AddHarHTMLMlet) ProjResponser.startMlet(coreSS,
					new StringValue(scripts), null, targetURL, elementID, "add HAR", pr.hcje,
					context, isSynchronized);

			return addHar;
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * 将一个工程全部绑定，更新到LPS中。但不保存
	 * 
	 * @param projID
	 * @param bdns
	 *            每一个Robot可能含有多个BindDeviceNode，所以为数组。此为工程下全部Robots的，故为数组的Vector
	 */
	static void updateProjectBindsToLPS(final String projID, final Vector<BindDeviceNode[]> bdns) {
		final int size = bdns.size();
		if (size > 0) {
			final Vector<String> dev_bind_ids = new Vector<String>();
			final Vector<String> conv_bind_ids = new Vector<String>();
			final Vector<BindDeviceNode> dev_binds = new Vector<BindDeviceNode>();

			for (int i = 0; i < size; i++) {
				toArrayReferDevice(projID, bdns.get(i), dev_bind_ids, conv_bind_ids, dev_binds);
			}

			// 更新到相应project
			updateOneProjBindToLPS(projID, dev_bind_ids, conv_bind_ids, dev_binds);
		}
	}

	private static void toArrayReferDevice(final String projID, final BindDeviceNode[] bdns,
			final Vector<String> dev_bind_ids, final Vector<String> cov_bind_ids,
			final Vector<BindDeviceNode> dev_binds) {
		final int size = bdns.length;
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				final BindDeviceNode referBDN = bdns[i];
				final String robotName = referBDN.lever2Name;

				final String bindID = DeviceBindInfo.buildStandardBindID(projID, robotName,
						referBDN.ref_dev_ID);

				if (referBDN.isBindedRealDevice()) {
					dev_bind_ids.add(bindID);
					dev_binds.add(referBDN);
				}

				if (referBDN.isBindedConverter()) {
					cov_bind_ids.add(bindID);
				}
			}
		}
	}

	static void updateOneProjBindToLPS(final String projID, final Vector<String> dev_bind_ids,
			final Vector<String> conv_bind_ids, final Vector<BindDeviceNode> dev_binds) {
		// CCoreUtil.checkAccess();//friend方式，关闭checkAccess

		final LinkProjectStore lps = LinkProjectManager.getProjByID(projID);
		if (lps != null) {
			lps.clearBindMap();

			{
				final int dev_bind_size = dev_bind_ids.size();
				if (dev_bind_size > 0) {
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
				if (conv_bind_size > 0) {
					final String[] ids = new String[conv_bind_size];
					final ConverterInfo[] binds = new ConverterInfo[conv_bind_size];

					final int size = dev_bind_ids.size();
					int index = 0;
					for (int i = 0; i < size; i++) {
						final ConverterInfo convBind = dev_binds.elementAt(i).convBind;
						if (convBind != null) {
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

	public static Map<String, Object> getMap(final File file) {
		return HCjar.loadHar(file, false);
	}

	/**
	 * 
	 * @param file
	 * @param map
	 * @param linkName
	 *            如果不是覆盖旧的，则为空串
	 * @param linkRemark
	 *            如果不是覆盖旧的，则为空串
	 * @return
	 */
	static LinkEditData buildAddHarDesc(final File file, final Map<String, Object> map,
			final String linkName, final String linkRemark) {
		final LinkEditData led = new LinkEditData();
		final LinkProjectStore lps = new LinkProjectStore();
		led.lps = lps;
		led.filePath = (file.getAbsolutePath());
		led.op = (LinkProjectManager.STATUS_NEW);
		led.status = (LinkProjectManager.STATUS_NEW);

		// lps.setCertificates(SignHelper.verifyJar(file,
		// null));//注意：LinkProjectManager.importLinkProject会执行此动作

		final boolean hasMenuItem = LinkProjectManager.hasMenuItemNumForMap(map);
		lps.put(LinkProjectStore.FIELD_HAS_MENU_ITEM_FOR_INSTALL, IConstant.toString(hasMenuItem));
		
		lps.setActive(true);
		lps.setLinkName(linkName);
		lps.setProjectRemark(linkRemark);
		lps.setProjectID((String) map.get(HCjar.PROJ_ID));
		lps.copyFrom(map, false);
		return led;
	}

	static boolean addHarToDeployArea(final J2SESession coreSS, final LinkEditData led,
			final LinkProjectStore lps, final boolean isRoot, final boolean isUpgrade,
			final File oldBackFile) {
		final File source = new File(led.filePath);// 不能加App.getBaseDir(),
													// 它是绝对路径。适合J2SE, Android
		final boolean result = LinkProjectManager.importLinkProject(lps, source, isUpgrade,
				oldBackFile, false);

		final int requMin = 200;
		if (PlatformManager.getService().getAvailableSize() < requMin * 1024 * 1024) {// <200M
			final String warnMsg = StringUtil.replace(ResourceUtil.get(coreSS, 9187), "{min}",
					String.valueOf(requMin));
			final int msgType = IConstant.WARN;
			showMsgForAddHar(msgType, warnMsg);
		}

		led.status = LinkProjectManager.STATUS_DEPLOYED;
		if (isRoot) {
			lps.setRoot(true);
		}
		return result;
	}

	public static void showMsgForAddHar(final int msgType, final String msg) {
		final AddHarHTMLMlet currMlet = AddHarHTMLMlet.getCurrAddHarHTMLMlet();
		if (currMlet != null) {
			currMlet.appendMessage(msg);
		}
		final J2SESession coreSS = AddHarHTMLMlet.getCurrAddHarHTMLMletCoreSession();
		TrayMenuUtil.displayMessage(ResourceUtil.get(coreSS, msgType), msg, msgType, null, 0);
	}

}
