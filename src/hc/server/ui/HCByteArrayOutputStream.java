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

	public void reset(byte[] bs, int offset) {
		buf = bs;
		this.offset = offset;
		count = offset;
	}

	public HCByteArrayOutputStream(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("Negative initial size: " + size);
		}
		buf = new byte[size];
	}

	public synchronized void write(int b) {
		int newcount = count + 1;
		if (newcount > buf.length) {
			byte[] nbuf = new byte[buf.length * 2];
			System.arraycopy(buf, offset, nbuf, offset, count - offset);
			buf = nbuf;
		}
		buf[count] = (byte) b;
		count = newcount;
	}

	public synchronized void write(byte b[], int off, int len) {
		if ((off < 0) || (off > b.length) || (len < 0)
				|| ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		int newcount = count + len;
		if (newcount > buf.length) {
			byte[] nbuf = new byte[buf.length * 2];
			System.arraycopy(buf, offset, nbuf, offset, count - offset);
			buf = nbuf;
		}
		System.arraycopy(b, off, buf, count, len);
		count = newcount;
	}

	public synchronized void writeTo(OutputStream out) throws IOException {
		out.write(buf, offset, count - offset);
	}

	public synchronized void reset() {
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

	public synchronized String toString() {
		return new String(buf, 0, count);
	}

	public synchronized String toString(String charsetName)
			throws UnsupportedEncodingException {
		return new String(buf, 0, count, charsetName);
	}

	// public synchronized String toString(int hibyte) {
	// return new String(buf, hibyte, 0, count);
	// }

	public void close() throws IOException {
	}

}
