package hc.server.ui.design.hpj;

public class HPMenu extends HPNode {
	public HPMenu(final String menuID, final int type, final String name, final int colNum, final boolean isMain) {
		super(type, name);
		this.colNum = colNum;
		this.menuID = menuID;
		this.isMainMenu = isMain;
	}

	public int colNum;
	public boolean isMainMenu = true;
	public String menuID;
}
