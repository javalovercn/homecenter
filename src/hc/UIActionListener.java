package hc;

import java.awt.Window;

import javax.swing.JButton;

public interface UIActionListener {
	public void actionPerformed(Window window, JButton ok, JButton cancel);
}
