package hc.server.ui.design;

import hc.server.msb.ConverterInfo;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.NameMapper;
import hc.server.msb.RealDeviceInfo;
import hc.server.msb.WorkingDeviceList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class IoTNameMapper extends NameMapper{
	boolean isFirstLoad = true;
	
	public IoTNameMapper(){
		reloadMap();
	}

	@Override
	public final void reloadMap() {
		if(isFirstLoad){
			isFirstLoad = false;
			final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIteratorInUserSysThread(true);
			while(it.hasNext()){
				final LinkProjectStore lps = it.next();
				if(lps.isActive()){
					appendBindToNameSet(lps);
				}
			}
		}else{
			final IoTNameMapper newMap = new IoTNameMapper();
			searchBindIDFromDevice = newMap.searchBindIDFromDevice;
			bind2RealDeviceBindInfo = newMap.bind2RealDeviceBindInfo;
			bind2ConverterBindInfo = newMap.bind2ConverterBindInfo;
			bind2ReferID = newMap.bind2ReferID;
		}
	}

	@Override
	public final void appendBindToNameSet(LinkProjectStore lps) {
		Object[] objs = lps.getDevBindMap();
		if(objs == null){//add har by QRCode时，自动绑定后，参数lps过时
			final LinkProjectStore newLPS = LinkProjectManager.getProjByID(lps.getProjectID());
			if (newLPS != null && newLPS != lps){
				lps = newLPS;
			}
			objs = lps.getDevBindMap();
		}
		if(objs != null){
			final String[] binds = (String[])objs[0];
			final RealDeviceInfo[] rdbis = (RealDeviceInfo[])objs[1];
			
			for (int i = 0; i < rdbis.length; i++) {
				final RealDeviceInfo rdbi = rdbis[i];
				HashMap<String, HashMap<String, Vector<String>>> devProjID = searchBindIDFromDevice.get(rdbi.proj_id);
				if(devProjID == null){
					devProjID = new HashMap<String, HashMap<String,Vector<String>>>();
					searchBindIDFromDevice.put(rdbi.proj_id, devProjID);
				}
				
				HashMap<String, Vector<String>> devDevName = devProjID.get(rdbi.dev_name);
				if(devDevName == null){
					devDevName = new HashMap<String, Vector<String>>();
					devProjID.put(rdbi.dev_name, devDevName);
				}
				
				Vector<String> bindid_set = devDevName.get(rdbi.dev_id);
				if(bindid_set == null){
					bindid_set = new Vector<String>();
					devDevName.put(rdbi.dev_id, bindid_set);
				}
				
				final String bind_id = binds[i];

				bindid_set.add(bind_id);
				
				bind2RealDeviceBindInfo.put(bind_id, rdbi);
				bind2ReferID.put(bind_id, DeviceBindInfo.decodeReferIDFromBindID(bind_id));
			}
		}
		
		objs = lps.getConvBindMap();
		if(objs != null){
			final String[] binds = (String[])objs[0];
			final ConverterInfo[] convs = (ConverterInfo[])objs[1];
			
			for (int j = 0; j < binds.length; j++) {
				bind2ConverterBindInfo.put(binds[j], convs[j]);
			}
		}
	}
	
//	final HashMap<String, DeviceList> workingDeviceList = new HashMap<String, DeviceList>(4);

	@Override
	public WorkingDeviceList getWorkingDeviceList(final String projectID) {
		final HashMap<String, HashMap<String, Vector<String>>> devProjID = searchBindIDFromDevice.get(projectID);
		if(devProjID == null){
			return new WorkingDeviceList();
		}
		
		final Vector<String> list = new Vector<String>(devProjID.keySet());
		return new WorkingDeviceList(list);
	}
	
//	public String searchBindIDFromDevice(final String proj_id, final String devName, final String devID){
//		return searchBindIDFromDevice.get(proj_id).get(devName).get(devID);
//	}
	
}
