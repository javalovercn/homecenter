package hc.server.ui.design.hpj;

import java.io.IOException;
import java.io.Writer;

import hc.core.util.LogManager;
import hc.core.util.StringUtil;

public class RubyWriter extends Writer {
	// file:/Users/homecenter/Documents/eclipse_workspace/homecenter/test_run/./jruby.jar!/jruby/java/core_ext/object.rb:73
	// warning: already initialized constant String
	private static final char[] warning = " warning: ".toCharArray();
	private static final char[] script = "<script>:".toCharArray();

	private static final int SIZE = 1024 * 1;
	char[] bs = new char[SIZE];
	int writeIdx = 0;
	final ConsoleWriter displayWriter;

	public RubyWriter(final ConsoleWriter displayWriter) {
		this.displayWriter = displayWriter;// Test JRuby console
	}

	/**
	 * <script>:10 warning: ambiguous Java methods found, using
	 * setListData(java.lang.Object[]) ==> <script>: 9 warning: ambiguous Java
	 * methods found, using setListData(java.lang.Object[])
	 * 
	 * @param cbuf
	 * @param off
	 * @param len
	 * @param warnIdx
	 */
	private final void replaceWarningLineNo(final char[] cbuf, final int off, final int len,
			final int warnIdx) {
		final int scriptLen = script.length;
		if (StringUtil.indexOf(cbuf, off, len, script, 0, scriptLen, 0) == 0) {
			final int startIdx = warnIdx - 1;

			for (int i = startIdx; i >= scriptLen; i--) {
				final char c = cbuf[i];
				if (c >= '0' && c <= '9') {
				} else {
					return;
				}
			}

			for (int i = startIdx; i >= scriptLen; i--) {
				final char c = cbuf[i];
				if (c == '0') {
					cbuf[i] = '9';
				} else {
					if (i == scriptLen) {
						cbuf[i] = ' ';
					} else {
						cbuf[i]--;
					}
				}
			}
		}
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		final int warnIdx = StringUtil.indexOf(cbuf, off, len, warning, 0, warning.length, 0);
		if (warnIdx >= 0) {
			replaceWarningLineNo(cbuf, off, len, warnIdx);
			writeImpl(cbuf, off, len);
			return;
		}

		if (writeIdx + len >= SIZE) {
			final char[] newbs = new char[bs.length * 2];
			System.arraycopy(bs, 0, newbs, 0, bs.length);
			bs = newbs;
		}
		System.arraycopy(cbuf, off, bs, writeIdx, len);
		writeIdx += len;

		writeImpl(cbuf, off, len);
	}

	private final void writeImpl(final char[] cbuf, final int off, final int len)
			throws IOException {
		if (displayWriter == null) {
			LogManager.warning(new String(cbuf, off, len));
		} else {
			displayWriter.writeError(cbuf, off, len);
		}
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

	public void reset() {
		writeIdx = 0;
	}

	public String getMessage() {
		if (writeIdx == 0) {
			return "";
		} else {
			return new String(bs, 0, writeIdx);
		}
	}
}