package hc.core.util.io;

import hc.core.util.ByteUtil;

import java.io.IOException;
import java.io.InputStream;

public class StreamReader {
	public byte[] bs;
	public int storeIdx;
	private final InputStream is;

	public StreamReader(final byte[] bs, final InputStream is) {
		if (bs == null) {
			this.bs = ByteUtil.byteArrayCacher.getFree(2048);
		} else {
			this.bs = bs;
		}
		this.is = is;
	}

	public final void readFull() {
		try {
			while (true) {
				final int n = is.read();
				if (n == -1) {
					break;
				} else {
					bs[storeIdx++] = (byte) n;
					final int lastLen = bs.length;
					if (storeIdx == lastLen) {
						final byte[] nextPara = ByteUtil.byteArrayCacher
								.getFree(lastLen * 2);
						System.arraycopy(bs, 0, nextPara, 0, lastLen);
						ByteUtil.byteArrayCacher.cycle(bs);
						bs = nextPara;
					}
				}
			}
		} catch (final Exception e) {
		} finally {
			try {
				is.close();
			} catch (final IOException e) {
			}
		}
	}

	public final void recycle() {
		if (bs != null) {
			ByteUtil.byteArrayCacher.cycle(bs);
		}
	}
}
