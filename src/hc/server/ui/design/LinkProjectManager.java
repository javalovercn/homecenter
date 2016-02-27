package hc.server.ui.design;

import hc.App;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.HCActionListener;
import hc.server.StarterManager;
import hc.server.ThirdlibManager;
import hc.server.data.StoreDirManager;
import hc.server.msb.ConverterInfo;
import hc.server.msb.RealDeviceInfo;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.HCLimitSecurityManager;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

public class LinkProjectManager{
	public static final String EDIT_HAR = "myedit.har";
	public static final String EDIT_BAK_HAR = "myedit.harbak";
	public static final String LINK_UPGRADE_DIR_NAME = "upgrade";
	//升级时，暂时下载放置目录
	public static final File LINK_UPGRADE_DIR = new File(StoreDirManager.LINK_DIR, LINK_UPGRADE_DIR_NAME);
	
	public static final int STATUS_NEW = 0, STATUS_DEPLOYED = 1, STATUS_MODIFIED = 2, STATUS_DELETED = 3;
	public static final String CURRENT_DIR = ".";
	
	final static Vector<LinkProjectStore> lpsVector = new Vector<LinkProjectStore>(5);
	
	static void reloadLinkProjects(){
		lpsVector.clear();
		try{
			final PropertiesSet projIDSet = AddHarHTMLMlet.getLinkProjSet();
			final int size = projIDSet.size();
			for (int i = 0; i < size; i++) {
				final String item = projIDSet.getItem(i);
				final LinkProjectStore lp = new LinkProjectStore();
				lp.restore(item);
				lpsVector.add(lp);
			}
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 仅删除记录。
	 * @param lps
	 */
	static void removeOnlyLPS(final LinkProjectStore lps){
		final int size = lpsVector.size();
		for (int i = 0; i < size; i++) {
			if(lpsVector.get(i).getProjectID().equals(lps.getProjectID())){
				lpsVector.remove(i);
				return;
			}
		}
	}
	
	static boolean upgradeDownloading(){
		if(enterDownloadStatus() == false){
			return false;
		}
		
		LinkProjectStatus.resetWantDesignOrLinkProjectsNotify();
		
		final Vector<LinkProjectStore> downloadingLPS = new Vector<LinkProjectStore>();
		try{
			log("starting download and upgrading project(s) service...");
			
			if(LinkProjectManager.LINK_UPGRADE_DIR.exists() == false){
				LinkProjectManager.LINK_UPGRADE_DIR.mkdirs();
			}
			
			final String curr_jvm_ver = String.valueOf(App.getJREVer());
			final String curr_hc_ver = StarterManager.getHCVersion();
			final String curr_jruby_ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer, "1.0");
			
			final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);
			final StringBuilder sb = new StringBuilder();
			while(it.hasNext()){
				final LinkProjectStore lp = it.next();
				if(lp.isActive() == false){
					continue;
				}
				final String upgradeurl = lp.getProjectUpgradeURL().trim();
				if(upgradeurl != null && upgradeurl.length() > 0){
					final Properties p_had = new Properties();
					try{
						loadHAD(upgradeurl, p_had);
					}catch (final Throwable e) {
						LogManager.errToLog("Fail upgrade project [" + lp.getProjectID() + "], exception : " + e.toString());
						continue;
					}
					
					//自动请求删除
					if(lp.getProjectID().equals(p_had.getProperty(HCjad.HAD_ID))){
						if(p_had.getProperty(HCjad.HAD_HAR_SIZE).equals("0")){
							log("remove deprecated project [" + lp.getProjectID() + "] by HAD size:0");
							lp.setVersion(LinkProjectStore.DEL_VERSION);
							lp.setDownloadingErr("deprecated project, remove it.");
							continue;
						}
					}
					
					{
					final String p_jvm_ver = p_had.getProperty(HCjad.HAD_JRE_MIN_VERSION);
					if(p_jvm_ver != null){
						if(StringUtil.higer(p_jvm_ver, curr_jvm_ver)){
							if(sb.length() > 0){
								sb.append("<br>");
							}
							sb.append("HAR Project ["+ lp.getProjectID() + ", " + lp.getProjectRemark() + "] require JRE Version (" + p_jvm_ver + "), but current JRE Version:" + curr_jvm_ver);
							continue;
						}
					}
					}
					{
					final String p_hc_ver = p_had.getProperty(HCjad.HAD_HC_MIN_VERSION);
					if(p_hc_ver != null){
						if(StringUtil.higer(p_hc_ver, curr_hc_ver)){
							if(sb.length() > 0){
								sb.append("<br>");
							}
							sb.append("HAR Project ["+ lp.getProjectID() + ", " + lp.getProjectRemark() + "] require HomeCenter Server Version (" + p_hc_ver + "), but current HomeCenter Server Version:" + curr_hc_ver);
							continue;
						}	
					}
					}
					{
						final String p_jruby_ver = p_had.getProperty(HCjad.HAD_JRUBY_MIN_VERSION);
						if(p_jruby_ver != null){
							if(StringUtil.higer(p_jruby_ver, curr_jruby_ver)){
								if(sb.length() > 0){
									sb.append("<br>");
								}
								sb.append("HAR Project ["+ lp.getProjectID() + ", " + lp.getProjectRemark() + "] require JRuby Version (" + p_jruby_ver + "), but current JRuby Version:" + curr_jruby_ver);
								continue;
							}	
						}
					}
					
					{
						final String p_ver = p_had.getProperty(HCjad.HAD_VERSION);
						if(p_ver != null){
							final String version = lp.getVersion();
							final String downVer = lp.getDownloadingVer();
							if(!version.equals(LinkProjectStore.DEFAULT_UNKOWN_VER) 
									&& StringUtil.higer(p_ver, version) 
									&& (downVer.equals(LinkProjectStore.DEFAULT_UNKOWN_VER)
											|| StringUtil.higer(p_ver, downVer))){
								if(!downVer.equals(LinkProjectStore.DEFAULT_UNKOWN_VER) && StringUtil.higer(p_ver, downVer)){
									//已下载，但未被应用，又产生了新版本。
									log("project [" + lp.getProjectID() + "] remote version is higher the local un-apply new version.");
								}
								//必需更新
								downloadHAR(lp, p_had);
								if(lp.getDownloadingErr().equals("") && lp.getDownloadingPosition() > 0){
									downloadingLPS.add(lp);										
								}
							}else{
								continue;
							}
						}else{
							if(sb.length() > 0){
								sb.append("<br>");
							}
							sb.append("HAR Project ["+ lp.getProjectID() + ", " + lp.getProjectRemark() + "] unknow remote version");
						}
					}
					
				}
			}
			
		}catch (final Exception e) {
		}finally{
			boolean hasSucc = false;
			{
				final Iterator<LinkProjectStore> it = downloadingLPS.iterator();
				while(it.hasNext()){
					final LinkProjectStore lps = it.next();
					final String downloadingErr = lps.getDownloadingErr();
					if(downloadingErr != null && downloadingErr.length() == 0 && lps.getDownloadingPosition() > 0){
						hasSucc = true;
						break;
					}
				}
			}
			final JScrollPane sp = buildErroDownloadingPanel();
			final String op = LinkProjectPanel.getNewLinkedInProjOp();
			
			if(op.equals(LinkProjectPanel.OP_ASK) && (hasSucc || sp != null)){
				final JPanel jpanel = new JPanel(new GridBagLayout());
				final GridBagConstraints c = new GridBagConstraints();
				JLabel label = null;
				if(hasSucc){
					String applyNowOrNextStartup = (String)ResourceUtil.get(9160);
					applyNowOrNextStartup = StringUtil.replace(applyNowOrNextStartup, "{yes}", (String)ResourceUtil.get(IContext.OK));
					applyNowOrNextStartup = StringUtil.replace(applyNowOrNextStartup, "{cancel}", (String)ResourceUtil.get(IContext.CANCEL));
					label = new JLabel("<html>" + applyNowOrNextStartup + "</html>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);	
				}else{
					label = new JLabel("");
				}
				c.gridx = 0;
				c.gridy = 0;
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.LINE_START;
				
				jpanel.add(label, c);
				if(sp != null){
					final JLabel er_label = new JLabel("Error donwload project(s):");
					final JPanel errPanel = new JPanel(new GridBagLayout());
					{
						final GridBagConstraints c_sub = new GridBagConstraints();
						c_sub.gridx = 0;
						c_sub.gridy = 0;
						c_sub.fill = GridBagConstraints.NONE;
						c_sub.anchor = GridBagConstraints.LINE_START;
						errPanel.add(er_label, c_sub);
						c_sub.gridy = 1;
						c_sub.fill = GridBagConstraints.BOTH;
						errPanel.add(sp, c_sub);
					}
					
					c.gridy = 1;
					c.fill = GridBagConstraints.BOTH;
					jpanel.add(errPanel, c);
				}
				
				//apply new version project(s) now?
				final String applyNow = (String)ResourceUtil.get(9159);
				
				final String title = hasSucc?applyNow:"Error upgrade project";
				App.showCenterPanel(jpanel, 0, 0, title, hasSucc, null, null, new HCActionListener(new Runnable() {
					@Override
					public void run() {
						appNewLinkedInProjNow(downloadingLPS, true);
						exitDownloadUpgrade();
					}
				}, App.getThreadPoolToken()), new HCActionListener(new Runnable() {
					@Override
					public void run() {
						exitDownloadUpgrade();
					}
				}, App.getThreadPoolToken()), null, false, false, null, false, false);
				
				//exitDownloadUpgrade置于showCenterPanel之中
				return true;
			}else if(op.equals(LinkProjectPanel.OP_IMMEDIATE)){
				appNewLinkedInProjNow(downloadingLPS, true);
			}else if(op.equals(LinkProjectPanel.OP_NEXT_START_UP)){
				updateToLinkProject();
			}
			
			exitDownloadUpgrade();
		}
		return true;
	}

	private static void exitDownloadUpgrade() {
		LinkProjectStatus.exitStatus();
		log("finish download and upgrading project(s) service.");
		
		if(LinkProjectStatus.isWantDesignOrLinkProjectsNotify()){
			LinkProjectStatus.resetWantDesignOrLinkProjectsNotify();
			final JPanel panel = new JPanel(new BorderLayout());
			final ActionListener listener = null;
			final ActionListener cancelListener = null;
			panel.add(new JLabel((String)ResourceUtil.get(9162), App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING), 
					BorderLayout.CENTER);
			App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.INFO), false, null, null, listener, cancelListener, null, false, true, null, false, true);
		}
	}

	public static boolean enterDownloadStatus() {
		return LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_UPGRADE_DOWNLOADING);
	}

	public static void loadHAD(final String upgradeurl, final Properties p_had) throws Exception {
		p_had.load(new URL(upgradeurl).openStream());
	}
	
	private static JScrollPane buildErroDownloadingPanel(){
		final Iterator<LinkProjectStore> it = getLinkProjsIterator(true);
		final Vector<LinkProjectStore> errLists = new Vector<LinkProjectStore>();
		while (it.hasNext()) {
			final LinkProjectStore lps = it.next();
			final String downloadingErr = lps.getDownloadingErr();
			if(downloadingErr != null && downloadingErr.length() > 0){
				errLists.add(lps);
			}
		}
		
		final int rows = errLists.size();
		if(rows > 0){
			final Object[][] playerInfo = new Object[rows][4];
			final String[] Names = { "Project ID", "Desc", "Status", "Upgrade URL" };
			for (int i = 0; i < rows; i++) {
				final LinkProjectStore linkProjectStore = errLists.get(i);
				playerInfo[i][0] = linkProjectStore.getProjectID();
				playerInfo[i][1] = linkProjectStore.getProjectRemark();
				playerInfo[i][2] = linkProjectStore.getDownloadingErr();
				playerInfo[i][3] = linkProjectStore.getProjectUpgradeURL();
				linkProjectStore.resetDownloading();
			}
			final JTable table = new JTable(playerInfo, Names);
			table.setPreferredScrollableViewportSize(new Dimension(600, 30));
			final JScrollPane scrollPane = new JScrollPane(table);
			
			updateToLinkProject();
			
			return scrollPane;
		}
		return null;
	}
	
	static void startLinkedInProjectUpgradeTimer(){
		final HCTimer timer = new HCTimer("", HCTimer.ONE_DAY, true) {
			@Override
			public final void doBiz() {
				CCoreUtil.getSecurityChecker().resetFastCheckThreads();

				final Thread t = new Thread(){
					@Override
					public void run(){
						while(true){
							try{
								Thread.sleep((App.isSimu()?5:60) * 1000);
							}catch (final Exception e) {
							}
							if(PropertiesManager.getValue(PropertiesManager.p_EnableLinkedInProjUpgrade, IConstant.TRUE).equals(IConstant.TRUE)){
								if(LinkProjectManager.upgradeDownloading()){
									break;
								}
							}else{
								break;
							}
						}
					}
				};
				t.setDaemon(true);
				t.start();
			}
		};
		timer.doBiz();
	}

	static LinkProjectStore searchRoot(final boolean mustActive){
		final int size = lpsVector.size();
		for (int i = 0; i < size; i++) {
			final LinkProjectStore lps = LinkProjectManager.lpsVector.elementAt(i);
			if(lps.isRoot()){
				if(mustActive){
					if(lps.isActive()){
						return lps;
					}
				}else{
					return lps;
				}
			}
		}
		return null;
	}
	
	/**
	 * 重新找到一个active作为Root
	 * @param lps
	 * @return
	 */
	static LinkProjectStore searchOtherActive(final LinkProjectStore lps){
		final int size = lpsVector.size();
		for (int i = 0; i < size; i++) {
			final LinkProjectStore search = lpsVector.elementAt(i);
			if(search != lps){
				if(search.isActive()){
					return search;
				}
			}
		}
		return null;
	}
	
	static void appNewLinkedInProjNow(final Vector<LinkProjectStore> v, final boolean isServing){
		final Iterator<LinkProjectStore> it = v.iterator();
		final Vector<LinkProjectStore> delProjs = new Vector<LinkProjectStore>();
		
		boolean upgraded = false;
		while(it.hasNext()){
			final LinkProjectStore lps = it.next();
			
			final String oldVersion = lps.getVersion();
			final String newVersion = lps.getDownloadingVer();
			final boolean isDelProj = newVersion.equals(LinkProjectStore.DEL_VERSION);
			
			if(isDelProj
					|| 
				(!newVersion.equals(LinkProjectStore.DEFAULT_UNKOWN_VER) 
					&& StringUtil.higer(newVersion, oldVersion)
					&& lps.getDownloadingErr().length() == 0
					&& lps.getDownloadingPosition() > 0)){
				if(upgraded == false){
					upgraded = true;
					
					if(isServing){
						//停止服务器
						log("stop service for upgrade project.");
						ServerUIUtil.promptAndStop(false, null);
					}
				}
				
				final File oldEditBackFile = LinkProjectManager.removeLinkProjectPhic(lps, false);
				if(isDelProj){
					//物理删除过时工程
					delProjs.add(lps);
					continue;
				}
				
				final File newVerHar = new File(LINK_UPGRADE_DIR, lps.getHarFile());

				LinkProjectManager.importLinkProject(lps, newVerHar, true, oldEditBackFile);
				log("Successful upgrade project [" + lps.getProjectID() + "] from " + oldVersion + " to " + newVersion);

				newVerHar.delete();
				if(L.isInWorkshop){
					L.V = L.O ? false : LogManager.log("del new upgraded file : " + newVerHar.getAbsolutePath());
				}
				if(newVerHar.exists()){
					PropertiesManager.addDelFile(newVerHar);
				}
				lps.resetDownloading();
			}
		}
		if(upgraded){
			final int size = delProjs.size();
			for (int i = 0; i < size; i++) {
				//物理删除过时工程
				final LinkProjectStore lps = delProjs.get(i);
				removeOnlyLPS(lps);
			}
			
			updateToLinkProject();
			if(isServing){
				ServerUIUtil.restartResponsorServerDelayMode(null, null);
			}
		}
	}
	
	private static void downloadHAR(final LinkProjectStore lps, final Properties p_had){
        final File file = new File(LINK_UPGRADE_DIR, lps.getHarFile());
        final String had_url = p_had.getProperty(HCjad.HAD_HAR_URL);
		final String url_har = (had_url!=null)?had_url:HCjad.convertToExtHar(lps.getProjectUpgradeURL());
		try {  
			final URL url = new URL(url_har);  
			L.V = L.O ? false : LogManager.log("download new version HAR [" + lps.getProjectID() + "] from : " + url_har);
			if(p_had.getProperty(HCjad.HAD_VERSION).equals(lps.getDownloadingVer())){
				//继续下载
				downloadContinue(lps, p_had, file, url, file.length());
			}else{
	            final long totalByted = Long.parseLong(p_had.getProperty(HCjad.HAD_HAR_SIZE));
                if(file.exists()){
                	file.delete();
                }
				final RandomAccessFile raf = new RandomAccessFile(file, "rw");  
                raf.setLength(totalByted);  
                raf.close();  

                lps.setDownloadingVersion(p_had.getProperty(HCjad.HAD_VERSION));
	            downloadContinue(lps, p_had, file, url, totalByted);
			}
        } catch (final Throwable e) { 
        	setError(lps, e.toString());
        	log("Error downloading HAR project : " + url_har);
        	e.printStackTrace();
        	
        	updateToLinkProject();
        }
	}

	private static void setError(final LinkProjectStore lps, final String str) {
		lps.setDownloadingErr(str);
		lps.setDownloadingPosition(0);
		log("Project ID [" + lps.getProjectID() + "] exception:" + str);
	}
	
	private static void downloadContinue(final LinkProjectStore lps, final Properties p_had, 
			final File file, final URL url, final long end){
		RandomAccessFile raf = null;
		try {  
			raf = new RandomAccessFile(file, "rw");  

            final int start = lps.getDownloadingPosition();
            lps.setDownloadingErr("");
            int tryNum = 0;
            int downloadBS = 0;
            //最多进行三次http异常
            final int maxTryNum = 3;
			while(tryNum < maxTryNum){
	            try{
		        	final HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
		            conn.setRequestMethod("GET");  
//		            conn.setReadTimeout(0); 
					conn.setRequestProperty("Range", "bytes=" + start + "-" + end);  
		            if (conn.getResponseCode() == 206) {  
		                raf.seek(start + downloadBS);  
		                final InputStream inStream = conn.getInputStream();  
	                    final int oneMS = 1024 * 1024;
		                final byte[] b = new byte[1024 * 10];  
		                int len = 0;  
		                int mb = 0;
		                while (((start + downloadBS) <= end) && (len = inStream.read(b)) != -1) {  
		                	raf.write(b, 0, len);  
		                    downloadBS += len;
							final int newMB = downloadBS / oneMS;
							if(newMB != mb){
								//每隔1MB进行一次保存
								mb = newMB;
								updateToLinkProject();
							}
		                }  
		            }else{
		            	setError(lps, "http/206 not support download file.");
		            	break;
		            }
		            conn.disconnect();
		            break;
	            }catch (final Exception e) {
	            	tryNum++;
	            	if(tryNum > maxTryNum){
	            		throw e;
	            	}
	            	Thread.sleep(1000);
				}
            }
            //进行校验
            final String md5 = ResourceUtil.getMD5(file);
            if(md5.toLowerCase().equals(p_had.getProperty(HCjad.HAD_HAR_MD5).toLowerCase())){
	            lps.setDownloadingErr("");
	            lps.setDownloadingPosition((int)end);
	            log("successful download new version(" + lps.getDownloadingVer() + ") of project [" + lps.getProjectID() + "].");
            }else{
            	setError(lps, "MD5 verify err file at " + lps.getProjectUpgradeURL());
            }
//            注意：考虑到完整性，不做保存，否则lps会更新hashID，而不在lpsVector
//            updateToLinkProject();
        } catch (final Exception e) { 
        	setError(lps, e.toString());
        }finally{
        	if(raf != null){
        		try {
					raf.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
        	}
        }
	}
	
	private static void log(final String msg){
		L.V = L.O ? false : LogManager.log("[ProjectManager] " + msg);
	}
	
	public static int getActiveProjNum(){
		final Iterator<LinkProjectStore> lpsIt = getLinkProjsIterator(true);
		int count = 0;
		while(lpsIt.hasNext()){
			final LinkProjectStore lps = lpsIt.next();
			if(!lps.isActive()){
				continue;
			}
			count++;
		}
		return count;
	}
	
	static Iterator<LinkProjectStore> getLinkProjsIterator(final boolean includeRoot){
		if(includeRoot){
			return lpsVector.iterator();
		}else{
			return new IteratorWithoutRoot();
		}
	}
	
	static final String getProjPaths(){
		final StringBuilder sb = new StringBuilder();
		final Iterator<LinkProjectStore> it = lpsVector.iterator();
		boolean isAdded = false;
		while(it.hasNext()){
			final LinkProjectStore lps = it.next();
			if(isAdded){
				sb.append(File.pathSeparator);
			}
			sb.append("." + File.separator + lps.getDeployTmpDir());
			isAdded = true;
		}
		return sb.toString();
	}

	static final LinkProjectStore getProjByID(final String projID){
		final int size = lpsVector.size();
		for (int i = 0; i < size; i++) {
			final LinkProjectStore tmp = lpsVector.elementAt(i);
			if(projID == null && tmp.isRoot()){
				return tmp;
			}else if(tmp.getProjectID().equals(projID)){
				return tmp;
			}
		}
		return null;
	}
	
	static int getProjectSize(){
		return lpsVector.size();
	}
	
	static final LinkProjectStore getProjLPSWithCreate(final String id){
		final int size = lpsVector.size();
		LinkProjectStore lps = null;
		for (int i = 0; i < size; i++) {
			final LinkProjectStore tmp = lpsVector.elementAt(i);
			if(tmp.getProjectID().equals(id)){
				lps = tmp;
				break;
			}
		}
		if(lps != null){
		}else{
			lps = new LinkProjectStore();
			lps.setProjectID(id);
			
//			if(context.equals(ROOT_PROJ_ID)){
//				//置于第一个
//				lpsVector.insertElementAt(lps, 0);
//			}else{
//				lpsVector.add(lps);
//			}
			lpsVector.add(lps);
		}
		return lps;
	}
	
	static File buildBackEditFile(final LinkProjectStore lps){
		return new File(new File(App.getBaseDir(), lps.getHarParentDir()), "edit_" + lps.getHarFile());
	}
	
	static final void saveToLinkBack(final LinkProjectStore lps, final File edit){
		ThirdlibManager.copy(edit, buildBackEditFile(lps));
	}

	static final void copyCurrEditFromStorage(final LinkProjectStore lps, final boolean useDeploy){
		File sourceFile = buildBackEditFile(lps);
		if(useDeploy || sourceFile.exists() == false){
			//将deploy版本覆盖backEdit版本
			final File deployFile = buildDepolyFile(lps);
			ThirdlibManager.copy(deployFile, sourceFile);
			lps.setChangedBeforeUpgrade(false);
			updateToLinkProject();
			
			sourceFile = deployFile;
		}
		ThirdlibManager.copy(sourceFile, new File(App.getBaseDir(), EDIT_HAR));
		
		PropertiesManager.setValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID, lps.getProjectID());
		PropertiesManager.setValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_VER, lps.getVersion());
		PropertiesManager.saveFile();

	}

	static final void saveProjConfig(final LinkProjectStore lps, final boolean isRoot, final boolean toActive){
		lps.setRoot(isRoot);
		lps.setActive(toActive);
		
		updateToLinkProject();
	}
	
	static void updateToLinkProject() {
		final int size = lpsVector.size();
		final LinkProjectStore[] lpss = new LinkProjectStore[size];
		
		for (int i = 0; i < size; i++) {
			lpss[i] = lpsVector.elementAt(i);
		}
		
		AddHarHTMLMlet.saveLinkStore(lpss, AddHarHTMLMlet.getLinkProjSet());
	}


	public static String buildSysProjID() {
		return "Proj_" + ResourceUtil.getTimeStamp();
	}

	static String deployToRandomDir(final File har) {
		String randomShareFolder;
		//系统资源处于未拆分到随机目录下，需要重新读取并拆分
		randomShareFolder = ResourceUtil.createRandomFileNameWithExt(App.getBaseDir(), "");
		final Map<String, Object> map = HCjar.loadHar(har, true);
		ProjResponser.deloyToWorkingDir(map, new File(App.getBaseDir(), randomShareFolder));
		return randomShareFolder;
	}

	static{
		reloadLinkProjects();
		
		try{
			UpgradeManager.upgradeToLinkProjectsMode();
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		
		//仅在启动时，更新上次下载的升级包
		appNewLinkedInProjNow(lpsVector, false);
	}

	static File removeLinkProjectPhic(LinkProjectStore lps, final boolean delPersistent){
		if(lps == null){
			lps = getProjByID(null);			
		}
		
		if(delPersistent){
			PropertiesManager.remove(PropertiesManager.p_PROJ_RECORD + lps.getProjectID());
			final String userProjIDPath = HCLimitSecurityManager.getUserDataBaseDir(lps.getProjectID());
			final String delPath = userProjIDPath.substring(0, userProjIDPath.length() - 1);//去掉最后一个/
			PropertiesManager.addDelDir(delPath);
		}
		
		//删除jar文件
		final File harFile = new File(new File(App.getBaseDir(), lps.getHarParentDir()), lps.getHarFile());
//			harFile.delete();
		PropertiesManager.addDelFile(harFile);
		final File oldBackEditFile = buildBackEditFile(lps);
		PropertiesManager.addDelFile(oldBackEditFile);
		
		boolean isChanged = lps.isChangedBeforeUpgrade();
		if(isChanged == false && oldBackEditFile.exists() && harFile.exists()){
			final String md5Editing = ResourceUtil.getMD5(oldBackEditFile);
			final String md5Har = ResourceUtil.getMD5(harFile);
			if(md5Har.equals(md5Editing) == false){
				isChanged = true;
			}
		}

		
		//删除发布目录
//		final File deployTmpDir = new File(lps.getDeployTmpDir());
//		由于导入时，需要创建新TmpDir，不进行删除，可以防止新的TmpDir在极端情形下相同与本删除的。
//		deployTmpDir.delete();
		final String deployTmpDir = lps.getDeployTmpDir();
		if(deployTmpDir.equals(LinkProjectStore.NO_DEPLOY_TMP_DIR) == false){
			PropertiesManager.addDelDir(deployTmpDir);	
		}
		
		ResourceUtil.delProjOptimizeDir(lps.getProjectID());
		
		PropertiesManager.saveFile();
		return isChanged?oldBackEditFile:null;
	}
	
	static boolean importLinkProject(final LinkProjectStore lps, final File sourceNewVer, final boolean isUpgrade,
			final File oldEditBackFile){
		final String fileName = ResourceUtil.createRandomFileNameWithExt(StoreDirManager.LINK_DIR, Designer.HAR_EXT);
	
		lps.setHarFile(fileName);
		lps.setHarParentDir(StoreDirManager.LINK_DIR_NAME);
	
		final File har = buildDepolyFile(lps);
		final File backEditFile = buildBackEditFile(lps);
		
		//发布工作文件
		ThirdlibManager.copy(sourceNewVer, har);
		if(backEditFile.exists() == false || isUpgrade == false){//注意：backEdit文件必须存在。但升级或安装时，不得覆盖可能的修改
			if(oldEditBackFile != null){
				if(isUpgrade){
					lps.setChangedBeforeUpgrade(true);
				}
				ThirdlibManager.copy(oldEditBackFile, backEditFile);
			}else{
				//编辑备份文件
				ThirdlibManager.copy(sourceNewVer, backEditFile);
			}
		}
		
		final String randomShareFolder = deployToRandomDir(har);
		lps.setDeployTmpDir(randomShareFolder);
		
		final Map<String, Object> map = HCjar.loadHar(sourceNewVer, false);
		lps.copyFrom(map);
		
		lps.setDoneBind(false);//新添加或升级的工程DoneBind=false
//		lps.clearBindMap();//注意：不能删除此行。旧的绑定关系仍将使用
		
		return true;
	}

	private static File buildDepolyFile(final LinkProjectStore lps) {
		return new File(StoreDirManager.LINK_DIR, lps.getHarFile());
	}

	/**
	 * check project is exist and active
	 * @param proj_id
	 * @return true if HAR project is exist and active
	 */
	public static boolean checkActiveProject(final String proj_id) {
		final LinkProjectStore lps = getProjByID(proj_id);
		return lps != null && lps.isActive();
	}

	/**
	 * @return 返回被依赖的一个非active工程。null表示完整。
	 */
	private static String[] checkReferencedDependency(final Vector<LinkProjectStore> stores){
		final int size = stores.size();
		final Vector<String> activeProjects = new Vector<String>();
		
		for (int i = 0; i < size; i++) {
			activeProjects.add(stores.elementAt(i).getProjectID());
		}
		
		for (int i = 0; i < size; i++) {
			final LinkProjectStore lps = stores.elementAt(i);
			final Object[] devs = lps.getDevBindMap();
			if(devs != null){
				final RealDeviceInfo[] tmpRealDevs = (RealDeviceInfo[])devs[1];
				for (int j = 0; j < tmpRealDevs.length; j++) {
					final RealDeviceInfo rdbi = tmpRealDevs[j];
					final String proj_id = rdbi.proj_id;
					if(activeProjects.contains(proj_id) == false){
						final String[] out = {proj_id, lps.getProjectID()};
						return out;
					}
				}
			}
			
			final Object[] convs = lps.getConvBindMap();
			if(convs != null){
				final ConverterInfo[] tmpConv = (ConverterInfo[])convs[1];
				for (int j = 0; j < tmpConv.length; j++) {
					final ConverterInfo rbi = tmpConv[j];
					final String proj_id = rbi.proj_id;
					if(activeProjects.contains(proj_id) == false){
						final String[] out = {proj_id, lps.getProjectID()};
						return out;
					}
				}
			}
		}
		
		return null;
	}

	/**
	 * 
	 * @param self
	 * @param stores
	 * @return false表示失败
	 */
	static boolean checkReferencedDependency(final JFrame self, final Vector<LinkProjectStore> stores) {
		final String[] projID = checkReferencedDependency(stores);
		
		if(projID != null){
			final JPanel panel = new JPanel(new BorderLayout());
			String text = "<html>" +
					(String)ResourceUtil.get(8012) +
					"</html>";
			text = StringUtil.replace(text, "{proj1}", projID[0]);
			text = StringUtil.replace(text, "{proj2}", projID[1]);
			panel.add(new JLabel(text, App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
			App.showCenterPanel(panel, 0, 0, App.getErrorI18N(), false, null, null, null, null, self, true, false, null, false, false);
			return false;
		}
		
		return true;
	}

}

class IteratorWithoutRoot implements Iterator<LinkProjectStore> {
	int cursor = 0;
	
	@Override
	public boolean hasNext() {
		if(cursor == LinkProjectManager.lpsVector.size()){
			return false;
		}
		final LinkProjectStore elementAt = LinkProjectManager.lpsVector.elementAt(cursor);
		if(elementAt.isRoot()){
			cursor++;
			return hasNext();
		}else{
			return true;
		}
	}

	@Override
	public LinkProjectStore next() {
		return LinkProjectManager.lpsVector.elementAt(cursor++);
	}

	@Override
	public void remove() {
	}
}