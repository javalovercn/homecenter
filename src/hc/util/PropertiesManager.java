package hc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import hc.App;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.PlatformManager;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCEventQueue;
import hc.server.util.HCLimitSecurityManager;

public class PropertiesManager {
	public static final String ILLEGAL_CLASS = "Illegal class attempts to access critical data or security codes.";
	public static long PropertiesLockThreadID = 0;// 置于最前
	private static String fileName = IConstant.propertiesFileName;
	private static File propFile;
	private static File propFileWriting;

	public final static String getPropertiesFileName() {
		return fileName;
	}

	public static final String p_stunServerPorts = "StunServerPorts";
	public static final String p_IsSimu = "isSimu";
	public static final String p_IsDevLogOn = "isDevLoggerOn";//deprecated
	public static final String p_IsForceNoLogger = "isForceNoLogger";
	public static final String p_IsLoggerOn = "isLoggerOn";
	public static final String p_IsMobiMenu = "mMenu";
	public static final String p_uuid = "uuid";
	public static final String p_password = "password";
	public static final String p_enableCache = "enableCache";
	public static final String p_ServerSide = "serverSide";
	/**
	 * 开机检查是否需要输入密码之用，deprecated
	 */
	public static final String p_AutoStart = "AutoStart";
	public static final String p_TCPPortFrom = "PortFrom";
	public static final String p_NetworkInterfaces = "NetworkInterfaces";
	public static final String p_UsingNetworkInterfaces = "UsingNetworkInterfaces";
	public static final String p_Token = "Token";
	public static final String p_isDonateOrVIPNowOrEver = "isDonateOrVIPNowOrEver";// 现在或曾经是。查看实时状态：TokenManager.isDonateToken()
	public static final String p_TokenRelay = "TokenRelay";
	public static final String p_TokenBack = "TokenBack";
	// 外网直联检测及开通
	public static final String p_DisableDirect = "DisableDirect";
	// 家庭内网直联检测及开通
	public static final String p_DisableHomeWireless = "DisableHomeWireless";
	// 仅使用中继，但不排斥家庭内网
	public static final String p_ForceRelay = IConstant.ForceRelay;
	public static final String p_UPnPUseSTUNIP = "UPnPUseSTUNIP";
	public static final String p_Log = "Log";//deprecated
	public static final String p_DirectUPnPExtPort = "DirectUPnPExtPort";
	public static final String p_RelayServerUPnPExtPort = "RelayUPnPExtPort";
	public static final String PWD_ERR_TRY = "pwdErrTry";
	public static final String PWD_ERR_LOCK_MINUTES = "pwdErrLockMinutes";
	public static final String p_LogPassword1 = "log1";
	public static final String p_LogPassword2 = "log2";
	public static final String p_LogCipherAlgorithm1 = "logCipherAlgorithm1";
	public static final String p_LogCipherAlgorithm2 = "logCipherAlgorithm2";
	public static final String p_IAgree = "iAgree";// 停止使用，由p_AgreeVersion代替
	public static final String p_AgreeVersion = "AgreeVersion";
	public static final String p_RelayServerLocalIP = "ServerIP";
	public static final String p_RelayServerLocalPort = "ServerPort";
	public static final String p_RelayBlockNum = "RelayBlockNum";
	public static final String p_HideIDForErrCert = "HideIDForErrCert";

	// 本地Relay服务器IP从p_RelayServerLocalIP中取
	@Deprecated
	public static final String p_ServerLocalIP = "ServerLocalIP";

	public static final String p_SetupVersion = "SetupVersion";
	public static final String p_ResourceID = "ResourceID";
	public static final String p_ResourcesMaybeUnusedNew = "ResourceMaybeUnusedNew";
	public static final String p_ResourcesMaybeUnusedOld = "ResourceMaybeUnusedOld";
	public static final String p_ForceEn = "ForceEn";
	public static final String p_isNotAutoUpgrade = "NotAutoUpgrade";
	public static final String p_isMultiUserMode = "MultiUserMode";
	public static final String p_LasterAutoUpgradeVer = "LasterUpgradeVer";

