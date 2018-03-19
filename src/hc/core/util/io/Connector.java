package hc.core.util.io;

import hc.core.ContextManager;
import hc.core.util.ByteUtil;

import java.io.InputStream;
import java.io.OutputStream;

public class Connector {
	final InputStream is;
	final IOBuffer buffer;
	final OutputStream os;

	public Connector(final InputStream is, final OutputStream os) {
		this.is = is;
		this.os = os;
		buffer = new IOBuffer(1024);

		ContextManager.getThreadPool().run(new Runnable() {
			public void run() {
				final int maxBlock = 1024 * 4;
				final byte[] cache = ByteUtil.byteArrayCacher.getFree(2048);
				try {
					while (true) {
						final int data = is.read();
						if (data == -1) {
							break;
						} else {
							buffer.write(data);
							int readLen;
							int bufferTotal = 0;
							do {
								readLen = is.read(cache);
								if (readLen > 0) {
									bufferTotal = buffer.write(cache, 0,
											readLen);
								}
							} while (readLen > 0 && bufferTotal < maxBlock);
							os.write(buffer.buffer, 0, bufferTotal);
							buffer.storeIdx = 0;
							os.flush();
						}
					}
				} catch (final Exception e) {
				} finally {
					try {
						os.close();
					} catch (final Exception e) {
					}
					try {
						is.close();
					} catch (final Exception e) {
					}

					ByteUtil.byteArrayCacher.cycle(cache);
					buffer.recycle();
				}
			}
		});
	}

}
