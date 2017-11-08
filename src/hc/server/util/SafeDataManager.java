package hc.server.util;

import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.util.ResourceUtil;

import java.io.File;
import java.util.HashMap;

public class SafeDataManager {
	final static HashMap<String, BackupTree> projBackFileMap = new HashMap<String, BackupTree>(4);
	final static HashMap<String, HCTimer> projBackTask = new HashMap<String, HCTimer>(4);
	
	public static void romoveBackupTask(final String projectID){
		synchronized (projBackFileMap) {
			final HCTimer task = projBackTask.get(projectID);
			if(task != null){
				task.remove();
				projBackTask.remove(projectID);
				projBackFileMap.remove(projectID).isRemoved = true;
			}
		}
	}
	
	public static void addBackupTask(final String projectID){
//		synchronized (projBackFileMap) {
//			projBackFileMap.put(projectID, new BackupTree(projectID));
//			projBackTask.put(projectID, new HCTimer("backup", HCTimer.ONE_MINUTE * 10, true) {
//				@Override
//				public void doBiz() {
//					startBackupProcess(projectID);
//				}
//			});
//		}
	}

	static void startBackupProcess(final String projectID){
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				BackupTree toMap;
				synchronized (projBackFileMap) {
					toMap = projBackFileMap.get(projectID);
					if(toMap == null){
						return;
					}
				}
				copyDirAndSubs(toMap.src, toMap.projTempRoot, toMap, true);
			}
		});
	}
	
	private static boolean copyDirAndSubs(final File srcDir, final File toDir, final BackupTree lastModiMap, 
			final boolean isCopyNotCheck){
		boolean isChanged;
		
		final File[] list = srcDir.listFiles();
		if(list == null){
			isChanged = true;
//			lastModiMap. todo
			ResourceUtil.deleteDirectoryNowAndExit(toDir, true);
			return isChanged;
		}
		
		for (int i = 0; i < list.length; i++) {
			final File subItem = list[i];
			final File toSubItem = new File(toDir, subItem.getName());
			if(subItem.isDirectory()){
				toSubItem.mkdirs();
				isChanged = copyDirAndSubs(subItem, toSubItem, lastModiMap, isCopyNotCheck);
			}else{
				
			}
		}
		
		return false;
	}
}
