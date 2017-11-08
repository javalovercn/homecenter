package hc.server.util;

import hc.server.data.StoreDirManager;

import java.io.File;
import java.util.HashMap;

public class BackupTree {
	final HashMap<File, File[]> items = new HashMap<File, File[]>(32);
	final HashMap<File, Long> lastModiMS = new HashMap<File, Long>(32);
	final File projBackRoot, projTempRoot, projStoreRoot, src;
	public boolean isRemoved;
	
	BackupTree(final String projectID){
		projBackRoot = new File(StoreDirManager.SAFE_DATA_DIR, projectID);
		projTempRoot = new File(projBackRoot, StoreDirManager.BACKUP_SUB_TEMP);
		projStoreRoot = new File(projBackRoot, StoreDirManager.BACKUP_SUB_STORE);
		
		projTempRoot.mkdirs();
		projStoreRoot.mkdirs();
		
		src = new File(HCLimitSecurityManager.getUserDataBaseDir(projectID));
	}
	
	public final File[] listFiles(final File file){
		return items.get(file);
	}
	
	public final void setListFiles(final File file, final File[] lists){
		items.put(file, lists);
	}
	
	public final long getLastModifyMS(final File file){
		return lastModiMS.get(file);
	}
	
	public final void setLastModifyMS(final File file, final long ms){
		lastModiMS.put(file, ms);
	}
}
