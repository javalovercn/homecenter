package hc.server.msb;

import java.util.ArrayList;

import hc.server.ui.design.BDNTree;
import hc.server.ui.design.ConverterTree;
import hc.server.ui.design.DevTree;
import hc.server.ui.design.MobiUIResponsor;
import hc.server.ui.design.ProjResponser;

public abstract class IoTSource {
	public abstract ArrayList<String> getProjectList();

	public abstract ArrayList<String> getRobotsByProjectID(String projID);

	public abstract ArrayList<DeviceBindInfo> getReferenceDeviceListByRobotName(String projID, String robotName) throws Exception;

	public abstract BDNTree buildTree(final MobiUIResponsor mobiResp) throws Exception ;
	
	public abstract ConverterTree buildConverterTree(final MobiUIResponsor mobiResp) throws Exception ;
	
	public abstract DevTree buildDevTree(final MobiUIResponsor mobiResp) throws Exception ;
	
	/**
	 * 可能返回null
	 * 
	 * @param projID
	 * @param robotName
	 * @param referenceDeviceID
	 * @return
	 */
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescByRobotName(String projID, String robotName,
			String referenceDeviceID) throws Exception;

	public abstract DataDeviceCapDesc getDataForDeviceCompatibleDesc(final ProjResponser pr, final DeviceCompatibleDescription devCompDesc);
	
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescByRobotToUserThread(final ProjResponser pr, final Robot r,
			final String referenceDeviceID);

	public abstract ArrayList<ConverterInfo> getConverterInAllProject() throws Exception;

	public abstract ArrayList<RealDeviceInfo> getRealDeviceInAllProject() throws Exception;

	public abstract RealDeviceInfo getRealDeviceBindInfo(String bind_id);

	public abstract ConverterInfo getConverterBindInfo(String bind_id);

	/**
	 * 指定工程下的Robot/ReferenceDevice之和
	 * 
	 * @param projID
	 * @return
	 */
	public int getTotalReferenceDeviceNumByProject(final String projID) throws Exception {
		final ArrayList<String> robotList = getRobotsByProjectID(projID);

		if (robotList.size() == 0) {
			return 0;
		}

		int totalReferDevNum = 0;
		for (int j = 0; j < robotList.size(); j++) {
			final String robotID = robotList.get(j);
			final ArrayList<DeviceBindInfo> refList = getReferenceDeviceListByRobotName(projID, robotID);
			totalReferDevNum += refList.size();
		}

		return totalReferDevNum;
	}
}
