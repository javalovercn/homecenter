package hc.server.util;

import hc.core.util.CCoreUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;

public class DelDeployedProjManager {
	static PropertiesSet deled = new PropertiesSet(PropertiesManager.S_DELED_DEPLOYED_PROJS);
	
	public static boolean isDeledDeployed(final String projectID){
		return deled.contains(projectID);
	}
	
	public static void addDeledDeployed(final String projectID){
		CCoreUtil.checkAccess();
		
		deled.appendItemIfNotContains(projectID);
		deled.save();
	}
}