	public static final String p_isAcceptAllHARLicenses = "isAcceptAllHARLicenses";

	public static final String p_CertKey = "CertKey";
	public static final String p_EnableTransNewCertKeyNow = "TransCertKey";
	public static final String p_NewCertIsNotTransed = "NewCertIsNotTransed";

	public static final String p_Deploy_RecentPassword = "DeployRecentPassword";
	public static final String p_Deploy_EnableReceive = "DeployEnableReceive";
	public static final String p_Deploy_RecentIP = "DeployRecentIP";

	public static final String p_isEnableHCAI = "isEnableHCAI";
	
	public static final String p_ApplicationServerID = "ApplicationServerID";

	public static final String p_SearchDialogColumnWidths = "SearchDialogColumnWidths";

	public static final String p_jrubyJarFile = "JRubyJarFile";// 停用并清空
	public static final String p_jrubyJarVer = "JRubyJarVer";
	@Deprecated
	public static final String p_SampleDeployNotify = "SampleNotify";
	public static final String p_DelDirNum = "DelDirNum";
	public static final String p_DelDirPre = "DelDirPre.";

	public static final String p_OpNewLinkedInProjVer = "OpNewLinkedInProjVer";
	public static final String p_OpAcceptNewPermissions = "OpAcceptNewPermissions";
	public static final String p_EnableLinkedInProjUpgrade = "EnableLinkedInProjUpgrade";
	/**
	 * 旧系统支持单一工程发布时，记录资源拆分目录的存储位。新系统支持多工程发布，统一存储
	 * 
	 * @Deprecated
	 */
	public static final String p_DeployTmpDir = "DeployTmpDir";

	public static final String p_isEnableAndroidLogCat = "isEnableAndroidLogCat";
	public static final String p_isEnableMSBLog = "isEnableMSBLog";
	public static final String p_isEnableMSBExceptionDialog = "isEnableMSBExceptionDialog";
	public static final String p_isEnableLimitHAR = "isEnableLimitHAR";

	public static final String p_wordCompletionModifierCode = "wordCompModCode";
	public static final String p_wordCompletionKeyCode = "wordCompKeyCode";
	public static final String p_wordCompletionKeyChar = "wordCompKeyChar";

	public static final String p_DesignerDividerLocation = "DesignerDividerLocation";
	public static final String p_DesignerCtrlDividerLocation = "DesignerCtrlDividerLocation";
	public static final String p_DesignerCtrlHOrV = "DesignerCtrlHOrV";

	public static final String p_ReadedMsgID = "ReadedMsgID";

	public static final String p_ServerSecurityKeyMD5 = "ServerSecurityKeyMD5";
	public static final String p_isNeedResetPwd = "isNeedResetPwd";
	public static final String p_SecurityCipher = "SecurityCipher";
	public static final String p_SecuritySDPVersion = "SecuritySDPVersion";
	public static final String p_SecurityLogVersion = "SecurityLogVersion";
	public static final String p_SecurityLogDESSecretKeySize = "SecurityLogDESSecretKeySize";
	public static final String p_SecuritySecretKeySize = "SecuritySecretKeySize";
	public static final String p_SecurityCheckAES = "SecurityCheckAES";
	public static final String S_SecurityProperties = "SecurityProperties";// 记录加密的属性

	public static final String p_isLowMemWarnInDesigner = "isLowMemWarnInDesigner";

	/**
	 * 初次安装的版本号，与后续升级无关
	 */
	public static final String p_InitVersion = "InitVersion";

	public static final String p_WindowX = "_wx";
	public static final String p_WindowY = "_wy";
	public static final String p_WindowWidth = "_ww";
	public static final String p_WindowHeight = "_wh";

	public static final String p_isNonUIServer = "isNonUIServer";// for Non-UI server, for example No-X11 CentOS or Android
	public static final String p_NonUIServerIP = "NonUIServerIP";

