package hc.server.ui.design;

import hc.core.util.HCURL;
import hc.server.ThirdlibManager;
import hc.server.ui.design.hpj.HCjar;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 数据结构及应用服务器的升级统一管理，而非Linked-In Project升级
 */
public class UpgradeManager {

	public static void upgradeToLinkProjectsMode() {
		final File edit = Designer.switchHar(new File(ResourceUtil.getBaseDir(), Designer.OLD_EDIT_JAR), new File(ResourceUtil.getBaseDir(), LinkProjectManager.EDIT_HAR));
		final File har = Designer.switchHar(new File(ResourceUtil.getBaseDir(), Designer.MY_DEPLOY_PROJ), 
				new File(ResourceUtil.getBaseDir(), Designer.MY_DEPLOY_PROJ_HAR));
	
		{
			final Map<String, Object> map = (har.exists()?HCjar.loadHar(har, false):new HashMap<String, Object>());//loadHar会产生异常信息，故先行判断
	
			//将ROOT改为原值
			final LinkProjectStore root = LinkProjectManager.getProjByID(HCURL.ROOT_MENU);
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
					
					final String randomShareFolder = PropertiesManager.getValue(PropertiesManager.p_DeployTmpDir);
					if(randomShareFolder != null){
						//系统资源已拆分到随机目录下的状态
						PropertiesManager.remove(PropertiesManager.p_DeployTmpDir);
					}
	
					//登记到新的存储数据结构上，关闭旧的数据结构
					final LinkProjectStore lps = LinkProjectManager.getProjLPSWithCreate((String)map.get(HCjar.PROJ_ID));
					LinkProjectManager.importLinkProject(lps, har, false, null);
	
					LinkProjectManager.saveProjConfig(lps, true, true);
				}
			}
		}
		
		if(PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID) == null){
			if(edit.exists()){
				final Map<String, Object> map = HCjar.loadHar(edit, false);
				Designer.recordEditProjInfo(map);
			}
		}
		
		final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);//必须为true，因为有可能正好编辑为非Root工程
		while(it.hasNext()){
			final LinkProjectStore lps = it.next();
			final File edit_file = LinkProjectManager.buildBackEditFile(lps);
			if(edit_file.exists() == false){
				ThirdlibManager.copy(new File(new File(ResourceUtil.getBaseDir(), lps.getHarParentDir()), lps.getHarFile()), 
						edit_file);
			}
		}
	}

}
