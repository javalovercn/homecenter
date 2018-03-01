package hc.util;

import java.io.ByteArrayInputStream;

/**
 * 可复用的ByteArrayInputStream
 *
 */
public class ReusableByteArrayInputStream extends ByteArrayInputStream {
	public ReusableByteArrayInputStream(byte buf[], int offset, int length) {
		super(buf, offset, length);
	}

	public void reset(byte buf[], int offset, int length) {
		super.buf = buf;
		super.pos = offset;
		super.count = Math.min(offset + length, buf.length);
		super.mark = offset;
	}

}