	public static final String p_isDemoServer = "isDemoServer";
	public static final String p_isDemoMaintenance = "isDemoMaintenance";// 演示不能同时供iPhone上架和普通演示

	public static final String p_TrayX = "_Tray_x";
	public static final String p_TrayY = "_Tray_y";

	public static final String p_ProjModiTabIdx = "_w_ModiTabIdx";

	public static final String p_LastSampleVer = "LastSampleVer";

	public static final String p_CapPreview = "CapPreview";
	public static final String p_CapSnapWidth = "CapSnapWidth";
	public static final String p_CapSnapHeight = "CapSnapHeight";
	public static final String p_CapDelDays = "CapDelDays";
	public static final String p_CapSnapMS = "CapSnapMS";
	public static final String p_CapNotSnap = "CapNotSnap";

	public static final String p_FileChooserDir = "FileChooserDir";

	public static final String p_IsReadedCAPCrash = "isReadedCrash";
	public static final String p_selectedNetwork = "selectedNetwork";
	public static final String p_selectedNetworkPort = "selectedNetworkPort";

	public static final String p_screenDeviceScale = "screenDeviceScale";
	
	public static final String p_J2SECACertsPassword = "J2SECACertsPassword";
	public static final String p_isAcceptCertsOnlyFromJ2SECACerts = "isAcceptCertsOnlyFromJ2SECACerts";

	/**
	 * for Android auto start or not
	 */
	public static final String p_autoStart = "autoStart";

	public static final String p_J2SEDocVersion = "J2SEDocVersion";

	public static final String p_intervalSecondsNextStartup = "intervalSecondsNextStartup";
	public static final String p_preloadAfterStartup = "preloadAfterStartup";

	public static final String p_WiFi_currSSID = "wifi_currSSID";
	public static final String p_WiFi_currPassword = "wifi_currpwd";
	public static final String p_WiFi_currSecurityOption = "wifi_currSO";
	public static final String p_WiFi_currIsAutoCreated = "wifi_currIsAutoCreated";
	public static final String p_WiFi_hasWiFiModule = "wifi_hasModu";
	public static final String p_WiFi_canCreateAP = "wifi_canCreateAP";

	public static final String p_DebugStackMS = "debugStackMS";

	// 用于设置WiFi密码，表示曾经走WiFi的手机连接过
	public static final String p_WiFi_isMobileViaWiFi = "wifi_isMobiViaWiFi";

	public static final String p_DesignerDocFontSize = "DesignerDocFontSize";

	public static final String p_isReportException = "isReportException";

	public static final String p_compackingAIDB = "compackingAIDB";
	public static final String p_compackAIDBLastMS = "compackAIDBLastMS";

	public static final String p_RMSServerUID = "RMSServerUID";
	public static final String p_clearRMSCacheVersion = "clearRMSCacheVersion";

	public static final String p_IsVerifiedEmail = "isVerifiedEmail";
	public static final String p_DevCertPassword = "DevCertPassword";
	public static final String p_isRememberDevCertPassword = "isRememberDevCertPassword";

	public static final String p_isShowJRubyTestConsole = "isShowJRubyTestConsole";
	public static final String p_JRubyTestConsoleDividerLocation = "JRubyTestConsoleDividerLocation";

	public static final String p_lastRootCfg = "LastRootCfg";

	public static final String p_SafeDataBackupIntervalMinutes = "SafeDataBackupIntervalMinutes";
	public static final String p_LogMaxDays = "LogMaxDays";

	public static final String p_isEnableClientAddHAR = "isEnableClientAddHAR";

	public static final String t_testClientLocale = "testClientLocale";

	public static final String S_DELED_DEPLOYED_PROJS = "deledDeployedProjs";// for
																				// remove
																				// only,
																				// not
																				// for
																				// delete
																				// and
																				// upgrade
	public static final String S_THIRD_DIR = "3libs";
	public static final String S_USER_LOOKANDFEEL = "lookfeel";
	public static final String S_LINK_PROJECTS = "linkProjs";// 注意：不能有与它相同的前缀，比如linkProjsHeight的属性
	public static final String S_LINK_PROJECTS_COLUMNS_WIDTH = "linkProjWidth";

