package hc.server.ui.design.hpj;


import java.io.File;
import java.io.FileInputStream;

public class HCShareFileResource extends HPNode {
	public HCShareFileResource(int type, String name) {
		super(type, name);
		this.ID = name;
	}
	
	public HCShareFileResource(int type, String name, File file) throws Throwable{
		this(type, name);
		this.ID = String.valueOf(MenuManager.getNextNodeIdx());
		loadContent(file);
	}

	String ID = "";
	public byte[] content = null;
	String ver = "0.0.1";
	
	public void loadContent(File file) throws Throwable{
		try {
			content = HCjar.readFromInputStream(new FileInputStream(file));
		} catch (Exception e) {
		}
	}
}