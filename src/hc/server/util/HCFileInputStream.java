package hc.server.util;

import hc.server.data.StoreDirManager;
import hc.server.ui.ProjectContext;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.InputStream;

public class HCFileInputStream extends HCInputStream {
	final String fileExtension;
	String fileName;
	
	protected static String nullFileName = null;
	
	protected HCFileInputStream(final InputStream is, final String fileExtension, final String fileName) {
		super(is);
		this.fileExtension = fileExtension;
		this.fileName = fileName;
	}
	
	
	/**
	 * create random file in <code>parent</code> directory.
	 * @param parent null means create random file in <code>TEMP</code> directory.
	 * @param fileExtension null means no file extension.
	 * @return
	 */
	public final File createRandomFile(File parent, final String fileExtension){
		if(parent == null){
			parent = StoreDirManager.getTmpSubForUserManagedByHcSys(ProjectContext.getProjectContext());
		}
		return ResourceUtil.createRandomFileWithExt(parent, fileExtension==null?null:("." + fileExtension));
	}

	/**
	 * returns the file extension, for example, "png", "jpg" etc.
	 * @return
	 */
	public final String getFileExtension(){
		return fileExtension;
	}
	
	/**
	 * return the file name, for example : "screen.png".
	 * @return
	 */
	public final String getFileName(){
		if(fileName == nullFileName){
			fileName = ResourceUtil.createDateTimeSerialUUID() + "." + fileExtension;
		}
		return fileName;
	}

}
