package hc.server.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFormattedTextField;

public class NumberFormatTextField extends JFormattedTextField {
	final String valueWhenEmpty;

	public NumberFormatTextField(final int valueWhenEmpty) {
		this(String.valueOf(valueWhenEmpty));
	}

	public NumberFormatTextField(final String valueWhenEmpty) {
		this(false, valueWhenEmpty);
	}

	public NumberFormatTextField(final boolean enableNegative, final String valueWhenEmpty) {
		super();// new NumberFormatter(NumberFormat.getInstance())
		this.valueWhenEmpty = valueWhenEmpty;

		this.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
				final char keyCh = e.getKeyChar();
				if ((enableNegative && keyCh == '-') || ((keyCh >= '0') && (keyCh <= '9'))) {
				} else {
					e.setKeyChar('\0');
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
			}

			@Override
			public void keyPressed(final KeyEvent e) {
			}
		});
	}

	@Override
	public void setText(String t) {
		if (t == null || t.length() == 0) {
			t = valueWhenEmpty;
		}
		super.setText(t);
	}

	@Override
	public String getText() {
		String out = super.getText();
		if (out == null || out.length() == 0) {
			out = valueWhenEmpty;
		}
		return out;
	}

}