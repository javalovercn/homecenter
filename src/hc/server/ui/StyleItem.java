package hc.server.ui;

import javax.swing.JComponent;

public class StyleItem extends TodoItem {
	public String className;
	public String styles;

	public StyleItem(final int forType, final JComponent component, final String className,
			final String styles) {
		super(forType, component);

		this.className = className;
		this.styles = styles;
	}
}
