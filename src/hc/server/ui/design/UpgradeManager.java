package hc.server.ui.design;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import hc.core.util.HCURL;
import hc.server.ThirdlibManager;
import hc.server.ui.design.hpj.HCjar;
import hc.util.PropertiesManager;

/**
 * 数据结构及应用服务器的升级统一管理，而非Linked-In Project升级
 */
public class UpgradeManager {
	public static final File RUN_TEST_DIR = new File("runtest");

	public static void createRunTestDir() {
		if(RUN_TEST_DIR.exists() == false){
			RUN_TEST_DIR.mkdir();
		}
	}

	public static void createLinkStoreDir() {
		//创建存储各Link库的原始jar存放目录
		if(LinkProjectManager.LINK_DIR.exists() == false){
			LinkProjectManager.LINK_DIR.mkdir();
		}
	}

	public static void upgradeToLinkProjectsMode() {
		final File edit = Designer.switchHar(new File(Designer.OLD_EDIT_JAR), new File(LinkProjectManager.EDIT_HAR));
		File har = Designer.switchHar(new File(Designer.MY_DEPLOY_PROJ), 
				new File(Designer.MY_DEPLOY_PROJ_HAR));
	
		{
			Map<String, Object> map = HCjar.loadHar(har, false);
	
			//将ROOT改为原值
			LinkProjectStore root = LinkProjectManager.getProjByID(HCURL.ROOT_MENU);
			if(root != null){
				root.setProjectID((String)map.get(HCjar.PROJ_ID));
				root.setVersion((String)map.get(HCjar.PROJ_VER));
				root.setRoot(true);
				LinkProjectManager.updateToLinkProject();
			}
		
			//解决原遗留系统只有一个主工程时，不含Link Project的已发布数据的迁移
			if(PropertiesManager.isTrue(PropertiesManager.p_IsMobiMenu)){
				//已发布状态
				if(LinkProjectManager.getProjByID(null) == null){
					//新数据结构中，不含主工程时
					
					String randomShareFolder = PropertiesManager.getValue(PropertiesManager.p_DeployTmpDir);
					if(randomShareFolder != null){
						//系统资源已拆分到随机目录下的状态
						PropertiesManager.remove(PropertiesManager.p_DeployTmpDir);
					}
	
					//登记到新的存储数据结构上，关闭旧的数据结构
					LinkProjectStore lps = LinkProjectManager.getProjLPSWithCreate((String)map.get(HCjar.PROJ_ID));
					LinkProjectManager.importLinkProject(lps, har);
	
					LinkProjectManager.saveProjConfig(lps, true, true);
				}
			}
		}
		
		if(PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID) == null){
			if(edit.exists()){
				Map<String, Object> map = HCjar.loadHar(edit, false);
				Designer.recordEditProjInfo(map);
			}
		}
		
		Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);//必须为true，因为有可能正好编辑为非Root工程
		while(it.hasNext()){
			LinkProjectStore lps = it.next();
			File edit_file = LinkProjectManager.buildBackEditFile(lps);
			if(edit_file.exists() == false){
				ThirdlibManager.copy(new File(lps.getHarParentDir(), lps.getHarFile()), 
						edit_file);
			}
		}
	}

}
