package hc.server.ui.design.hpj;

import java.io.IOException;
import java.io.Writer;

public class ConsoleWriter extends Writer {
	private ConsoleTextPane ctp;

	public ConsoleWriter(final ConsoleTextPane ctp) {
		this.ctp = ctp;
	}

	public final void setCTP(final ConsoleTextPane newCTP) {
		this.ctp = newCTP;
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		writeImpl(cbuf, off, len, false);
	}

	private final void writeImpl(final char[] cbuf, final int off, final int len,
			final boolean isError) {
		final ScriptEditPanel sep = ctp.sep;
		if (sep != null) {
			sep.showConsole(true);
		}

		ctp.write(cbuf, off, len, isError);
	}

	public final void writeError(final char[] cbuf, final int off, final int len)
			throws IOException {
		writeImpl(cbuf, off, len, true);
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

}
