package hc.server.util;

import java.io.File;
import java.io.InputStream;

import hc.server.ui.ProjectContext;
import hc.util.ResourceUtil;

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
	 * create random file in <code>TEMP</code> directory (managed by server and will be empty at
	 * next startup).
	 * 
	 * @param fileExtension
	 *            null means no file extension.
	 * @return
	 * @see #createRandomFile(File, String)
	 * @deprecated
	 */
	@Deprecated
	public final File createRandomFile(final String fileExtension) {
		return createRandomFile(null, fileExtension);
	}

	/**
	 * create random file in <code>parent</code> directory.
	 * 
	 * @param parent
	 *            null means create random file in <code>TEMP</code> directory (managed by server
	 *            and will be empty at next startup).
	 * @param fileExtension
	 *            null means no file extension.
	 * @return
	 * @see #createRandomFile(String)
	 * @deprecated
	 */
	@Deprecated
	public final File createRandomFile(final File parent, final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(ProjectContext.getProjectContext(), parent, fileExtension);
	}

	/**
	 * create temporary file in <code>parent</code> directory. <BR>
	 * <BR>
	 * it is equals with {@link #getTempFile(File, String)}.
	 * 
	 * @param parent
	 *            null means create random file in <code>TEMP</code> directory (managed by server
	 *            and will be empty at next startup).
	 * @param fileExtension
	 *            null means no file extension.
	 * @return
	 */
	public final File createTempFile(final File parent, final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(ProjectContext.getProjectContext(), parent, fileExtension);
	}

	/**
	 * create temporary file in <code>TEMP</code> directory (managed by server and will be empty at
	 * next startup). <BR>
	 * <BR>
	 * it is equals with {@link #getTempFile(String)}.
	 * 
	 * @param fileExtension
	 *            null means no file extension.
	 * @return
	 */
	public final File createTempFile(final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(ProjectContext.getProjectContext(), null, fileExtension);
	}

	/**
	 * get temporary file in <code>parent</code> directory. <BR>
	 * <BR>
	 * it is equals with {@link #createTempFile(File, String)}.
	 * 
	 * @param parent
	 *            null means create random file in <code>TEMP</code> directory (managed by server
	 *            and will be empty at next startup).
	 * @param fileExtension
	 *            null means no file extension.
	 * @return
	 */
	public final File getTempFile(final File parent, final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(ProjectContext.getProjectContext(), parent, fileExtension);
	}

	/**
	 * get temporary file in <code>TEMP</code> directory (managed by server and will be empty at
	 * next startup). <BR>
	 * <BR>
	 * it is equals with {@link #createTempFile(String)}.
	 * 
	 * @param fileExtension
	 *            null means no file extension.
	 * @return
	 */
	public final File getTempFile(final String fileExtension) {
		return ResourceUtil.createTempFileForHAR(ProjectContext.getProjectContext(), null, fileExtension);
	}

	/**
	 * returns the file extension, for example, "png", "jpg" etc.
	 * 
	 * @return
	 */
	public final String getFileExtension() {
		return fileExtension;
	}

	/**
	 * return the file name, for example : "screen.png".
	 * 
	 * @return
	 */
	public final String getFileName() {
		if (fileName == nullFileName) {
			fileName = ResourceUtil.createDateTimeSerialUUID() + "." + fileExtension;
		}
		return fileName;
	}

}
