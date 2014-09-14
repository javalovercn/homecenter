package hc.server.ui.design;

import hc.App;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.StarterManager;
import hc.server.ThirdlibManager;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;


public class LinkProjectManager{
	public static final String EDIT_HAR = "myedit.har";
	public static final String EDIT_BAK_HAR = "myedit.harbak";
	public static final String LINK_DIR_NAME = "link";
	public static final String TEMP_DIR_NAME = "temp";
	public static final String LINK_UPGRADE_DIR_NAME = "upgrade";
	public static final File LINK_DIR = new File(LINK_DIR_NAME);
	public static final File TEMP_DIR = new File(TEMP_DIR_NAME);
	//升级时，暂时下载放置目录
	public static final File LINK_UPGRADE_DIR = new File(LINK_DIR, LINK_UPGRADE_DIR_NAME);
	
	public static final int STATUS_NEW = 0, STATUS_DEPLOYED = 1, STATUS_MODIFIED = 2, STATUS_DELETED = 3;
	public static final int MAX_LINK_PROJ_NUM = 30;
	public static final String CURRENT_DIR = ".";
	
	public static Vector<LinkProjectStore> lpsVector;
	
	public static void reloadLinkProjects(){
		lpsVector = new Vector<LinkProjectStore>(0);
		try{
			final PropertiesSet projIDSet = new PropertiesSet(PropertiesManager.S_LINK_PROJECTS);
			final int size = projIDSet.size();
			for (int i = 0; i < size; i++) {
				String item = projIDSet.getItem(i);
				LinkProjectStore lp = new LinkProjectStore();
				lp.restore(item);
				lpsVector.add(lp);
			}
		}catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static void removeOnlyLPS(final LinkProjectStore lps){
		final int size = lpsVector.size();
		for (int i = 0; i < size; i++) {
			if(lpsVector.get(i).getProjectID().equals(lps.getProjectID())){
				lpsVector.remove(i);
				return;
			}
		}
	}
	
	public static boolean upgradeDownloading(){
		if(LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_UPGRADE_DOWNLOADING) == false){
			return false;
		}
		
		final Vector<LinkProjectStore> downloadingLPS = new Vector<LinkProjectStore>();
		Vector<LinkProjectStore> removed = new Vector<LinkProjectStore>();
		try{
			log("starting download and upgrading project(s) service...");
			
			if(LinkProjectManager.LINK_UPGRADE_DIR.exists() == false){
				LinkProjectManager.LINK_UPGRADE_DIR.mkdir();
			}
			
			final String curr_jvm_ver = String.valueOf(App.getJREVer());
			final String curr_hc_ver = StarterManager.getHCVersion();
			final String curr_jruby_ver = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer, "1.0");
			
			Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);
			final StringBuilder sb = new StringBuilder();
			while(it.hasNext()){
				LinkProjectStore lp = it.next();
				if(lp.isActive() == false){
					continue;
				}
				final String upgradeurl = lp.getProjectUpgradeURL();
				if(upgradeurl != null && upgradeurl.length() > 0){
					final Properties p_had = new Properties();
					try{
						loadHAD(upgradeurl, p_had);
					}catch (Throwable e) {
						LogManager.errToLog("Fail upgrade project [" + lp.getProjectID() + "], exception : " + e.toString());
						continue;
					}
					
					//自动请求删除
					if(lp.getProjectID().equals(p_had.getProperty(HCjad.HAD_ID))){
						if(p_had.getProperty(HCjad.HAD_HAR_SIZE).equals("0")){
							log("remove deprecated project [" + lp.getProjectID() + "] by HAD size:0");
							removed.add(lp);
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
			
		}catch (Exception e) {
		}finally{
			boolean hasSucc = false;
			{
				Iterator<LinkProjectStore> it = downloadingLPS.iterator();
				while(it.hasNext()){
					LinkProjectStore lps = it.next();
					final String downloadingErr = lps.getDownloadingErr();
					if(downloadingErr != null && downloadingErr.length() == 0 && lps.getDownloadingPosition() > 0){
						hasSucc = true;
						break;
					}
				}
			}
			final JScrollPane sp = buildErroDownloadingPanel();
			final String op = LinkProjectPanel.getNewLinkedInProjOp();
			
			//物理删除过时工程
			if(removed.size() > 0){
				final int size = removed.size();
				for (int i = 0; i < size; i++) {
					LinkProjectStore lps = removed.elementAt(i);
					removeOnlyLPS(lps);
					removeLinkProjectPhic(lps, true);
				}
				updateToLinkProject();
			}
			if(op.equals(LinkProjectPanel.OP_ASK) && (hasSucc || sp != null)){
				JPanel jpanel = new JPanel(new GridBagLayout());
				final GridBagConstraints c = new GridBagConstraints();
				JLabel label = null;
				if(hasSucc){
					label = new JLabel("<html>System successful download new version project(s), apply now?(if no, then apply at next startup)" +
							"<br><br><strong>Note :</strong>if some project(s) works not properly, please restart HomeCenter server!</html>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);	
				}else{
					label = new JLabel("");
				}
				c.gridx = 0;
				c.gridy = 0;
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.LINE_START;
				
				jpanel.add(label, c);
				if(sp != null){
					JLabel er_label = new JLabel("Err donwload project(s):");
					JPanel errPanel = new JPanel(new GridBagLayout());
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
				final String title = hasSucc?"apply new version project(s)?":"Error upgrade project";
				App.showCenterPanel(jpanel, 0, 0, title, hasSucc, null, null, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						appNewLinkedInProjNow(downloadingLPS, true);
					}
				}, null, null, false, false, null, false, false);
			}else if(op.equals(LinkProjectPanel.OP_IMMEDIATE)){
				appNewLinkedInProjNow(downloadingLPS, true);
			}

			LinkProjectStatus.exitStatus();
			log("finish download and upgrading project(s) service.");
		}
		return true;
	}

	public static void loadHAD(final String upgradeurl, final Properties p_had) throws Exception {
		p_had.load(new URL(upgradeurl).openStream());
	}
	
	private static JScrollPane buildErroDownloadingPanel(){
		Iterator<LinkProjectStore> it = getLinkProjsIterator(true);
		Vector<LinkProjectStore> errLists = new Vector<LinkProjectStore>();
		while (it.hasNext()) {
			LinkProjectStore lps = it.next();
			final String downloadingErr = lps.getDownloadingErr();
			if(downloadingErr != null && downloadingErr.length() > 0){
				errLists.add(lps);
			}
		}
		
		final int rows = errLists.size();
		if(rows > 0){
			Object[][] playerInfo = new Object[rows][4];
			String[] Names = { "Project ID", "Desc", "Status", "Upgrade URL" };
			for (int i = 0; i < rows; i++) {
				final LinkProjectStore linkProjectStore = errLists.get(i);
				playerInfo[i][0] = linkProjectStore.getProjectID();
				playerInfo[i][1] = linkProjectStore.getProjectRemark();
				playerInfo[i][2] = linkProjectStore.getDownloadingErr();
				playerInfo[i][3] = linkProjectStore.getProjectUpgradeURL();
				linkProjectStore.resetDownloading();
			}
			JTable table = new JTable(playerInfo, Names);
			table.setPreferredScrollableViewportSize(new Dimension(600, 30));
			JScrollPane scrollPane = new JScrollPane(table);
			
			updateToLinkProject();
			
			return scrollPane;
		}
		return null;
	}
	
	public static void startLinkedInProjectUpgradeTimer(){
		HCTimer timer = new HCTimer("", HCTimer.ONE_DAY, true) {
			@Override
			public void doBiz() {
				new Thread(){
					public void run(){
						while(true){
							try{
								Thread.sleep(60 * 1000);
							}catch (Exception e) {
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
				}.start();
			}
		};
		timer.doBiz();
	}

	public static LinkProjectStore searchRoot(boolean mustActive){
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
	public static LinkProjectStore searchOtherActive(final LinkProjectStore lps){
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
	
	public static void appNewLinkedInProjNow(final Vector<LinkProjectStore> v, final boolean isServing){
		final Iterator<LinkProjectStore> it = v.iterator();
		
		boolean upgraded = false;
		while(it.hasNext()){
			LinkProjectStore lps = it.next();
			
			final String downloadingVer = lps.getDownloadingVer();
			if(!downloadingVer.equals(LinkProjectStore.DEFAULT_UNKOWN_VER) 
					&& StringUtil.higer(downloadingVer, lps.getVersion())
					&& lps.getDownloadingErr().length() == 0
					&& lps.getDownloadingPosition() > 0){
				if(upgraded == false){
					upgraded = true;
					
					if(isServing){
						//停止服务器
						log("stop service for upgrade project.");
						ServerUIUtil.promptAndStop(false, null);
					}
				}
				
				LinkProjectManager.removeLinkProjectPhic(lps, false);
				final File newVerHar = new File(LINK_UPGRADE_DIR, lps.getHarFile());

				log("Successful upgrade project [" + lps.getProjectID() + "] from " + lps.getVersion() + " to " + lps.getDownloadingVer());
				
				LinkProjectManager.importLinkProject(lps, newVerHar);

				newVerHar.delete();
				if(newVerHar.exists()){
					PropertiesManager.addDelFile(newVerHar);
				}
				lps.resetDownloading();
			}
		}
		if(upgraded){
			updateToLinkProject();
			if(isServing){
				ServerUIUtil.restartResponsorServerDelayMode();
			}
		}
	}
	
	private static void downloadHAR(final LinkProjectStore lps, final Properties p_had){
        final File file = new File(LINK_UPGRADE_DIR, lps.getHarFile());
        final String had_url = p_had.getProperty(HCjad.HAD_HAR_URL);
		final String url_har = (had_url!=null)?had_url:HCjad.convertToExtHar(lps.getProjectUpgradeURL());
		try {  
			final URL url = new URL(url_har);  
			if(p_had.getProperty(HCjad.HAD_VERSION).equals(lps.getDownloadingVer())){
				//继续下载
				downloadContinue(lps, p_had, file, url, file.length());
			}else{
	            long totalByted = Long.parseLong(p_had.getProperty(HCjad.HAD_HAR_SIZE));
                if(file.exists()){
                	file.delete();
                }
				RandomAccessFile raf = new RandomAccessFile(file, "rw");  
                raf.setLength(totalByted);  
                raf.close();  

                lps.setDownloadingVersion(p_had.getProperty(HCjad.HAD_VERSION));
	            downloadContinue(lps, p_had, file, url, totalByted);
			}
        } catch (Throwable e) { 
        	setError(lps, e.toString());
        	log("Error downloading HAR project : " + url_har);
        	e.printStackTrace();
        	
        	updateToLinkProject();
        }
	}

	private static void setError(final LinkProjectStore lps, String str) {
		lps.setDownloadingErr(str);
		lps.setDownloadingPosition(0);
		log("Project ID [" + lps.getProjectID() + "] exception:" + str);
	}
	
	private static void downloadContinue(final LinkProjectStore lps, final Properties p_had, 
			final File file, final URL url, final long end){
		RandomAccessFile raf = null;
		try {  
			raf = new RandomAccessFile(file, "rw");  

            int start = lps.getDownloadingPosition();
            lps.setDownloadingErr("");
            int tryNum = 0;
            int downloadBS = 0;
            //最多进行三次http异常
            final int maxTryNum = 3;
			while(tryNum < maxTryNum){
	            try{
		        	HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
		            conn.setRequestMethod("GET");  
//		            conn.setReadTimeout(0); 
					conn.setRequestProperty("Range", "bytes=" + start + "-" + end);  
		            if (conn.getResponseCode() == 206) {  
		                raf.seek(start + downloadBS);  
		                InputStream inStream = conn.getInputStream();  
	                    final int oneMS = 1024 * 1024;
		                byte[] b = new byte[1024 * 10];  
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
	            }catch (Exception e) {
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
            updateToLinkProject();
        } catch (Exception e) { 
        	setError(lps, e.toString());
        }finally{
        	if(raf != null){
        		try {
					raf.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }
	}
	
	private static void log(String msg){
		L.V = L.O ? false : LogManager.log("[ProjectManager] " + msg);
	}
	
	public static Iterator<LinkProjectStore> getLinkProjsIterator(final boolean includeRoot){
		if(includeRoot){
			return lpsVector.iterator();
		}else{
			return new IteratorWithoutRoot();
		}
	}
	
	public static final String getProjPaths(){
		final StringBuilder sb = new StringBuilder();
		Iterator<LinkProjectStore> it = lpsVector.iterator();
		boolean isAdded = false;
		while(it.hasNext()){
			LinkProjectStore lps = it.next();
			if(isAdded){
				sb.append(File.pathSeparator);
			}
			sb.append("." + File.separator + lps.getDeployTmpDir());
			isAdded = true;
		}
		return sb.toString();
	}

	public static final LinkProjectStore getProjByID(String context){
		final int size = lpsVector.size();
		for (int i = 0; i < size; i++) {
			LinkProjectStore tmp = lpsVector.elementAt(i);
			if(context == null && tmp.isRoot()){
				return tmp;
			}else if(tmp.getProjectID().equals(context)){
				return tmp;
			}
		}
		return null;
	}
	
	public static int getProjectSize(){
		return lpsVector.size();
	}
	
	public static final LinkProjectStore getProjLPSWithCreate(String id){
		final int size = lpsVector.size();
		LinkProjectStore lps = null;
		for (int i = 0; i < size; i++) {
			LinkProjectStore tmp = lpsVector.elementAt(i);
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
	
	public static File buildBackEditFile(final LinkProjectStore lps){
		return new File(lps.getHarParentDir(), "edit_" + lps.getHarFile());
	}
	
	public static final void saveToLinkBack(final LinkProjectStore lps, final File edit){
		ThirdlibManager.copy(edit, buildBackEditFile(lps));
	}

	public static final void copyCurrEditFromStorage(LinkProjectStore lps){
		ThirdlibManager.copy(buildBackEditFile(lps), new File(EDIT_HAR));
	}

	public static final void saveProjConfig(LinkProjectStore lps, final boolean isRoot, final boolean toActive){
		lps.setRoot(isRoot);
		lps.setActive(toActive);
		
		updateToLinkProject();
	}
	
	public static void updateToLinkProject() {
		final int size = lpsVector.size();
		Object[] objs = new Object[size];
		
		for (int i = 0; i < size; i++) {
			objs[i] = lpsVector.elementAt(i).toSerial();
		}
		
		PropertiesSet projIDSet = new PropertiesSet(PropertiesManager.S_LINK_PROJECTS);
		projIDSet.refill(objs);
		projIDSet.save();
	}


	public static String buildSysProjID() {
		return "Proj_" + ResourceUtil.getTimeStamp();
	}

	public static String deployToRandomDir(File har) {
		String randomShareFolder;
		//系统资源处于未拆分到随机目录下，需要重新读取并拆分
		randomShareFolder = ResourceUtil.createRandomFileNameWithExt(new File(CURRENT_DIR), "");
		Map<String, Object> map = HCjar.loadHar(har, true);
		ProjResponser.deloyToWorkingDir(map, new File(randomShareFolder));
		return randomShareFolder;
	}

	static{
		UpgradeManager.createLinkStoreDir();

		reloadLinkProjects();
		
		try{
			UpgradeManager.upgradeToLinkProjectsMode();
		}catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static boolean removeLinkProjectPhic(LinkProjectStore lps, boolean delPersistent){
			if(lps == null){
				lps = getProjByID(null);			
			}
			
			if(delPersistent){
				PropertiesManager.remove(PropertiesManager.p_PROJ_RECORD + lps.getProjectID());
			}
			
			//删除jar文件
			final File harFile = new File(lps.getHarParentDir(), lps.getHarFile());
	//			harFile.delete();
			PropertiesManager.addDelFile(harFile);
			PropertiesManager.addDelFile(buildBackEditFile(lps));
			
			//删除发布目录
	//		final File deployTmpDir = new File(lps.getDeployTmpDir());
	//		由于导入时，需要创建新TmpDir，不进行删除，可以防止新的TmpDir在极端情形下相同与本删除的。
	//		deployTmpDir.delete();
			final String deployTmpDir = lps.getDeployTmpDir();
			if(deployTmpDir.equals(LinkProjectStore.NO_DEPLOY_TMP_DIR) == false){
				PropertiesManager.addDelDir(deployTmpDir);	
			}
			
			PropertiesManager.saveFile();
			return true;
		}
	
	public static File getTempFileName(final String extFileName){
		if(TEMP_DIR.exists() == false){
			TEMP_DIR.mkdir();
		}
		final String fileName = ResourceUtil.createRandomFileNameWithExt(TEMP_DIR, extFileName);
		final File outTempFile = new File(TEMP_DIR, fileName);
		return outTempFile;
	}

	public static boolean importLinkProject(LinkProjectStore lps, final File sourceNewVer){
		final String fileName = ResourceUtil.createRandomFileNameWithExt(LINK_DIR, Designer.HAR_EXT);
	
		lps.setHarFile(fileName);
		lps.setHarParentDir(LINK_DIR_NAME);
	
		final File har = new File(LINK_DIR, fileName);
		ThirdlibManager.copy(sourceNewVer, har);
		ThirdlibManager.copy(sourceNewVer, buildBackEditFile(lps));
		
		String randomShareFolder = deployToRandomDir(har);
		lps.setDeployTmpDir(randomShareFolder);
		
		Map<String, Object> map = HCjar.loadHar(sourceNewVer, false);
		lps.copyFrom(map);
		
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