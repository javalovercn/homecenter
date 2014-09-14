package hc.server.ui.video;

import hc.App;
import hc.core.IContext;
import hc.core.RootConfig;
import hc.core.util.IMsgNotifier;
import hc.core.util.StringUtil;
import hc.util.IBiz;
import hc.util.MultiThreadDownloader;
import hc.util.ResourceUtil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class CapManager {

	public static void installJMF(){
		//检查安装环境只能为Windows
		final boolean isWindow = ResourceUtil.isWindowsOS();
		if(isWindow == false){
			String linuxDesc = "<html>" +
				"JMF online install only support Windows." +
				" please download and install JMF for other OS.<BR><BR>" +
				"NOTE : Curr Ver only support RGB format video, NOT YUV, <BR>" +
				"please make sure device and driver support RGB format.<BR><BR>" +
				"The JMF 2.1.1 Reference Implementation supports SunVideo / SunVideoPlus capture devices on Solaris. " +
				"<BR>On Linux, devices that have a Video4Linux driver are expected to work, " +
				"but not extensively tested.<BR><BR>More info, please read 'Capture Devices' of Java Media Framework." +
				"<BR><BR>NOTE : If installed, please restart HomeCenter Server.</html>";
			JPanel notify = new JPanel();
			notify.add(new JLabel(linuxDesc, App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEFT));
			App.showCenterPanel(notify, 0, 0, "install for other OS", false, null, null, null, null, null, false, false, null, false, false);
			return;
		}
		
		String windowDesc = "<html><body style=\"width:500\">" +
				"<STRONG>Important</STRONG> : <BR>" +
				"Windows USB camera driver support YUV format, maybe NOT support RGB format, <BR>" +
				"Please install USB camera driver of your device to support RGB format first!<BR><BR>" +
				"click '" + (String)ResourceUtil.get(IContext.OK) + "' to install JMF online." +
				"<BR>JMF install will auto detect and find USB camera(s). please wait for it." +
				"<BR><BR>If you had installed JMF(Java Media Framework) 2.1.1, " +
				"please reinstall it, or check %CLASSPATH% system variable, " +
				"click '" + (String) ResourceUtil.get(1018) + "' to cancel install." +
				"</body></html>";
		JPanel notify = new JPanel();
		notify.add(new JLabel(windowDesc, App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEFT));
		App.showCenterPanel(notify, 0, 0, "Install JMF (Java Media Framework)", true, null, 
				null, 
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						downloadAndInstall();
					}
				}, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
					}
				}, null, false, false, null, false, false);
	
	}

	private static void downloadAndInstall() {
		//ProgressMonitorInputStream需要另开线程来进行画面刷新
		new Thread(){
    		public void run(){
				final Properties thirdlibs = ResourceUtil.loadThirdLibs();
				if(thirdlibs == null){
					return;
				}
				
				final String md5Backup = "70129c04d56ce7301cf2c93857784268";
			
				
				String fromURL = thirdlibs.getProperty("jmf.url");
				String tmpDir = System.getProperty("java.io.tmpdir");
				if(tmpDir == null ||tmpDir.length() == 0){
					tmpDir = "c:\\";
				}
				final File jfmExeFile = new File(tmpDir, "jmfInstall.exe");
				if(jfmExeFile.exists()){
					jfmExeFile.delete();
				}
				MultiThreadDownloader mtd = new MultiThreadDownloader();
				IBiz biz = new IBiz() {
					@Override
					public void start() {
						final String exeFileStr = jfmExeFile.getPath();
						try{
							Process process = Runtime.getRuntime().exec(exeFileStr);
							process.waitFor();  

							JPanel panel = new JPanel();
							panel.add(new JLabel("Please restart HomeCenter Server later.", 
									App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEFT));
							App.showCenterPanel(panel, 0, 0, "Restart", false, null, null, null, null, null, false, false, null, false, false);
							
							if(jfmExeFile.exists()){
								jfmExeFile.delete();
							}
						} catch (Exception e) {  
							//没有管理员特权
							JPanel panel = new JPanel();
							panel.add(new JLabel("<html>Fail to setup JMF, need administrator privileges.<BR><BR>" +
									"please try to run ["+exeFileStr+"] by manual.</html>", 
									App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
							App.showCenterPanel(panel, 0, 0, "Fail setup", false, null, null, null, null, null, false, false, null, false, false);
							return;
						}
					}
					
					@Override
					public void setMap(HashMap map) {
					}
				};
				mtd.download(StringUtil.split(fromURL, RootConfig.CFG_SPLIT), jfmExeFile, md5Backup, biz, null, false);
    		}
		}.start();
	}

	//	private Raster rgbRaster;
	private static Vector<IMsgNotifier> listener = new Vector<IMsgNotifier>(1);

	public static void addListener(IMsgNotifier notify){
		synchronized(listener){
			if(listener.contains(notify) == false){
				listener.add(notify);
			}
		}
	}

	public static void removeListener(IMsgNotifier notify){
		listener.removeElement(notify);
	}

	public static void notifyMsg(int capStatus){
		synchronized(listener){
			final int size = CapManager.listener.size();
			final String strStatus = String.valueOf(capStatus);
			for (int i = 0; i < size; i++) {
				IMsgNotifier noti = CapManager.listener.elementAt(i);
				try{
					noti.notifyNewMsg(strStatus);
				}catch (Throwable e) {
				}
			}
		}
	}

}
