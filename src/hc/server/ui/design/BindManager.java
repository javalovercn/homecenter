package hc.server.ui.design;

import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.msb.ConverterInfo;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.DeviceMatchManager;
import hc.server.msb.IoTSource;
import hc.server.msb.MSBAgent;
import hc.server.msb.RealDeviceInfo;
import hc.server.msb.RobotReferBindInfo;
import hc.server.ui.design.hpj.HCjarHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class BindManager {
	public static Set<LinkProjectStore> getNoBindedProjSet(){
		CCoreUtil.checkAccess();
		
		final HashSet<LinkProjectStore> set = new HashSet<LinkProjectStore>();
		
		final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(true);
		while(it.hasNext()){
			final LinkProjectStore lps = it.next();
			if(lps.isActive() && (lps.isDoneBind() == false)){
				set.add(lps);
			}
		}
		
		return set;
	}
	
	public static boolean hasProjNotBinded(){
		return getNoBindedProjSet().size() > 0;
	}
	
	public static boolean checkSrcOnRealDeviceBindInfo(final MobiUIResponsor mobiResp, final RealDeviceInfo rdbi){
		if(rdbi == null){
			return false;
		}
		
		final ProjResponser pr = mobiResp.getProjResponser(rdbi.proj_id);
		if(pr == null){
			return false;
		}
		
		final Vector<String>[] vectors = HCjarHelper.getDevicesSrc(pr.map);
		final Vector<String> names = vectors[0];
		
		return names.contains(rdbi.dev_name);
	}
	
	public static boolean checkSrcOnConverterBindInfo(final MobiUIResponsor mobiResp, final ConverterInfo cbi){
		if(cbi == null){
			return false;
		}
		
		final ProjResponser pr = mobiResp.getProjResponser(cbi.proj_id);
		if(pr == null){
			return false;
		}
		
		final Vector<String>[] vectors = HCjarHelper.getConvertersSrc(pr.map);
		final Vector<String> names = vectors[0];
		
		return names.contains(cbi.name);
	}
	
	/**
	 * 如果发现未绑定或新变动的工程，会尝试自动绑定，并返回最终结果
	 * @param bindSource
	 * @return true:还有尚未绑定的工程; false：全部完成绑定
	 * @throws Exception
	 */
	public static boolean findNewUnbind(final BindRobotSource bindSource) throws Exception{
		final MobiUIResponsor respo = bindSource.respo;
		
		boolean isNewUnbind = false;
		boolean isModiLPS = false;
		
		final Iterator<LinkProjectStore> it = getNoBindedProjSet().iterator();//因为 新添加或升级的工程DoneBind=false
		while(it.hasNext()){
			final LinkProjectStore check_lps = it.next();
			final String projID = check_lps.getProjectID();
			final Map<String, Object> harMap = respo.getHarMap(projID);
			final Object[] oldStoreBindDevMap = check_lps.getDevBindMap();
			
			if(bindSource.getTotalReferenceDeviceNumByProject(projID) == 0){
				if(oldStoreBindDevMap != null){
					//清空旧的
					check_lps.clearBindMap();
					check_lps.setDoneBind(true);
					isModiLPS = true;
				}
			}else{
				if(oldStoreBindDevMap == null){
					//未建绑定，需新建绑定
					isNewUnbind = true;
					continue;
				}
				
				final String[] oldStoreRefBindIDs = (String[])oldStoreBindDevMap[0];

				try{
					boolean isDiff = false;
					
					final int newRobotsNum = HCjarHelper.getRobotNum(harMap);

					//检查工程的Robot/BindID是否在lps中是否存在
					for (int i = 0; i < newRobotsNum && isDiff == false; i++) {
						final ArrayList<DeviceBindInfo> refDevs = bindSource.getReferenceDeviceListByRobotName(projID, HCjarHelper.getRobotNameAtIdx(harMap, i));
						final int sizeBind = refDevs.size();
						
						
						for (int j = 0; j < sizeBind; j++) {
							boolean isFindExited = false;
							final DeviceBindInfo deviceBindInfo = refDevs.get(j);
							final String newBindID = deviceBindInfo.bind_id;
							
							for (int k = 0; k < oldStoreRefBindIDs.length; k++) {
								if(newBindID.equals(oldStoreRefBindIDs[k])){
									isFindExited = true;
									break;
								}
							}
							
							if(isFindExited == false){//新的未绑定BindID
								isDiff = true;
								break;
							}
						}
					}
					
					if(isDiff == false){
						//检查lps中的引用Dev工程是否存在
						final RealDeviceInfo[] rdbis = (RealDeviceInfo[])oldStoreBindDevMap[1];
						if(rdbis != null){
							for (int i = 0; i < rdbis.length; i++) {
								final RealDeviceInfo rdbi = rdbis[i];
								final String rd_proj_id = rdbi.proj_id;
								final LinkProjectStore dev_lps = LinkProjectManager.getProjByID(rd_proj_id);
								if(dev_lps == null || dev_lps.isActive() == false){
									//依赖的Dev工程不存在或不是Active
									L.V = L.O ? false : LogManager.log("Project [" + rd_proj_id + "] is not exists or not active, which is relied by Project[" + projID + "].");
									isDiff = true;
									break;
								}
								
								//检查Dev是否存在
								if(checkSrcOnRealDeviceBindInfo(respo, rdbi) == false){
									L.V = L.O ? false : LogManager.log("Device[" + rdbi.dev_name + "] in Project [" + rd_proj_id + "] is not exists , which is relied by Project[" + projID + "].");
									isDiff = true;
									break;
								}
							}
						}
					}
					
					if(isDiff == false){
						final Object[] oldConvBindMap = check_lps.getConvBindMap();
						
						//不需要检查bindID，而只要检查Conv所在工程是否存在或active
						final ConverterInfo[] oldConvBindInfo = (ConverterInfo[])oldConvBindMap[1];
						if(oldConvBindInfo != null){
							for (int i = 0; i < oldConvBindInfo.length; i++) {
								final ConverterInfo cbi = oldConvBindInfo[i];
								final String cb_proj_id = cbi.proj_id;
								final LinkProjectStore dev_lps = LinkProjectManager.getProjByID(cb_proj_id);
								if(dev_lps == null || dev_lps.isActive() == false){
									//依赖Conv所在的工程不存在或不是Active
									L.V = L.O ? false : LogManager.log("Project [" + cb_proj_id + "] is not exists or not active, which is relied by Project[" + projID + "].");
									isDiff = true;
									break;
								}
								
								//检查Conv是否存在
								if(checkSrcOnConverterBindInfo(respo, cbi) == false){
									L.V = L.O ? false : LogManager.log("Converter[" + cbi.name + "] in Project [" + cb_proj_id + "] is not exists , which is relied by Project[" + projID + "].");
									isDiff = true;
									break;
								}
							}
						}
					}
					
					if(isDiff){
						isNewUnbind = true;
						continue;
					}else{
						//工程修改，但未增加新的BindID，故不需要绑定
						check_lps.setDoneBind(true);
						isModiLPS = true;
					}
				}catch (final Throwable e) {
					e.printStackTrace();
				}
			}
		}
		
		if(isNewUnbind){
			isNewUnbind = BindManager.autoBind(bindSource);
			LinkProjectManager.updateToLinkProject();
		}
		
		//保存，更新
		if(isNewUnbind == false && isModiLPS){
			//工程修改，但未增加新的BindID
			LinkProjectManager.updateToLinkProject();
		}
		
		return isNewUnbind;
	}

	private static final ConverterInfo searchConverterForCompatible(final BindRobotSource source, final ProjResponser pr,
			final DeviceCompatibleDescription compDesc, final ArrayList<ConverterInfo> cbi){
		if(compDesc == null){
			return null;
		}
		
		final String[] source_compatibleItem = MSBAgent.getCompatibleItem(compDesc);
	
		final int size = cbi.size();
		for (int i = 0; i < size; i++) {
			final ConverterInfo cInfo = cbi.get(i);
			source.getConverterDescUpDown(pr, cInfo);
			final boolean match = DeviceMatchManager.match(
					source_compatibleItem, MSBAgent.getCompatibleItem(cInfo.upDeviceCompatibleDescriptionCache));
			if(match){
				return cInfo;
			}
		}
		return null;
	}

	private static final RealDeviceInfo searchDeviceForCompatible(final BindRobotSource source,  final ProjResponser pr,
			final DeviceCompatibleDescription compDesc, final ArrayList<RealDeviceInfo> rdbi){
		if(compDesc == null){
			return null;
		}
		
		final String[] source_compatibleItem = MSBAgent.getCompatibleItem(compDesc);
	
		final int size = rdbi.size();
		for (int i = 0; i < size; i++) {
			final RealDeviceInfo rdi = rdbi.get(i);
			source.getDeviceCompatibleDescByDevice(pr, rdi);
			final boolean match = DeviceMatchManager.match(
					source_compatibleItem, MSBAgent.getCompatibleItem(rdi.deviceCompatibleDescriptionCache));
			if(match){
				return rdi;
			}
		}
		
		return null;
	}

	/**
		 * 
		 * @param bindSource
		 * @return true : need bind by hand (isNotFullBinded), false : auto finish binding.
		 */
		public static boolean autoBind(final BindRobotSource bindSource){
			boolean isNotFullBinded = false;
			
			ArrayList<RealDeviceInfo> rdbi = null;
			ArrayList<ConverterInfo> cbi = null;
			try{
				rdbi = bindSource.getRealDeviceInAllProject();
				cbi = bindSource.getConverterInAllProject();
			}catch (final Exception e) {
				e.printStackTrace();
				return true;
			}
			
			final Set<LinkProjectStore> set = getNoBindedProjSet();
			final Iterator<LinkProjectStore> itlps = set.iterator();
			while(itlps.hasNext()){
				boolean isRobotNotBind = false;
				boolean hasDeviceReference = false;
				final LinkProjectStore lps = itlps.next();
				final String projectID = lps.getProjectID();
				final ArrayList<String> robots = bindSource.getRobotsByProjectID(projectID);
				final int size = robots.size();
				final Vector<String> dev_id_vector = new Vector<String>();
				final Vector<String> conv_id_vector = new Vector<String>();
				final Vector<BindDeviceNode> bind_vector = new Vector<BindDeviceNode>();
				
				//将旧的绑定保留
				final Object[] oldDevBindMap = lps.getDevBindMap();
				if(oldDevBindMap != null){
					for (int i = 0; i < oldDevBindMap.length; i++) {
						final String bind_id = (String)oldDevBindMap[0];
						
						//Robot下的ref_id是否还存在使用
						final RobotReferBindInfo rrbi = DeviceBindInfo.decodeReferIDFromBindID(bind_id);
						if(searchReferenceIDOfRobot(bindSource, projectID, rrbi, bind_id) == false){
							L.V = L.O ? false : LogManager.log("remove old bind [" + bind_id + "], because Robot [" + rrbi.robot_name + "] in project [" + projectID + "] or ref ID [" + rrbi.refer_id + "] is not exists.");
							continue;
						}
						
						final RealDeviceInfo rdi = (RealDeviceInfo)oldDevBindMap[1];
						
						//检查Device或real_device_id是否还存在
						if(searchRealDeviceInfo(rdbi, rdi) == false){
							L.V = L.O ? false : LogManager.log("remove old bind [" + bind_id + "], because Device [" + rdi.dev_name + "] or real device ID [" + rdi.dev_id + "] is not exists.");
							continue;
						}
						
						final BindDeviceNode bdn = new BindDeviceNode(rdi, null);
						dev_id_vector.add(bind_id);
						bind_vector.add(bdn);
					}
				}
				final Object[] oldConvBindMap = lps.getConvBindMap();
				if(oldConvBindMap != null){
					for (int i = 0; i < oldConvBindMap.length; i++) {
						final String bind_id = (String)oldConvBindMap[0];
						final ConverterInfo cInfo = (ConverterInfo)oldConvBindMap[1];
						final int idx = dev_id_vector.indexOf(bind_id);
						
						//有可能因驱动变动，而导致旧绑定失效，所以要先检查可用性
						if(idx >= 0){
							if(searchConverterInfo(cbi, cInfo) == false){
								dev_id_vector.remove(idx);
								bind_vector.remove(idx);
								
								L.V = L.O ? false : LogManager.log("remove old bind [" + bind_id + "], because Converter is not exists.");
								continue;
							}
							
							conv_id_vector.add(bind_id);
							bind_vector.get(idx).convBind = cInfo;
						}
					}
				}
				
				for (int i = 0; i < size; i++) {
					final String robotName = robots.get(i);
					
					try{
						final ArrayList<DeviceBindInfo> robot_dbi_set = bindSource.getReferenceDeviceListByRobotName(projectID, robotName);
						final int sizeRobotDBI = robot_dbi_set.size();
						final ProjResponser pr = bindSource.respo.getProjResponser(projectID);
						for (int j = 0; j < sizeRobotDBI; j++) {
							final DeviceBindInfo robot_dbinfo = robot_dbi_set.get(j);
							if(hasDeviceReference == false){
								hasDeviceReference = true;
								L.V = L.O ? false : LogManager.log("try auto-match for project [" + projectID + "]...");
							}
							
							final String bind_id = robot_dbinfo.bind_id;
							if(dev_id_vector.indexOf(bind_id) >= 0){
								L.V = L.O ? false : LogManager.log("find exists binds [" + bind_id + "] in project [" + projectID + "], reuse old bind.");
								continue;
							}
							
							final String ref_dev_id = robot_dbinfo.ref_dev_id;
							final DeviceCompatibleDescription compDesc = bindSource.getDeviceCompatibleDescByRobotName(projectID, robotName, ref_dev_id);
							pr.threadPool.runAndWait(new ReturnableRunnable() {
								@Override
								public Object run() {
									MSBAgent.getCompatibleItem(compDesc);//初始化
									return null;
								}
							});
							
							//先直接从设备集中找匹配的设备
							final RealDeviceInfo rdi = searchDeviceForCompatible(bindSource, pr, compDesc, rdbi);
							if(rdi != null){
								//成功找到匹配设备
								dev_id_vector.add(bind_id);
								bind_vector.add(new BindDeviceNode(rdi, null));
								L.V = L.O ? false : LogManager.log("success auto-match Robot<->Device as [" + ref_dev_id + "]<->[" + rdi.toString() + "]");
								continue;
							}else{
								//从转换器中upCap的转换器
								final ConverterInfo cInfo = searchConverterForCompatible(bindSource, pr, compDesc, cbi);
								if(cInfo != null){
									//找到匹配upCap转换器
									
									//找downCap的设备
									final RealDeviceInfo realDevBind = searchDeviceForCompatible(bindSource, pr, cInfo.downDeviceCompatibleDescriptionCache, rdbi);
									if(realDevBind != null){
										dev_id_vector.add(bind_id);
										conv_id_vector.add(bind_id);
										bind_vector.add(new BindDeviceNode(realDevBind, cInfo));
										L.V = L.O ? false : LogManager.log("success auto-match {Robot~Converter~Device} => { [" + ref_dev_id + "] ~ [" + cInfo.toString() + "] ~ [" + realDevBind.toString() + "] }");
										continue;
									}else{
										isRobotNotBind = true;
									}
								}else{
									isRobotNotBind = true;
								}
							}
						}
					}catch (final Exception e) {
						e.printStackTrace();
						continue;
					}
				}
	//			final bindSource.get
				if(hasDeviceReference){
					if(isRobotNotBind == false){
						AddHarHTMLMlet.updateOneProjBindToLPS(projectID, dev_id_vector, conv_id_vector, bind_vector);
						lps.setDoneBind(true);
						L.V = L.O ? false : LogManager.log("success finish auto-bind for project [" + projectID + "]");
					}else{
						isNotFullBinded = true;
					}
				}else{
					L.V = L.O ? false : LogManager.log("set done-bind for NO-ROBOT project [" + projectID + "]");
					lps.setDoneBind(true);
				}
			}
			
			return isNotFullBinded;
		}

	private static boolean searchRealDeviceInfo(final ArrayList<RealDeviceInfo> rdbi, final RealDeviceInfo rdi) {
		final int size = rdbi.size();
		for (int j = 0; j < size; j++) {
			if(rdbi.get(j).equals(rdi)){
				return true;
			}
		}
		return false;
	}
	
	private static boolean searchConverterInfo(final ArrayList<ConverterInfo> c_list, final ConverterInfo cInfo) {
		final int size = c_list.size();
		for (int j = 0; j < size; j++) {
			if(c_list.get(j).equals(cInfo)){
				return true;
			}
		}
		return false;
	}

	private static boolean searchReferenceIDOfRobot(final BindRobotSource bindSource, final String projectID, 
			final RobotReferBindInfo rrbi, final String search_bind_id) {
		try{
			final ArrayList<DeviceBindInfo> currDBI = bindSource.getReferenceDeviceListByRobotName(projectID, rrbi.robot_name);
			for (int j = 0; j < currDBI.size(); j++) {
				if(currDBI.get(j).bind_id.equals(search_bind_id)){
					return true;
				}
			}
		}catch (final Exception e) {
		}
		return false;
	}

	/**
	 * 返回全部的projectID
	 * @param rs
	 * @return
	 */
	public static ArrayList<String> getProjectIDList(final IoTSource rs) {
		return rs.getProjectList();
	}

	public static int getTotalReferenceDeviceNumByProject(final IoTSource ioTSource, final String projID)
			throws Exception {
		return ioTSource.getTotalReferenceDeviceNumByProject(projID);
	}

	/**
	 * 返回指定projectID下的全部Robot的robotName
	 * @param ioTSource
	 * @param projID
	 * @return
	 */
	public static ArrayList<String> getRobotsByProjectID(final IoTSource ioTSource, final String projID) {
		return ioTSource.getRobotsByProjectID(projID);
	}

	public static BindDeviceNode buildDataNodeForProject(final MobiUIResponsor resp, final String projID) {
		return new BindDeviceNode(resp, BindDeviceNode.PROJ_NODE, projID, "", null, null);
	}

	public static ArrayList<DeviceBindInfo> getReferenceDeviceListByRobotName(
			final IoTSource ioTSource, final String projID, final String robotID) throws Exception {
		return ioTSource.getReferenceDeviceListByRobotName(projID, robotID);
	}

	public static BindDeviceNode buildDataNodeForRobot(final MobiUIResponsor resp, final String projID,
			final String robotID) {
		return new BindDeviceNode(resp, BindDeviceNode.ROBOT_NODE, projID, robotID, null, null);
	}

	public static BindDeviceNode buildDataNodeForRefDevInRobot(final MobiUIResponsor resp, final String projID,
			final String robotID, final DeviceBindInfo di, final IoTSource iotSource) {
		return new BindDeviceNode(resp, BindDeviceNode.REAL_DEV_ID_NODE, projID, robotID, di, iotSource);
	}

}