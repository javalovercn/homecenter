package hc.util;

import hc.core.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public class IPv4Field extends JTextField {
	static final String DELETE = "Delete";
	static final String RIGHT = "Right";
	static final String BACK_SPACE = "BackSpace";
	static final String LEFT = "Left";
	static final String ENTER = "Enter";

	private JLabel[] dots;
	private JTextField[] fields;

	@Override
	public void addKeyListener(final KeyListener listener) {
		for (int i = 0; i < fields.length; i++) {
			fields[i].addKeyListener(listener);
		}
	}

	public IPv4Field(final String ipv4Address) {
		init();
		setAddress(ipv4Address);
	}

	@Override
	public void setText(final String text) {
		setAddress(text);
	}

	@Override
	public String getText() {
		return getAddress();
	}

	private final void init() {
		setPrefersize();

		setFocusable(false);
		setLayout(new GridLayout(1, 4, 0, 0));

		this.fields = new JTextField[4];
		this.dots = new JLabel[3];
		final JPanel[] fieldPanes = new JPanel[4];

		final KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) {
				final JTextComponent field = (JTextComponent) e.getComponent();
				final char keyChar = e.getKeyChar();
				final String text = field.getText();
				final String selText = field.getSelectedText();
				if (("0123456789".indexOf(keyChar) >= 0)) {
					if (selText == null) {
						final String afterInputStr = text + keyChar;
						final int ipInt = (text.length() == 0 ? 0
								: Integer.parseInt(afterInputStr));

						if (ipInt > 255) {
							e.setKeyChar('\0');
							return;
						}
					}
				} else if ((keyChar == '.')) {
					if (!text.isEmpty() && (selText == null)) {
						e.setKeyChar('\0');
						field.firePropertyChange(RIGHT, 0, 1);
						return;
					}
				} else {
					e.setKeyChar('\0');
				}
			}

			@Override
			public void keyPressed(final KeyEvent e) {
				final JTextComponent field = (JTextComponent) e.getComponent();
				final int keyCode = e.getKeyCode();
				final char keyChar = e.getKeyChar();
				final String text = field.getText();
				final boolean isNotSeleted = (field.getSelectedText() == null);
				final int caretPos = field.getCaretPosition();
				final int textLength = text.length();
				if (isNotSeleted && (keyCode == KeyEvent.VK_LEFT) && (caretPos == 0)) {
					field.firePropertyChange(LEFT, 0, 1);
				} else if (isNotSeleted && (keyCode == KeyEvent.VK_RIGHT)
						&& (caretPos == textLength)) {
					field.firePropertyChange(RIGHT, 0, 1);
				} else if (isNotSeleted && (keyCode == KeyEvent.VK_BACK_SPACE) && (caretPos == 0)) {
					field.firePropertyChange(BACK_SPACE, 0, 1);
				} else if (isNotSeleted && (keyCode == KeyEvent.VK_DELETE)
						&& (caretPos == textLength)) {
					field.firePropertyChange(DELETE, 0, 1);
				} else if ((keyCode == KeyEvent.VK_ENTER)) {
					field.firePropertyChange(ENTER, 0, 1);
				} else if (("0123456789".indexOf(keyChar) >= 0)) {
					if (isNotSeleted) {
						final String afterInputStr = text + keyChar;
						final int ipInt = (text.length() == 0 ? 0
								: Integer.parseInt(afterInputStr));

						if (ipInt > 25 || afterInputStr.length() == 3) {
							field.firePropertyChange(RIGHT, 0, 1);
						}
					}
				}
			}
		};

		for (int i = 0; i < fields.length; i++) {
			final JTextField field = new JTextField();
			field.setHorizontalAlignment(JTextField.CENTER);
			field.setBorder(BorderFactory.createEmptyBorder());
			field.setMargin(new Insets(0, 0, 0, 0));

			this.fields[i] = field;
			fieldPanes[i] = new JPanel();

			this.fields[i].addKeyListener(keyListener);
			fieldPanes[i].setOpaque(false);
			fieldPanes[i].setLayout(new BorderLayout());
			if (i != dots.length) {
				final JLabel dot = new JLabel(".");
				dot.setOpaque(false);
				dot.setBorder(BorderFactory.createEmptyBorder());
				dot.setHorizontalAlignment(JLabel.CENTER);
				dots[i] = dot;

				fieldPanes[i].add(dots[i], BorderLayout.EAST);
			}
			fieldPanes[i].add(this.fields[i], BorderLayout.CENTER);
			add(fieldPanes[i]);
		}

		for (int i = 0; i < fields.length; i++) {
			if (i == 0) {
				this.fields[i]
						.addPropertyChangeListener(new JumpListener(null, this.fields[(i + 1)]));
			} else if (i == (fields.length - 1)) {
				this.fields[i]
						.addPropertyChangeListener(new JumpListener(this.fields[(i - 1)], null));
			} else {
				this.fields[i].addPropertyChangeListener(
						new JumpListener(this.fields[(i - 1)], this.fields[(i + 1)]));
			}
		}
	}

	private final void setPrefersize() {
		final int fontSize = getFont().getSize();
		final Dimension size = new Dimension(fontSize * 12, (int) (fontSize * 1.5F));
		setPreferredSize(size);
	}

	@Override
	public void requestFocus() {
		fields[0].requestFocus();
	}

	public final String getAddress() {
		final StringBuilder ipText = StringBuilderCacher.getFree();
		for (int i = 0; i < fields.length; i++) {
			final String str = fields[i].getText();

			if (i != 0) {
				ipText.append('.');
			}
			ipText.append(str);
		}
		final String out = ipText.toString();
		StringBuilderCacher.cycle(ipText);
		return out;
	}

	@Override
	public void setEnabled(final boolean isEnable) {
		super.setEnabled(isEnable);

		for (int i = 0; i < fields.length; i++) {
			fields[i].setEnabled(isEnable);
		}

		for (int i = 0; i < dots.length; i++) {
			dots[i].setEnabled(isEnable);
		}
	}

	public final void setAddress(final String ip) {
		int index;

		if (ip != null && ip.length() > 0) {
			final String[] part = StringUtil.splitToArray(ip, ".");
			index = 0;
			for (int i = 0; i < fields.length; i++) {
				fields[i].setText(part[(index++)]);
			}
		} else {
			for (int i = 0; i < fields.length; i++) {
				fields[i].setText("");
			}
		}
	}

	public final void clear() {
		for (int i = 0; i < fields.length; i++) {
			fields[i].setText("");
		}
	}

	private class JumpListener implements PropertyChangeListener {
		private final JTextField preField;
		private final JTextField nextField;

		public JumpListener(final JTextField preField, final JTextField nextField) {
			this.preField = preField;
			this.nextField = nextField;
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {
			final String name = e.getPropertyName();

			if ((this.preField != null) && ((name == LEFT) || (name == BACK_SPACE))) {
				this.preField.requestFocus();
			} else if ((this.nextField != null)
					&& ((name == RIGHT) || (name == DELETE) || (name == ENTER))) {
				this.nextField.requestFocus();
			}
		}
	}

}