	public static final String p_LINK_CURR_EDIT_PROJ_ID = "currEditProjID";
	public static final String p_LINK_CURR_EDIT_PROJ_VER = "currEditProjVer";

	public static final String C_SYSTEM_DEFAULT_FONT_SIZE = "c_SysDftFontSize";
	public static final String C_FONT_NAME = "c_FontName";
	public static final String C_FONT_SIZE = "c_FontSize";
	public static final String C_SKIN = "c_skin";

	public static final String PRE_USER_PROJ = "+";

	public static final String p_PROJ_RECORD = PRE_USER_PROJ + "record_";

	public static final String p_PROJ_CACHE_LAST_ACCESS_TIME = "projCacheLastAccessTime";

	public static final String CAP_PREFIX = "Cap_";

	private static final int getDelDirNum() {
		final String v = getValue(p_DelDirNum);
		if (v == null) {
			return 0;
		} else {
			return Integer.parseInt(v);
		}
	}

	public static void setSimuToTrue() {
		CCoreUtil.checkAccess();
		isSimuCache = null;
		setValue(p_IsSimu, IConstant.TRUE);
	}

	private static Boolean isSimuCache;

	public static boolean isSimu() {
		if (isSimuCache == null) {
			isSimuCache = isTrue(p_IsSimu);
		}
		return isSimuCache;
	}

	private static int delDirNum = 0;

	public static final void addDelDir(final File file) {
		addDelFile(file);
	}

	public static final void addDelFile(final File file) {
		addDelDir(file.getPath());
	}

	/**
	 * 增加一个待删除的目录或文件，不含自动存储指令
	 * 
	 * @param dirName
	 */
	public static final void addDelDir(final String dirName) {
		CCoreUtil.checkAccess();

		setValue(p_DelDirPre + delDirNum, dirName);
		++delDirNum;
		setValue(p_DelDirNum, String.valueOf(delDirNum));

		saveFile();
	}

	public static final void emptyDelDir() {
		delDirNum = getDelDirNum();
		for (int i = 0; i < delDirNum; i++) {
			final String key = p_DelDirPre + i;
			final String delFileName = getValue(key);
			if (delFileName != null) {
				// System.out.println("del path in emptyDelDir : " +
				// delFileName);

				// 删除相对型
				{
					final File delFile = new File(ResourceUtil.getBaseDir(), delFileName);
					if (L.isInWorkshop) {
						LogManager.log("delete file/dir : " + delFile.getAbsolutePath());
					}
					ResourceUtil.deleteDirectoryNow(delFile, true);
				}

				// 删除绝对路径型，如privateFiel/user_data
				{
					final File delFile = new File(delFileName);
					if (L.isInWorkshop) {
						LogManager.log("delete file/dir : " + delFile.getAbsolutePath());
					}
					ResourceUtil.deleteDirectoryNow(delFile, true);
				}

				remove(key);
			}
		}

		delDirNum = 0;
		remove(p_DelDirNum);
		saveFile();
	}

	static boolean statusChanged = false;
	final static Properties propertie = new Properties();
	private static final Object writeNotify = new Object();
	private static Thread eventDispatchThread;
	private static HCEventQueue hcEventQueue;
	// private static boolean isShutdownHook = false;

	// public static void notifyShutdownHook(){
	// CCoreUtil.checkAccess();
	// isShutdownHook = true;
	// saveFile();
	// }

