package hc.server.ui.design.hpj;

import hc.server.util.ListAction;

public class SurroundListAction extends ListAction {
	public static final String BLOCK_BEGIN = "BEGIN";
	public static final String BLOCK_END = "END";

	final String extendInfo;

	public SurroundListAction(final String name) {
		this(name, null);
	}

	public SurroundListAction(final String name, final String extendInfo) {
		super(name);
		this.extendInfo = extendInfo;
	}

	@Override
	public String getDisplayName() {
		return "<html>surround <STRONG>" + name + "</STRONG></html>";
	}
}
