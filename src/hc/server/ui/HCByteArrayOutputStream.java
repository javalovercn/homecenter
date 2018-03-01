package hc.server.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class HCByteArrayOutputStream extends OutputStream {
	public byte buf[];

	private int offset;
	protected int count;

	public HCByteArrayOutputStream() {
		this(32);
	}

	public void reset(final byte[] bs, final int offset) {
		buf = bs;
		this.offset = offset;
		count = offset;
	}

	public HCByteArrayOutputStream(final int size) {
		if (size < 0) {
			throw new IllegalArgumentException("Negative initial size: " + size);
		}
		buf = new byte[size];
	}

	@Override
	public synchronized void write(final int b) {
		final int newcount = count + 1;
		if (newcount > buf.length) {
			final byte[] nbuf = new byte[buf.length * 2];
			System.arraycopy(buf, offset, nbuf, offset, count - offset);
			buf = nbuf;
		}
		buf[count] = (byte) b;
		count = newcount;
	}

	@Override
	public synchronized void write(final byte b[], final int off, final int len) {
		if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
				|| ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		final int newcount = count + len;
		if (newcount > buf.length) {
			final byte[] nbuf = new byte[buf.length * 2];
			System.arraycopy(buf, 0, nbuf, 0, buf.length);
			buf = nbuf;
		}
		System.arraycopy(b, off, buf, count, len);
		count = newcount;
	}

	public synchronized void writeTo(final OutputStream out) throws IOException {
		out.write(buf, offset, count - offset);
	}

	public synchronized void reset() {
		// 重要：不能使用count = offset = 0;参见byteArrayOutputStream.reset();其offset为非0
		count = offset;
	}

	/**
	 * Creates a newly allocated byte array. Its size is the current size of
	 * this output stream and the valid contents of the buffer have been copied
	 * into it.
	 * 
	 * @return the current contents of this output stream, as a byte array.
	 * @see java.io.ByteArrayOutputStream#size()
	 */
	// public synchronized byte toByteArray()[] {
	// return Arrays.copyOf(buf, count);
	// }

	public synchronized int size() {
		return count - offset;
	}

	@Override
	public synchronized String toString() {
		return new String(buf, offset, size());
	}

	public synchronized String toString(final String charsetName)
			throws UnsupportedEncodingException {
		return new String(buf, offset, size(), charsetName);
	}

	// public synchronized String toString(int hibyte) {
	// return new String(buf, hibyte, 0, count);
	// }

	@Override
	public void close() throws IOException {
	}

}
