package hc.server;

import hc.App;
import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.ui.LinkProjectStatus;
import hc.util.IBiz;
import hc.util.MultiThreadDownloader;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class JRubyInstaller {
	static final String JRUBY_VER_1_7_3 = "1.7.3";
	static MultiThreadDownloader mtd;

	private static String getInnverJRubyMD5(String outerVersion, String outerMD5){
		String[] versions = {JRUBY_VER_1_7_3};
		String[] innerMD5 = {"75a612d9ba57a61f01dcd6e3e586a34b"};
		
		for (int i = 0; i < versions.length; i++) {
			if(versions[i].equals(outerVersion)){
				return innerMD5[i];
			}
		}
		
		return outerMD5;
	}

	public static boolean checkInstalledJRuby(){
		return PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer) != null;
	}
	
	public static void startInstall(){
		if(LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_JRUBY_INSTALL)){
			new Thread(){
				public void run(){
					callDownload();
				}
			}.start();
		}
	}
	
	public static void callDownload(){
		L.V = L.O ? false : LogManager.log("download JRuby engine...");

		ConditionWatcher.addWatcher(new IWatcher() {
			final long ms = System.currentTimeMillis();
			@Override
			public boolean watch() {
				if(System.currentTimeMillis() - ms > 5000 || ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON){
					ContextManager.getContextInstance().displayMessage(
							(String) ResourceUtil.get(IContext.INFO), 
							StringUtil.replace((String)ResourceUtil.get(9066), "{design}", (String)ResourceUtil.get(9034)), 
							IContext.INFO, null, 0);
					return true;
				}
				return false;
			}
			
			@Override
			public void setPara(Object p) {
			}
			
			@Override
			public boolean isNotCancelable() {
				return false;
			}
			
			@Override
			public void cancel() {
			}
		});
		

		final Properties thirdlibs = ResourceUtil.loadThirdLibs();
		if(thirdlibs == null){
			try{
				Thread.sleep(2000);
				callDownload();
			}catch (Exception e) {
			}
			return;
		}

		final String _lastJrubyVer = JRUBY_VER_1_7_3;//thirdlibs.getProperty("jruby.ver");
		final String _lastJrubyMd5 = thirdlibs.getProperty("jruby.md5");

		final String md5 = getInnverJRubyMD5(_lastJrubyVer, _lastJrubyMd5);
		String fromURL = thirdlibs.getProperty("jruby.url");
		String storeFile = J2SEContext.jrubyjarname;
		File rubyjar = new File(storeFile);
		if(rubyjar.exists()){
			rubyjar.delete();
		}
		
		IBiz biz = new IBiz() {
			@Override
			public void start() {
				PropertiesManager.setValue(PropertiesManager.p_jrubyJarFile, J2SEContext.jrubyjarname);	
				PropertiesManager.setValue(PropertiesManager.p_jrubyJarVer, _lastJrubyVer);
				PropertiesManager.saveFile();
				
				L.V = L.O ? false : LogManager.log("successful installed JRuby.");
				LinkProjectStatus.exitStatus();		
				
				RootServerConnector.notifyLineOffType("lof=jrubyOK");

				if(needNotify){
					notifySuccessInstalled();
				}
			}
			
			@Override
			public void setMap(HashMap map) {
			}
		};
		IBiz failBiz = new IBiz() {
			@Override
			public void start() {
				callDownload();
			}
			
			@Override
			public void setMap(HashMap map) {
			}
		};
		if(mtd == null){
			mtd = new MultiThreadDownloader();
		}
		mtd.download(StringUtil.split(fromURL, RootConfig.CFG_SPLIT), rubyjar, md5, biz, failBiz, false);
	}
	
	private static boolean needNotify = false;
	
	public static JProgressBar getFinishPercent(){
		return mtd.getFinishPercent();
	}
	
	private static void needNotify(){
		needNotify = true;
	}
	
	public static void notifySuccessInstalled(){
		if(LinkProjectStatus.isIdle()){
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JLabel((String)ResourceUtil.get(9079), 
						App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
			
			App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.INFO), true, null, null, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(LinkProjectStatus.isIdle()){
						LinkMenuManager.startDesigner();
					}
				}
			}, null, null, false, true, null, false, false);
		}
	}
	
	private static Window progressWindow;

	public static void showProgressWindow(JFrame parent) {
			needNotify();
					
			if(progressWindow == null || (progressWindow.isVisible() == false)){
				JLabel label = new JLabel("<html>" + (String)ResourceUtil.get(9084) +
		//							"<br>if we have finished, a notify window will display." +
						"</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING);
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(label, BorderLayout.CENTER);
				final JProgressBar finishPercent = getFinishPercent();
				if(finishPercent != null){
					panel.add(finishPercent, BorderLayout.SOUTH);
				}
				
				progressWindow = App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.INFO), 
						false, null, null, null, null, parent, true, true, null, false, false);
			}else{
				progressWindow.toFront();
			}
		}
}
