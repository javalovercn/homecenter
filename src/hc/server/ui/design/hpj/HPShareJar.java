package hc.server.ui.design.hpj;

import java.io.File;

public class HPShareJar extends HCShareFileResource{
	public HPShareJar(int type, String name) {
		super(type, name);
	}

	public HPShareJar(int type, String name, File file) throws Throwable{
		super(type, name, file);
	}

	public String toString(){
		return name + ", version:" + ver;
	}

}