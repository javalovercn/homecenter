package hc.util;

import hc.App;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.PlatformManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JOptionPane;

public class PropertiesManager {
	public static long PropertiesLockThreadID = 0;//置于最前
	private static String fileName = IConstant.propertiesFileName;
	private static boolean enableLock = IConstant.enableInitLock;
	
	public final static String getPropertiesFileName(){
		return fileName;
	}
	
	public static final String p_stunServerPorts = "StunServerPorts";
	public static final String p_IsSimu = "isSimu";
	public static final String p_IsMobiMenu = "mMenu";
	public static final String p_uuid = "uuid";
	public static final String p_password = "password";
	public static final String p_ServerSide = "serverSide";
	public static final String p_AutoStart = "AutoStart";
	public static final String p_TCPPortFrom = "PortFrom";
	public static final String p_NetworkInterfaces = "NetworkInterfaces";
	public static final String p_UsingNetworkInterfaces = "UsingNetworkInterfaces";
	public static final String p_Token = "Token";
	public static final String p_TokenRelay = "TokenRelay";
	public static final String p_TokenBack = "TokenBack";
	//外网直联检测及开通
	public static final String p_DisableDirect = "DisableDirect";
	//家庭内网直联检测及开通
	public static final String p_DisableHomeWireless = "DisableHomeWireless";
	//仅使用中继，但不排斥家庭内网
	public static final String p_ForceRelay = IConstant.ForceRelay;
	public static final String p_UPnPUseSTUNIP = "UPnPUseSTUNIP";
	public static final String p_Log = "Log";
	public static final String p_DirectUPnPExtPort = "DirectUPnPExtPort";
	public static final String p_RelayServerUPnPExtPort = "RelayUPnPExtPort";
	public static final String PWD_ERR_TRY = "pwdErrTry";
	public static final String PWD_ERR_LOCK_MINUTES = "pwdErrLockMinutes";
	public static final String p_LogPassword1 = "log1";
	public static final String p_LogPassword2 = "log2";
	public static final String p_LogCipherAlgorithm1 = "logCipherAlgorithm1";
	public static final String p_LogCipherAlgorithm2 = "logCipherAlgorithm2";
	public static final String p_IAgree = "iAgree";//停止使用，由p_AgreeVersion代替
	public static final String p_AgreeVersion = "AgreeVersion";
	public static final String p_RelayServerLocalIP = "ServerIP";
	public static final String p_RelayServerLocalPort = "ServerPort";
	public static final String p_RelayBlockNum = "RelayBlockNum";
	
	//本地Relay服务器IP从p_RelayServerLocalIP中取
	@Deprecated
	public static final String p_ServerLocalIP = "ServerLocalIP";
	
	public static final String p_ResourceID = "ResourceID";
	public static final String p_ResourcesMaybeUnusedNew = "ResourceMaybeUnusedNew";
	public static final String p_ResourcesMaybeUnusedOld = "ResourceMaybeUnusedOld";
	public static final String p_ForceEn = "ForceEn";
	public static final String p_isNotAutoUpgrade = "NotAutoUpgrade";
	public static final String p_isMultiUserMode = "MultiUserMode";
	public static final String p_LasterAutoUpgradeVer = "LasterUpgradeVer";
	
	public static final String p_CertKey = "CertKey";
	public static final String p_EnableTransNewCertKeyNow = "TransCertKey";
	public static final String p_NewCertIsNotTransed = "NewCertIsNotTransed";
	
	public static final String p_jrubyJarFile = "JRubyJarFile";
	public static final String p_jrubyJarVer = "JRubyJarVer";
	@Deprecated
	public static final String p_SampleDeployNotify = "SampleNotify";
	public static final String p_DelDirNum = "DelDirNum";
	public static final String p_DelDirPre = "DelDirPre.";
	
	public static final String p_OpNewLinkedInProjVer = "OpNewLinkedInProjVer";
	public static final String p_EnableLinkedInProjUpgrade = "EnableLinkedInProjUpgrade";
	/**
	 * 旧系统支持单一工程发布时，记录资源拆分目录的存储位。新系统支持多工程发布，统一存储
	 * @Deprecated
	 */
	public static final String p_DeployTmpDir = "DeployTmpDir";
	
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
	
