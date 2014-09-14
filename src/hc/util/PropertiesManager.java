package hc.util;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JOptionPane;

public class PropertiesManager {
	static String fileName = IConstant.propertiesFileName;
	
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
	public static final String p_IAgree = "iAgree";
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
	
	public static final String p_DesignerDividerLocation = "DesignerDividerLocation";
	public static final String p_DesignerCtrlDividerLocation = "DesignerCtrlDividerLocation";
	public static final String p_DesignerCtrlHOrV = "DesignerCtrlHOrV";

	public static final String p_ReadedMsgID = "ReadedMsgID";
	
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

	private static int getDelDirNum(){
		String v = getValue(p_DelDirNum);
		if(v == null){
			return 0;
		}else{
			return Integer.parseInt(v);
		}
	}
	
	private static int delDirNum = 0;
	
	public static void addDelFile(final File file){
		addDelDir(file.getPath());
	}
	
	/**
	 * 增加一个待删除的目录或文件，不含自动存储指令
	 * @param dirName
	 */
	public static void addDelDir(String dirName){
		setValue(p_DelDirPre + delDirNum, dirName);
		++delDirNum;
		setValue(p_DelDirNum, String.valueOf(delDirNum));
	}
	
	
	public static void emptyDelDir(){
		delDirNum = getDelDirNum();
		for (int i = 0; i < delDirNum; i++) {
			final String key = p_DelDirPre + i;
			final String delFileName = getValue(key);
			if(delFileName != null){
//				System.out.println("delete file : " + new File(delFileName).getAbsolutePath());
				deleteDirectoryNowAndExit(new File(delFileName));
				remove(key);
			}
		}
		
		delDirNum = 0;
		remove(p_DelDirNum);
		saveFile();
	}
	
	private static boolean deleteDirectoryNowAndExit(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectoryNowAndExit(files[i]);
	                }
	                else {
	                    if(files[i].delete() == false){
	                    	L.V = L.O ? false : LogManager.log("fail del file : " + files[i].getAbsolutePath());
	                    }
	                }
	            }
	        }
	    }
	    boolean isDel = directory.delete();
	    if(isDel == false){
	    	L.V = L.O ? false : LogManager.log("fail del dir/file : " + directory.getAbsolutePath());
	    }
	    return isDel;
	}
	

	
	//"DonateKey" 做特殊处理

	static boolean statusChanged = false;
	
	static Properties propertie;
	static FileLocker locker;
	public static boolean enableLock = true;

	public static synchronized void saveFile(){
		if(propertie == null
				|| (statusChanged == false)){
			return;
		}
        try{
        	if(enableLock){
        		locker.release();
        	}
        	
        	FileOutputStream outputFile = new FileOutputStream(fileName);
            propertie.store(outputFile, null);
            outputFile.close();
            
            if(enableLock){
            	locker.lock();
            }
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(null, "write data to properties file error!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
	
	public static void setValue(String key, String value){
		if(propertie == null){
			init();
		}
		final String oldValue = (String)propertie.get(key);
		if(oldValue != null && value.equals(oldValue)){
			
		}else{
			statusChanged = true;
			propertie.setProperty(key, value);
		}
    }
	
	public static boolean isTrue(String key){
		String v = getValue(key);
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
	public static String getValue(String key){
		if(propertie == null){
			init();
		}
		
        if(propertie.containsKey(key)){
        	return propertie.getProperty(key);//得到某一属
        }else{
            return null;
        }
    }
	
	public static String getValue(String key, String defaultValue){
		final String v = getValue(key);
		if(v == null){
			return defaultValue;
		}else{
			return v;
		}
	}
	
	public static void remove(String key){
		if(propertie == null){
			init();
		}
		propertie.remove(key);
	}
	
	public static void init(){
		propertie = new Properties();
		
    	if(fileName == null){
    		fileName = "hc_config.properties";
    	}

    	try{
    		locker = new FileLocker(new File(fileName), FileLocker.READ_WRITE_MODE);
    		FileInputStream inputFile = new FileInputStream(fileName);
            propertie.load(inputFile);
            inputFile.close();
            
            if(enableLock){
            	locker.lock();
            }
        } catch (Exception ex){
        	//因为可以初次使用，会产生异常
        	//ex.printStackTrace();
        }
	}

}
