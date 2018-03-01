package hc.util;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JTextField;

public class IntTextField extends JTextField {

	public IntTextField() {
		this(0, Integer.MAX_VALUE);
	}

	public IntTextField(final int min, final int max) {
		super();

		this.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				try {
					char keyCh = e.getKeyChar();
					if ((keyCh >= '0' && keyCh <= '9') || keyCh == '-') {
						String newValue = getText() + keyCh;
						final Integer intValue = Integer.valueOf(newValue);
						if (intValue > max) {
							e.setKeyChar('\0');
						}
					} else {
						e.setKeyChar('\0');
					}
				} catch (Throwable t) {
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
	}

}