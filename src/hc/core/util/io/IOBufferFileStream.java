package hc.core.util.io;

import java.io.InputStream;

public class IOBufferFileStream {
	public final int size;
	public final InputStream is;
	
	public IOBufferFileStream(final int size, final InputStream is){
		this.size = size;
		this.is = is;
	}
}