	/**
	 * 初次安装的版本号，与后续升级无关
	 */
	public static final String p_InitVersion = "InitVersion";
	
	public static final String p_WindowX = "_wx";
	public static final String p_WindowY = "_wy";
	public static final String p_WindowWidth = "_ww";
	public static final String p_WindowHeight = "_wh";
	
	public static final String p_TrayX = "_Tray_x";
	public static final String p_TrayY = "_Tray_y";
	
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
	
	public static final String p_autoStart = "autoStart";
	
	public static final String p_intervalSecondsNextStartup = "intervalSecondsNextStartup";
	public static final String p_preloadAfterStartup = "preloadAfterStartup";
	
	public static final String p_WiFi_currSSID = "wifi_currSSID";
	public static final String p_WiFi_currPassword = "wifi_currpwd";
	public static final String p_WiFi_currSecurityOption = "wifi_currSO";
	public static final String p_WiFi_currIsAutoCreated = "wifi_currIsAutoCreated";
	public static final String p_WiFi_hasWiFiModule = "wifi_hasModu";
	public static final String p_WiFi_canCreateAP = "wifi_canCreateAP";

	//用于设置WiFi密码，表示曾经走WiFi的手机连接过
	public static final String p_WiFi_isMobileViaWiFi = "wifi_isMobiViaWiFi";
	
	public static final String p_DesignerDocFontSize = "DesignerDocFontSize";

	public static final String S_THIRD_DIR = "3libs";
	public static final String S_USER_LOOKANDFEEL = "lookfeel";
	public static final String S_LINK_PROJECTS = "linkProjs";
	public static final String S_LINK_PROJECTS_COLUMNS_WIDTH = "linkProjWidth";
	public static final String p_LINK_CURR_EDIT_PROJ_ID = "currEditProjID";
	public static final String p_LINK_CURR_EDIT_PROJ_VER = "currEditProjVer";
	
	public static final String C_SYSTEM_DEFAULT_FONT_SIZE = "c_SysDftFontSize";
	public static final String C_FONT_NAME = "c_FontName";
	public static final String C_FONT_SIZE = "c_FontSize";
	public static final String C_SKIN = "c_skin";
	
	public static final String PRE_USER_PROJ = "+";
	
	public static final String p_PROJ_RECORD = PRE_USER_PROJ + "record_";

	private static final int getDelDirNum(){
		final String v = getValue(p_DelDirNum);
		if(v == null){
			return 0;
		}else{
			return Integer.parseInt(v);
		}
	}
	
	private static int delDirNum = 0;
	
	public static final void addDelFile(final File file){
		CCoreUtil.checkAccess();
		
		addDelDir(file.getPath());
	}
	
	/**
	 * 增加一个待删除的目录或文件，不含自动存储指令
	 * @param dirName
	 */
	public static final void addDelDir(final String dirName){
		CCoreUtil.checkAccess();
		
		setValue(p_DelDirPre + delDirNum, dirName);
		++delDirNum;
		setValue(p_DelDirNum, String.valueOf(delDirNum));
	}
	
	public static final void emptyDelDir(){
		delDirNum = getDelDirNum();
		for (int i = 0; i < delDirNum; i++) {
			final String key = p_DelDirPre + i;
			final String delFileName = getValue(key);
			if(delFileName != null){
				final File delFile = new File(delFileName);
				if(L.isInWorkshop){
					L.V = L.O ? false : LogManager.log("delete file/dir : " + delFile.getAbsolutePath());
				}
				ResourceUtil.deleteDirectoryNowAndExit(delFile);
				remove(key);
			}
		}
		
		delDirNum = 0;
		remove(p_DelDirNum);
		saveFile();
	}
	
	//"DonateKey" 做特殊处理

	static boolean statusChanged = false;
	
	private static Properties propertie;
	static FileLocker locker;
	private static final Boolean LOCK = new Boolean(true);
	private static final Thread saveThread = buildAndStart();
	
	private static boolean isShutdownHook = false;
	