	private static void buildAndStart() {
		final Thread t = new Thread() {

			@Override
			public void run() {
				synchronized (writeNotify) {
					while (true) {
						try {
							writeNotify.wait();
						} catch (final Exception e) {
						}

						// if(isShutdownHook){
						// break;
						// }

						if (statusChanged == false) {
							continue;
						}

						try {
							final FileOutputStream outputFile = new FileOutputStream(propFile);// propFileWriting
							propertie.store(outputFile, null);
							outputFile.close();
							statusChanged = false;
							// propFile.delete();
							// propFileWriting.renameTo(propFile);
						} catch (final Exception e) {
							ExceptionReporter.printStackTrace(e);
							App.showMessageDialog(null, "write data to properties file error!", "Error", JOptionPane.ERROR_MESSAGE);
							// System.exit(0);
						}
					}
				}
			}
		};
		t.setDaemon(true);
		t.setPriority(ThreadPriorityManager.FILE_SAVE_PRIORITY);
		t.start();

		PropertiesLockThreadID = t.getId();

		init();
	}

	public static final void saveFile() {
		// CCoreUtil.checkAccess();此处不需限权，
		synchronized (writeNotify) {
			writeNotify.notify();
		}
	}

	private static boolean isSecurityProperties(final String key) {//注意：如无必要，请添加到setValue中
		for (int i = 0; i < needSecurityProperties.length; i++) {
			if (key.equals(needSecurityProperties[i])) {
				return true;
			}
		}

		if (p_SecurityCheckAES.equals(key)) {
			return true;
		}

		return false;
	}

	public static final void setValue(final String key, final boolean value) {
		setValue(key, value ? IConstant.TRUE : IConstant.FALSE);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 *            if value == null, then remove it.
	 */
	public static final void setValue(final String key, String value) {
		if (key.startsWith(PropertiesManager.p_PROJ_RECORD, 0)) {
			checkValidProjectThreadPool(key);
		} else {
			CCoreUtil.checkAccess();

			final boolean isSecurityData = isSecurityProperties(key);

			if (isSecurityData || key.startsWith(S_LINK_PROJECTS, 0)// S_LINK_PROJECTS
																	// + "Lists"
					|| key.startsWith(S_THIRD_DIR, 0) || key.startsWith(CAP_PREFIX, 0) || key.startsWith(S_SecurityProperties, 0)
					|| key.equals(p_isDemoServer)// 影响securityManager
					|| key.equals(p_NewCertIsNotTransed) || key.equals(p_EnableTransNewCertKeyNow) || key.equals(p_HideIDForErrCert)
					|| key.equals(p_IsLoggerOn) || key.equals(p_IsForceNoLogger) || key.equals(p_SecurityCipher) || key.equals(p_SecuritySDPVersion)
					|| key.equals(p_SecuritySecretKeySize) || key.equals(p_isRememberDevCertPassword) || key.equals(p_ApplicationServerID)
					|| key.equals(p_J2SECACertsPassword) || key.equals(p_isAcceptCertsOnlyFromJ2SECACerts)) {// 注意：如果增加逻辑，请同步到remove中
				ResourceUtil.checkHCStackTrace();
			}

			if (isSecurityData && value != null) {
				if (L.isInWorkshop) {
					LogManager.log("[SecurityDataProtector] try encode property : " + key);
				}
				value = SecurityDataProtector.encode(value);
			}
		}

		if (value == null) {
			remove(key);
			return;
		}

		final String oldValue;
		synchronized (writeNotify) {
			oldValue = (String) propertie.setProperty(key, value);
			if (oldValue != null && value.equals(oldValue)) {
			} else {
				statusChanged = true;
			}
		}
	}

	public static final boolean isTrue(final String key) {
		return isTrue(key, false);
	}

	public static final boolean isTrue(final String key, final boolean defaultValue) {
		final String v = getValue(key);
		if (v == null) {
			return defaultValue;
		} else {
			return v.equals(IConstant.TRUE);
		}
	}

	/**
	 * 注意：<BR>
	 * 如果增加项，请考虑增加逻辑到notifyErrorOnSecurityProperties
	 */
	static final String[] needSecurityProperties = { p_CertKey, p_password, p_LogPassword1, p_LogPassword2, p_DevCertPassword,
			p_Deploy_RecentPassword };

	final static void notifyErrorOnSecurityProperties() {
		final String[] securityProperties = PropertiesManager.needSecurityProperties;
		for (int i = 0; i < securityProperties.length; i++) {
			remove(securityProperties[i]);
		}
		remove(p_SecurityCheckAES);

		resetDevCert();

		setPasswordAsInput(ResourceUtil.createRandomVariable(12, 0));// 设置非null需要的初始密码

		{
			final byte[] certKeys = new byte[CCoreUtil.CERT_KEY_LEN];
			CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), certKeys, 0, CCoreUtil.CERT_KEY_LEN);
			PropertiesManager.updateCertKey(certKeys);
		}

