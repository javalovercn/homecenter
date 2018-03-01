package hc.server.ui;

import javax.swing.JComponent;

public class TodoItem {
	public static final int FOR_DIV = 1;
	public static final int FOR_JCOMPONENT = 2;
	public static final int FOR_JTOGGLEBUTTON = 3;
	public static final int FOR_RTL = 4;

	public JComponent component;
	public int forType;

	public TodoItem(final int forType, final JComponent component) {
		this.forType = forType;
		this.component = component;
	}
}