	public static void notifyShutdownHook(){
		CCoreUtil.checkAccess();
		isShutdownHook = true;
	}
	
	private static Thread buildAndStart(){
		init();
		
		if(enableLock == false){
			return null;
		}
		
		final Thread t = new Thread(){
			Object globalLock;
			
			@Override
			public void run(){
				while(true){
					synchronized (LOCK) {
						try{
							LOCK.wait();
						}catch (final Exception e) {
						}
						
						if(isShutdownHook){
							continue;
						}
						
						if(globalLock == null){
							globalLock = CCoreUtil.getGlobalLock();
						}
						save(globalLock);
					}
				}
			}
		};
		t.setDaemon(true);
		t.setPriority(ThreadPriorityManager.FILE_SAVE_PRIORITY);
		t.start();
		
		PropertiesLockThreadID = t.getId();
		
		return t;
	}
	
	private static final void save(final Object globalLock){
		if(propertie == null
				|| (statusChanged == false)){
			return;
		}
        try{
        	synchronized (LOCK) {
        		if(enableLock && locker.lockFile.exists()){
            		locker.release();
        		}
            	
            	synchronized (globalLock) {
                	final FileOutputStream outputFile = new FileOutputStream(fileName);
                    propertie.store(outputFile, null);
                    outputFile.close();
				}
                
                if(enableLock){
                	locker.lock();
                }
			}
        } catch (final Exception e) {
        	e.printStackTrace();
        	App.showMessageDialog(null, "write data to properties file error!", "Error", JOptionPane.ERROR_MESSAGE);
//            System.exit(0);
        }
    }

	public static final void saveFile(){
//		CCoreUtil.checkAccess();此处不需限权，
		if(enableLock){
			synchronized (LOCK) {
				LOCK.notify();
			}
		}else{
			save(CCoreUtil.getGlobalLock());
		}
    }
	
	public static final void setValue(final String key, final String value){
		if(key.startsWith(PropertiesManager.p_PROJ_RECORD, 0)){
			
		}else{
			CCoreUtil.checkAccess();
		}
		
		synchronized (LOCK) {
			final String oldValue = (String)propertie.get(key);
			if(oldValue != null && value.equals(oldValue)){
				
			}else{
				statusChanged = true;
				propertie.setProperty(key, value);
			}
		}
    }

	public static final boolean isTrue(final String key){
		final String v = getValue(key);
		if(v == null){
			return false;
		}else{
			return v.equals(IConstant.TRUE);
		}
	}
	
	/**
	 * 如果没有，则返回null
	 * @param key
	 * @return
	 */
	public static final String getValue(final String key){
		if(key.startsWith(PropertiesManager.p_PROJ_RECORD, 0)){
			
		}else{
			CCoreUtil.checkAccess();
		}
		
        if(propertie.containsKey(key)){
        	return propertie.getProperty(key);//得到某一属
        }else{
            return null;
        }
    }
	
	public static final String getValue(final String key, final String defaultValue){
		final String v = getValue(key);
		if(v == null){
			return defaultValue;
		}else{
			return v;
		}
	}
	
	public static final void remove(final String key){
		CCoreUtil.checkAccess();
		
		propertie.remove(key);
	}
	
	private static final void init(){
		propertie = new Properties();
		
    	if(fileName == null){
    		fileName = "hc_config.properties";
    	}

    	try{
    		final File file = new File(new File(System.getProperty("user.dir")), fileName);
    		locker = new FileLocker(file, FileLocker.READ_WRITE_MODE);
    		
    		if(file.exists()){
	    		final FileInputStream inputFile = new FileInputStream(fileName);
	            propertie.load(inputFile);
	            inputFile.close();
	    		
	            if(enableLock){
	            	while(locker.lock() == false){
	            		Thread.sleep(100);
	            	}
	    		}
            }
        } catch (final Exception ex){
        	App.showMessageDialog(null, "<html>error on read data from properties file!" +
        			"<BR><BR>file may be using by application!</html>", "Error", JOptionPane.ERROR_MESSAGE);
        	PlatformManager.getService().exitSystem();
        }
 	}

}
