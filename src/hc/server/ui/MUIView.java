package hc.server.ui;


public abstract class MUIView {
	public abstract String buildJcip();
	
	public MUIView() {
		super();
	}

	public void appendBool(StringBuilder sb, boolean b, boolean withDouhao) {
		sb.append('\'');
		sb.append(b);
		sb.append('\'');
		if(withDouhao){
			sb.append(',');
		}
	}

	public void appendInt(StringBuilder sb, int i, boolean withDouhao) {
		sb.append('\'');
		sb.append(i);
		sb.append('\'');
		if(withDouhao){
			sb.append(',');
		}
	}

	public void appendString(final StringBuilder sb, String title, boolean withDouhao) {
		JcipManager.appendStringItem(sb, title);
		if(withDouhao){
			sb.append(',');
		}
	}

}