package hc.core.util;

public class ByteJointer {
	private byte[] bs;
	private int writeLen;

	public ByteJointer(final int minSize) {
		this.bs = ByteUtil.byteArrayCacher.getFree(minSize);
	}

	public final void write(final byte[] dataBS) {
		write(dataBS, 0, dataBS.length);
	}

	public final void write(final byte[] dataBS, final int offset,
			final int len) {
		final int newWriteLen = writeLen + len;
		if (bs.length < newWriteLen) {
			final byte[] newBS = ByteUtil.byteArrayCacher
					.getFree(newWriteLen * 2);
			System.arraycopy(bs, 0, newBS, 0, writeLen);
			ByteUtil.byteArrayCacher.cycle(bs);
			bs = newBS;
		}
		System.arraycopy(dataBS, offset, bs, writeLen, len);
		writeLen = newWriteLen;
	}

	public final byte[] getData() {
		return bs;
	}

	public final int getLength() {
		return writeLen;
	}

	public final void release() {
		ByteUtil.byteArrayCacher.cycle(bs);
	}
}
