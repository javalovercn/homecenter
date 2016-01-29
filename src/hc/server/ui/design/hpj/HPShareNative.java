package hc.server.ui.design.hpj;

import java.io.File;

public class HPShareNative extends HCShareFileResource {
	public HPShareNative(int type, String name) {
		super(type, name);
	}

	public HPShareNative(int type, String name, File file) throws Throwable{
		super(type, name, file);
	}

	public String toString(){
		return name + ", version:" + ver;
	}
}
