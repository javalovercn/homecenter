package hc.server;

import hc.PlatformTrayIcon;
import hc.core.util.WiFiDeviceManager;

import java.awt.Image;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JPopupMenu;

public interface PlatformService {
	public static final ClassLoader SYSTEM_CLASS_LOADER = PlatformService.class.getClassLoader();
	public static final int BIZ_GET_ANDROID_KEYCODE = 1;
	public static final int BIZ_CONVERT_J2SE_KE_TO_ANDROID_KEY = 2;
	public static final int BIZ_CTRL_V = 3;
	public static final int BIZ_DEL_HAR_OPTIMIZE_DIR = 4;
	public static final int BIZ_BIND_FORCE_ANDROID_KEYCODE = 5;
	public static final int BIZ_INIT_RUBOTO_ENVIROMENT = 6;
	public static final int BIZ_GO_HOME = 7;
	public static final int BIZ_BCL = 8;
	
	public static final int LOG_LEVEL_INFO = 1;
	public static final int LOG_LEVEL_WARN = 2;
	public static final int LOG_LEVEL_ERROR = 3;
	
	/**
	 * 
	 * @return the free memory in M.
	 */
	public long getFreeMem();
	
	public Object doExtBiz(int bizID, Object para);
	
	public void setWindowOpaque(Window win, boolean bool);
	
	public void addJCEProvider();
	
	public void setAutoStart(boolean isAutoStart);
	
	public void setJRubyHome(final String version, final String absPath);
	
	public boolean isLockScreen();
	
	public File getJRubyAndroidOptimizBaseDir();
	
	public String[] listAssets(final Object path);
	
	public WiFiDeviceManager getWiFiManager();
	
	/**
	 * 
	 * @return 剩余工作区可用磁盘字节数
	 */
	public long getAvailableSize();
	
	/**
	 * 
	 * @param files null for last 3rdClassLoader; if not null, create new instance for 3rd class loader.
	 * @return
	 */
	public ClassLoader get3rdClassLoader(File[] files);
	
	public ClassLoader loadClasses(File[] filePaths, ClassLoader parent, boolean isDex, String loadOpID);
	
	public BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius);
	
	public BufferedImage composeImage(BufferedImage base, BufferedImage cover);
	
	public Shape getImageShape(Image img);
	
	public void extLog(final int level, final String msg);
	
	public void setWindowShape(Window win, Shape shape);
	
	public Object createRobotPeer(Robot robot) throws Throwable;
	
	public void buildCaptureMenu(JPopupMenu popupTi, ThreadGroup threadPoolToken);
	
	public void startCapture();
	
	public void stopCapture();
	
	public void printAndroidServerInfo();
	
	public void exitSystem();
	
	/**
	 * 在J2SE环境下，直接调用ExitManager.startExitSystem()；Android服务器模式下，增加后台运行选项
	 */
	public void startExitSystem();
	
	public PlatformTrayIcon buildPlatformTrayIcon(final Image image, String toolTip, final JPopupMenu menu);
	
	public File getBaseDir();
	
	public void addSystemLib(File jardexFile, boolean isReload);
	
	public BufferedImage resizeImage(final BufferedImage bufferedimage, final int w, final int h);
}
