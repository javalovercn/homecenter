package hc.server.ui;

import hc.server.ui.design.J2SESession;

import java.util.Vector;


public abstract class MUIView {
	public abstract void transMenuWithCache(final J2SESession coreSS);
	
	public abstract String buildJcip(final J2SESession coreSS, final Vector<MenuItem> menuItems);
	
	public MUIView() {
		super();
	}

	public void appendBool(final StringBuilder sb, final boolean b, final boolean withDouhao) {
		sb.append('\'');
		sb.append(b);
		sb.append('\'');
		if(withDouhao){
			sb.append(',');
		}
	}

	public void appendInt(final StringBuilder sb, final int i, final boolean withDouhao) {
		sb.append('\'');
		sb.append(i);
		sb.append('\'');
		if(withDouhao){
			sb.append(',');
		}
	}

	public void appendString(final StringBuilder sb, final String title, final boolean withDouhao) {
		JcipManager.appendStringItem(sb, title);
		if(withDouhao){
			sb.append(',');
		}
	}

}