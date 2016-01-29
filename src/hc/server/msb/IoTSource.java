package hc.server.msb;

import hc.server.ui.design.ProjResponser;

import java.util.ArrayList;

public abstract class IoTSource {
	public abstract ArrayList<String> getProjectList();
	
	public abstract ArrayList<String> getRobotsByProjectID(String projID);
	
	public abstract ArrayList<DeviceBindInfo> getReferenceDeviceListByRobotName(String projID, String robotName) throws Exception;
	
	/**
	 * 可能返回null
	 * @param projID
	 * @param robotName
	 * @param referenceDeviceID
	 * @return
	 */
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescByRobotName(String projID, String robotName, 
			String referenceDeviceID) throws Exception;
	
	public abstract void getDeviceCompatibleDescByDevice(final ProjResponser pr, final RealDeviceInfo deviceInfo);
	
	public abstract void getConverterDescUpDown(final ProjResponser pr, final ConverterInfo converterInfo);
	
	public abstract DeviceCompatibleDescription getDeviceCompatibleDescByRobot(final ProjResponser pr, final Robot r, final String referenceDeviceID);
	
	/**
	 * @param pr
	 * @param devCompDesc 支持null对象
	 * @return
	 */
	public abstract DataDeviceCapDesc getDataForDeviceCompatibleDesc(final ProjResponser pr, final DeviceCompatibleDescription devCompDesc);
	
	public abstract ArrayList<ConverterInfo> getConverterInAllProject() throws Exception;
	
	public abstract ArrayList<RealDeviceInfo> getRealDeviceInAllProject() throws Exception;
	
	public abstract RealDeviceInfo getRealDeviceBindInfo(String bind_id);
	
	public abstract ConverterInfo getConverterBindInfo(String bind_id);
	
	/**
	 * 指定工程下的Robot/ReferenceDevice之和
	 * @param projID
	 * @return
	 */
	public int getTotalReferenceDeviceNumByProject(final String projID) throws Exception{
		final ArrayList<String> robotList = getRobotsByProjectID(projID);
		
		if(robotList.size() == 0){
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
