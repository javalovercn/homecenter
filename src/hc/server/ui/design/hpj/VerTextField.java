package hc.server.ui.design.hpj;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFormattedTextField;

public class VerTextField extends JFormattedTextField {
	public VerTextField() {
		super();// new NumberFormatter(NumberFormat.getInstance())
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				char keyCh = e.getKeyChar();
				if ((keyCh >= '0' && keyCh <= '9') || keyCh == '.') {
				} else {
					e.setKeyChar('\0');
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