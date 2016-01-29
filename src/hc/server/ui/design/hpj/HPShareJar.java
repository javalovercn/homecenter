package hc.server.ui.design.hpj;

import java.io.File;

public class HPShareJar extends HCShareFileResource{
	public HPShareJar(final int type, final String name) {
		super(type, name);
	}

	public HPShareJar(final int type, final String name, final File file) throws Throwable{
		super(type, name, file);
	}

	@Override
	public String toString(){
		return name + ", version:" + ver;
	}

}