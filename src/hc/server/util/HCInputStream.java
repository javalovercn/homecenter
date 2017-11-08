package hc.server.util;

import hc.core.util.ByteUtil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class HCInputStream extends DataInputStream {
	HCInputStream(final InputStream is){
		super(is);
	}
	
	/**
	 * true means successful save stream to file.
	 * @param file
	 * @return
	 */
	public final boolean saveTo(final File file){
		FileOutputStream fos = null;
		final byte[] bytes = ByteUtil.byteArrayCacher.getFree(1024);
		try{
			final File parent = file.getParentFile();
			if(parent != null){
				parent.mkdirs();
			}
			fos = new FileOutputStream(file);
			int read = 0;

			while ((read = read(bytes)) != -1) {
				fos.write(bytes, 0, read);
			}
			fos.flush();
			return true;
		}catch (final Exception e) {
			e.printStackTrace();
		}finally{
			try{
				fos.close();
			}catch (final Exception e) {
			}
			ByteUtil.byteArrayCacher.cycle(bytes);
		}
		return false;
	}
}
