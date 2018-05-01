package hc.server.ui.design;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.ui.ServerUIUtil;
import hc.util.ResourceUtil;

public abstract class PlugSource {
	public static final int requMin = 200;
	
	public abstract void sendMessage(final String msg);
	public abstract void sendError(final String error);
	public abstract void sendWarn(final String warn);
	
	public abstract J2SESession getJ2SESession();
	
	public final String getLowDiskSpaceMessage() {
		return StringUtil.replace(ResourceUtil.get(getJ2SESession(), 9187), "{min}", String.valueOf(requMin));
	};
	
	public final String getSuccessMessage() {
		return ResourceUtil.get(getJ2SESession(), 9128);//9128=Successful add project(s)
	}
	
	public abstract void sendInitCheckActivingMessaeg();
	
	public abstract void enterMobileBindInSysThread(final MobiUIResponsor mobiResp, BindRobotSource bindSource);
	
	public final void initSessionContext(final ProjResponser[] appendRespNotAll) {
		final J2SESession j2seSession = getJ2SESession();
		if(j2seSession != null) {
			{
				for (int i = 0; i < appendRespNotAll.length; i++) {
					final ProjResponser pr = appendRespNotAll[i];

					pr.initSessionContext(j2seSession);//供device input token
				}
			}
		}
	}
	
	/**
	 * 本方法内异常会被拦截，并转显到手机
	 * <BR><BR>
	 * 此逻辑被QR_Add和Local_Deploy使用
	 * @param fileHar
	 * @param map
	 * @param isInstallFromClient true means installed by client QR or WiFi, false means by localNetDeploy
	 * @return true isNeedCloseLicenseMlet
	 */
	public final void startPlugForQROrLocalDeployInSysThread(final File fileHar, final Map<String, Object> map, final boolean isInstallFromClient)
			throws Exception {
		
		final boolean isUpgrade = false;
		final File oldBackEditFile = null;
		
		final ArrayList<LinkProjectStore> appendLPS = AddHarHTMLMlet.appendMapToSavedLPSInSysThread(this, fileHar, map, false, isUpgrade, oldBackEditFile);
		LinkProjectManager.reloadLinkProjects();// 十分重要

		final MobiUIResponsor mobiResp = (MobiUIResponsor) ServerUIUtil.getResponsor();
		
		if(mobiResp == null || (mobiResp instanceof MobiUIResponsor) == false) {
			sendError("please active one HAR project on target server at least!");
			return;
		}

		final ProjResponser[] appendRespNotAll = mobiResp.appendNewHarProject(isInstallFromClient);// 仅扩展新工程，不启动或不运行

		initSessionContext(appendRespNotAll);//供device input token

		sendInitCheckActivingMessaeg();

		BindRobotSource bindSource = mobiResp.bindRobotSource;
		if (bindSource == null) {
			bindSource = new BindRobotSource(mobiResp);
		}

		if (BindManager.findNewUnbind(bindSource)) {// 进行自动绑定
			// 提前释放可能的异常
			bindSource.getRealDeviceInAllProject();
			bindSource.getConverterInAllProject();

			//进入手机的人工绑定界面
			enterMobileBindInSysThread(mobiResp, bindSource);
		}

		mobiResp.fireSystemEventListenerOnAppendProject(appendRespNotAll, appendLPS);// 进入运行状态

		final ProjResponser resp = mobiResp.findContext(mobiResp.findRootContextID());//添加菜单到主工程
		final JarMainMenu linkMenu = resp.jarMainMenu;
		linkMenu.appendProjToMenuItemArray(mobiResp.maps, mobiResp.responserSize, appendLPS);
		// final String menuData = linkMenu.buildJcip(coreSS);
		//
		// {
		// final String projID = resp.context.getProjectID();
		// final byte[] projIDbs = StringUtil.getBytes(projID);
		// final String softUID =
		// UserThreadResourceUtil.getMobileSoftUID(coreSS);
		// final byte[] softUidBS = ByteUtil.getBytes(softUID, IConstant.UTF_8);
		// final String urlID = CacheManager.ELE_URL_ID_MENU;
		// final byte[] urlIDbs = CacheManager.ELE_URL_ID_MENU_BS;
		//
		// final CacheComparator menuRefreshComp = new CacheComparator(projID,
		// softUID, urlID, projIDbs, softUidBS, urlIDbs) {
		// @Override
		// public void sendData(final Object[] paras) {
		// coreSS.context.send(MsgBuilder.E_MENU_REFRESH, menuData);
		//// HCURLUtil.sendEClass(HCURLUtil.CLASS_BODY_TO_MOBI, bodyBS, 0,
		// bodyBS.length);
		// }
		// };
		// final byte[] data = StringUtil.getBytes(menuData);
		//
		// //注意：不能走cache，因为历史安装过，可能存在相同cache，导致生成一个新Menu
		// menuRefreshComp.encodeGetCompare(coreSS, false, data, 0, data.length,
		// null);
		// }

		LogManager.log("successful apply added project to ACTIVE status.");
	}
}
