package hc.server.ui.design.hpj;

import hc.core.util.CCoreUtil;
import hc.util.ResourceUtil;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

public class ConsoleTextPane {
	ScriptEditPanel sep;
	private int lastOffset;

	private static final SimpleAttributeSet ERR_LIGHTER = ResourceUtil.buildAttrSet(Color.RED,
			false);

	final JTextPane textPane = new JTextPane();
	private final Document textPaneDoc = textPane.getDocument();

	public ConsoleTextPane() {
		try {
			((DefaultCaret) textPane.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);// 自动文尾
		} catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	public final void setScriptEditPanel(final ScriptEditPanel sep) {
		this.sep = sep;
	}

	public final void clearText() {
		synchronized (ERR_LIGHTER) {
			try {
				textPaneDoc.remove(0, textPaneDoc.getLength());
				lastOffset = 0;
			} catch (final BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	final void write(final char[] cbuf, final int off, final int len, final boolean isError) {
		final String str = new String(cbuf, off, len);
		synchronized (ERR_LIGHTER) {
			try {
				textPaneDoc.insertString(lastOffset, str, isError ? ERR_LIGHTER : null);
				lastOffset += str.length();
			} catch (final BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	public final void clearScriptEditPanel() {
		CCoreUtil.checkAccess();
		sep = null;
	}

	public final JTextPane getTextPane() {
		CCoreUtil.checkAccess();
		return textPane;
	}
}
