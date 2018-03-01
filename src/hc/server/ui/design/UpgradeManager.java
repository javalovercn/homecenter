package hc.server.ui.design;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import hc.core.util.HCURL;
import hc.server.ThirdlibManager;
import hc.server.ui.design.hpj.HCjar;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

/**
 * 数据结构及应用服务器的升级统一管理，而非Linked-In Project升级
 */
public class UpgradeManager {
	private final static Pattern schedulerPackagePattern = Pattern
			.compile("hc\\.server\\.util\\.scheduler\\.");

	public static String preProcessScript(final String script) {
		return schedulerPackagePattern.matcher(script).replaceAll("hc.server.util.calendar.");
	}

	public static void upgradeToLinkProjectsMode() {
		final File edit = Designer.switchHar(
				new File(ResourceUtil.getBaseDir(), Designer.OLD_EDIT_JAR),
				new File(ResourceUtil.getBaseDir(), LinkProjectManager.EDIT_HAR));
		final File har = Designer.switchHar(
				new File(ResourceUtil.getBaseDir(), Designer.MY_DEPLOY_PROJ),
				new File(ResourceUtil.getBaseDir(), Designer.MY_DEPLOY_PROJ_HAR));

		{
			final Map<String, Object> map = (har.exists() ? HCjar.loadHar(har, false)
					: new HashMap<String, Object>());// loadHar会产生异常信息，故先行判断

			// 将ROOT改为原值
			final LinkProjectStore root = LinkProjectManager.getProjByID(HCURL.ROOT_MENU);
			if (root != null) {
				root.setProjectID((String) map.get(HCjar.PROJ_ID));
				root.setVersion((String) map.get(HCjar.PROJ_VER));
				root.setRoot(true);
				LinkProjectManager.updateToLinkProject();
			}
		}

		if (PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID) == null) {
			if (edit.exists()) {
				final Map<String, Object> map = HCjar.loadHar(edit, false);
				Designer.recordEditProjInfo(map);
			}
		}

		final Iterator<LinkProjectStore> it = LinkProjectManager
				.getLinkProjsIteratorInUserSysThread(true);// 必须为true，因为有可能正好编辑为非Root工程
		while (it.hasNext()) {
			final LinkProjectStore lps = it.next();
			final File edit_file = LinkProjectManager.buildBackEditFile(lps);
			if (edit_file.exists() == false) {
				ThirdlibManager
						.copy(new File(new File(ResourceUtil.getBaseDir(), lps.getHarParentDir()),
								lps.getHarFile()), edit_file);
			}
		}
	}

}
