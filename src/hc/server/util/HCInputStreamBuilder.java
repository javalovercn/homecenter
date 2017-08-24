package hc.server.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HCInputStreamBuilder {
	public static HCInputStream build(final byte[] bs, final String type){
		return new HCInputStream() {
			final ByteArrayInputStream bis = new ByteArrayInputStream(bs);
			
			@Override
			public String getFileType() {
				return type;
			}
			
			@Override
			public int read() throws IOException {
				return bis.read();
			}
			
		    @Override
			public int read(final byte b[]) throws IOException {
		        return bis.read(b);
		    }

		    @Override
			public int read(final byte b[], final int off, final int len) throws IOException {
		    	return bis.read(bs, off, len);
		    }

		    @Override
			public long skip(final long n) throws IOException {
		    	return bis.skip(n);
		    }

		    @Override
			public int available() throws IOException {
		        return bis.available();
		    }

		    @Override
			public void close() throws IOException {
		    	bis.close();
		    }

		    @Override
			public synchronized void mark(final int readlimit) {
		    	bis.mark(readlimit);
		    }

		    @Override
			public synchronized void reset() throws IOException {
		        bis.reset();
		    }

		    @Override
			public boolean markSupported() {
		        return bis.markSupported();
		    }
		};
	}
}