		{
			final File filebak = new File(ResourceUtil.getBaseDir(), ImageSrc.HC_LOG_BAK);
			filebak.delete();
		}
		{
			final File filebak = new File(ResourceUtil.getBaseDir(), ImageSrc.HC_LOG);
			filebak.delete();
		}
	}

	public static void resetDevCert() {
		setValue(p_isRememberDevCertPassword, IConstant.FALSE);
		remove(p_DevCertPassword);
	}

	public static void setPasswordAsInput(final String pwdText) {
		setValue(p_password, ResourceUtil.getBASE64(pwdText));
	}

	public static String getPasswordAsInput() {
		return ResourceUtil.getFromBASE64(PropertiesManager.getValue(PropertiesManager.p_password));
	}

	final static void encodeSecurityDataFromTextMode() {
		final PropertiesSet securityPropertiesSet = new PropertiesSet(PropertiesManager.S_SecurityProperties);
		boolean isChanged = false;

		for (int i = 0; i < needSecurityProperties.length; i++) {
			final String p = needSecurityProperties[i];

			if (securityPropertiesSet.contains(p) == false) {
				String sData = propertie.getProperty(p);// 将未加密状态取出

				if (sData != null) {
					if (L.isInWorkshop) {
						LogManager.log("[SecurityDataProtector] change property to encoded : " + p);
					}
					sData = SecurityDataProtector.encode(sData);
					propertie.setProperty(p, sData);// 加密后，存入
				}

				// 注意：下行代码不能并入sData != null
				securityPropertiesSet.appendItemIfNotContains(p);

				isChanged = true;
			}
		}

		if (isChanged) {
			synchronized (writeNotify) {
				statusChanged = true;
			}
			securityPropertiesSet.save();
		}
	}

	/**
	 * @param key
	 * @return 0 means key is not exists.
	 */
	public static final int getIntValue(final String key) {
		final String value = getValue(key);
		try {
			if (value != null) {
				return Integer.valueOf(value);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static final int getIntValue(final String key, final int defaultValue) {
		final String value = getValue(key);
		try {
			if (value != null) {
				return Integer.valueOf(value);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return defaultValue;
	}

	/**
	 * 如果没有，则返回null
	 * 
	 * @param key
	 * @return
	 */
	public static final String getValue(final String key) {
		boolean isSecurityData = false;

		if (key.startsWith(PropertiesManager.p_PROJ_RECORD, 0)) {
			checkValidProjectThreadPool(key);
		} else {
			CCoreUtil.checkAccess();

			isSecurityData = isSecurityProperties(key);

			if (isSecurityData || key.startsWith(S_LINK_PROJECTS, 0) || key.equals(p_SecurityCipher) || key.equals(p_SecuritySDPVersion)
					|| key.equals(p_J2SECACertsPassword)) {
				ResourceUtil.checkHCStackTrace();
			}
		}

		String storeData;
		synchronized (writeNotify) {
			storeData = propertie.getProperty(key);
		}
		if (isSecurityData && storeData != null) {
			if (L.isInWorkshop) {
				LogManager.log("[SecurityDataProtector] try decode property : " + key);
			}
			storeData = SecurityDataProtector.decode(storeData);
		}
		return storeData;// 得到某一属
	}

	/**
	 * 有可能系统级进行本操作，比如删除工程时，要移去全部工程级
	 * 
	 * @param key
	 */
	private static void checkValidProjectThreadPool(final String key) {
		final Thread currentThread = Thread.currentThread();
		ContextSecurityConfig csc = null;
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null))
				|| (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null) {
			final String projID = csc.getProjectContext().getProjectID();
			if (key.startsWith(PropertiesManager.p_PROJ_RECORD + projID, 0) == false) {
				throw new Error("invalid project id [" + projID + "] for key : " + key);
			}
		} else {
			// 比如删除工程时，要移去全部工程级
		}
	}

	public static void initCSCCheck() {
		if (eventDispatchThread == null) {
			eventDispatchThread = HCLimitSecurityManager.getEventDispatchThread();
			hcEventQueue = HCLimitSecurityManager.getHCEventQueue();
		}
	}

	public static final String getValue(final String key, final String defaultValue) {
		final String v = getValue(key);
		if (v == null) {
			return defaultValue;
		} else {
			return v;
		}
	}

	public static final void removeSet(final String key) {
		remove(PropertiesSet.buildSetKey(key));
	}
	
	public static boolean isAcceptCertsOnlyFromJ2SECACerts() {
		return isTrue(p_isAcceptCertsOnlyFromJ2SECACerts, true);
	}

	public static final void remove(final String key) {
		if (key.startsWith(PropertiesManager.p_PROJ_RECORD, 0)) {
			checkValidProjectThreadPool(key);
		} else {
			CCoreUtil.checkAccess();

			if (key.equals(p_NewCertIsNotTransed) || key.equals(p_EnableTransNewCertKeyNow) || key.equals(p_HideIDForErrCert)
					|| key.equals(p_isRememberDevCertPassword) || key.equals(p_J2SECACertsPassword)
					|| key.equals(p_isAcceptCertsOnlyFromJ2SECACerts)) {
				ResourceUtil.checkHCStackTrace();
			}
		}
		propertie.remove(key);
		synchronized (writeNotify) {
			statusChanged = true;
		}
	}

	private static final void init() {
		if (fileName == null) {
			fileName = "hc_config.properties";
		}

		final String fileNameWriting = fileName + "Writing";

		try {
			boolean isWritingExists = false;
			if (ResourceUtil.isStandardJ2SEServer()) {
				propFile = new File(fileName);// 遗留系统，故如此
				propFileWriting = new File(fileNameWriting);
			} else {
				propFile = new File(ResourceUtil.getBaseDir(), fileName);
				propFileWriting = new File(ResourceUtil.getBaseDir(), fileNameWriting);
			}

			File loadFile = null;
			if (propFile.exists()) {
				loadFile = propFile;
			} else if (propFileWriting.exists()) {
				isWritingExists = true;
				loadFile = propFileWriting;
			}

			if (loadFile != null) {
				final FileInputStream inputFile = new FileInputStream(loadFile);
				propertie.load(inputFile);
				inputFile.close();

				if (isWritingExists) {
					propFileWriting.delete();
				}
			} else {
				// 为新建服务器，创建缺省项
			}
		} catch (final Throwable ex) {
			ExceptionReporter.printStackTrace(ex);

			final JPanel panel = App.buildMessagePanel(
					"<html>error on read data from properties file!" + "<BR><BR>file may be using by application!</html>",
					App.getSysIcon(App.SYS_ERROR_ICON));
			App.showCenterPanelMain(panel, 0, 0, "Error", false, null, null, new HCActionListener(new Runnable() {
				@Override
				public void run() {
					PlatformManager.getService().exitSystem();
				}
			}), null, null, false, true, null, false, false);

			while (true) {
				try {
					Thread.sleep(100 * 1000);
				} catch (final InterruptedException e) {
				}
			}
		}
	}

	public static void updateCertKey(final byte[] value) {
		setValue(p_CertKey, ByteUtil.encodeBase64(value));
	}

	// 注意：必须置于最后
	static {
		buildAndStart();
	}

}
