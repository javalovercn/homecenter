package hc.server.ui;

import javax.swing.JComponent;

public class StyleItem {
	public static final int FOR_DIV = 1;
	public static final int FOR_JCOMPONENT = 2;
	public static final int FOR_JTOGGLEBUTTON = 3;
	
	public JComponent component;
	public String className;
	public String styles;
	public int forType;
	
	public StyleItem(int forType, JComponent component, String className, String styles){
		this.forType = forType;
		
		this.component = component;
		this.className = className;
		this.styles = styles;
	}
}
